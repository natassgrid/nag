// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestoneGraphDto(
        String currentPhase,
        String targetExam,
        boolean gatekeeperPassed,
        List<MilestonePhaseDto> phases,
        List<SubtopicClassificationResult> conceptualGaps,
        List<SubtopicClassificationResult> applicationGaps,
        List<SubtopicClassificationResult> proficientTopics,
        int totalSubtopicsEvaluated,
        double overallReadinessScore
) implements Serializable {

    public MilestoneGraphDto(
            List<MilestonePhaseDto> phases,
            List<SubtopicClassificationResult> conceptualGaps,
            List<SubtopicClassificationResult> applicationGaps,
            List<SubtopicClassificationResult> proficientTopics,
            int totalSubtopicsEvaluated,
            double overallReadinessScore) {
        this(
                resolveInitialPhase(phases),
                "GENERAL_COMPETITIVE",
                phases != null && phases.stream().allMatch(p -> "COMPLETED".equalsIgnoreCase(p.status())),
                phases,
                conceptualGaps,
                applicationGaps,
                proficientTopics,
                totalSubtopicsEvaluated,
                overallReadinessScore
        );
    }

    private static String resolveInitialPhase(List<MilestonePhaseDto> phases) {
        if (phases == null || phases.isEmpty()) return "COMPLETED";
        for (MilestonePhaseDto p : phases) {
            if ("IN_PROGRESS".equalsIgnoreCase(p.status())) {
                return "PHASE_" + p.phaseNumber() + "_" + (p.phaseNumber() == 1 ? "REMEDIATION" : p.phaseNumber() == 2 ? "ADAPTIVE_QUIZ" : "GATEKEEPER");
            }
        }
        return "COMPLETED";
    }
}
