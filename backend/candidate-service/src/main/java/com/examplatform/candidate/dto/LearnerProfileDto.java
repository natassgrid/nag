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

package com.examplatform.candidate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearnerProfileDto(
        UUID candidateId,
        String displayName,
        String educationLevel,
        String preferredLanguage,
        Integer dailyStudyMinutes,
        List<String> preferredLessonTypes,
        String priorExperience,
        LocalDate targetDate,
        Instant completedAt,
        boolean isComplete
) {}
