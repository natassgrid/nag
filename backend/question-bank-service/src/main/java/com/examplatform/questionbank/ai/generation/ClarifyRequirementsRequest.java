/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.generation;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request payload for conversational requirement elicitation.
 * Triggered when author generation specifications are broad or ambiguous.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClarifyRequirementsRequest {

    @NotBlank(message = "Subject is required")
    private String subject;

    private String topic;

    private String subtopic;

    /** Author's natural language input or brief (e.g. "Create tough questions on electrochemistry like the sample"). */
    private String authorPrompt;

    /** Target exam standard: JEE_ADV, UPSC_CSE, NEET, GATE, CBSE_12, etc. */
    private String targetExam;

    private String difficulty;

    private String cognitiveLevel;

    private String questionType;

    /** Optional reference/sample questions in text format. */
    private List<String> sampleQuestions;
}
