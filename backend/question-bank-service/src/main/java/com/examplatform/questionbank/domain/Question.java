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
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Question entity with per-question AES-256 encryption for content fields
 * and pgvector embedding column for similarity detection.
 *
 * <h3>Hierarchy references (numeric FKs)</h3>
 * <p>Each question references the Subject &rarr; Topic &rarr; Subtopic hierarchy
 * by compact numeric ({@code BIGINT}) foreign keys: {@code subject_id} (required),
 * {@code topic_id} (required), and {@code subtopic_id} (optional). The
 * corresponding {@code subject}, {@code topic}, and {@code subtopic} name columns
 * are also stored (denormalized) because reviewer routing, full-text search,
 * similarity detection, version diffs, and human-readable export all operate on
 * the names. The ids are the source of truth for the hierarchy link; the names
 * are kept in sync by the service layer at write time.
 *
 * <h3>Partitioning Strategy (NFR-5)</h3>
 * <p>The underlying table {@code question_service.question} is hash-partitioned
 * by {@code subject_id} into 8 partitions. The database-level primary key is a
 * composite {@code (id, subject_id)} — required by PostgreSQL since the partition
 * key must be part of the primary key constraint. Partitioning by the numeric
 * {@code subject_id} (rather than the {@code subject} name string) yields a
 * narrower partition key and more even hash distribution.
 *
 * <p><strong>JPA Compatibility:</strong> This entity uses only {@code id} (UUID v7)
 * as the JPA {@link jakarta.persistence.Id @Id}. This is valid because:
 * <ul>
 *   <li>UUID v7 is globally unique across all partitions — no collision possible.</li>
 *   <li>{@code findById(UUID)} scans all partitions but still returns at most one row.</li>
 *   <li>Queries that include {@code subjectId} in the WHERE clause benefit from
 *       partition pruning (PostgreSQL only scans the target partition).</li>
 *   <li>A JPA {@code @IdClass} or {@code @EmbeddedId} is NOT required because
 *       JPA identity only needs application-level uniqueness, which UUID v7 guarantees.</li>
 * </ul>
 *
 * <p><strong>Performance Note:</strong> Repository queries SHOULD include
 * {@code subjectId} in their predicates whenever possible to enable partition
 * pruning.
 *
 * Validates: Requirements 4.1, 4.5, NFR-5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "question", schema = "question_service")
public class Question extends BaseEntity {

    /** Numeric FK to {@code question_service.subject.id}. Part of the composite PK / partition key. */
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    /** Numeric FK to {@code question_service.topic.id}. */
    @Column(name = "topic_id", nullable = false)
    private Long topicId;

    /** Numeric FK to {@code question_service.subtopic.id}. Optional. */
    @Column(name = "subtopic_id")
    private Long subtopicId;

    /** Denormalized subject name, kept in sync with {@link #subjectId} at write time. */
    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    /** Denormalized topic name, kept in sync with {@link #topicId} at write time. */
    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

    /** Denormalized subtopic name, kept in sync with {@link #subtopicId} at write time. */
    @Column(name = "subtopic", length = 200)
    private String subtopic;

    @Column(name = "chapter", length = 200)
    private String chapter;

    @Column(name = "difficulty", nullable = false, length = 20)
    private String difficulty;

    @Column(name = "cognitive_level", nullable = false, length = 20)
    private String cognitiveLevel;

    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "answer_key", columnDefinition = "TEXT")
    private String answerKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<QuestionOption> options;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "\"references\"", columnDefinition = "TEXT")
    private String references;

    @Transient
    private float[] embedding;

    @Column(name = "state", nullable = false, length = 20)
    @Builder.Default
    private String state = "DRAFT";

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private int usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "used_in_exam_ids_json", columnDefinition = "jsonb")
    private String usedInExamIdsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "used_in_shift_ids_json", columnDefinition = "jsonb")
    private String usedInShiftIdsJson;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;
}
