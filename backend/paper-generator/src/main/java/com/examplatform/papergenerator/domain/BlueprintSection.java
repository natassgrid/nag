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

/**
 * First-class section within a blueprint targeting a canonical taxonomy subject.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"blueprint", "rules"})
@ToString(callSuper = true, exclude = {"blueprint", "rules"})
@Entity
@Table(name = "blueprint_section", schema = "paper_generator")
@AttributeOverride(name = "tenantId", column = @Column(name = "tenant_id", nullable = true, columnDefinition = "varchar(255)"))
@AttributeOverride(name = "version", column = @Column(name = "version_opt", nullable = false))
public class BlueprintSection extends BaseEntity {

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blueprint_id", nullable = false)
    private Blueprint blueprint;

    @Column(name = SharedConstants.NAME, nullable = false)
    private String name;

    @Builder.Default
    @Column(name = "order_index", nullable = false)
    private int orderIndex = 0;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Builder.Default
    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BlueprintRule> rules = new ArrayList<>();

    public void addRule(BlueprintRule rule) {
        if (rules == null) rules = new ArrayList<>();
        rules.add(rule);
        rule.setSection(this);
    }
}
