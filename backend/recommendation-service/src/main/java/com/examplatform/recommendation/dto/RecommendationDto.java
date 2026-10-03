// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.time.Instant;
import java.util.UUID;

public record RecommendationDto(
    UUID id,
    UUID candidateId,
    UUID triggerSessionId,
    String status,
    Instant generatedAt,
    String weakTopicRecommendations,
    String studyPlanItems,
    String suggestedPracticeSetIds,
    String motivationalMessage
) {}
