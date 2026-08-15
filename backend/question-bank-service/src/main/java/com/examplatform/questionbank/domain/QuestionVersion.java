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

package com.examplatform.questionbank.domain;

import com.examplatform.questionbank.crypto.EncryptedFieldConverter;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks every modification to a question's content or metadata.
 * Each update creates a new version record with a JSON diff of changed fields
 * and an encrypted full snapshot of the question at that point in time.
 *
 * Validates: Requirements 4.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "question_version", schema = "question_service")
public class QuestionVersion extends BaseEntity {

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_json", nullable = false, columnDefinition = "jsonb")
    private String diffJson;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;
}
