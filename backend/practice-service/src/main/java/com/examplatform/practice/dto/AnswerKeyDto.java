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
    Map<String, Map<String, Object>> translations,
    Long subjectId,
    Long subtopicId,
    String subtopicName,
    int benchmarkTimeSeconds
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
        String subject,
        Map<String, Map<String, Object>> translations
    ) {
        this(questionId, answerKey, questionType, topicId, topicName, difficulty, marks, content, optionsJson, explanation, subject, translations, null, null, null, 90);
    }

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
        this(questionId, answerKey, questionType, topicId, topicName, difficulty, marks, content, optionsJson, explanation, subject, null, null, null, null, 90);
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
        this(questionId, answerKey, questionType, topicId, topicName, difficulty, marks, null, null, null, null, null, null, null, null, 90);
    }
}
