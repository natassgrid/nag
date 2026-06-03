package com.examplatform.result.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * DTO representing per-question analytics for an exam.
 * Includes difficulty index, discrimination index, and response distribution.
 *
 * Validates: Requirements 26.1, 26.5
 */
@Data
@Builder
public class QuestionAnalyticsResult {

    /**
     * The question identifier.
     */
    private UUID questionId;

    /**
     * Difficulty index: proportion of candidates who answered correctly.
     * Range: 0.0 (hardest) to 1.0 (easiest).
     */
    private double difficultyIndex;

    /**
     * Discrimination index: difference between top 27% and bottom 27% correct rates.
     * Range: -1.0 to 1.0. Higher values indicate better discrimination.
     */
    private double discriminationIndex;

    /**
     * Response distribution: count per option selected.
     * Key is the option identifier (e.g., "A", "B", "C", "D"),
     * value is the number of candidates who selected that option.
     */
    private Map<String, Integer> responseDistribution;
}
