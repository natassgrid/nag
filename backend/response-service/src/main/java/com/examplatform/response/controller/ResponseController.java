package com.examplatform.response.controller;

import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.service.ResponseSaveService;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
