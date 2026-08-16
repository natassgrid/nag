/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
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

/**
 * Request payload for creating a new question.
 * Supports rich content types: HTML5, SVG, LaTeX, MathML, or references to media files.
 *
 * Validates: Requirements 4.1, 4.2, 4.3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestionRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    @NotBlank(message = "Topic is required")
    private String topic;

    private String subtopic;

    private String chapter;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficulty;

    @NotNull(message = "Cognitive level is required")
    private CognitiveLevel cognitiveLevel;

    @NotNull(message = "Question type is required")
    private QuestionType questionType;

    @NotBlank(message = "Content is required")
    private String content;

    private String answerKey;

    /** Detailed explanation of the correct answer (shown post-evaluation) */
    private String explanation;

    /** Source references: textbook, chapter, page, URL, etc. */
    private String references;

    private String contentType;

    /** Options for MCQ/MSQ questions (2-6 items, A-F) */
    @Valid
    private java.util.List<QuestionOption> options;
}
