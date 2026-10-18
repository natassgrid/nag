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

package com.examplatform.examination.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Examination configuration entity.
 * Uses a single canonical blueprint ({@link #blueprintId}) and relational sections
 * and syllabus nodes defining its curriculum scope.
 *
 * Validates: Requirements 7.1, 7.2, 7b.1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"sections", "syllabusNodes"})
@ToString(callSuper = true, exclude = {"sections", "syllabusNodes"})
@Entity
@Table(name = "examination", schema = "examination_service")
public class Examination extends BaseEntity {

    @Column(name = SharedConstants.NAME, nullable = false, length = 500)
    private String name;

    /** Short unique examination code per tenant (e.g. "JEE-MAIN-2027"). */
    @Column(name = SharedConstants.CODE, length = 100)
    private String code;

    /** Authority that conducts this examination (e.g. "NTA", "UPSC"). */
    @Column(name = "conducting_authority")
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

    /** Exactly one blueprint per examination contract. */
    @Column(name = "blueprint_id", nullable = false, columnDefinition = "uuid")
    private UUID blueprintId;

    /** Optional parent examination link for multi-stage exams (e.g. Mains referencing Prelims). */
    @Column(name = "parent_examination_id", columnDefinition = "uuid")
    private UUID parentExaminationId;

    @Builder.Default
    @OneToMany(mappedBy = "examination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExaminationSection> sections = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "examination", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExamSyllabusNode> syllabusNodes = new ArrayList<>();

    /** DRAFT / APPROVED / PUBLISHED / CANCELLED / COMPLETED */
    @Column(name = SharedConstants.STATUS, nullable = false, length = 30)
    private String status;

    /** UUID of the Exam Controller who created this examination. */
    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    public void addSection(ExaminationSection section) {
        if (sections == null) {
            sections = new ArrayList<>();
        }
        sections.add(section);
        section.setExamination(this);
    }

    public void addSyllabusNode(ExamSyllabusNode node) {
        if (syllabusNodes == null) {
            syllabusNodes = new ArrayList<>();
        }
        syllabusNodes.add(node);
        node.setExamination(this);
    }
}
