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

package com.examplatform.recommendation.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks individual micro-lesson interactions by enrollment, preventing duplicate lessons
 * and enabling variety in the next-best-activity engine.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "learner_lesson_progress", schema = "recommendation_service")
public class LearnerLessonProgress extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false, columnDefinition = "uuid")
    private UUID enrollmentId;

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "lesson_id", nullable = false, columnDefinition = "uuid")
    private UUID lessonId;

    @Column(name = "subtopic_id", nullable = false)
    private Long subtopicId;

    @Column(name = "lesson_type", length = 50)
    private String lessonType; // CONCEPT | WORKED_EXAMPLES | TRAPS_AND_PITFALLS | CHEAT_SHEET | VIDEO | VISUAL | etc.

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = SharedConstants.COMPLETED; // IN_PROGRESS | COMPLETED | SKIPPED

    @Column(name = "helpful_rating")
    private Boolean helpfulRating; // true = helpful, false = unhelpful, null = unrated

    @Column(name = "completed_at", nullable = false)
    @Builder.Default
    private Instant completedAt = Instant.now();

    @Override
    public void setId(UUID id) {
        super.setId(id);
    }
}
