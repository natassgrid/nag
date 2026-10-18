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

package com.examplatform.questionbank.domain;

import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.NumericBaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Represents a topic within a subject (e.g. Indian History under General Studies).
 *
 * <p>Canonical platform topics have {@code tenant_id IS NULL}.
 * Tenant-specific topics have their tenant ID and optional {@code base_node_id}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "topic", schema = "question_service")
@AttributeOverride(name = "tenantId", column = @Column(name = "tenant_id", updatable = false, nullable = true, columnDefinition = "varchar(255)"))
public class Topic extends NumericBaseEntity {

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = SharedConstants.CODE, length = 50)
    private String code;

    @Column(name = SharedConstants.NAME, nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Column(name = "base_node_id")
    private Long baseNodeId;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Builder.Default
    @Transient
    private boolean platformScope = false;

    @PrePersist
    protected void onTopicPrePersist() {
        if (platformScope || super.getTenantId() == null) {
            super.setTenantId(null);
        }
    }
}
