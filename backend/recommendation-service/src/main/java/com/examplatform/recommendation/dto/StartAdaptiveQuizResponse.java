// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record StartAdaptiveQuizResponse(
        UUID quizSessionId,
        String currentPhase,
        double targetAccuracy,
        int questionCount,
        List<String> targetSubtopics,
        String message
) implements Serializable {}
