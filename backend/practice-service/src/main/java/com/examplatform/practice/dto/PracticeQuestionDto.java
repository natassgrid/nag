// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

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
    String difficulty
) {}
