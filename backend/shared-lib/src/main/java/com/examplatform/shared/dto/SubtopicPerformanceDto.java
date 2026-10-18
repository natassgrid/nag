/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.shared.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.Serializable;

/**
 * Granular telemetry DTO representing aggregated candidate performance for a single Sub-Topic
 * across accuracy, pacing (against benchmark), and question skipping metrics.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubtopicPerformanceDto(
        Long subjectId,
        String subjectName,
        Long topicId,
        String topicName,
        Long subtopicId,
        String subtopicName,
        int totalQuestions,
        int attemptedCount,
        int correctCount,
        int incorrectCount,
        int skippedCount,
        long totalTimeSpentMs,
        long avgTimeSpentMs,
        long benchmarkTimeMs,
        double accuracyPercent,
        double skipRatePercent,
        double paceRatio
) implements Serializable {

    /**
     * Compact constructor allowing instantiation with numeric IDs only, leaving display names null.
     * Display names can be resolved later via TaxonomyClient or TaxonomyStore.
     */
    public SubtopicPerformanceDto(
            Long subjectId,
            Long topicId,
            Long subtopicId,
            int totalQuestions,
            int attemptedCount,
            int correctCount,
            int incorrectCount,
            int skippedCount,
            long totalTimeSpentMs,
            long avgTimeSpentMs,
            long benchmarkTimeMs,
            double accuracyPercent,
            double skipRatePercent,
            double paceRatio
    ) {
        this(subjectId, null, topicId, null, subtopicId, null,
                totalQuestions, attemptedCount, correctCount, incorrectCount, skippedCount,
                totalTimeSpentMs, avgTimeSpentMs, benchmarkTimeMs, accuracyPercent, skipRatePercent, paceRatio);
    }

    /**
     * Returns a new instance enriched with resolved display names.
     */
    public SubtopicPerformanceDto withDisplayNames(String subjectName, String topicName, String subtopicName) {
        return new SubtopicPerformanceDto(
                this.subjectId,
                subjectName,
                this.topicId,
                topicName,
                this.subtopicId,
                subtopicName,
                this.totalQuestions,
                this.attemptedCount,
                this.correctCount,
                this.incorrectCount,
                this.skippedCount,
                this.totalTimeSpentMs,
                this.avgTimeSpentMs,
                this.benchmarkTimeMs,
                this.accuracyPercent,
                this.skipRatePercent,
                this.paceRatio
        );
    }
}
