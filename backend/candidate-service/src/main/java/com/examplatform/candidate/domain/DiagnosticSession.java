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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Diagnostic assessment session conducted post-enrollment to calibrate the learner's initial
 * subtopic mastery and seed the Dynamic Learning Path.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "diagnostic_session", schema = "candidate_service")
public class DiagnosticSession extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false, columnDefinition = "uuid")
    private UUID enrollmentId;

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "examination_id", nullable = false, columnDefinition = "uuid")
    private UUID examinationId;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "IN_PROGRESS"; // IN_PROGRESS | COMPLETED | SKIPPED

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "questions_json", columnDefinition = "jsonb")
    private String questionsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "responses_json", columnDefinition = "jsonb")
    private String responsesJson;

    @Column(name = "score", nullable = false)
    @Builder.Default
    private double score = 0.0;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default
    private int totalQuestions = 0;

    @Column(name = "correct_count", nullable = false)
    @Builder.Default
    private int correctCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "subtopic_results_json", columnDefinition = "jsonb")
    private String subtopicResultsJson;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private Instant startedAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;

    @Override
    public void setId(UUID id) {
        super.setId(id);
    }
}
