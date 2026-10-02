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
    int marks
) {}
