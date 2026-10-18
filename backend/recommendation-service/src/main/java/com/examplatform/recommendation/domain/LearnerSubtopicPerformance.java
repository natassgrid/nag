// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "learner_subtopic_performance", schema = "recommendation_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerSubtopicPerformance extends BaseEntity {

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "subtopic_id")
    private Long subtopicId;

    @Column(name = "subject_name", nullable = false, length = 100)
    private String subjectName;

    @Column(name = "topic_name", nullable = false, length = 200)
    private String topicName;

    @Column(name = "subtopic_name", nullable = false, length = 200)
    private String subtopicName;

    @Column(name = "total_questions", nullable = false)
    @Builder.Default private int totalQuestions = 0;

    @Column(name = "total_attempts", nullable = false)
    @Builder.Default private int totalAttempts = 0;

    @Column(name = "total_correct", nullable = false)
    @Builder.Default private int totalCorrect = 0;

    @Column(name = "total_skipped", nullable = false)
    @Builder.Default private int totalSkipped = 0;

    @Column(name = "avg_accuracy_percent", nullable = false)
    @Builder.Default private double avgAccuracyPercent = 0.0;

    @Column(name = "avg_pace_ratio", nullable = false)
    @Builder.Default private double avgPaceRatio = 1.0;

    @Column(name = "avg_skip_rate_percent", nullable = false)
    @Builder.Default private double avgSkipRatePercent = 0.0;

    @Column(name = "last_tested_at")
    private Instant lastTestedAt;
}
