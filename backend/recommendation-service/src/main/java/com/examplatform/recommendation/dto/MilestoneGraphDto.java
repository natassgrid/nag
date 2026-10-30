// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
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

    @JsonProperty("currentPhaseNumber")
    public int getCurrentPhaseNumber() {
        if (currentPhase == null) return 1;
        return switch (currentPhase) {
            case "DIAGNOSTIC_REQUIRED" -> 0;
            case "PHASE_1_REMEDIATION" -> 1;
            case "PHASE_2_ADAPTIVE_QUIZ" -> 2;
            case "PHASE_3_GATEKEEPER" -> 3;
            case SharedConstants.COMPLETED -> 4;
            default -> 1;
        };
    }

    public boolean isGatekeeperPassed() {
        return gatekeeperPassed;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    @JsonIgnore
    public MilestonePhaseDto getPhase(int number) {
        if (phases == null) return null;
        return phases.stream().filter(p -> p.phaseNumber() == number).findFirst().orElse(null);
    }

    @JsonIgnore
    public MilestonePhaseDto getPhase1() {
        return getPhase(1);
    }

    @JsonIgnore
    public MilestonePhaseDto getPhase2() {
        return getPhase(2);
    }

    @JsonIgnore
    public MilestonePhaseDto getPhase3() {
        return getPhase(3);
    }

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
                phases != null && phases.stream().allMatch(p -> SharedConstants.COMPLETED.equalsIgnoreCase(p.status())),
                phases,
                conceptualGaps,
                applicationGaps,
                proficientTopics,
                totalSubtopicsEvaluated,
                overallReadinessScore
        );
    }

    private static String resolveInitialPhase(List<MilestonePhaseDto> phases) {
        if (phases == null || phases.isEmpty()) return SharedConstants.COMPLETED;
        for (MilestonePhaseDto p : phases) {
            if (SharedConstants.IN_PROGRESS.equalsIgnoreCase(p.status())) {
                return "PHASE_" + p.phaseNumber() + "_" + (p.phaseNumber() == 1 ? "REMEDIATION" : p.phaseNumber() == 2 ? "ADAPTIVE_QUIZ" : "GATEKEEPER");
            }
        }
        return SharedConstants.COMPLETED;
    }
}
