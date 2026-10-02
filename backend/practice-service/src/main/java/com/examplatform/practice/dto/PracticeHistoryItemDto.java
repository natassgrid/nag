// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;
import java.time.Instant;
import java.util.UUID;
public record PracticeHistoryItemDto(
    UUID sessionId,
    UUID practiceSetId,
    String practiceSetName,
    Instant submittedAt,
    int obtainedMarks,
    int totalMarks,
    double accuracyPercent,
    int totalQuestions,
    int correctCount,
    int incorrectCount
) {}
