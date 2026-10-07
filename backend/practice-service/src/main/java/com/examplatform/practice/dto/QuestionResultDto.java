// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import java.util.UUID;

public record QuestionResultDto(
    UUID questionId,
    String candidateAnswer,
    String correctAnswer,
    boolean correct,
    int marksAwarded,
    long timeSpentMs,
    boolean markedForReview,
    String content,
    String optionsJson,
    String explanation,
    String topic,
    String subject,
    String questionType
) {
    public QuestionResultDto(
        UUID questionId,
        String candidateAnswer,
        String correctAnswer,
        boolean correct,
        int marksAwarded,
        long timeSpentMs,
        boolean markedForReview,
        String content,
        String optionsJson,
        String explanation,
        String topic,
        String subject
    ) {
        this(questionId, candidateAnswer, correctAnswer, correct, marksAwarded, timeSpentMs, markedForReview, content, optionsJson, explanation, topic, subject, null);
    }

    public QuestionResultDto(
        UUID questionId,
        String candidateAnswer,
        String correctAnswer,
        boolean correct,
        int marksAwarded,
        long timeSpentMs,
        boolean markedForReview
    ) {
        this(questionId, candidateAnswer, correctAnswer, correct, marksAwarded, timeSpentMs, markedForReview, null, null, null, null, null, null);
    }
}
