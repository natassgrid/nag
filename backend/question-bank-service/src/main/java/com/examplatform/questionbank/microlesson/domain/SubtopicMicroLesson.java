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

package com.examplatform.questionbank.microlesson.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Micro-lesson entity v2: supports multiple multi-modal lessons per subtopic
 * varied by {@code lessonType} and {@code difficultyLevel}.
 */
@Entity
@Table(name = "subtopic_micro_lesson", schema = "question_service")
@AttributeOverride(name = "tenantId", column = @Column(name = "tenant_id", updatable = false, nullable = true, columnDefinition = "varchar(255)"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtopicMicroLesson extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    @Column(name = "subtopic_id", nullable = false)
    private Long subtopicId;

    @Column(name = "lesson_type", nullable = false, length = 30)
    @Builder.Default
    private String lessonType = "CONCEPT";

    @Column(name = "difficulty_level", nullable = false, length = 20)
    @Builder.Default
    private String difficultyLevel = "CORE";

    @Column(name = "sequence_no", nullable = false)
    @Builder.Default
    private int sequenceNo = 1;

    @Column(name = "prerequisite_lesson_id")
    private UUID prerequisiteLessonId;

    @Column(name = "estimated_minutes", nullable = false)
    @Builder.Default
    private int estimatedMinutes = 5;

    @Column(name = "language", nullable = false, length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "core_concept", nullable = false, columnDefinition = "text")
    private String coreConcept;

    @Column(name = "trap", nullable = false, columnDefinition = "text")
    private String trap;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cheat_sheet", nullable = false, columnDefinition = "jsonb")
    private String cheatSheetJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "step_by_step_breakdown", nullable = false, columnDefinition = "jsonb")
    private String stepByStepBreakdownJson;

    @Column(name = SharedConstants.STATUS, nullable = false, length = 30)
    @Builder.Default
    private String status = "AI_GENERATED";

    @Column(name = "reviewed_by", columnDefinition = "uuid")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comments", columnDefinition = "text")
    private String reviewComments;

    @Column(name = "quality_score")
    private Double qualityScore;

    @Column(name = "content_version", nullable = false)
    @Builder.Default
    private int contentVersion = 1;
}
