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
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Worker to process large-scale asynchronous background translation jobs.
 * Supports whole-bank, subject-filtered, or paper-scoped batch translations with:
 * - Direct question ID batch chunking (for Paper Generation translations)
 * - Pagination streaming (for full-bank or subject-filtered translations)
 * - Concurrency throttling via Semaphore
 * - Overwrite toggle support (clean upsert vs. reuse existing)
 * - Target status assignment (PUBLISHED default)
 * - Cancellation checks and error recovery
 * - Configurable development pause intervals
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

    @Value("${translation.batch.chunk-pause-interval-questions:15}")
    private int chunkPauseIntervalQuestions = 15;

    @Value("${translation.batch.chunk-pause-duration-seconds:60}")
    private int chunkPauseDurationSeconds = 60;

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

        List<UUID> explicitQuestionIds = job.getQuestionIds();
        boolean hasExplicitQuestions = (explicitQuestionIds != null && !explicitQuestionIds.isEmpty());

        try {
            int questionsSinceLastPause = 0;

            if (hasExplicitQuestions) {
                // Paper generation batch: translate exact list of questions for the paper
                int total = explicitQuestionIds.size();
                Optional<BatchTranslationJob> freshJobOpt = jobRepository.findById(jobId);
                if (freshJobOpt.isPresent()) {
                    BatchTranslationJob freshJob = freshJobOpt.get();
                    freshJob.setTotalQuestions(total);
                    jobRepository.save(freshJob);
                    log.info("Batch job {} for paper {} matched explicit question count={}", jobId, job.getPaperId(), total);
                }

                for (int fromIndex = 0; fromIndex < total; fromIndex += batchSize) {
                    if (isJobCancelled(jobId)) {
                        log.info("Job {} cancelled mid-batch. Halting execution.", jobId);
                        return;
                    }

                    int toIndex = Math.min(fromIndex + batchSize, total);
                    List<UUID> chunkIds = explicitQuestionIds.subList(fromIndex, toIndex);
                    List<Question> questions = questionRepository.findQuestionsByIdsIn(chunkIds, tenantId);

                    Map<UUID, Question> questionMap = questions.stream()
                            .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

                    for (UUID qId : chunkIds) {
                        if (isJobCancelled(jobId)) {
                            log.info("Job {} cancelled mid-batch. Halting execution.", jobId);
                            return;
                        }

                        Question question = questionMap.get(qId);
                        if (question == null) {
                            log.warn("Question ID {} from paper list not found in repository. Recording failure.", qId);
                            jobRepository.incrementFailure(jobId);
                            continue;
                        }

                        try {
                            concurrencyLimiter.acquire();
                            processSingleQuestion(question, targetLang, targetStatus, overwriteExisting, tenantId, jobId);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Batch worker interrupted while acquiring semaphore for job {}", jobId);
                            return;
                        } finally {
                            concurrencyLimiter.release();
                        }

                        questionsSinceLastPause++;

                        if (throttleDelayMs > 0) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(throttleDelayMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                log.warn("Throttle sleep interrupted for job {}", jobId);
                                return;
                            }
                        }

                        // Check if chunk pause interval reached in dev environment
                        boolean hasMore = (toIndex < total);
                        if (hasMore && chunkPauseIntervalQuestions > 0 && chunkPauseDurationSeconds > 0
                                && questionsSinceLastPause >= chunkPauseIntervalQuestions) {
                            log.info("Batch translation job {}: Reached chunk interval of {} questions. Pausing for {}s...",
                                    jobId, questionsSinceLastPause, chunkPauseDurationSeconds);
                            questionsSinceLastPause = 0;

                            for (int s = 0; s < chunkPauseDurationSeconds; s++) {
                                if (isJobCancelled(jobId)) {
                                    log.info("Job {} cancelled during chunk interval pause. Halting execution.", jobId);
                                    return;
                                }
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        }
                    }
                }
            } else {
                // Paginate by subject filter or all questions using robust Specification
                int pageNumber = 0;
                Page<Question> page;
                Specification<Question> spec = buildBatchSpecification(job.getSubjectFilter(), tenantId);

                do {
                    if (isJobCancelled(jobId)) {
                        log.info("Batch translation job {} was cancelled by user. Terminating worker loop.", jobId);
                        return;
                    }

                    PageRequest pageRequest = PageRequest.of(pageNumber, batchSize, Sort.by("createdAt").ascending());
                    page = questionRepository.findAll(spec, pageRequest);

                    if (pageNumber == 0) {
                        Optional<BatchTranslationJob> freshJobOpt = jobRepository.findById(jobId);
                        if (freshJobOpt.isPresent()) {
                            BatchTranslationJob freshJob = freshJobOpt.get();
                            freshJob.setTotalQuestions((int) page.getTotalElements());
                            jobRepository.save(freshJob);
                            log.info("Batch job {} matched totalQuestions={}", jobId, page.getTotalElements());
                        }
                    }

                    List<Question> questions = page.getContent();
                    for (int i = 0; i < questions.size(); i++) {
                        Question question = questions.get(i);

                        if (isJobCancelled(jobId)) {
                            log.info("Job {} cancelled mid-batch. Halting execution.", jobId);
                            return;
                        }

                        try {
                            concurrencyLimiter.acquire();
                            processSingleQuestion(question, targetLang, targetStatus, overwriteExisting, tenantId, jobId);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Batch worker interrupted while acquiring semaphore for job {}", jobId);
                            return;
                        } finally {
                            concurrencyLimiter.release();
                        }

                        questionsSinceLastPause++;

                        if (throttleDelayMs > 0) {
                            try {
                                TimeUnit.MILLISECONDS.sleep(throttleDelayMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                log.warn("Throttle sleep interrupted for job {}", jobId);
                                return;
                            }
                        }

                        boolean hasMoreQuestions = (i < questions.size() - 1) || page.hasNext();
                        if (hasMoreQuestions && chunkPauseIntervalQuestions > 0 && chunkPauseDurationSeconds > 0
                                && questionsSinceLastPause >= chunkPauseIntervalQuestions) {
                            log.info("Batch translation job {}: Reached chunk interval of {} questions. Pausing for {}s to prevent local dev model overload...",
                                    jobId, questionsSinceLastPause, chunkPauseDurationSeconds);
                            questionsSinceLastPause = 0;

                            for (int s = 0; s < chunkPauseDurationSeconds; s++) {
                                if (isJobCancelled(jobId)) {
                                    log.info("Job {} cancelled during chunk interval pause. Halting execution.", jobId);
                                    return;
                                }
                                try {
                                    TimeUnit.SECONDS.sleep(1);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    return;
                                }
                            }
                        }
                    }

                    pageNumber++;
                } while (page.hasNext());
            }

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

    private Specification<Question> buildBatchSpecification(String subjectFilter, String tenantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null && !tenantId.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get("tenantId"), tenantId),
                        cb.equal(root.get("tenantId"), "default")
                ));
            }

            if (subjectFilter != null && !subjectFilter.isBlank()) {
                String trimmed = subjectFilter.trim();
                try {
                    Long parsedSubjectId = Long.parseLong(trimmed);
                    predicates.add(cb.equal(root.get("subjectId"), parsedSubjectId));
                } catch (NumberFormatException e) {
                    String pattern = "%" + trimmed.toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.equal(cb.lower(root.get("subject")), trimmed.toLowerCase()),
                            cb.like(cb.lower(root.get("subject")), pattern)
                    ));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void processSingleQuestion(
            Question question,
            String targetLang,
            Translation.TranslationStatus targetStatus,
            boolean overwriteExisting,
            String tenantId,
            UUID jobId) {

        UUID questionId = question.getId();
        try {
            if (!overwriteExisting) {
                List<Translation> existing = translationRepository
                        .findByQuestionIdAndLanguageCodeAndTenantId(questionId, targetLang, tenantId);
                if (!existing.isEmpty()) {
                    log.info("Translation for questionId={} and lang={} already exists and overwriteExisting=false. Skipping translation call.", questionId, targetLang);
                    jobRepository.incrementSuccess(jobId);
                    return;
                }
            }

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
                    "Batch AI Auto-Translate (Paper Generation)",
                    tenantId
            );

            // Atomic database increment avoids Hibernate @Version optimistic locking collisions
            jobRepository.incrementSuccess(jobId);

        } catch (Exception ex) {
            log.error("Failed to translate questionId={} in batch {}: {}", questionId, jobId, ex.getMessage());
            jobRepository.incrementFailure(jobId);
        }
    }

    private boolean isJobCancelled(UUID jobId) {
        Optional<BatchTranslationJob> current = jobRepository.findById(jobId);
        return current.isPresent() && current.get().getStatus() == BatchTranslationJobStatus.CANCELLED;
    }

    public void setChunkPauseIntervalQuestions(int chunkPauseIntervalQuestions) {
        this.chunkPauseIntervalQuestions = chunkPauseIntervalQuestions;
    }

    public void setChunkPauseDurationSeconds(int chunkPauseDurationSeconds) {
        this.chunkPauseDurationSeconds = chunkPauseDurationSeconds;
    }
}
