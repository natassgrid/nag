/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import com.examplatform.questionbank.translation.ai.IndicTrans2Service;

import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.translation.domain.BatchTranslationJob;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.AutoTranslateResponse;
import com.examplatform.questionbank.translation.repository.BatchTranslationJobRepository;
import com.examplatform.questionbank.translation.repository.TranslationRepository;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
 * - Resilient auto-resume and duplicate execution prevention
 * - Configurable development pause intervals
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncBatchTranslationWorker {

    private static final UUID SYSTEM_BATCH_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String JOB_CANCELLED_MID_BATCH_LOG = "Job {} cancelled mid-batch. Halting execution.";

    private final BatchTranslationJobRepository jobRepository;
    private final QuestionRepository questionRepository;
    private final TranslationRepository translationRepository;
    private final IndicTrans2Service indicTrans2Service;
    private final TranslationWorkflowService translationWorkflowService;

    private final Set<UUID> activeJobIds = ConcurrentHashMap.newKeySet();

    @Setter
    @Value("${translation.batch.chunk-pause-interval-questions:15}")
    private int chunkPauseIntervalQuestions = 15;

    @Setter
    @Value("${translation.batch.chunk-pause-duration-seconds:60}")
    private int chunkPauseDurationSeconds = 60;

    private record TranslationExecutionContext(
            UUID jobId,
            String tenantId,
            String targetLanguage,
            Translation.TranslationStatus targetStatus,
            boolean overwriteExisting,
            Instant jobCreatedAt,
            int batchSize,
            int throttleDelayMs,
            Semaphore concurrencyLimiter
    ) {}

    @Async
    public void processBatchTranslationJob(UUID jobId, String tenantId) {
        log.info("Starting asynchronous batch translation worker for jobId={}, tenant={}", jobId, tenantId);

        if (!activeJobIds.add(jobId)) {
            log.warn("Batch translation job {} is already actively running. Skipping duplicate execution.", jobId);
            return;
        }

        try {
            TenantContext.setTenantId(tenantId);

            Optional<BatchTranslationJob> jobOpt = loadJobWithRetries(jobId);
            if (jobOpt.isEmpty()) {
                log.error("BatchTranslationJob {} not found after retries", jobId);
                return;
            }

            BatchTranslationJob job = jobOpt.get();
            if (isJobTerminal(job)) {
                return;
            }

            initializeJobProgress(job);
            TranslationExecutionContext context = createExecutionContext(job, tenantId);
            processJobQuestions(job, context);
            completeJob(jobId);

        } catch (Exception ex) {
            handleFatalError(jobId, ex);
        } finally {
            activeJobIds.remove(jobId);
            TenantContext.clear();
        }
    }

    private Optional<BatchTranslationJob> loadJobWithRetries(UUID jobId) {
        for (int attempt = 0; attempt < 5; attempt++) {
            Optional<BatchTranslationJob> jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isPresent()) {
                return jobOpt;
            }
            if (sleepInterrupted(100)) {
                break;
            }
        }
        return Optional.empty();
    }

    private boolean sleepInterrupted(long millis) {
        try {
            Thread.sleep(millis);
            return false;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return true;
        }
    }

    private boolean isJobTerminal(BatchTranslationJob job) {
        if (job.getStatus() == BatchTranslationJobStatus.CANCELLED) {
            log.info("Batch translation job {} is already CANCELLED. Exiting.", job.getId());
            return true;
        }
        if (job.getStatus() == BatchTranslationJobStatus.COMPLETED) {
            log.info("Batch translation job {} is already COMPLETED. Exiting.", job.getId());
            return true;
        }
        return false;
    }

    private void initializeJobProgress(BatchTranslationJob job) {
        job.setStatus(BatchTranslationJobStatus.IN_PROGRESS);
        if (job.getStartedAt() == null) {
            job.setStartedAt(Instant.now());
        }
        job.setProcessedQuestions(0);
        job.setSuccessfulQuestions(0);
        job.setFailedQuestions(0);
        job.setFailedQuestionIds(new ArrayList<>());
        job.setErrorMessage(null);
        jobRepository.save(job);
    }

    private TranslationExecutionContext createExecutionContext(BatchTranslationJob job, String tenantId) {
        String targetStatusStr = job.getTargetStatus();
        Translation.TranslationStatus targetStatus = QuestionStateConstants.APPROVED.equalsIgnoreCase(targetStatusStr)
                ? Translation.TranslationStatus.APPROVED
                : Translation.TranslationStatus.PUBLISHED;

        return new TranslationExecutionContext(
                job.getId(),
                tenantId,
                job.getTargetLanguage(),
                targetStatus,
                job.isOverwriteExisting(),
                job.getCreatedAt(),
                Math.max(1, job.getBatchSize()),
                Math.max(0, job.getThrottleDelayMs()),
                new Semaphore(Math.max(1, job.getMaxConcurrency()))
        );
    }

    private void processJobQuestions(BatchTranslationJob job, TranslationExecutionContext context) {
        List<UUID> explicitQuestionIds = job.getQuestionIds();
        if (explicitQuestionIds != null && !explicitQuestionIds.isEmpty()) {
            processExplicitQuestions(job, explicitQuestionIds, context);
        } else {
            processPaginatedQuestions(job, context);
        }
    }

    private void processExplicitQuestions(
            BatchTranslationJob job,
            List<UUID> explicitQuestionIds,
            TranslationExecutionContext context) {
        UUID jobId = context.jobId();
        int total = explicitQuestionIds.size();
        updateTotalQuestionsIfPresent(jobId, total, job.getPaperId());

        int questionsSinceLastPause = 0;
        for (int fromIndex = 0; fromIndex < total; fromIndex += context.batchSize()) {
            if (isJobCancelled(jobId)) {
                log.info(JOB_CANCELLED_MID_BATCH_LOG, jobId);
                return;
            }

            int toIndex = Math.min(fromIndex + context.batchSize(), total);
            List<UUID> chunkIds = explicitQuestionIds.subList(fromIndex, toIndex);
            questionsSinceLastPause = processExplicitChunk(chunkIds, context, questionsSinceLastPause, toIndex < total);
            if (questionsSinceLastPause < 0) {
                return;
            }
        }
    }

    private int processExplicitChunk(
            List<UUID> chunkIds,
            TranslationExecutionContext context,
            int questionsSinceLastPause,
            boolean hasMoreChunks) {
        UUID jobId = context.jobId();
        List<Question> questions = questionRepository.findQuestionsByIdsIn(chunkIds, context.tenantId());
        Map<UUID, Question> questionMap = questions.stream()
                .collect(Collectors.toMap(Question::getId, q -> q, (a, b) -> a));

        int currentPauseCount = questionsSinceLastPause;
        for (int i = 0; i < chunkIds.size(); i++) {
            UUID qId = chunkIds.get(i);
            if (isJobCancelled(jobId)) {
                log.info(JOB_CANCELLED_MID_BATCH_LOG, jobId);
                return -1;
            }

            Question question = questionMap.get(qId);
            if (question == null) {
                log.warn("Question ID {} from paper list not found in repository. Recording failure.", qId);
                jobRepository.incrementFailure(jobId);
                continue;
            }

            if (executeThrottledQuestion(question, context)) {
                return -1;
            }

            currentPauseCount++;
            boolean hasMore = hasMoreChunks || (i < chunkIds.size() - 1);
            currentPauseCount = checkAndApplyPause(jobId, currentPauseCount, hasMore);
            if (currentPauseCount < 0) {
                return -1;
            }
        }
        return currentPauseCount;
    }

    private void processPaginatedQuestions(BatchTranslationJob job, TranslationExecutionContext context) {
        UUID jobId = context.jobId();
        int pageNumber = 0;
        Page<Question> page;
        Specification<Question> spec = buildBatchSpecification(job.getSubjectFilter(), context.tenantId());
        int questionsSinceLastPause = 0;

        do {
            if (isJobCancelled(jobId)) {
                log.info("Batch translation job {} was cancelled by user. Terminating worker loop.", jobId);
                return;
            }

            PageRequest pageRequest = PageRequest.of(pageNumber, context.batchSize(), Sort.by(SharedConstants.CREATED_AT).ascending());
            page = questionRepository.findAll(spec, pageRequest);

            if (pageNumber == 0) {
                updateTotalQuestionsIfPresent(jobId, (int) page.getTotalElements(), null);
            }

            questionsSinceLastPause = processPageQuestions(page, context, questionsSinceLastPause);
            if (questionsSinceLastPause < 0) {
                return;
            }

            pageNumber++;
        } while (page.hasNext());
    }

    private int processPageQuestions(
            Page<Question> page,
            TranslationExecutionContext context,
            int questionsSinceLastPause) {
        UUID jobId = context.jobId();
        List<Question> questions = page.getContent();
        int currentPauseCount = questionsSinceLastPause;

        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            if (isJobCancelled(jobId)) {
                log.info(JOB_CANCELLED_MID_BATCH_LOG, jobId);
                return -1;
            }

            if (executeThrottledQuestion(question, context)) {
                return -1;
            }

            currentPauseCount++;
            boolean hasMore = (i < questions.size() - 1) || page.hasNext();
            currentPauseCount = checkAndApplyPause(jobId, currentPauseCount, hasMore);
            if (currentPauseCount < 0) {
                return -1;
            }
        }
        return currentPauseCount;
    }

    private boolean shouldPause(int count, boolean hasMore) {
        return hasMore && chunkPauseIntervalQuestions > 0 && chunkPauseDurationSeconds > 0
                && count >= chunkPauseIntervalQuestions;
    }

    private int checkAndApplyPause(UUID jobId, int count, boolean hasMore) {
        if (!shouldPause(count, hasMore)) {
            return count;
        }
        log.info("Batch translation job {}: Reached chunk interval of {} questions. Pausing for {}s...",
                jobId, count, chunkPauseDurationSeconds);
        return pauseChunkIntervalIfDue(jobId, chunkPauseDurationSeconds) ? 0 : -1;
    }

    private void updateTotalQuestionsIfPresent(UUID jobId, int total, UUID paperId) {
        Optional<BatchTranslationJob> freshJobOpt = jobRepository.findById(jobId);
        if (freshJobOpt.isPresent()) {
            BatchTranslationJob freshJob = freshJobOpt.get();
            freshJob.setTotalQuestions(total);
            jobRepository.save(freshJob);
            if (paperId != null) {
                log.info("Batch job {} for paper {} matched explicit question count={}", jobId, paperId, total);
            } else {
                log.info("Batch job {} matched totalQuestions={}", jobId, total);
            }
        }
    }

    private void completeJob(UUID jobId) {
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
    }

    private void handleFatalError(UUID jobId, Exception ex) {
        log.error("Fatal error during batch translation execution for job {}: {}", jobId, ex.getMessage(), ex);
        Optional<BatchTranslationJob> failedJobOpt = jobRepository.findById(jobId);
        if (failedJobOpt.isPresent()) {
            BatchTranslationJob failedJob = failedJobOpt.get();
            failedJob.setStatus(BatchTranslationJobStatus.FAILED);
            failedJob.setErrorMessage(ex.getMessage());
            failedJob.setCompletedAt(Instant.now());
            jobRepository.save(failedJob);
        }
    }

    private Specification<Question> buildBatchSpecification(String subjectFilter, String tenantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null && !tenantId.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get(SharedConstants.TENANT_ID), tenantId),
                        cb.equal(root.get(SharedConstants.TENANT_ID), SharedConstants.DEFAULT_TENANT)
                ));
            }

            if (subjectFilter != null && !subjectFilter.isBlank()) {
                String trimmed = subjectFilter.trim();
                try {
                    Long parsedSubjectId = Long.parseLong(trimmed);
                    predicates.add(cb.equal(root.get("subjectId"), parsedSubjectId));
                } catch (NumberFormatException _) {
                    String pattern = "%" + trimmed.toLowerCase() + "%";
                    predicates.add(cb.or(
                            cb.equal(cb.lower(root.get(SharedConstants.SUBJECT)), trimmed.toLowerCase()),
                            cb.like(cb.lower(root.get(SharedConstants.SUBJECT)), pattern)
                    ));
                }
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void processSingleQuestion(Question question, TranslationExecutionContext context) {
        UUID questionId = question.getId();
        UUID jobId = context.jobId();
        String targetLang = context.targetLanguage();
        try {
            List<Translation> existing = translationRepository
                    .findByQuestionIdAndLanguageCodeAndTenantId(questionId, targetLang, context.tenantId());

            if (!existing.isEmpty() && shouldSkipExistingTranslation(questionId, existing, context)) {
                jobRepository.incrementSuccess(jobId);
                return;
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
                    context.targetStatus(),
                    SYSTEM_BATCH_UUID,
                    "Batch AI Auto-Translate (Paper Generation)",
                    context.tenantId()
            );

            // Atomic database increment avoids Hibernate @Version optimistic locking collisions
            jobRepository.incrementSuccess(jobId);

        } catch (Exception ex) {
            log.error("Failed to translate questionId={} in batch {}: {}", questionId, jobId, ex.getMessage());
            jobRepository.incrementFailure(jobId);
        }
    }

    private boolean shouldSkipExistingTranslation(UUID questionId, List<Translation> existing, TranslationExecutionContext context) {
        if (!context.overwriteExisting()) {
            log.info("Translation for questionId={} and lang={} already exists and overwriteExisting=false. Skipping translation call.",
                    questionId, context.targetLanguage());
            return true;
        }

        // If overwriteExisting is true, check if this translation was already created/updated as part of this job execution
        boolean alreadyTranslatedByThisJob = existing.stream().anyMatch(t -> {
            Instant tTime = t.getUpdatedAt() != null ? t.getUpdatedAt() : t.getCreatedAt();
            return tTime != null && context.jobCreatedAt() != null && !tTime.isBefore(context.jobCreatedAt());
        });

        if (alreadyTranslatedByThisJob) {
            log.info("Translation for questionId={} and lang={} was already processed for job {}. Skipping redundant AI call.",
                    questionId, context.targetLanguage(), context.jobId());
            return true;
        }

        return false;
    }

    private boolean isJobCancelled(UUID jobId) {
        Optional<BatchTranslationJob> current = jobRepository.findById(jobId);
        return current.isPresent() && current.get().getStatus() == BatchTranslationJobStatus.CANCELLED;
    }

    public boolean isJobActive(UUID jobId) {
        return activeJobIds.contains(jobId);
    }

    private boolean executeThrottledQuestion(Question question, TranslationExecutionContext context) {
        try {
            context.concurrencyLimiter().acquire();
            processSingleQuestion(question, context);
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            log.warn("Batch worker interrupted while acquiring semaphore for job {}", context.jobId());
            return true;
        } finally {
            context.concurrencyLimiter().release();
        }

        if (context.throttleDelayMs() > 0) {
            try {
                TimeUnit.MILLISECONDS.sleep(context.throttleDelayMs());
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
                log.warn("Throttle sleep interrupted for job {}", context.jobId());
                return true;
            }
        }
        return false;
    }

    private boolean pauseChunkIntervalIfDue(UUID jobId, int chunkPauseDurationSeconds) {
        for (int s = 0; s < chunkPauseDurationSeconds; s++) {
            if (isJobCancelled(jobId)) {
                log.info("Job {} cancelled during chunk interval pause. Halting execution.", jobId);
                return false;
            }
            if (sleepOneSecond()) {
                return false;
            }
        }
        return true;
    }

    private boolean sleepOneSecond() {
        try {
            TimeUnit.SECONDS.sleep(1);
            return false;
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return true;
        }
    }
}
