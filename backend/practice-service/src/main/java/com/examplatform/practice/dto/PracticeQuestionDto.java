// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import java.util.Map;
import java.util.UUID;

public record PracticeQuestionDto(
    UUID id,
    int order,
    String questionCode,
    String content,
    String optionsJson,
    int marks,
    double negativeMarks,
    String subject,
    String topic,
    String difficulty,
    String primaryLanguage,
    boolean fallbackToEnglish,
    Map<String, Object> primaryTranslation,
    Map<String, Map<String, Object>> translations
) {
    public PracticeQuestionDto(
        UUID id,
        int order,
        String questionCode,
        String content,
        String optionsJson,
        int marks,
        double negativeMarks,
        String subject,
        String topic,
        String difficulty
    ) {
        this(id, order, questionCode, content, optionsJson, marks, negativeMarks, subject, topic, difficulty, "en", false, null, null);
    }
}
