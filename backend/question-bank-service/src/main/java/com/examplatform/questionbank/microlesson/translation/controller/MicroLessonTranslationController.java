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

package com.examplatform.questionbank.microlesson.translation.controller;

import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.translation.dto.*;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonBatchTranslationService;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationWorkflowService;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import com.examplatform.shared.tenant.TenantContext;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/microlessons", "/api/v1/micro-lessons"})
@RequiredArgsConstructor
@Tag(name = "Micro-Lesson Translation", description = "Endpoints for micro-lesson multilingual translation and batch jobs")
public class MicroLessonTranslationController {

    private final MicroLessonTranslationWorkflowService workflowService;
    private final MicroLessonBatchTranslationService batchService;

    /**
     * Auto-translates micro-lesson {id} into {lang} via IndicTrans2.
     * Returns 200 OK with MicroLessonTranslationResponse.
     */
    @PostMapping("/{id}/auto-translate/{lang}")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<MicroLessonTranslationResponse> autoTranslate(
            @PathVariable UUID id,
            @PathVariable String lang,
            @AuthenticationPrincipal Jwt jwt) {

        UUID translatorId = extractUserId(jwt);
        MicroLessonTranslationResponse response = workflowService.autoTranslateSingleLesson(
                id, lang, translatorId, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Triggers async batch translation job.
     * Returns 202 Accepted with MicroLessonBatchTranslationJobResponse.
     */
    @PostMapping("/batch/auto-translate")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<MicroLessonBatchTranslationJobResponse> triggerBatchJob(
            @Valid @RequestBody MicroLessonBatchTranslationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID initiatedBy = extractUserId(jwt);
        MicroLessonBatchTranslationJobResponse response = batchService.startBatchJob(
                request, initiatedBy, tenantId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Query batch translation job status and live progress.
     * Returns 200 OK with MicroLessonBatchTranslationJobResponse.
     */
    @GetMapping("/batch/{jobId}")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<MicroLessonBatchTranslationJobResponse> getBatchJobStatus(
            @PathVariable UUID jobId) {

        MicroLessonBatchTranslationJobResponse response = batchService.getJobStatus(jobId, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * List batch translation jobs with pagination.
     * Returns 200 OK with Page<MicroLessonBatchTranslationJobResponse>.
     */
    @GetMapping("/batch")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<Page<MicroLessonBatchTranslationJobResponse>> listBatchJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<MicroLessonBatchTranslationJobResponse> page = batchService.listJobs(tenantId(), pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Cancel a running batch job.
     * Returns 200 OK with MicroLessonBatchTranslationJobResponse.
     */
    @PostMapping("/batch/{jobId}/cancel")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<MicroLessonBatchTranslationJobResponse> cancelBatchJob(
            @PathVariable UUID jobId) {

        MicroLessonBatchTranslationJobResponse response = batchService.cancelJob(jobId, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Fetch approved/published localized micro-lesson for candidate consumption.
     * If lang is "en" or no PUBLISHED/APPROVED translation exists, falls back to English base lesson.
     */
    @GetMapping("/{id}/language/{lang}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MicroLessonResponse> getLocalizedMicroLesson(
            @PathVariable UUID id,
            @PathVariable String lang) {

        MicroLessonResponse response = workflowService.getLocalizedMicroLesson(id, lang, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * List all translations for a micro-lesson for administrative review.
     */
    @GetMapping("/{id}/translations")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<List<MicroLessonTranslationResponse>> getLessonTranslations(
            @PathVariable UUID id) {

        List<MicroLessonTranslationResponse> response = workflowService.getTranslationsForLesson(id, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Approve translation {id}.
     */
    @PostMapping("/translations/{id}/approve")
    @PreAuthorize("hasAnyRole('REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<MicroLessonTranslationResponse> approveTranslation(
            @PathVariable UUID id,
            @RequestBody(required = false) MicroLessonTranslationReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID reviewerId = extractUserId(jwt);
        String reviewComments = request != null ? request.getEffectiveComments() : null;
        MicroLessonTranslationResponse response = workflowService.approveTranslation(
                id, reviewerId, reviewComments, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Reject translation {id}.
     */
    @PostMapping("/translations/{id}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<MicroLessonTranslationResponse> rejectTranslation(
            @PathVariable UUID id,
            @Valid @RequestBody MicroLessonTranslationReviewRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID reviewerId = extractUserId(jwt);
        String reviewComments = request != null ? request.getEffectiveComments() : "";
        MicroLessonTranslationResponse response = workflowService.rejectTranslation(
                id, reviewerId, reviewComments, tenantId());
        return ResponseEntity.ok(response);
    }

    private String tenantId() {
        return TenantContext.getTenantIdOrDefault();
    }

    private UUID extractUserId(Jwt jwt) {
        return UserContext.resolveUserIdOrDefault(jwt);
    }
}
