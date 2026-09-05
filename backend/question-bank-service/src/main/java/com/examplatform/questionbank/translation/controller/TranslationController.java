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

package com.examplatform.questionbank.translation.controller;

import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.TranslationRequest;
import com.examplatform.questionbank.translation.dto.TranslationReviewRequest;
import com.examplatform.questionbank.translation.service.TranslationReviewService;
import com.examplatform.questionbank.translation.service.TranslationWorkflowService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for question translation workflow endpoints.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/translations")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationWorkflowService translationWorkflowService;
    private final TranslationReviewService translationReviewService;

    /**
     * Request a new translation for a question.
     * POST /api/v1/translations
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> requestTranslation(
            @Valid @RequestBody TranslationRequest request) {

        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        Translation translation = translationWorkflowService.requestTranslation(
                request.getQuestionId(),
                request.getLanguageCode(),
                request.getTranslatorId(),
                tenantId
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "translationId", translation.getId(),
                "status", translation.getStatus().name(),
                "message", "Translation request created successfully"
        ));
    }

    /**
     * Approve a translation.
     * POST /api/v1/translations/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('REVIEWER', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> approveTranslation(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {

        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";
        UUID reviewerId = UUID.fromString(body.get("reviewerId"));

        Translation translation = translationReviewService.approve(id, reviewerId, tenantId);

        return ResponseEntity.ok(Map.of(
                "translationId", translation.getId(),
                "status", translation.getStatus().name(),
                "message", "Translation approved successfully"
        ));
    }

    /**
     * Reject a translation.
     * POST /api/v1/translations/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> rejectTranslation(
            @PathVariable UUID id,
            @Valid @RequestBody TranslationReviewRequest request) {

        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        translationReviewService.reject(id, request.getReviewerId(), request.getComments(), tenantId);

        return ResponseEntity.ok(Map.of(
                "translationId", id,
                "status", "REJECTED",
                "message", "Translation rejected; translator notified"
        ));
    }
}
