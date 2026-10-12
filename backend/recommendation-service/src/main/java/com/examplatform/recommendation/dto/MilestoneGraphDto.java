// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestoneGraphDto(
        List<MilestonePhaseDto> phases,
        List<SubtopicClassificationResult> conceptualGaps,
        List<SubtopicClassificationResult> applicationGaps,
        List<SubtopicClassificationResult> proficientTopics,
        int totalSubtopicsEvaluated,
        double overallReadinessScore
) implements Serializable {}
