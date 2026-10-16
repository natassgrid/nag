// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record DynamicLearningPathResponse(
        UUID id,
        UUID candidateId,
        UUID sourceSessionId,
        String examTarget,
        String status,
        int conceptualGapsCount,
        int applicationGapsCount,
        int proficientCount,
        MilestoneGraphDto milestoneGraph,
        Instant createdAt,
        Instant updatedAt
) implements Serializable {

    @JsonProperty("targetExam")
    public String getTargetExam() {
        return examTarget;
    }

    @JsonProperty("gatekeeperPassed")
    public boolean isGatekeeperPassed() {
        return milestoneGraph != null && milestoneGraph.isGatekeeperPassed();
    }
}
