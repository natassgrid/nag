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
    boolean markedForReview
) {}
