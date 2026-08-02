package com.examplatform.analytics.domain;

import com.examplatform.shared.util.UuidV7Generator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing computed analytics for an examination.
 *
 * <p>Does not extend {@code BaseEntity} (no tenant isolation, no optimistic
 * lock, append-only compute results). ID is assigned as UUID v7 in
 * {@link #prePersist()} for time-ordered inserts.
 */
@Entity
@Table(name = "exam_analytics", schema = "analytics_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAnalytics {

    @Id
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "total_registered", nullable = false)
    private Long totalRegistered;

    @Column(name = "total_appeared", nullable = false)
    private Long totalAppeared;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_distribution_json", columnDefinition = "jsonb")
    private String scoreDistributionJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "section_averages_json", columnDefinition = "jsonb")
    private String sectionAveragesJson;

    @Column(name = "top_10_percentile_threshold", precision = 10, scale = 4)
    private BigDecimal top10PercentileThreshold;

    @Column(name = "bottom_10_percentile_threshold", precision = 10, scale = 4)
    private BigDecimal bottom10PercentileThreshold;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @PrePersist
    protected void prePersist() {
        if (this.id == null) {
            this.id = UuidV7Generator.generate();
        }
    }
}
