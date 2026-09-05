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

import com.examplatform.questionbank.translation.dto.AutoTranslateResponse;
import com.examplatform.questionbank.translation.dto.TranslationRequest;
import com.examplatform.questionbank.translation.dto.TranslationResponse;
import com.examplatform.questionbank.translation.dto.TranslationReviewRequest;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.service.IndicTrans2Service;
import com.examplatform.questionbank.translation.service.TranslationQueryService;
import com.examplatform.questionbank.translation.service.TranslationReviewService;
import com.examplatform.questionbank.translation.service.TranslationWorkflowService;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for question translation workflow and retrieval endpoints.
 *
 * <h3>Write endpoints</h3>
 * <ul>
 *   <li>POST   /api/v1/translations                                              — submit new translation</li>
 *   <li>PUT    /api/v1/translations/{id}                                         — resubmit after rejection</li>
 *   <li>POST   /api/v1/translations/{id}/approve                                 — approve</li>
 *   <li>POST   /api/v1/translations/{id}/reject                                  — reject with comments</li>
 *   <li>POST   /api/v1/translations/question/{questionId}/auto-translate/{lang}  — auto-translate using IndicTrans2</li>
 * </ul>
 *
 * <h3>Read endpoints</h3>
 * <ul>
 *   <li>GET /api/v1/translations/question/{questionId}                           — all translations (admin)</li>
 *   <li>GET /api/v1/translations/question/{questionId}/language/{lang}           — approved translation for delivery</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/translations")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationWorkflowService translationWorkflowService;
    private final TranslationReviewService translationReviewService;
    private final TranslationQueryService translationQueryService;
    private final IndicTrans2Service indicTrans2Service;

    // -------------------------------------------------------------------------
    // Auto-Translate via IndicTrans2
    // -------------------------------------------------------------------------

    /**
     * Auto-translate a question into the specified target language using IndicTrans2.
     * POST /api/v1/translations/question/{questionId}/auto-translate/{lang}
     */
    @PostMapping("/question/{questionId}/auto-translate/{lang}")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'ADMIN')")
    public ResponseEntity<AutoTranslateResponse> autoTranslate(
            @PathVariable UUID questionId,
            @PathVariable String lang) {

        AutoTranslateResponse response = indicTrans2Service.autoTranslateQuestion(questionId, lang);
        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Write endpoints
    // -------------------------------------------------------------------------

    /**
     * Submit a new translation (content + options + explanation) for a question.
     * POST /api/v1/translations
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> requestTranslation(
            @Valid @RequestBody TranslationRequest request) {

        String tenantId = tenantId();
        Translation translation = translationWorkflowService.requestTranslation(request, tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "translationId", translation.getId(),
                "status", translation.getStatus().name(),
                "message", "Translation request created successfully"
        ));
    }

    /**
     * Resubmit a translation after rejection.
     * PUT /api/v1/translations/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> resubmitTranslation(
            @PathVariable UUID id,
            @Valid @RequestBody TranslationRequest request) {

        String tenantId = tenantId();
        Translation translation = translationWorkflowService.resubmitTranslation(id, request, tenantId);

        return ResponseEntity.ok(Map.of(
                "translationId", translation.getId(),
                "status", translation.getStatus().name(),
                "message", "Translation resubmitted successfully"
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

        UUID reviewerId = UUID.fromString(body.get("reviewerId"));
        Translation translation = translationReviewService.approve(id, reviewerId, tenantId());

        return ResponseEntity.ok(Map.of(
                "translationId", translation.getId(),
                "status", translation.getStatus().name(),
                "message", "Translation approved successfully"
        ));
    }

    /**
     * Reject a translation with mandatory reviewer comments.
     * POST /api/v1/translations/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('REVIEWER', 'EXAM_CONTROLLER')")
    public ResponseEntity<Map<String, Object>> rejectTranslation(
            @PathVariable UUID id,
            @Valid @RequestBody TranslationReviewRequest request) {

        translationReviewService.reject(id, request.getReviewerId(), request.getComments(), tenantId());

        return ResponseEntity.ok(Map.of(
                "translationId", id,
                "status", "DRAFT",
                "message", "Translation rejected; translator notified"
        ));
    }

    // -------------------------------------------------------------------------
    // Read endpoints
    // -------------------------------------------------------------------------

    /**
     * List all translations for a question (all languages and statuses).
     * Used by the admin / translator dashboard.
     * GET /api/v1/translations/question/{questionId}
     */
    @GetMapping("/question/{questionId}")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'ADMIN')")
    public ResponseEntity<List<TranslationResponse>> listTranslations(
            @PathVariable UUID questionId) {

        List<TranslationResponse> translations =
                translationQueryService.listTranslationsForQuestion(questionId, tenantId());
        return ResponseEntity.ok(translations);
    }

    /**
     * Fetch the approved translation for a specific question and language.
     * Used by the delivery service when serving a localized exam to a candidate.
     * Returns 404 if no approved translation exists yet.
     * GET /api/v1/translations/question/{questionId}/language/{lang}
     */
    @GetMapping("/question/{questionId}/language/{lang}")
    @PreAuthorize("hasAnyRole('TRANSLATOR', 'REVIEWER', 'EXAM_CONTROLLER', 'ADMIN', 'DELIVERY_SERVICE')")
    public ResponseEntity<TranslationResponse> getApprovedTranslation(
            @PathVariable UUID questionId,
            @PathVariable String lang) {

        return translationQueryService
                .getApprovedTranslation(questionId, lang, tenantId())
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No approved translation found for question " + questionId
                                + " in language " + lang));
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    private String tenantId() {
        String t = TenantContext.get();
        return t != null ? t : "default";
    }
}
