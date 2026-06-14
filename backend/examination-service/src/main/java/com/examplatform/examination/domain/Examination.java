package com.examplatform.examination.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Examination configuration entity.
 * Sections are stored as JSONB in {@link #sectionsJson} rather than as a
 * separate table, allowing flexible section definitions without schema changes.
 *
 * Validates: Requirements 7.1, 7.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "examination", schema = "examination_service")
public class Examination extends BaseEntity {

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(name = "total_marks", nullable = false)
    private int totalMarks;

    @Column(name = "negative_marking_enabled", nullable = false)
    private boolean negativeMarkingEnabled;

    @Column(name = "negative_marking_value")
    private double negativeMarkingValue;

    @Column(name = "navigation_policy", nullable = false, length = 20)
    private String navigationPolicy;

    @Column(name = "calculator_policy", nullable = false, length = 20)
    private String calculatorPolicy;

    @Column(name = "review_flag_enabled", nullable = false)
    private boolean reviewFlagEnabled;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections_json", columnDefinition = "jsonb")
    private String sectionsJson;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}
