package com.examplatform.result.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request body for triggering result computation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComputeResultsRequest {

    @NotNull
    private UUID examId;

    @NotEmpty
    @Valid
    private List<CandidateScoreInput> candidateScores;

    @Builder.Default
    private boolean normalizeShifts = false;
}
