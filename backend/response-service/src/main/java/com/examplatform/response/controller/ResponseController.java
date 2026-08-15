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

package com.examplatform.response.controller;

import com.examplatform.response.domain.Response;
import com.examplatform.response.dto.BulkSaveRequest;
import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.service.BulkSaveService;
import com.examplatform.response.service.ResponseHistoryService;
import com.examplatform.response.service.ResponseSaveService;
import com.examplatform.response.service.SessionFinalizationService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for candidate response operations.
 * Provides the save endpoint used by the exam delivery frontend.
 *
 * Validates: Requirements 10.1, 20.3
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/responses")
@RequiredArgsConstructor
public class ResponseController {

    private final ResponseSaveService responseSaveService;
    private final BulkSaveService bulkSaveService;
    private final ResponseHistoryService responseHistoryService;
    private final SessionFinalizationService sessionFinalizationService;

    /**
     * Save a candidate's response to a question within an active exam session.
     * Persists the response and waits for Kafka acks=all before returning 200 OK.
     * Logs a warning if processing exceeds 150ms (approaching 200ms p99 SLA).
     *
     * @param sessionId the exam session UUID
     * @param request   the save request payload (validated)
     * @param jwt       the authenticated JWT principal (candidate)
     * @param tenantId  tenant identifier from X-Tenant-Id header
     * @return 200 OK with the save confirmation
     */
    @PostMapping("/{sessionId}/save")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<SaveResponseResponse>> saveResponse(
            @PathVariable UUID sessionId,
            @Valid @RequestBody SaveResponseRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        long startTime = System.currentTimeMillis();

        UUID candidateId = UUID.fromString(jwt.getSubject());

        log.debug("Saving response: sessionId={}, questionId={}, candidate={}, tenant={}",
                sessionId, request.getQuestionId(), candidateId, tenantId);

        SaveResponseResponse response = responseSaveService.saveResponse(
                sessionId, request, candidateId, tenantId);

        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed > 150) {
            log.warn("Response save approaching SLA limit: {}ms (sessionId={}, questionId={})",
                    elapsed, sessionId, request.getQuestionId());
        }

        return ResponseEntity.ok(ApiResponse.success(response, "Response saved successfully"));
    }

    /**
     * Bulk-save offline-buffered responses with deduplication.
     * Reconciles each response against server-side revision state:
     * new responses are persisted, already-saved ones are skipped.
     *
     * @param sessionId the exam session UUID
     * @param request   the bulk save request with ordered responses
     * @param jwt       the authenticated JWT principal (candidate)
     * @param tenantId  tenant identifier from X-Tenant-Id header
     * @return 200 OK with list of save confirmations for newly persisted responses
     */
    @PostMapping("/{sessionId}/bulk-save")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<SaveResponseResponse>>> bulkSave(
            @PathVariable UUID sessionId,
            @Valid @RequestBody BulkSaveRequest request,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID candidateId = UUID.fromString(jwt.getSubject());

        log.debug("Bulk-saving responses: sessionId={}, candidate={}, count={}, tenant={}",
                sessionId, candidateId, request.getResponses().size(), tenantId);

        List<SaveResponseResponse> results = bulkSaveService.bulkSave(
                sessionId, request, candidateId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(results, "Bulk save completed"));
    }

    /**
     * Submit (finalize) an exam session: marks all responses as final and
     * locks the response set against further modifications.
     *
     * @param sessionId the exam session UUID
     * @param jwt       the authenticated JWT principal (candidate)
     * @param tenantId  tenant identifier from X-Tenant-Id header
     * @return 200 OK confirmation of submission
     */
    @PostMapping("/{sessionId}/submit")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Void>> submitSession(
            @PathVariable UUID sessionId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        UUID candidateId = UUID.fromString(jwt.getSubject());

        log.debug("Submitting session: sessionId={}, candidate={}, tenant={}",
                sessionId, candidateId, tenantId);

        sessionFinalizationService.submitSession(sessionId, candidateId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(null, "Session submitted successfully"));
    }

    /**
     * Retrieves full revision history for all responses in an exam session.
     * Ordered by questionId + revisionSequence for audit/evaluation purposes.
     *
     * @param sessionId the exam session UUID
     * @param tenantId  tenant identifier from X-Tenant-Id header
     * @return 200 OK with all responses for the session
     */
    @GetMapping("/{sessionId}/responses")
    @PreAuthorize("hasAnyRole('EVALUATOR', 'AUDITOR')")
    public ResponseEntity<ApiResponse<List<Response>>> getSessionResponses(
            @PathVariable UUID sessionId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.debug("Retrieving session responses: sessionId={}, tenant={}", sessionId, tenantId);

        List<Response> responses = responseHistoryService.getSessionResponses(sessionId, tenantId);

        return ResponseEntity.ok(ApiResponse.success(responses, "Session responses retrieved"));
    }
}
