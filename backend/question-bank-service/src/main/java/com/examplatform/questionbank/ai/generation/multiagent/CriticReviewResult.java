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
 * Result of review by {@link PsychometricCriticAgent}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriticReviewResult {

    /** Whether the question passed all psychometric and technical rubric checks. */
    private boolean approved;

    /** Overall psychometric quality score (0.0 to 1.0). */
    private double score;

    /** Specific critiques or detected issues. */
    private List<String> issues;

    /** Verified checklist items. */
    private List<String> verifiedRubrics;
}
