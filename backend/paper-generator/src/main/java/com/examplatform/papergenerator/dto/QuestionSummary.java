package com.examplatform.papergenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary DTO representing a question retrieved from the Question Bank.
 * Used during paper assembly to evaluate reuse policies and compute
 * difficulty scores.
 *
 * Validates: Requirements 8.3, 8.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSummary {

    private UUID questionId;

    private String subject;

    private String topic;

    /**
     * Difficulty level: EASY, MEDIUM, or HARD.
     */
    private String difficulty;

    private String cognitiveLevel;

    private int usageCount;

    private Instant lastUsedAt;

    /**
     * Reuse policy: NEVER, 1_YEAR, 2_YEARS, or CUSTOM.
     */
    private String reusePolicy;
}
