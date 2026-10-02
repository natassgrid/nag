// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.util.UUID;

public record LearnerProfileDto(
    UUID candidateId,
    int totalPracticeSessions,
    double overallAccuracy,
    String weakTopics,
    String strongTopics,
    String topicAccuracyMap
) {}
