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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Paper entity representing a generated examination paper.
 * Stores question selection (IDs + ordering) as JSONB, along with
 * statistical metadata and encryption references for shift-specific
 * AES-256 paper encryption.
 *
 * Validates: Requirements 8.7
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "paper", schema = "paper_generator")
public class Paper extends BaseEntity {

    @Column(name = "exam_id", nullable = false, columnDefinition = "uuid")
    private UUID examId;

    @Column(name = "shift_id", nullable = false)
    private String shiftId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "paper_definition_json", columnDefinition = "jsonb")
    private String paperDefinitionJson;

    @Column(name = "difficulty_score")
    private double difficultyScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_distribution_json", columnDefinition = "jsonb")
    private String topicDistributionJson;

    @Column(name = "encrypted_package_ref")
    private String encryptedPackageRef;

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "generated_by", columnDefinition = "uuid")
    private UUID generatedBy;
}
