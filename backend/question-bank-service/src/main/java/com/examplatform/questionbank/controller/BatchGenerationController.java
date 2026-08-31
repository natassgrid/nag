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
package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.ai.batch.BatchGenerationJob;
import com.examplatform.questionbank.ai.batch.BatchGenerationJobRepository;
import com.examplatform.questionbank.ai.batch.BatchGenerationRequest;
import com.examplatform.questionbank.ai.batch.BatchJobResponse;
import com.examplatform.questionbank.ai.batch.BedrockBatchService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for batch question generation endpoints.
 * Supports submitting, monitoring, and cancelling async batch generation jobs
 * powered by AWS Bedrock models via LiteLLM.
 *
 * Validates: Requirements FR-3 (AI Question Generation — Batch Mode)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions/batch")
@RequiredArgsConstructor
public class BatchGenerationController {

    private final BedrockBatchService bedrockBatchService;
    private final BatchGenerationJobRepository jobRepository;

    /**
     * Submits a new batch generation job. Returns immediately with the job metadata.
     * The job is processed asynchronously in the background.
     *
     * @param request  batch generation parameters (subject, topic, count up to 100, etc.)
     * @param tenantId tenant identifier
     * @param jwt      authenticated user's JWT
     * @return the created job with PENDING status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<BatchJobResponse>> submitBatchJob(
            @Valid @RequestBody BatchGenerationRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID authorId = UUID.fromString(jwt.getSubject());
        log.info("Batch generation job submitted: items={}, tenant={}",
                request.getItems().size(), tenantId);

        BatchJobResponse response = bedrockBatchService.submitBatchJob(request, authorId, tenantId);

        return ResponseEntity.accepted().body(
                ApiResponse.success(response, "Batch generation job submitted. Use the job ID to track progress."));
    }

    /**
     * Gets the status of a specific batch job.
     *
     * @param jobId    the batch job UUID
     * @param tenantId tenant identifier
     * @return the current job state including progress
     */
    @GetMapping("/{jobId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<BatchJobResponse>> getJobStatus(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        BatchGenerationJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new EntityNotFoundException("Batch job not found: " + jobId));

        if (!job.getTenantId().equals(tenantId)) {
            throw new EntityNotFoundException("Batch job not found: " + jobId);
        }

        return ResponseEntity.ok(ApiResponse.success(BatchJobResponse.from(job), "Job status retrieved."));
    }

    /**
     * Lists batch jobs for the current tenant with pagination.
     *
     * @param page     page number (0-based)
     * @param size     page size
     * @param tenantId tenant identifier
     * @return paginated list of batch jobs
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<Page<BatchJobResponse>>> listJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        Page<BatchJobResponse> jobs = jobRepository
                .findByTenantIdOrderByCreatedAtDesc(tenantId, PageRequest.of(page, size))
                .map(BatchJobResponse::from);

        return ResponseEntity.ok(ApiResponse.success(jobs, "Batch jobs retrieved."));
    }

    /**
     * Cancels a pending or processing batch job.
     *
     * @param jobId    the batch job UUID to cancel
     * @param tenantId tenant identifier
     * @return the updated job with CANCELLED status
     */
    @PostMapping("/{jobId}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<BatchJobResponse>> cancelJob(
            @PathVariable UUID jobId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        BatchJobResponse response = bedrockBatchService.cancelJob(jobId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(response, "Batch job cancelled."));
    }
}
