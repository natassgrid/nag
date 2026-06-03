package com.examplatform.delivery.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response returned after a session is successfully started.
 * Contains the first question content (decrypted in memory) to meet the 500ms SLA.
 */
@Data
@Builder
public class SessionStartResponse {

    private UUID sessionId;
    private UUID examId;
    private UUID shiftId;
    private Instant startedAt;
    private Instant scheduledEndAt;
    private String firstQuestionContent;
    private int totalQuestions;
}
