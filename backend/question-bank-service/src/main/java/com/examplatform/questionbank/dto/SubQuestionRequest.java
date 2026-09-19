/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.questionbank.dto;

import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Sub-question payload within a comprehension / passage set request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubQuestionRequest {

    /** Optional existing question ID for updates */
    private UUID id;

    @NotNull(message = "Question type is required")
    @Builder.Default
    private QuestionType questionType = QuestionType.SINGLE_MCQ;

    @NotBlank(message = "Question content is required")
    private String content;

    @Valid
    private List<QuestionOption> options;

    private String answerKey;

    private String explanation;

    private String references;

    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    @Builder.Default
    private CognitiveLevel cognitiveLevel = CognitiveLevel.UNDERSTAND;

    private String chapter;

    private boolean hasImages;

    private Integer passageOrderIndex;
}
