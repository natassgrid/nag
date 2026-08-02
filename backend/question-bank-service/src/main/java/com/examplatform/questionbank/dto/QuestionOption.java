package com.examplatform.questionbank.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single answer option for MCQ/MSQ questions.
 * Option IDs are assigned A-F based on position in the list.
 *
 * Validates: Requirement 30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {

    /** Option identifier: A, B, C, D, E, or F */
    private String id;

    /** The visible text of this option */
    @NotBlank(message = "Option text must not be blank")
    private String text;

    /** True if this option is part of the correct answer */
    @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
    private boolean correct;
}
