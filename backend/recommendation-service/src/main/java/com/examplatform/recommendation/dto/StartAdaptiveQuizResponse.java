// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.dto;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record StartAdaptiveQuizResponse(
        UUID quizSessionId,
        UUID practiceSetId,
        String practiceLauncherUrl,
        String currentPhase,
        double targetAccuracy,
        int questionCount,
        List<String> targetSubtopics,
        String message
) implements Serializable {
    public StartAdaptiveQuizResponse(
            UUID quizSessionId,
            String currentPhase,
            double targetAccuracy,
            int questionCount,
            List<String> targetSubtopics,
            String message
    ) {
        this(quizSessionId, quizSessionId, "/practice/session/" + quizSessionId, currentPhase, targetAccuracy, questionCount, targetSubtopics, message);
    }
}
