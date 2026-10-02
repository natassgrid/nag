// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="learner_profile", schema="recommendation_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearnerProfile extends BaseEntity {

    @Column(name="candidate_id", nullable=false, unique=true, columnDefinition="uuid")
    private UUID candidateId;
    
    @Column(name="last_updated_at")
    private Instant lastUpdatedAt;
    
    @Column(name="total_practice_sessions", nullable=false)
    @Builder.Default private int totalPracticeSessions = 0;
    
    @Column(name="total_questions_attempted", nullable=false)
    @Builder.Default private int totalQuestionsAttempted = 0;
    
    @Column(name="overall_accuracy", nullable=false)
    @Builder.Default private double overallAccuracy = 0.0;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="topic_accuracy_map", columnDefinition="jsonb")
    private String topicAccuracyMap;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="weak_topics", columnDefinition="jsonb")
    private String weakTopics;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="strong_topics", columnDefinition="jsonb")
    private String strongTopics;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="difficulty_accuracy_map", columnDefinition="jsonb")
    private String difficultyAccuracyMap;
}
