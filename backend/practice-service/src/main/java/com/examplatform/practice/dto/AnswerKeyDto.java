// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import java.util.UUID;

public record AnswerKeyDto(
    UUID questionId,
    String answerKey,
    String questionType,
    String topicId,
    String topicName,
    String difficulty,
    int marks,
    String content,
    String optionsJson,
    String explanation,
    String subject
) {
    public AnswerKeyDto(
        UUID questionId,
        String answerKey,
        String questionType,
        String topicId,
        String topicName,
        String difficulty,
        int marks
    ) {
        this(questionId, answerKey, questionType, topicId, topicName, difficulty, marks, null, null, null, null);
    }
}
