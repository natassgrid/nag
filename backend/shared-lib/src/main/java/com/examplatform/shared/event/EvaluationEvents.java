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

package com.examplatform.shared.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.Instant;

/**
 * Domain events for evaluation outcomes and audit events.
 */
public final class EvaluationEvents {

    private EvaluationEvents() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EvaluationCompleted(
            String eventType,
            String sessionId,
            String candidateId,
            String examId,
            String shiftId,
            double totalScore,
            double maxScore,
            double percentage,
            String grade,
            String tenantId,
            String evaluatedAt
    ) implements Serializable {
        public static EvaluationCompleted of(String sessionId, String candidateId, String examId, String shiftId,
                                            double totalScore, double maxScore, double percentage, String grade, String tenantId) {
            return new EvaluationCompleted(
                    "EVALUATION_COMPLETED",
                    sessionId,
                    candidateId,
                    examId,
                    shiftId,
                    totalScore,
                    maxScore,
                    percentage,
                    grade,
                    tenantId,
                    Instant.now().toString()
            );
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EvaluationAudit(
            String eventType,
            String sessionId,
            String candidateId,
            int evaluationCount,
            String evaluationType,
            String tenantId,
            boolean anonymized,
            String occurredAt
    ) implements Serializable {
        public static EvaluationAudit of(String sessionId, String candidateId, int count, String type, String tenantId, boolean anonymized) {
            return new EvaluationAudit(
                    "EVALUATION_CREATED",
                    sessionId,
                    candidateId,
                    count,
                    type,
                    tenantId,
                    anonymized,
                    Instant.now().toString()
            );
        }
    }
}
