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
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Blueprint entity representing the sharing contract between exams and taxonomy/questions.
 * Independent of exams and reusable across multiple examinations.
 * {@code tenant_id IS NULL} indicates a platform-default blueprint available to all tenants.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"sections"})
@ToString(callSuper = true, exclude = {"sections"})
@Entity
@Table(name = "blueprint", schema = "paper_generator")
@AttributeOverride(name = "tenantId", column = @Column(name = "tenant_id", nullable = true, columnDefinition = "varchar(255)"))
@AttributeOverride(name = "version", column = @Column(name = "version_opt", nullable = false))
public class Blueprint extends BaseEntity {

    @Column(name = SharedConstants.CODE, length = 100)
    private String code;

    @Column(name = SharedConstants.NAME, nullable = false)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Builder.Default
    @Column(name = "version", nullable = false)
    private int blueprintVersion = 1;

    @Builder.Default
    @Column(name = SharedConstants.STATUS, nullable = false, length = 30)
    private String status = "ACTIVE";

    @Builder.Default
    @Column(name = "total_questions", nullable = false)
    private int totalQuestions = 0;

    @Column(name = "total_marks")
    private Double totalMarks;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Builder.Default
    @OneToMany(mappedBy = "blueprint", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BlueprintSection> sections = new ArrayList<>();

    public void addSection(BlueprintSection section) {
        if (sections == null) sections = new ArrayList<>();
        sections.add(section);
        section.setBlueprint(this);
    }

    public boolean isPlatformDefault() {
        return getTenantId() == null;
    }
}
