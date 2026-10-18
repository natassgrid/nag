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

import com.examplatform.shared.entity.BaseEntity;
import com.examplatform.shared.taxonomy.TaxonomyNodeType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Entity representing an in-scope syllabus node (Subject, Topic, or Subtopic)
 * explicitly scoped to an examination to drive learning paths and blueprint validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"examination"})
@ToString(callSuper = true, exclude = {"examination"})
@Entity
@Table(
    name = "exam_syllabus_node",
    schema = "examination_service",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_exam_syllabus_node",
        columnNames = {"examination_id", "node_type", "node_id"}
    )
)
public class ExamSyllabusNode extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "examination_id", nullable = false)
    private Examination examination;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 20)
    private TaxonomyNodeType nodeType;

    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "weightage_pct")
    private Double weightagePct;
}
