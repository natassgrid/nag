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
@Table(name="recommendation", schema="recommendation_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation extends BaseEntity {

    @Column(name="candidate_id", nullable=false, columnDefinition="uuid")
    private UUID candidateId;
    
    @Column(name="trigger_session_id", columnDefinition="uuid")
    private UUID triggerSessionId;
    
    @Column(nullable=false, length=30)
    @Builder.Default private String status = "PENDING";
    
    @Column(name="generated_at")
    private Instant generatedAt;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="weak_topic_recommendations", columnDefinition="jsonb")
    private String weakTopicRecommendations;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="study_plan_items", columnDefinition="jsonb")
    private String studyPlanItems;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name="suggested_practice_set_ids", columnDefinition="jsonb")
    private String suggestedPracticeSetIds;
    
    @Column(name="motivational_message", columnDefinition="text")
    private String motivationalMessage;
    
    @Column(name="model_used", length=100)
    private String modelUsed;
}
