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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

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

import java.util.UUID;

/**
 * Examination configuration entity.
 * Sections are stored as JSONB in {@link #sectionsJson} rather than as a
 * separate table, allowing flexible section definitions without schema changes.
 *
 * Validates: Requirements 7.1, 7.2, 7b.1
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

    /** Short unique examination code per tenant (e.g. "JEE-MAIN-2027"). */
    @Column(name = "code", length = 100)
    private String code;

    /** Authority that conducts this examination (e.g. "NTA", "UPSC"). */
    @Column(name = "conducting_authority", length = 255)
    private String conductingAuthority;

    /** RECRUITMENT / ENTRANCE / CERTIFICATION / DEPARTMENTAL */
    @Column(name = "category", length = 30)
    private String category;

    /** PRELIMINARY / MAIN / SKILL_TEST / INTERVIEW / PHYSICAL_TEST */
    @Column(name = "examination_type", length = 30)
    private String examinationType;

    /** Academic year or recruitment cycle, e.g. "2026-27". */
    @Column(name = "academic_year", length = 20)
    private String academicYear;

    /** CBT / OMR / HYBRID */
    @Column(name = "examination_mode", length = 20)
    private String examinationMode;

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

    /** Indicates whether this examination is configured for candidate practice and learning. */
    @Column(name = "is_practice", nullable = false)
    private boolean isPractice;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections_json", columnDefinition = "jsonb")
    private String sectionsJson;

    /** DRAFT / APPROVED / PUBLISHED / CANCELLED / COMPLETED */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** UUID of the Exam Controller who created this examination. */
    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;
}
