package com.examplatform.delivery.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

/**
 * Response DTO for the session status endpoint.
 * Contains current session state, remaining time, and fullscreen lock state.
 *
 * Validates: Requirements 9.3, 9.6, 9.8, 22.6
 */
@Data
@Builder
public class SessionStatusResponse {

    private UUID sessionId;
    private String status;
    private long timeRemainingSeconds;
    private boolean fullScreenLocked;
}
