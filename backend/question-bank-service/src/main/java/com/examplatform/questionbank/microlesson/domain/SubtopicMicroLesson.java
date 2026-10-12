// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.questionbank.microlesson.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subtopic_micro_lesson", schema = "question_service")
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

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    @Column(name = "subtopic", nullable = false, length = 200)
    private String subtopic;

    @Column(name = "target_exam", nullable = false, length = 50)
    @Builder.Default
    private String targetExam = "JEE_MAIN";

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

    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private String status = "AI_GENERATED";

    @Column(name = "reviewed_by", columnDefinition = "uuid")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_comments", columnDefinition = "text")
    private String reviewComments;

    @Column(name = "content_version", nullable = false)
    @Builder.Default
    private int contentVersion = 1;
}
