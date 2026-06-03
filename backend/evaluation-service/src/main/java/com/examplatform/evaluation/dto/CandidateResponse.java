package com.examplatform.evaluation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * POJO representing a candidate's finalized response to a single question.
 * Used as input to the auto-evaluation pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateResponse {

    private UUID questionId;

    /**
     * JSON array of selected option IDs (for MCQ questions).
     * e.g. ["opt-1", "opt-3"]
     */
    private String selectedOptionIds;

    /**
     * The value entered by the candidate (for numerical questions).
     */
    private String enteredValue;

    /**
     * False if both selectedOptionIds and enteredValue are null/blank
     * (i.e. the candidate did not attempt this question).
     */
    private boolean attempted;
}
