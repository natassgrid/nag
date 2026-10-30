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

import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearnerEnrollmentResponse(
        UUID id,
        UUID candidateId,
        UUID examinationId,
        String examName,
        String examinationTitle,
        String examinationCode,
        LocalDate targetDate,
        String status,
        String diagnosticStatus,
        boolean diagnosticCompleted,
        UUID diagnosticSessionId,
        int readinessScore,
        Instant startedAt,
        Instant enrolledAt
) {
    public LearnerEnrollmentResponse(
            UUID id,
            UUID candidateId,
            UUID examinationId,
            String examName,
            LocalDate targetDate,
            String status,
            String diagnosticStatus,
            UUID diagnosticSessionId,
            Instant startedAt
    ) {
        this(
                id,
                candidateId,
                examinationId,
                examName,
                examName,
                null,
                targetDate,
                status,
                diagnosticStatus,
                SharedConstants.COMPLETED.equalsIgnoreCase(diagnosticStatus) || "SKIPPED".equalsIgnoreCase(diagnosticStatus),
                diagnosticSessionId,
                "SKIPPED".equalsIgnoreCase(diagnosticStatus) ? 50 : 0,
                startedAt,
                startedAt
        );
    }
}
