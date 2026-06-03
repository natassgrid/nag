package com.examplatform.delivery.controller;

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.dto.SessionStatusResponse;
import com.examplatform.delivery.service.SessionTimerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.UUID;

/**
 * REST controller for querying session status including time remaining
 * and fullscreen lock state. Accessible by CANDIDATE role only.
 *
 * Validates: Requirements 9.3, 9.6, 9.8, 22.6
 */
@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionStatusController {

    private final SessionTimerService sessionTimerService;

    /**
     * Returns the current session status, remaining time, and fullscreen lock state.
     *
     * @param sessionId the exam session ID
     * @return session status response
     */
    @GetMapping("/{sessionId}/status")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<SessionStatusResponse> getSessionStatus(@PathVariable UUID sessionId) {
        ExamSession session = sessionTimerService.getSession(sessionId);
        Duration remaining = sessionTimerService.getTimeRemaining(sessionId);

        long timeRemainingSeconds = Math.max(0, remaining.getSeconds());

        SessionStatusResponse response = SessionStatusResponse.builder()
                .sessionId(session.getSessionId())
                .status(session.getStatus().name())
                .timeRemainingSeconds(timeRemainingSeconds)
                .fullScreenLocked(session.getFullScreenExitCount() != null && session.getFullScreenExitCount() > 0)
                .build();

        return ResponseEntity.ok(response);
    }
}
