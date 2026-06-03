package com.examplatform.delivery.controller;

import com.examplatform.delivery.dto.NavigationRequest;
import com.examplatform.delivery.dto.NavigationResponse;
import com.examplatform.delivery.service.NavigationService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for exam session navigation.
 * Handles navigation requests within active exam sessions,
 * enforcing navigation policy rules per session configuration.
 *
 * Validates: Requirements 9.2, 9.5
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class NavigationController {

    private final NavigationService navigationService;

    /**
     * Navigate within an active exam session.
     * Validates the navigation against the session's policy (Sequential, Flexible, Restricted).
     * Returns 422 if the navigation violates the policy.
     *
     * @param sessionId the session to navigate within
     * @param request   the navigation request with target indices
     * @param jwt       the authenticated candidate's JWT
     * @return navigation response with new position and allowed actions
     */
    @PostMapping("/{sessionId}/navigate")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<NavigationResponse> navigate(
            @PathVariable UUID sessionId,
            @Valid @RequestBody NavigationRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID candidateId = UUID.fromString(jwt.getSubject());
        String tenantId = jwt.getClaimAsString("tenant_id");

        // Ensure path variable matches request body
        request.setSessionId(sessionId);

        log.info("Navigation request: session={}, candidate={}, targetQuestion={}, targetSection={}",
                sessionId, candidateId, request.getTargetQuestionIndex(), request.getTargetSectionIndex());

        NavigationResponse response = navigationService.navigate(request, candidateId, tenantId);

        return ResponseEntity.ok(response);
    }
}
