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

import com.examplatform.questionbank.microlesson.translation.domain.MicroLessonBatchTranslationJob;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonBatchTranslationJobResponse;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonBatchTranslationRequest;
import com.examplatform.questionbank.microlesson.translation.repository.MicroLessonBatchTranslationJobRepository;
import com.examplatform.questionbank.translation.service.TranslationWorkflowService;
import com.examplatform.shared.constants.SharedConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MicroLessonBatchTranslationService {

    private final MicroLessonBatchTranslationJobRepository jobRepository;
    private final AsyncMicroLessonBatchTranslationWorker asyncWorker;

    private static final UUID DEFAULT_SYSTEM_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public synchronized MicroLessonBatchTranslationJobResponse startBatchJob(
            MicroLessonBatchTranslationRequest request,
            UUID initiatedBy,
            String tenantId) {

        String targetLang = resolveAndValidateTargetLanguage(request.getTargetLanguage());
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        MicroLessonBatchTranslationJob job = buildJobEntity(request, targetLang, initiatedBy, effectiveTenant);
        MicroLessonBatchTranslationJob savedJob = jobRepository.save(job);

        log.info("Created micro-lesson batch translation job: id={}, targetLang={}, subject={}, lessonCount={}, tenant={}",
                savedJob.getId(), targetLang, savedJob.getSubjectFilter(),
                savedJob.getLessonIds() != null ? savedJob.getLessonIds().size() : 0, effectiveTenant);

        asyncWorker.processBatchTranslationJob(savedJob.getId(), effectiveTenant);

        return toResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public MicroLessonBatchTranslationJobResponse getJobStatus(UUID jobId, String tenantId) {
        MicroLessonBatchTranslationJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch translation job not found: " + jobId));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public Page<MicroLessonBatchTranslationJobResponse> listJobs(String tenantId, Pageable pageable) {
        return jobRepository.findByTenantId(tenantId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public MicroLessonBatchTranslationJobResponse cancelJob(UUID jobId, String tenantId) {
        MicroLessonBatchTranslationJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch translation job not found: " + jobId));

        if ("PENDING".equalsIgnoreCase(job.getStatus()) || SharedConstants.IN_PROGRESS.equalsIgnoreCase(job.getStatus())) {
            job.setStatus(SharedConstants.CANCELLED);
            job.setCompletedAt(Instant.now());
            job = jobRepository.save(job);
            log.info("Micro-lesson batch translation job {} cancelled by user", jobId);
        }

        return toResponse(job);
    }

    public MicroLessonBatchTranslationJobResponse toResponse(MicroLessonBatchTranslationJob job) {
        double progressPercentage = 0.0;
        if (job.getTotalLessons() > 0) {
            progressPercentage = Math.min(100.0, (job.getProcessedLessons() * 100.0) / job.getTotalLessons());
        } else if (SharedConstants.COMPLETED.equalsIgnoreCase(job.getStatus())) {
            progressPercentage = 100.0;
        }

        return MicroLessonBatchTranslationJobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .status(job.getStatus())
                .sourceLanguage(job.getSourceLanguage())
                .targetLanguage(job.getTargetLanguage())
                .targetStatus(job.getTargetStatus())
                .subjectFilter(job.getSubjectFilter())
                .topicFilter(job.getTopicFilter())
                .targetExamFilter(job.getTargetExamFilter())
                .lessonIds(job.getLessonIds())
                .overwriteExisting(job.isOverwriteExisting())
                .totalLessons(job.getTotalLessons())
                .processedLessons(job.getProcessedLessons())
                .successfulLessons(job.getSuccessfulLessons())
                .failedLessons(job.getFailedLessons())
                .progressPercentage(progressPercentage)
                .failedLessonIds(job.getFailedLessonIds())
                .batchSize(job.getBatchSize())
                .throttleDelayMs(job.getThrottleDelayMs())
                .maxConcurrency(job.getMaxConcurrency())
                .initiatedBy(job.getInitiatedBy())
                .startedAt(job.getStartedAt())
                .completedAt(job.getCompletedAt())
                .errorMessage(job.getErrorMessage())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .build();
    }

    private MicroLessonBatchTranslationJob buildJobEntity(
            MicroLessonBatchTranslationRequest request,
            String targetLang,
            UUID initiatedBy,
            String tenantId) {

        List<UUID> lessonIds = request.getLessonIds() != null
                ? new ArrayList<>(request.getLessonIds())
                : new ArrayList<>();

        MicroLessonBatchTranslationJob job = MicroLessonBatchTranslationJob.builder()
                .status("PENDING")
                .sourceLanguage(request.getSourceLanguage() != null ? request.getSourceLanguage() : "en")
                .targetLanguage(targetLang)
                .targetStatus(request.getTargetStatus() != null ? request.getTargetStatus() : "PUBLISHED")
                .subjectFilter(cleanFilter(request.getSubject()))
                .topicFilter(cleanFilter(request.getTopic()))
                .targetExamFilter(cleanFilter(request.getTargetExam()))
                .lessonIds(lessonIds)
                .overwriteExisting(request.getOverwriteExisting() == null || request.getOverwriteExisting())
                .batchSize(request.getBatchSize() != null ? Math.max(1, request.getBatchSize()) : 20)
                .throttleDelayMs(request.getThrottleDelayMs() != null ? Math.max(0, request.getThrottleDelayMs()) : 50)
                .maxConcurrency(request.getMaxConcurrency() != null ? Math.max(1, request.getMaxConcurrency()) : 2)
                .initiatedBy(initiatedBy != null ? initiatedBy : DEFAULT_SYSTEM_UUID)
                .build();

        job.setTenantId(tenantId);
        return job;
    }

    private String cleanFilter(String filter) {
        if (filter == null || filter.isBlank()) {
            return null;
        }
        String trimmed = filter.trim();
        if ("all".equalsIgnoreCase(trimmed) || "all subjects".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return trimmed;
    }

    private String resolveAndValidateTargetLanguage(String targetLanguage) {
        String targetLang = (targetLanguage != null && !targetLanguage.isBlank())
                ? targetLanguage.toLowerCase().trim()
                : "hi";

        if (!TranslationWorkflowService.SUPPORTED_LANGUAGES.contains(targetLang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported target language code: " + targetLang);
        }
        return targetLang;
    }
}
