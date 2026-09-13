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

import com.examplatform.questionbank.translation.domain.BatchTranslationJob;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import com.examplatform.questionbank.translation.dto.BatchTranslationJobResponse;
import com.examplatform.questionbank.translation.dto.BatchTranslationRequest;
import com.examplatform.questionbank.translation.repository.BatchTranslationJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchTranslationService {

    private final BatchTranslationJobRepository jobRepository;
    private final AsyncBatchTranslationWorker asyncWorker;

    @Transactional
    public BatchTranslationJobResponse startBatchJob(BatchTranslationRequest request, UUID initiatedBy, String tenantId) {
        String targetLang = (request.getTargetLanguage() != null && !request.getTargetLanguage().isBlank())
                ? request.getTargetLanguage().toLowerCase()
                : "hi";

        if (!TranslationWorkflowService.SUPPORTED_LANGUAGES.contains(targetLang)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported target language code: " + targetLang);
        }

        BatchTranslationJob job = BatchTranslationJob.builder()
                .status(BatchTranslationJobStatus.PENDING)
                .sourceLanguage(request.getSourceLanguage() != null ? request.getSourceLanguage() : "en")
                .targetLanguage(targetLang)
                .targetStatus(request.getTargetStatus() != null ? request.getTargetStatus() : "PUBLISHED")
                .subjectFilter(request.getSubject())
                .overwriteExisting(request.getOverwriteExisting() == null || request.getOverwriteExisting())
                .batchSize(request.getBatchSize() != null ? request.getBatchSize() : 50)
                .throttleDelayMs(request.getThrottleDelayMs() != null ? request.getThrottleDelayMs() : 50)
                .maxConcurrency(request.getMaxConcurrency() != null ? request.getMaxConcurrency() : 2)
                .initiatedBy(initiatedBy != null ? initiatedBy : UUID.fromString("00000000-0000-0000-0000-000000000001"))
                .build();
        job.setTenantId(tenantId);

        BatchTranslationJob savedJob = jobRepository.save(job);
        log.info("Created batch translation job: id={}, targetLang={}, tenant={}", savedJob.getId(), targetLang, tenantId);

        // Fire async background worker
        asyncWorker.processBatchTranslationJob(savedJob.getId(), tenantId);

        return toResponse(savedJob);
    }

    @Transactional(readOnly = true)
    public BatchTranslationJobResponse getJobStatus(UUID jobId, String tenantId) {
        BatchTranslationJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch translation job not found: " + jobId));
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public List<BatchTranslationJobResponse> listJobs(String tenantId) {
        return jobRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BatchTranslationJobResponse cancelJob(UUID jobId, String tenantId) {
        BatchTranslationJob job = jobRepository.findByIdAndTenantId(jobId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Batch translation job not found: " + jobId));

        if (job.getStatus() == BatchTranslationJobStatus.PENDING || job.getStatus() == BatchTranslationJobStatus.IN_PROGRESS) {
            job.setStatus(BatchTranslationJobStatus.CANCELLED);
            job.setCompletedAt(Instant.now());
            job = jobRepository.save(job);
            log.info("Batch translation job {} cancelled by user", jobId);
        }

        return toResponse(job);
    }

    public BatchTranslationJobResponse toResponse(BatchTranslationJob job) {
        double progressPercentage = 0.0;
        if (job.getTotalQuestions() > 0) {
            progressPercentage = Math.min(100.0, (job.getProcessedQuestions() * 100.0) / job.getTotalQuestions());
        } else if (job.getStatus() == BatchTranslationJobStatus.COMPLETED) {
            progressPercentage = 100.0;
        }

        return BatchTranslationJobResponse.builder()
                .id(job.getId())
                .tenantId(job.getTenantId())
                .status(job.getStatus())
                .sourceLanguage(job.getSourceLanguage())
                .targetLanguage(job.getTargetLanguage())
                .targetStatus(job.getTargetStatus())
                .subjectFilter(job.getSubjectFilter())
                .overwriteExisting(job.isOverwriteExisting())
                .totalQuestions(job.getTotalQuestions())
                .processedQuestions(job.getProcessedQuestions())
                .successfulQuestions(job.getSuccessfulQuestions())
                .failedQuestions(job.getFailedQuestions())
                .progressPercentage(progressPercentage)
                .failedQuestionIds(job.getFailedQuestionIds())
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
}
