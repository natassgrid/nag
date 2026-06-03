package com.examplatform.papergenerator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single rule within a paper generation blueprint.
 * Specifies the subject, topic, difficulty, cognitive level, and
 * the number of questions to select matching these criteria.
 *
 * Validates: Requirements 8.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlueprintRule {

    private String subject;

    private String topic;

    /**
     * Difficulty filter: EASY, MEDIUM, HARD, or null for any difficulty.
     */
    private String difficulty;

    /**
     * Cognitive level filter, or null for any cognitive level.
     */
    private String cognitiveLevel;

    private int questionCount;
}
