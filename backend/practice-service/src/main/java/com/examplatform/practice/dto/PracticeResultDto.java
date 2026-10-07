// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import java.util.List;
import java.util.UUID;

public record PracticeResultDto(
    UUID sessionId,
    int correctCount,
    int incorrectCount,
    int skippedCount,
    int obtainedMarks,
    int totalMarks,
    double accuracyPercent,
    String topicWiseBreakdown,
    String difficultyBreakdown,
    String timingBreakdown,
    List<QuestionResultDto> questionResults,
    String practiceSetName,
    String mode,
    int flaggedCount
) {
    public PracticeResultDto(
        UUID sessionId,
        int correctCount,
        int incorrectCount,
        int skippedCount,
        int obtainedMarks,
        int totalMarks,
        double accuracyPercent,
        String topicWiseBreakdown,
        String difficultyBreakdown,
        String timingBreakdown,
        List<QuestionResultDto> questionResults,
        String practiceSetName,
        String mode
    ) {
        this(sessionId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks, accuracyPercent, topicWiseBreakdown, difficultyBreakdown, timingBreakdown, questionResults, practiceSetName, mode, 0);
    }

    public PracticeResultDto(
        UUID sessionId,
        int correctCount,
        int incorrectCount,
        int skippedCount,
        int obtainedMarks,
        int totalMarks,
        double accuracyPercent,
        String topicWiseBreakdown,
        String difficultyBreakdown,
        String timingBreakdown,
        List<QuestionResultDto> questionResults
    ) {
        this(sessionId, correctCount, incorrectCount, skippedCount, obtainedMarks, totalMarks, accuracyPercent, topicWiseBreakdown, difficultyBreakdown, timingBreakdown, questionResults, null, null, 0);
    }
}
