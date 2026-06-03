package com.examplatform.delivery.controller;

import com.examplatform.delivery.dto.SessionStartRequest;
import com.examplatform.delivery.dto.SessionStartResponse;
import com.examplatform.delivery.service.SessionStartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * REST controller for exam session lifecycle operations.
 * Handles session start with JWT authentication and enforces the 500ms SLA.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private static final long SLA_WARNING_THRESHOLD_MS = 400;

    private final SessionStartService sessionStartService;

    /**
     * Start a new exam session for the authenticated candidate.
     * Extracts candidateId from the JWT subject claim and tenantId from the JWT tenant claim.
     *
     * @param request the session start request
     * @param jwt     the authenticated candidate's JWT
     * @return 201 Created with session details and first question
     */
    @PostMapping("/start")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<SessionStartResponse> startSession(
            @Valid @RequestBody SessionStartRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        Instant startTime = Instant.now();

        UUID candidateId = UUID.fromString(jwt.getSubject());
        String tenantId = jwt.getClaimAsString("tenant_id");

        SessionStartResponse response = sessionStartService.startSession(request, candidateId, tenantId);

        long elapsedMs = Duration.between(startTime, Instant.now()).toMillis();
        if (elapsedMs > SLA_WARNING_THRESHOLD_MS) {
            log.warn("Session start approaching 500ms SLA: took {}ms for candidate={}, exam={}, shift={}",
                    elapsedMs, candidateId, request.getExamId(), request.getShiftId());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
