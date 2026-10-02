/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
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

package com.examplatform.analytics.domain;

import com.examplatform.shared.util.UuidV7Generator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing individual candidate evaluation results ingested for analytics computation.
 *
 * <p>Persisted idempotently based on (examId, candidateId) unique constraint to support
 * incremental real-time analytics aggregation and on-demand batch recomputation.
 */
@Entity
@Table(
        name = "candidate_analytics_results",
        schema = "analytics_service",
        uniqueConstraints = @UniqueConstraint(name = "uq_candidate_analytics_exam", columnNames = {"exam_id", "candidate_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateAnalyticsResult {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "total_raw_score", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalRawScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_scores_json", columnDefinition = "jsonb")
    private String sectionScoresJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_level_scores_json", columnDefinition = "jsonb")
    private String questionLevelScoresJson;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UuidV7Generator.generate();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
