// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;
import java.util.UUID;
public record PracticeSetDto(
    UUID id,
    String name,
    String description,
    String source,
    int durationMinutes,
    String subjectSlug,
    boolean published,
    int totalQuestions
) {}
