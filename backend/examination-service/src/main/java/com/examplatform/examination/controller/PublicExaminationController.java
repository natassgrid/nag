/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.controller;

import com.examplatform.examination.dto.ExamApplicationResponse;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.service.ExamApplicationService;
import com.examplatform.examination.service.ExaminationService;
import com.examplatform.shared.api.ApiResponse;
import com.examplatform.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for candidate-facing examination endpoints.
 *
 * Provides:
 * <ul>
 *   <li>Public exam listing (no auth required)</li>
 *   <li>Candidate apply for exam</li>
 *   <li>Candidate view their applications</li>
 * </ul>
 *
 * Validates: Requirements 1.6, 6.1
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/examinations")
@RequiredArgsConstructor
public class PublicExaminationController {

    private final ExaminationService examinationService;
    private final ExamApplicationService examApplicationService;

    /**
     * List all PUBLISHED examinations.
     * This endpoint is publicly accessible — no authentication required.
     * Candidates can browse before registering.
     *
     * @param search optional search keyword matched against exam name
     * @param page   zero-based page number (default: 0)
     * @param size   page size (default: 20)
     * @return paginated list of published examinations
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<ExaminationResponse>>> listPublished(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Use TenantContext if available; fall back to "default" for unauthenticated callers
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.debug("Public exam listing request: search={}, page={}, size={}, tenant={}", search, page, size, tenantId);

        Page<ExaminationResponse> responses = examinationService.listPublishedPaged(tenantId, search, page, size);

        return ResponseEntity.ok(ApiResponse.success(responses, "Published examinations retrieved successfully"));
    }

    /**
     * Get a single published examination by ID.
     * Publicly accessible.
     *
     * @param examId the examination UUID
     * @return the examination response
     */
    @GetMapping("/public/{examId}")
    public ResponseEntity<ApiResponse<ExaminationResponse>> getPublishedById(
            @PathVariable UUID examId) {

        ExaminationResponse response = examinationService.getById(examId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination retrieved successfully"));
    }

    /**
     * Apply the authenticated candidate for a specific examination.
     * Returns 409 Conflict if already applied.
     *
     * @param examId the examination UUID
     * @param jwt    the authenticated candidate's JWT
     * @return the created application
     */
    @PostMapping("/{examId}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ExamApplicationResponse>> apply(
            @PathVariable UUID examId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID candidateId = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.info("Exam application: candidate={}, exam={}, tenant={}", candidateId, examId, tenantId);

        try {
            ExamApplicationResponse response = examApplicationService.apply(examId, candidateId, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Application submitted successfully"));
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error("Already applied for this examination"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * List all examinations the authenticated candidate has applied for.
     *
     * @param jwt the authenticated candidate's JWT
     * @return list of the candidate's applications
     */
    @GetMapping("/my-exams")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<ExamApplicationResponse>>> getMyExams(
            @AuthenticationPrincipal Jwt jwt) {

        UUID candidateId = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.debug("My exams request: candidate={}, tenant={}", candidateId, tenantId);

        List<ExamApplicationResponse> applications = examApplicationService.getMyApplications(candidateId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(applications, "Applications retrieved successfully"));
    }
}
