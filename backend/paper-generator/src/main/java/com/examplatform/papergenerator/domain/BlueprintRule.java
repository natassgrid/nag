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

package com.examplatform.papergenerator.domain;

import com.examplatform.shared.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA entity representing a relational blueprint rule targeting numeric IDs.
 * Granularity is the narrowest non-null node identifier: Subject -> Topic -> SubTopic.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"section"})
@ToString(callSuper = true, exclude = {"section"})
@Entity
@Table(name = "blueprint_rule", schema = "paper_generator")
@AttributeOverride(name = "tenantId", column = @Column(name = "tenant_id", nullable = true, columnDefinition = "varchar(255)"))
@AttributeOverride(name = "version", column = @Column(name = "version_opt", nullable = false))
public class BlueprintRule extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_section_id", nullable = false)
    private BlueprintSection section;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "topic_id")
    private Long topicId;

    @Column(name = "subtopic_id")
    private Long subtopicId;

    @Column(name = "difficulty", length = 20)
    private String difficulty;

    @Column(name = "cognitive_level", length = 30)
    private String cognitiveLevel;

    @Column(name = "question_count", nullable = false)
    private int questionCount;

    @Column(name = "marks_override")
    private Double marksOverride;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;
}
