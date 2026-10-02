// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;
import java.time.Instant;
import java.util.UUID;
public record PracticeSessionDto(
    UUID id,
    UUID practiceSetId,
    String mode,
    String status,
    Instant startedAt,
    int totalQuestions,
    int durationMinutes
) {}
