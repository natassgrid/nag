// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
) implements Serializable {}
