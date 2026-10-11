// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import java.util.Map;
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
    String subject,
    Map<String, Map<String, Object>> translations
) {
    public AnswerKeyDto(
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
        this(questionId, answerKey, questionType, topicId, topicName, difficulty, marks, content, optionsJson, explanation, subject, null);
    }

    public AnswerKeyDto(
        UUID questionId,
        String answerKey,
        String questionType,
        String topicId,
        String topicName,
        String difficulty,
        int marks
    ) {
        this(questionId, answerKey, questionType, topicId, topicName, difficulty, marks, null, null, null, null, null);
    }
}
