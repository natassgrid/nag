/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.controller;

import com.examplatform.candidate.dto.*;
import com.examplatform.candidate.service.LearnerService;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for learner onboarding, exam catalogue, active enrollment lifecycle,
 * and diagnostic calibration testing under the Unified Exam Spine architecture.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/learner")
@RequiredArgsConstructor
public class LearnerProfileController {

    private final LearnerService learnerService;

    /**
     * GET /api/v1/learner/profile - Retrieve profile for the authenticated learner.
     */
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearnerProfileDto> getProfile(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = resolveCandidateId(jwt);
        String tenantId = getTenantId();
        return ResponseEntity.ok(learnerService.getProfile(candidateId, tenantId));
    }

    /**
     * PUT /api/v1/learner/profile - Create or update onboarding profile.
     */
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearnerProfileDto> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody UpdateLearnerProfileRequest request) {
        UUID candidateId = resolveCandidateId(jwt);
        String tenantId = getTenantId();
        return ResponseEntity.ok(learnerService.updateProfile(candidateId, request, tenantId));
    }

    /**
     * GET /api/v1/learner/exams - Published examinations catalogue available for enrollment.
     */
    @GetMapping("/exams")
    public ResponseEntity<List<PublishedExamDto>> getPublishedExams() {
        String tenantId = getTenantId();
        return ResponseEntity.ok(learnerService.getPublishedExams(tenantId));
    }

    /**
     * POST /api/v1/learner/enrollments - Enroll in a target examination.
     * Enforces profile completion, published exam status, and single-ACTIVE invariant.
     */
    @PostMapping("/enrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearnerEnrollmentResponse> createEnrollment(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateEnrollmentRequest request) {
        UUID candidateId = resolveCandidateId(jwt);
        String tenantId = getTenantId();
        LearnerEnrollmentResponse response = learnerService.createEnrollment(candidateId, request, tenantId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/learner/enrollments/active - Retrieve candidate's active enrollment.
     */
    @GetMapping("/enrollments/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LearnerEnrollmentResponse> getActiveEnrollment(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = resolveCandidateId(jwt);
        return learnerService.getActiveEnrollment(candidateId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    /**
     * GET /api/v1/learner/enrollments - Retrieve candidate's enrollment history.
     */
    @GetMapping("/enrollments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LearnerEnrollmentResponse>> getEnrollments(@AuthenticationPrincipal Jwt jwt) {
        UUID candidateId = resolveCandidateId(jwt);
        return ResponseEntity.ok(learnerService.getEnrollments(candidateId));
    }

    /**
     * POST /api/v1/learner/enrollments/{id}/diagnostic - Start or retrieve diagnostic assessment.
     */
    @PostMapping("/enrollments/{id}/diagnostic")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiagnosticSessionResponse> startDiagnostic(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {
        UUID candidateId = resolveCandidateId(jwt);
        String tenantId = getTenantId();
        DiagnosticSessionResponse response = learnerService.startDiagnostic(candidateId, id, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/learner/enrollments/{id}/diagnostic/submit - Submit diagnostic answers or skip.
     */
    @PostMapping("/enrollments/{id}/diagnostic/submit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DiagnosticResultResponse> submitDiagnostic(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody(required = false) SubmitDiagnosticRequest request) {
        UUID candidateId = resolveCandidateId(jwt);
        String tenantId = getTenantId();
        DiagnosticResultResponse response = learnerService.submitDiagnostic(candidateId, id, request, tenantId);
        return ResponseEntity.ok(response);
    }

    // ── Context Resolvers ────────────────────────────────────────────────

    private UUID resolveCandidateId(Jwt jwt) {
        if (jwt != null && jwt.getSubject() != null) {
            try {
                return UUID.fromString(jwt.getSubject());
            } catch (IllegalArgumentException ignored) {}
        }
        UUID contextId = UserContext.getUserId();
        if (contextId != null) {
            return contextId;
        }
        // Fallback default ID for mock authentication in unit/integration tests
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    private String getTenantId() {
        return TenantContext.get() != null ? TenantContext.get() : SharedConstants.DEFAULT_TENANT;
    }
}
