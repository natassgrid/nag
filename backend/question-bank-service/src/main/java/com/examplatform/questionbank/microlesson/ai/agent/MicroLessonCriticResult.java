/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 Open Digital Public Infrastructure (DPI) Platform Contributors
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
package com.examplatform.questionbank.microlesson.ai.agent;

import java.util.List;

/**
 * Result of micro-lesson review by {@link MicroLessonCriticAgent}.
 * Evaluates structural completeness, KaTeX validity, domain coherence, and pedagogical depth.
 */
public record MicroLessonCriticResult(
        boolean approved,
        double score,
        List<String> issues,
        List<String> verifiedRubrics
) {
    public MicroLessonCriticResult {
        issues = issues != null ? List.copyOf(issues) : List.of();
        verifiedRubrics = verifiedRubrics != null ? List.copyOf(verifiedRubrics) : List.of();
    }

    /**
     * Getter for boolean approved state, ensuring compatibility with standard bean naming.
     */
    public boolean isApproved() {
        return approved;
    }

    public static MicroLessonCriticResult success(List<String> verifiedRubrics) {
        return new MicroLessonCriticResult(true, 1.0, List.of(), verifiedRubrics);
    }

    public static MicroLessonCriticResult failure(double score, List<String> issues, List<String> verifiedRubrics) {
        return new MicroLessonCriticResult(false, score, issues, verifiedRubrics);
    }
}
