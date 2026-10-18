/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.candidate.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosticResultResponse(
        UUID sessionId,
        UUID enrollmentId,
        String status,
        double score,
        int totalQuestions,
        int correctCount,
        int correctAnswers,
        double accuracyPercent,
        int initialReadinessScore,
        int seededSubtopicsCount,
        List<String> strengths,
        List<String> weaknesses,
        String baselineLabel,
        int neutralPriorPercent,
        Map<Long, Double> subtopicBreakdown,
        Instant completedAt
) {
    public DiagnosticResultResponse(
            UUID sessionId,
            UUID enrollmentId,
            String status,
            double score,
            int totalQuestions,
            int correctCount,
            double accuracyPercent,
            Map<Long, Double> subtopicBreakdown,
            Instant completedAt
    ) {
        this(
                sessionId,
                enrollmentId,
                status,
                score,
                totalQuestions,
                correctCount,
                correctCount,
                accuracyPercent,
                "SKIPPED".equalsIgnoreCase(status) ? 50 : (int) Math.round(accuracyPercent),
                subtopicBreakdown != null ? subtopicBreakdown.size() : 0,
                Collections.emptyList(),
                Collections.emptyList(),
                "SKIPPED".equalsIgnoreCase(status) ? "Neutral Baseline (50% Prior)" : "Calibrated Diagnostic Baseline",
                50,
                subtopicBreakdown,
                completedAt
        );
    }
}
