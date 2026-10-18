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

import com.examplatform.questionbank.crypto.EncryptedFieldConverter;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
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
 * <p>Each question strictly belongs to exactly one SubTopic via mandatory {@code subtopic_id} (NOT NULL),
 * referencing the Subject &rarr; Topic &rarr; Subtopic relational spine. Denormalized string columns
 * have been dropped in favor of ID-based lookups and TaxonomyClient display resolution.</p>
 *
 * <h3>Partitioning Strategy (NFR-5)</h3>
 * <p>The underlying table {@code question_service.question} is hash-partitioned
 * by {@code subject_id} into 8 partitions. The database-level primary key is a
 * composite {@code (id, subject_id)}.</p>
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

    /** Mandatory numeric FK to {@code question_service.subtopic.id}. */
    @Column(name = "subtopic_id", nullable = false)
    private Long subtopicId;

    @Column(name = "chapter", length = 200)
    private String chapter;

    @Column(name = SharedConstants.DIFFICULTY, nullable = false, length = 20)
    private String difficulty;

    /** Expected completion time in seconds for pacing telemetry and performance analysis. */
    @Column(name = "benchmark_time_seconds", nullable = false)
    @Builder.Default
    private int benchmarkTimeSeconds = 90;

    @Column(name = "cognitive_level", nullable = false, length = 20)
    private String cognitiveLevel;

    @Column(name = "question_type", nullable = false, length = 30)
    private String questionType;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = SharedConstants.CONTENT, columnDefinition = "TEXT")
    private String content;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "answer_key", columnDefinition = "TEXT")
    private String answerKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private List<QuestionOption> options;

    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "source_references", columnDefinition = "TEXT")
    private String sourceReferences;

    @Transient
    private float[] embedding;

    /** Lightweight flag indicating this question contains image/SVG media. */
    @Column(name = "has_images", nullable = false)
    @Builder.Default
    private boolean hasImages = false;

    @Column(name = "state", nullable = false, length = 20)
    @Builder.Default
    private String state = QuestionStateConstants.DRAFT;

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private int usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "used_in_shift_ids_json", columnDefinition = "jsonb")
    private String usedInShiftIdsJson;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "reviewer_id")
    private UUID reviewerId;

    /** Reviewer feedback comments persisted when a question is rejected back to DRAFT. Visible to the author for revision. */
    @Column(name = "review_comments", columnDefinition = "TEXT")
    private String reviewComments;

    /** Optional FK to passage if this question belongs to a paragraph/comprehension group. */
    @Column(name = "passage_id")
    private UUID passageId;

    /** 0-based order index of this sub-question within its passage group. */
    @Column(name = "passage_order_index")
    private Integer passageOrderIndex;
}
