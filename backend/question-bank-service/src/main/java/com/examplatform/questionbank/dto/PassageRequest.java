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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for creating/updating a comprehension passage along with its 2–6 sub-questions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PassageRequest {

    private String title;

    @NotBlank(message = "Passage content is required")
    private String content;

    @Builder.Default
    private String contentFormat = "MIXED";

    @NotNull(message = "Subject ID is required")
    private Long subjectId;

    private Long topicId;

    private String subject;

    private String topic;

    private boolean hasImages;

    @NotNull(message = "Sub-questions list is required")
    @Size(min = 2, max = 6, message = "Passage must have between 2 and 6 sub-questions")
    @Valid
    private List<SubQuestionRequest> subQuestions;
}
