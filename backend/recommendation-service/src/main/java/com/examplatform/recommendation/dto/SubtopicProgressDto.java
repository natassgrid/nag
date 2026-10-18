/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.recommendation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubtopicProgressDto(
        Long subjectId,
        String subjectName,
        Long topicId,
        String topicName,
        Long subtopicId,
        String subtopicName,
        double masteryScore,
        double confidence,
        int attempts,
        int correct,
        String status,
        Instant nextReviewAt,
        boolean isReviewDue,
        double blueprintWeight
) {}
