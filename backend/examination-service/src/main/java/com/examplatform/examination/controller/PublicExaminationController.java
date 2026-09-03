/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.controller;

import com.examplatform.examination.dto.AdmitCardResponse;
import com.examplatform.examination.dto.ExamApplicationRequest;
import com.examplatform.examination.dto.ExamApplicationResponse;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.dto.PublicCentreResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
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
 *   <li>Public examination centre directory</li>
 *   <li>Candidate multi-step application with centre & shift preferences</li>
 *   <li>Candidate applications list</li>
 *   <li>Hall Ticket / Admit Card retrieval</li>
 * </ul>
 *
 * Validates: Requirements 1.6, 6.1, 7b.5
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
     */
    @GetMapping("/public")
    public ResponseEntity<ApiResponse<Page<ExaminationResponse>>> listPublished(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.debug("Public exam listing request: search={}, page={}, size={}, tenant={}", search, page, size, tenantId);

        Page<ExaminationResponse> responses = examinationService.listPublishedPaged(tenantId, search, page, size);

        return ResponseEntity.ok(ApiResponse.success(responses, "Published examinations retrieved successfully"));
    }

    /**
     * Get a single published examination by ID.
     * Publicly accessible.
     */
    @GetMapping("/public/{examId}")
    public ResponseEntity<ApiResponse<ExaminationResponse>> getPublishedById(
            @PathVariable UUID examId) {

        ExaminationResponse response = examinationService.getById(examId);
        return ResponseEntity.ok(ApiResponse.success(response, "Examination retrieved successfully"));
    }

    /**
     * List active examination centres available across India for candidate preference selection.
     * Publicly accessible.
     */
    @GetMapping("/centres/public")
    public ResponseEntity<ApiResponse<List<PublicCentreResponse>>> listPublicCentres(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city) {

        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";
        List<PublicCentreResponse> centres = examApplicationService.listPublicCentres(tenantId, state, city);
        return ResponseEntity.ok(ApiResponse.success(centres, "Examination centres retrieved successfully"));
    }

    /**
     * Apply the authenticated candidate for a specific examination with preferences.
     * Returns 409 Conflict if already applied.
     */
    @PostMapping("/{examId}/apply")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ExamApplicationResponse>> apply(
            @PathVariable UUID examId,
            @RequestBody(required = false) ExamApplicationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID candidateId = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        log.info("Exam application: candidate={}, exam={}, tenant={}", candidateId, examId, tenantId);

        try {
            ExamApplicationResponse response = examApplicationService.apply(examId, candidateId, tenantId, request);
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

    /**
     * Get the Admit Card / Hall Ticket for the authenticated candidate by Exam ID.
     */
    @GetMapping("/{examId}/admit-card")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<AdmitCardResponse>> getAdmitCard(
            @PathVariable UUID examId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID candidateId = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        AdmitCardResponse admitCard = examApplicationService.getAdmitCard(examId, candidateId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(admitCard, "Admit card retrieved successfully"));
    }

    /**
     * Get the Admit Card / Hall Ticket for an application by Application ID.
     */
    @GetMapping("/applications/{applicationId}/admit-card")
    @PreAuthorize("hasAnyRole('CANDIDATE', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AdmitCardResponse>> getAdmitCardByApplicationId(
            @PathVariable UUID applicationId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID candidateId = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        AdmitCardResponse admitCard = examApplicationService.getAdmitCardByApplicationId(applicationId, candidateId, tenantId);
        return ResponseEntity.ok(ApiResponse.success(admitCard, "Admit card retrieved successfully"));
    }
}
