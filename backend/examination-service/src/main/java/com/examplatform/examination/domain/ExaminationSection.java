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
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Examination section entity representing an explicit subject section within an examination.
 * Captures section-level question quotas and marking policies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"examination"})
@ToString(callSuper = true, exclude = {"examination"})
@Entity
@Table(name = "examination_section", schema = "examination_service")
public class ExaminationSection extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = SharedConstants.NAME, nullable = false, length = 255)
    private String name;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "marks_per_question", nullable = false)
    private double marksPerQuestion;

    @Builder.Default
    @Column(name = "negative_marks", nullable = false)
    private double negativeMarks = 0.0;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;
}
