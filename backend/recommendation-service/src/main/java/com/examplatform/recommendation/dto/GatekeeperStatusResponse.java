// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record GatekeeperStatusResponse(
        boolean gatekeeperRequired,
        boolean clearedForFullMock,
        String currentPhase,
        UUID gatekeeperSessionId,
        int remainingMilestonesCount,
        List<String> remedialSubtopics,
        List<String> conceptualGaps,
        List<String> applicationGaps
) implements Serializable {

    public GatekeeperStatusResponse(
            boolean gatekeeperRequired,
            boolean clearedForFullMock,
            String currentPhase,
            UUID gatekeeperSessionId,
            int remainingMilestonesCount,
            List<String> remedialSubtopics) {
        this(
                gatekeeperRequired,
                clearedForFullMock,
                currentPhase,
                gatekeeperSessionId,
                remainingMilestonesCount,
                remedialSubtopics,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }
}
