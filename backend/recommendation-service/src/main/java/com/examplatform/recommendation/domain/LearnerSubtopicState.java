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

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Canonical subtopic mastery state under Unified Exam Spine.
 * Keyed strictly by numeric taxonomy IDs and bound to a specific learner_enrollment.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "learner_subtopic_state", schema = "recommendation_service")
public class LearnerSubtopicState extends BaseEntity {

    @Column(name = "enrollment_id", nullable = false, columnDefinition = "uuid")
    private UUID enrollmentId;

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "examination_id", columnDefinition = "uuid")
    private UUID examinationId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "subtopic_id", nullable = false)
    private Long subtopicId;

    @Column(name = "mastery_score", nullable = false)
    @Builder.Default
    private double masteryScore = 0.0; // [0.0, 1.0]

    @Column(name = "confidence", nullable = false)
    @Builder.Default
    private double confidence = 0.1; // [0.0, 1.0]

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "correct", nullable = false)
    @Builder.Default
    private int correct = 0;

    @Column(name = "last_practiced_at")
    private Instant lastPracticedAt;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "NOT_STARTED"; // NOT_STARTED | LEARNING | PRACTISING | MASTERED | REVIEW_DUE

    @Column(name = "preferred_lesson_type", length = 50)
    private String preferredLessonType;

    @Override
    public void setId(UUID id) {
        super.setId(id);
    }
}
