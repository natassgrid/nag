// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.examplatform.recommendation.domain.PerformanceBracket;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MilestoneItemDto(
        String id,
        String subtopicName,
        String topicName,
        String subjectName,
        PerformanceBracket bracket,
        String actionType,
        String title,
        String description,
        Integer estimatedMinutes,
        boolean completed
) implements Serializable {}
