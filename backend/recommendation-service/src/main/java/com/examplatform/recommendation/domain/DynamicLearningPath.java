// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "dynamic_learning_path", schema = "recommendation_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicLearningPath extends BaseEntity {

    @Column(name = "candidate_id", nullable = false, columnDefinition = "uuid")
    private UUID candidateId;

    @Column(name = "enrollment_id", columnDefinition = "uuid")
    private UUID enrollmentId;

    @Column(name = "source_session_id", columnDefinition = "uuid")
    private UUID sourceSessionId;

    @Column(name = "exam_target", length = 50)
    private String examTarget;

    @Column(name = "readiness_score", nullable = false)
    @Builder.Default
    private double readinessScore = 0.0;

    @Column(name = SharedConstants.STATUS, nullable = false, length = 30)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, PAUSED, COMPLETED, SUPERSEDED

    @Column(name = "conceptual_gaps_count", nullable = false)
    @Builder.Default
    private int conceptualGapsCount = 0;

    @Column(name = "application_gaps_count", nullable = false)
    @Builder.Default
    private int applicationGapsCount = 0;

    @Column(name = "proficient_count", nullable = false)
    @Builder.Default
    private int proficientCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "milestone_graph_json", nullable = false, columnDefinition = "jsonb")
    private String milestoneGraphJson;

    @Override
    public void setId(UUID id) {
        super.setId(id);
    }
}
