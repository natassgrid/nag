package com.examplatform.questionbank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Analytics data for a question including difficulty index,
 * discrimination index, and usage statistics.
 *
 * Validates: Requirements 26.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionAnalytics {

    private double difficultyIndex;
    private double discriminationIndex;
    private int usageCount;
}
