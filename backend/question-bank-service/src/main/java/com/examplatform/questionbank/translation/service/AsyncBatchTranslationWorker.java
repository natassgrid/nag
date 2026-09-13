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

/**
 * Asynchronous worker for processing bulk translation of questions without
 * overloading or killing the system.
 *
 * Employs streaming pagination, bounded concurrency (Semaphore), inter-batch
 * throttle pacing, and per-question transaction & error isolation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBatchTranslationWorker {

    private final BatchTranslationJobRepository jobRepository;
    private final QuestionRepository questionRepository;
    private final TranslationRepository translationRepository;
    private final IndicTrans2Service indicTrans2Service;
    private final TranslationWorkflowService translationWorkflowService;

    @Async
    public void processBatchTranslationJob(UUID jobId, String tenantId) {
        log.info("Starting asynchronous batch translation worker for jobId={}, tenant={}", jobId, tenantId);
        TenantContext.setTenantId(tenantId);

        Optional<BatchTranslationJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isEmpty()) {
            log.error("BatchTranslationJob {} not found", jobId);
            return;
        }

        BatchTranslationJob job = jobOpt.get();
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

                    boolean shouldTranslate = true;
                    if (!job.isOverwriteExisting()) {
                        List<Translation> existing = translationRepository.findByQuestionIdAndLanguageCodeAndTenantId(
                                question.getId(), targetLang, tenantId);
                        if (!existing.isEmpty() && (existing.get(0).getStatus() == Translation.TranslationStatus.PUBLISHED
                                || existing.get(0).getStatus() == Translation.TranslationStatus.APPROVED)) {
                            shouldTranslate = false;
                        }
                    }

                    if (shouldTranslate) {
                        try {
                            concurrencyLimiter.acquire();
                            try {
                                AutoTranslateResponse translated = indicTrans2Service.autoTranslateQuestionEntity(question, targetLang);
                                translationWorkflowService.upsertTranslation(
                                        question.getId(),
                                        targetLang,
                                        translated.getTranslatedContent(),
                                        translated.getTranslatedOptions(),
                                        translated.getTranslatedExplanation(),
                                        targetStatus,
                                        job.getInitiatedBy(),
                                        "Auto-translated asynchronously via IndicTrans2 (job: " + jobId + ")",
                                        tenantId
                                );
                                job.setSuccessfulQuestions(job.getSuccessfulQuestions() + 1);
                            } finally {
                                concurrencyLimiter.release();
                            }
                        } catch (Exception e) {
                            log.error("Failed to translate questionId={} in batchJobId={}: {}", question.getId(), jobId, e.getMessage());
                            job.setFailedQuestions(job.getFailedQuestions() + 1);
                            List<String> failedIds = job.getFailedQuestionIds() != null
                                    ? new ArrayList<>(job.getFailedQuestionIds())
                                    : new ArrayList<>();
                            failedIds.add(question.getId().toString());
                            job.setFailedQuestionIds(failedIds);
                        }
                    } else {
                        job.setSuccessfulQuestions(job.getSuccessfulQuestions() + 1);
                    }

                    job.setProcessedQuestions(job.getProcessedQuestions() + 1);
                }

                jobRepository.save(job);

                if (throttleDelayMs > 0 && page.hasNext()) {
                    try {
                        Thread.sleep(throttleDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Worker thread interrupted during throttle delay for job {}", jobId);
                        break;
                    }
                }

                pageNumber++;
            } while (page.hasNext());

            job.setStatus(BatchTranslationJobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
            log.info("Batch translation job {} completed successfully. Processed: {}, Succeeded: {}, Failed: {}",
                    jobId, job.getProcessedQuestions(), job.getSuccessfulQuestions(), job.getFailedQuestions());

        } catch (Exception fatalError) {
            log.error("Fatal error during batch translation job {}: {}", jobId, fatalError.getMessage(), fatalError);
            job.setStatus(BatchTranslationJobStatus.FAILED);
            job.setErrorMessage(fatalError.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isJobCancelled(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(j -> j.getStatus() == BatchTranslationJobStatus.CANCELLED)
                .orElse(false);
    }
}
