package com.examplatform.response.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Request payload for saving a candidate's response to a question.
 *
 * Validates: Requirements 10.1, 20.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveResponseRequest {

    /**
     * The question being answered.
     */
    @NotNull(message = "questionId is required")
    private UUID questionId;

    /**
     * JSON array of selected option IDs (for MCQ/MRQ questions).
     * Nullable for non-MCQ question types.
     */
    private String selectedOptionIds;

    /**
     * Free-text or computed value entered by the candidate.
     * Nullable for MCQ question types.
     */
    private String enteredValue;

    /**
     * Client-side timestamp when this response was captured.
     */
    @NotNull(message = "timestamp is required")
    private Instant timestamp;

    /**
     * Total cumulative time the candidate has spent on this question (milliseconds).
     */
    @Min(value = 0, message = "cumulativeTimeSpentMs must be >= 0")
    private long cumulativeTimeSpentMs;

    /**
     * Source of the save action. Must be one of: AUTO, MANUAL, NAVIGATION, OFFLINE.
     */
    @NotBlank(message = "saveSource is required")
    @Pattern(regexp = "AUTO|MANUAL|NAVIGATION|OFFLINE", message = "saveSource must be one of: AUTO, MANUAL, NAVIGATION, OFFLINE")
    private String saveSource;

    /**
     * Client-assigned revision sequence for offline reconciliation.
     * Used during bulk-save to determine which responses are new vs already persisted.
     * Optional for regular saves (server auto-increments), required for bulk-save reconciliation.
     */
    private Integer revisionSequence;
}
