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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.papergenerator.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Persisted blueprint template — a named, reusable set of blueprint rules
 * that an Exam Controller can save once and load when generating papers.
 *
 * {@code rulesJson} stores the ordered array of BlueprintRule objects as JSONB:
 * <pre>
 * [{"subject":"Mathematics","topic":"Algebra","difficulty":"EASY",
 *   "cognitiveLevel":"APPLY","questionCount":5}, ...]
 * </pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "blueprint_template", schema = "paper_generator")
public class BlueprintTemplate extends BaseEntity {

    /** Human-readable name, unique per tenant. */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /** Optional longer description of what this template is for. */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Optional exam UUID. When set, the template is shown as a suggestion
     * when generating papers for this specific exam.
     */
    @Column(name = "exam_id", columnDefinition = "uuid")
    private UUID examId;

    /**
     * Serialised array of BlueprintRule objects stored as JSONB.
     * Deserialization is handled by the service layer via ObjectMapper.
     */
    @Column(name = "rules_json", nullable = false, columnDefinition = "jsonb")
    private String rulesJson;

    /** UUID of the Exam Controller who created this template. */
    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;
}
