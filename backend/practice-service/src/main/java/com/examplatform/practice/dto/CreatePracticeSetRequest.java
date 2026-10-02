// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CreatePracticeSetRequest(
    @NotBlank String name,
    String description,
    @NotNull Integer durationMinutes,
    String subjectSlug,
    List<UUID> questionIds,
    String source,
    Integer totalQuestions
) {}
