// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

public record StudyPlanItemDto(
    int day,
    String topicName,
    int estimatedMinutes
) {}
