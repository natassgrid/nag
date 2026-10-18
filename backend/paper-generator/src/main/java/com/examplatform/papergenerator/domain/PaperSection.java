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

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Relational section within a generated paper, preserving section boundaries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"paper", "questions"})
@ToString(callSuper = true, exclude = {"paper", "questions"})
@Entity
@Table(name = "paper_section", schema = "paper_generator")
public class PaperSection extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id", nullable = false)
    private Paper paper;

    @Column(name = "blueprint_section_id", columnDefinition = "uuid")
    private UUID blueprintSectionId;

    @Column(name = "examination_section_id", columnDefinition = "uuid")
    private UUID examinationSectionId;

    @Column(name = SharedConstants.NAME, nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Builder.Default
    @OneToMany(mappedBy = "paperSection", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaperQuestion> questions = new ArrayList<>();

    public void addQuestion(PaperQuestion question) {
        if (questions == null) questions = new ArrayList<>();
        questions.add(question);
        question.setPaperSection(this);
    }
}
