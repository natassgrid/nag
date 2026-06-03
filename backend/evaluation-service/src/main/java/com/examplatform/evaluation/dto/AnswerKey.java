package com.examplatform.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * POJO representing an answer key entry for a single question.
 * Used during auto-evaluation to determine correct answers and marking scheme.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerKey {

    private UUID questionId;

    /**
     * Question type: SINGLE_MCQ, MULTI_MCQ, NUMERICAL
     */
    private String questionType;

    /**
     * For MCQ: JSON array of correct option IDs e.g. ["opt-2"]
     * For Numerical: the correct numeric value as a string e.g. "3.14"
     */
    private String correctAnswer;

    private double marksPerQuestion;

    /**
     * Negative marks to deduct for a wrong answer. 0 if no negative marking.
     */
    private double negativeMarks;
}
