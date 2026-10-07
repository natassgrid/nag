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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO from the Requirement Analyst Agent for interactive elicitation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClarifyRequirementsResponse {

    /** Whether the author request requires additional clarification before generation. */
    private boolean clarificationNeeded;

    /** Specific clarifying questions to display to the author in the UI. */
    private List<String> clarificationQuestions;

    /** Recommended subtopics or trap concepts based on syllabus taxonomy. */
    private List<String> suggestedSubtopics;

    /** Supported question formats tailored to the target examination standard. */
    private List<String> suggestedFormats;

    /** Recommended blueprint / generation specification synthesized by the agent. */
    private String recommendedBlueprint;

    /** Detected target exam code or standard. */
    private String resolvedTargetExam;
}
