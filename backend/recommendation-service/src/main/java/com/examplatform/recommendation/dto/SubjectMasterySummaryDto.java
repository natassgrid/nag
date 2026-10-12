// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubjectMasterySummaryDto(
        String subjectName,
        int totalSubtopics,
        int conceptualGapsCount,
        int applicationGapsCount,
        int proficientCount,
        double avgAccuracyPercent
) implements Serializable {}
