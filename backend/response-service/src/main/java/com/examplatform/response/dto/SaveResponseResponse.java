package com.examplatform.response.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload returned after a successful response save.
 *
 * Validates: Requirements 10.1, 20.3
 */
@Data
@Builder
public class SaveResponseResponse {

    private UUID responseId;
    private UUID sessionId;
    private UUID questionId;
    private int revisionSequence;
    private String saveSource;
    private Instant savedAt;
}
