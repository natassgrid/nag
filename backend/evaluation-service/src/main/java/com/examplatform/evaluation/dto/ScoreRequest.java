package com.examplatform.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for recording a manual evaluation score.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRequest {

    @NotNull(message = "evaluatorId is required")
    private UUID evaluatorId;

    @NotNull(message = "score is required")
    @PositiveOrZero(message = "score must be zero or positive")
    private Double score;

    private String comments;
}
