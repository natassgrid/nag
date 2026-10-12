// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPathSummaryResponse(
        UUID candidateId,
        UUID activePathId,
        String examTarget,
        String status,
        int totalSubtopics,
        int conceptualGapsCount,
        int applicationGapsCount,
        int proficientCount,
        double masteryPercentage,
        Map<String, SubjectMasterySummaryDto> subjectSummaries
) implements Serializable {}
