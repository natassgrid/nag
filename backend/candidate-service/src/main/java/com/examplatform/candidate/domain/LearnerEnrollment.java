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

package com.examplatform.candidate.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Sole canonical anchor binding a learner to a target examination.
 *
 * Under the Unified Exam Spine, a learner has one ACTIVE primary enrollment.
 * Switching target exams sets the previous enrollment to PAUSED while preserving all history.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "learner_enrollment", schema = "candidate_service")
public class LearnerEnrollment extends BaseEntity {

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "examination_id", nullable = false, columnDefinition = "uuid")
    private UUID examinationId;

    @Column(name = "target_date")
    private LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE | PAUSED | COMPLETED

    @Column(name = "diagnostic_session_id", columnDefinition = "uuid")
    private UUID diagnosticSessionId;

    @Column(name = "diagnostic_status", nullable = false, length = 30)
    @Builder.Default
    private String diagnosticStatus = "NOT_STARTED"; // NOT_STARTED | IN_PROGRESS | COMPLETED | SKIPPED

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Override
    public void setId(UUID id) {
        super.setId(id);
    }
}
