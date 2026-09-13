/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.questionbank.translation.service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.domain.BatchTranslationJob;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.AutoTranslateResponse;
import com.examplatform.questionbank.translation.repository.BatchTranslationJobRepository;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import com.examplatform.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Worker to process large-scale asynchronous background translation jobs.
 * Supports pagination chunks, concurrency throttling, cancellation checks, and error recovery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBatchTranslationWorker {

    private static final UUID SYSTEM_BATCH_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final BatchTranslationJobRepository jobRepository;
    private final QuestionRepository questionRepository;
    private final TranslationRepository translationRepository;
    private final IndicTrans2Service indicTrans2Service;
    private final TranslationWorkflowService translationWorkflowService;

    @Async
    public void processBatchTranslationJob(UUID jobId, String tenantId) {
        log.info("Starting asynchronous batch translation worker for jobId={}, tenant={}", jobId, tenantId);
        TenantContext.setTenantId(tenantId);

        // Resilient retry loop to guarantee visibility across transaction boundaries / connection pools
        Optional<BatchTranslationJob> jobOpt = Optional.empty();
        for (int attempt = 0; attempt < 5; attempt++) {
            jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isPresent()) {
                break;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (jobOpt.isEmpty()) {
            log.error("BatchTranslationJob {} not found after retries", jobId);
            return;
        }

        BatchTranslationJob job = jobOpt.get();
        if (job.getStatus() == BatchTranslationJobStatus.CANCELLED) {
            log.info("Batch translation job {} is already CANCELLED. Exiting.", jobId);
            return;
        }

        job.setStatus(BatchTranslationJobStatus.IN_PROGRESS);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        String targetLang = job.getTargetLanguage();
        String targetStatusStr = job.getTargetStatus();
        Translation.TranslationStatus targetStatus = "APPROVED".equalsIgnoreCase(targetStatusStr)
                ? Translation.TranslationStatus.APPROVED
                : Translation.TranslationStatus.PUBLISHED;

        int batchSize = Math.max(1, job.getBatchSize());
        int throttleDelayMs = Math.max(0, job.getThrottleDelayMs());
        int maxConcurrency = Math.max(1, job.getMaxConcurrency());
        Semaphore concurrencyLimiter = new Semaphore(maxConcurrency);
        boolean overwriteExisting = job.isOverwriteExisting();

        try {
            int pageNumber = 0;
            Page<Question> page;

            do {
                // Check if job was cancelled
                Optional<BatchTranslationJob> currentJobOpt = jobRepository.findById(jobId);
                if (currentJobOpt.isPresent() && currentJobOpt.get().getStatus() == BatchTranslationJobStatus.CANCELLED) {
                    log.info("Batch translation job {} was cancelled by user. Terminating worker loop.", jobId);
                    return;
                }

                PageRequest pageRequest = PageRequest.of(pageNumber, batchSize, Sort.by("createdAt").ascending());
                if (job.getSubjectFilter() != null && !job.getSubjectFilter().isBlank()) {
                    page = questionRepository.findBySubjectAndTenantId(job.getSubjectFilter(), tenantId, pageRequest);
                } else {
                    page = questionRepository.findByTenantId(tenantId, pageRequest);
                }

                if (pageNumber == 0) {
                    job.setTotalQuestions((int) page.getTotalElements());
                    jobRepository.save(job);
                }

                List<Question> questions = page.getContent();
                for (Question question : questions) {
                    // Check cancellation periodically
                    if (isJobCancelled(jobId)) {
                        log.info("Job {} cancelled mid-batch. Halting execution.", jobId);
                        return;
                    }

                    try {
                        concurrencyLimiter.acquire();
                        processSingleQuestion(question, targetLang, targetStatus, overwriteExisting, tenantId, job);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Batch worker interrupted while acquiring semaphore for job {}", jobId);
                        return;
                    } finally {
                        concurrencyLimiter.release();
                    }

                    if (throttleDelayMs > 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(throttleDelayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Throttle sleep interrupted for job {}", jobId);
                            return;
                        }
                    }
                }

                pageNumber++;
            } while (page.hasNext());

            // Mark job as completed
            Optional<BatchTranslationJob> finalJobOpt = jobRepository.findById(jobId);
            if (finalJobOpt.isPresent()) {
                BatchTranslationJob finalJob = finalJobOpt.get();
                if (finalJob.getStatus() != BatchTranslationJobStatus.CANCELLED) {
                    finalJob.setStatus(BatchTranslationJobStatus.COMPLETED);
                    finalJob.setCompletedAt(Instant.now());
                    jobRepository.save(finalJob);
                    log.info("Batch translation job {} finished successfully. Total processed={}", jobId, finalJob.getProcessedQuestions());
                }
            }

        } catch (Exception ex) {
            log.error("Fatal error during batch translation execution for job {}: {}", jobId, ex.getMessage(), ex);
            Optional<BatchTranslationJob> failedJobOpt = jobRepository.findById(jobId);
            if (failedJobOpt.isPresent()) {
                BatchTranslationJob failedJob = failedJobOpt.get();
                failedJob.setStatus(BatchTranslationJobStatus.FAILED);
                failedJob.setErrorMessage(ex.getMessage());
                failedJob.setCompletedAt(Instant.now());
                jobRepository.save(failedJob);
            }
        } finally {
            TenantContext.clear();
        }
    }

    private void processSingleQuestion(
            Question question,
            String targetLang,
            Translation.TranslationStatus targetStatus,
            boolean overwriteExisting,
            String tenantId,
            BatchTranslationJob job) {

        UUID questionId = question.getId();
        try {
            // Translate question via AI model
            AutoTranslateResponse translationResponse = indicTrans2Service.autoTranslateQuestionEntity(question, targetLang);

            // Upsert into translation domain
            translationWorkflowService.upsertTranslation(
                    questionId,
                    targetLang,
                    translationResponse.getTranslatedContent(),
                    translationResponse.getTranslatedOptions(),
                    translationResponse.getTranslatedExplanation(),
                    targetStatus,
                    SYSTEM_BATCH_UUID,
                    "Batch AI Auto-Translate (Hindi)",
                    tenantId
            );

            // Update in-memory job counts & persist periodically
            synchronized (job) {
                job.setProcessedQuestions(job.getProcessedQuestions() + 1);
                job.setSuccessfulQuestions(job.getSuccessfulQuestions() + 1);
                jobRepository.save(job);
            }

        } catch (Exception ex) {
            log.error("Failed to translate questionId={} in batch {}: {}", questionId, job.getId(), ex.getMessage());
            synchronized (job) {
                job.setProcessedQuestions(job.getProcessedQuestions() + 1);
                job.setFailedQuestions(job.getFailedQuestions() + 1);
                if (job.getFailedQuestionIds() == null) {
                    job.setFailedQuestionIds(new ArrayList<>());
                }
                job.getFailedQuestionIds().add(questionId.toString());
                jobRepository.save(job);
            }
        }
    }

    private boolean isJobCancelled(UUID jobId) {
        Optional<BatchTranslationJob> current = jobRepository.findById(jobId);
        return current.isPresent() && current.get().getStatus() == BatchTranslationJobStatus.CANCELLED;
    }
}
