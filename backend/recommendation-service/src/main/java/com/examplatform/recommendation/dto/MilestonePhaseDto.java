// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestonePhaseDto(
        int phaseNumber,
        String phaseName,
        String targetDescription,
        int durationWeeks,
        String status, // IN_PROGRESS, LOCKED, COMPLETED
        Double targetAccuracy,
        Double achievedAccuracy,
        UUID quizSessionId,
        Double passThreshold,
        UUID gatekeeperSessionId,
        Boolean passed,
        List<MilestoneItemDto> items
) implements Serializable {

    public UUID getQuizSessionId() {
        return quizSessionId;
    }

    public UUID getGatekeeperSessionId() {
        return gatekeeperSessionId;
    }

    public String getStatus() {
        return status;
    }

    public MilestonePhaseDto(
            int phaseNumber,
            String phaseName,
            String targetDescription,
            String status,
            List<MilestoneItemDto> items) {
        this(
                phaseNumber,
                phaseName,
                targetDescription,
                phaseNumber == 1 ? 2 : 1,
                status,
                phaseNumber == 2 ? 70.0 : null,
                null,
                null,
                phaseNumber == 3 ? 75.0 : null,
                null,
                phaseNumber == 3 ? false : null,
                items
        );
    }
}
