package com.examplatform.result.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

/**
 * Input DTO carrying a single candidate's raw scores for result computation.
 * Contains the raw total score, section-wise breakdown, and shift statistics
 * needed for normalization.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandidateScoreInput {

    private UUID candidateId;

    private UUID examId;

    private String shiftId;

    /**
     * Sum of all evaluation scores for this candidate.
     */
    private double totalRawScore;

    /**
     * Section name → score mapping.
     */
    private Map<String, Double> sectionScores;

    /**
     * Mean score of all candidates in this candidate's shift (for normalization).
     * 0 if not applicable.
     */
    private double shiftMean;

    /**
     * Standard deviation of scores in this candidate's shift (for normalization).
     * 0 if not applicable.
     */
    private double shiftStdDev;
}
