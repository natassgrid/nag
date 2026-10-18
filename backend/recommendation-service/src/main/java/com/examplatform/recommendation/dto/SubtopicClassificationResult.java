// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import com.examplatform.recommendation.domain.PerformanceBracket;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;

/**
 * Result of evaluating a sub-topic against the three-tier rule engine
 * with pace ratio and skip rate modifiers.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubtopicClassificationResult(
        Long subjectId,
        String subjectName,
        Long topicId,
        String topicName,
        Long subtopicId,
        String subtopicName,
        PerformanceBracket bracket,
        double accuracyPercent,
        double paceRatio,
        double skipRatePercent,
        boolean slowPace,
        String pedagogicalAction,
        String recommendationNote
) implements Serializable {}
