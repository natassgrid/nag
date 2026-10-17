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

package com.examplatform.questionbank.microlesson.translation.service;

import com.examplatform.questionbank.microlesson.domain.SubtopicMicroLesson;
import com.examplatform.questionbank.microlesson.repository.SubtopicMicroLessonRepository;
import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonBatchTranslationJob;
import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonTranslation;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonBatchTranslationJobRepository;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonTranslationRepository;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Worker for processing large-scale asynchronous background translation jobs for micro-lessons.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMicroLessonBatchTranslationWorker {

    private static final UUID SYSTEM_BATCH_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final MicroLessonBatchTranslationJobRepository jobRepository;
    private final SubtopicMicroLessonRepository lessonRepository;
    private final MicroLessonTranslationRepository translationRepository;
    private final MicroLessonTranslationPayloadService payloadService;
    private final MicroLessonTranslationWorkflowService workflowService;

    private final Set<UUID> activeJobIds = ConcurrentHashMap.newKeySet();

    @Async
    public void processBatchTranslationJob(UUID jobId, String tenantId) {
        log.info("Starting micro-lesson batch translation worker for jobId={}, tenant={}", jobId, tenantId);

        if (!activeJobIds.add(jobId)) {
            log.warn("Micro-lesson batch translation job {} is already actively running. Skipping duplicate execution.", jobId);
            return;
        }

        try {
            TenantContext.setTenantId(tenantId);

            Optional<MicroLessonBatchTranslationJob> jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isEmpty()) {
                log.error("MicroLessonBatchTranslationJob {} not found", jobId);
                return;
            }

            MicroLessonBatchTranslationJob job = jobOpt.get();
            if ("COMPLETED".equalsIgnoreCase(job.getStatus()) || "CANCELLED".equalsIgnoreCase(job.getStatus())) {
                return;
            }

            job.setStatus("IN_PROGRESS");
            job.setStartedAt(Instant.now());
            jobRepository.save(job);

            Semaphore semaphore = new Semaphore(Math.max(1, job.getMaxConcurrency()));

            if (job.getLessonIds() != null && !job.getLessonIds().isEmpty()) {
                processExplicitLessons(job, semaphore, tenantId);
            } else {
                processPaginatedLessons(job, semaphore, tenantId);
            }

            completeJobIfActive(jobId);

        } catch (Exception ex) {
            handleFatalError(jobId, ex);
        } finally {
            activeJobIds.remove(jobId);
            TenantContext.clear();
        }
    }

    private void processExplicitLessons(
            MicroLessonBatchTranslationJob job,
            Semaphore semaphore,
            String tenantId) {

        UUID jobId = job.getId();
        List<UUID> explicitIds = job.getLessonIds();
        int total = explicitIds.size();

        job.setTotalLessons(total);
        jobRepository.save(job);

        int batchSize = Math.max(1, job.getBatchSize());

        for (int from = 0; from < total; from += batchSize) {
            if (isJobCancelled(jobId)) {
                log.info("Micro-lesson job {} cancelled mid-batch. Halting execution.", jobId);
                return;
            }

            int to = Math.min(from + batchSize, total);
            List<UUID> slice = explicitIds.subList(from, to);

            for (UUID lessonId : slice) {
                if (isJobCancelled(jobId)) {
                    log.info("Micro-lesson job {} cancelled mid-batch. Halting execution.", jobId);
                    return;
                }

                Optional<SubtopicMicroLesson> lessonOpt = lessonRepository.findById(lessonId);
                if (lessonOpt.isEmpty()) {
                    log.warn("Micro-lesson ID {} not found in repository. Recording failure.", lessonId);
                    jobRepository.incrementFailure(jobId, "[\"" + lessonId + "\"]");
                    continue;
                }

                translateAndUpsertLesson(lessonOpt.get(), job, semaphore, tenantId);
                throttle(job.getThrottleDelayMs());
            }
        }
    }

    private void processPaginatedLessons(
            MicroLessonBatchTranslationJob job,
            Semaphore semaphore,
            String tenantId) {

        UUID jobId = job.getId();
        Specification<SubtopicMicroLesson> spec = buildSpecification(job, tenantId);
        int batchSize = Math.max(1, job.getBatchSize());
        int pageNumber = 0;
        Page<SubtopicMicroLesson> page;

        do {
            if (isJobCancelled(jobId)) {
                log.info("Micro-lesson batch job {} cancelled by user. Terminating worker loop.", jobId);
                return;
            }

            PageRequest pageRequest = PageRequest.of(pageNumber, batchSize, Sort.by("createdAt").ascending());
            page = lessonRepository.findAll(spec, pageRequest);

            if (pageNumber == 0) {
                job.setTotalLessons((int) page.getTotalElements());
                jobRepository.save(job);
            }

            for (SubtopicMicroLesson lesson : page.getContent()) {
                if (isJobCancelled(jobId)) {
                    log.info("Micro-lesson batch job {} cancelled mid-batch. Halting execution.", jobId);
                    return;
                }

                translateAndUpsertLesson(lesson, job, semaphore, tenantId);
                throttle(job.getThrottleDelayMs());
            }

            pageNumber++;
        } while (page.hasNext());
    }

    private void translateAndUpsertLesson(
            SubtopicMicroLesson lesson,
            MicroLessonBatchTranslationJob job,
            Semaphore semaphore,
            String tenantId) {

        UUID jobId = job.getId();
        UUID lessonId = lesson.getId();
        String targetLang = job.getTargetLanguage();

        try {
            // Check existing translation if overwriteExisting is false
            if (!job.isOverwriteExisting()) {
                Optional<MicroLessonTranslation> existing = translationRepository
                        .findByMicrolessonIdAndLanguageCodeAndTenantId(lessonId, targetLang, tenantId);
                if (existing.isPresent()) {
                    log.info("Translation for lessonId={} and lang={} already exists and overwriteExisting=false. Skipping translation call.",
                            lessonId, targetLang);
                    jobRepository.incrementSuccess(jobId);
                    return;
                }
            }

            semaphore.acquire();
            try {
                MicroLessonTranslationPayloadService.TranslatedMicroLessonPayload payload =
                        payloadService.translateMicroLesson(lesson, targetLang);

                workflowService.upsertTranslation(
                        lessonId,
                        targetLang,
                        payload.coreConcept(),
                        payload.trap(),
                        payload.cheatSheetJson(),
                        payload.stepByStepBreakdownJson(),
                        job.getTargetStatus(),
                        SYSTEM_BATCH_UUID,
                        null,
                        "Batch AI Auto-Translate via IndicTrans2",
                        lesson.getContentVersion(),
                        tenantId
                );

                jobRepository.incrementSuccess(jobId);

            } finally {
                semaphore.release();
            }

        } catch (Exception ex) {
            log.error("Failed to translate micro-lesson lessonId={} in batch {}: {}", lessonId, jobId, ex.getMessage());
            jobRepository.incrementFailure(jobId, "[\"" + lessonId + "\"]");
        }
    }

    private void throttle(int throttleDelayMs) {
        if (throttleDelayMs > 0) {
            try {
                Thread.sleep(throttleDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean isJobCancelled(UUID jobId) {
        Optional<MicroLessonBatchTranslationJob> job = jobRepository.findById(jobId);
        return job.isPresent() && "CANCELLED".equalsIgnoreCase(job.get().getStatus());
    }

    private void completeJobIfActive(UUID jobId) {
        Optional<MicroLessonBatchTranslationJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            MicroLessonBatchTranslationJob job = jobOpt.get();
            if (!"CANCELLED".equalsIgnoreCase(job.getStatus())) {
                job.setStatus("COMPLETED");
                job.setCompletedAt(Instant.now());
                jobRepository.save(job);
                log.info("Micro-lesson batch translation job {} finished successfully. Total processed={}",
                        jobId, job.getProcessedLessons());
            }
        }
    }

    private void handleFatalError(UUID jobId, Exception ex) {
        log.error("Fatal error during micro-lesson batch translation execution for job {}: {}", jobId, ex.getMessage(), ex);
        Optional<MicroLessonBatchTranslationJob> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent()) {
            MicroLessonBatchTranslationJob job = jobOpt.get();
            job.setStatus("FAILED");
            job.setErrorMessage(ex.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepository.save(job);
        }
    }

    private Specification<SubtopicMicroLesson> buildSpecification(MicroLessonBatchTranslationJob job, String tenantId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tenantId != null && !tenantId.isBlank()) {
                predicates.add(cb.or(
                        cb.equal(root.get(SharedConstants.TENANT_ID), tenantId),
                        cb.equal(root.get(SharedConstants.TENANT_ID), SharedConstants.DEFAULT_TENANT)
                ));
            }

            if (job.getSubjectFilter() != null && !job.getSubjectFilter().isBlank()) {
                String sub = job.getSubjectFilter().trim();
                try {
                    Long subjectId = Long.parseLong(sub);
                    predicates.add(cb.equal(root.get("subjectId"), subjectId));
                } catch (NumberFormatException ignored) {
                    predicates.add(cb.equal(cb.lower(root.get("subject")), sub.toLowerCase()));
                }
            }

            if (job.getTopicFilter() != null && !job.getTopicFilter().isBlank()) {
                String top = job.getTopicFilter().trim();
                predicates.add(cb.equal(cb.lower(root.get("topic")), top.toLowerCase()));
            }

            if (job.getTargetExamFilter() != null && !job.getTargetExamFilter().isBlank()) {
                String exam = job.getTargetExamFilter().trim();
                predicates.add(cb.equal(cb.upper(root.get("targetExam")), exam.toUpperCase()));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
