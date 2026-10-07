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
package com.examplatform.questionbank.ai.generation.multiagent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result returned by {@link ComplexityEvaluator} determining whether the generation
 * executes via Single-Model Fast Path or Multi-Agent Collaborative Committee.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplexityEvaluationResult {

    /** True if escalated to Multi-Agent pipeline; false if routed to Single-Model Fast Path. */
    private boolean useMultiAgent;

    /** Human-readable explanation of why this path was chosen. */
    private String rationale;

    /** Specific triggers activated (e.g. HIGH_COGNITIVE_LEVEL, COMPLEX_QUESTION_TYPE, MATH_OR_CHEM_DETECTED). */
    private List<String> triggeredFactors;

    /** Suggested model for the execution path. */
    private String targetModel;
}
