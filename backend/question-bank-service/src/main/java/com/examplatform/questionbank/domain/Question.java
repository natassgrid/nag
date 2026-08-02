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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Question entity with per-question AES-256 encryption for content fields
 * and pgvector embedding column for similarity detection.
 *
 * Hash-partitioned across 16 partitions by id for horizontal scalability.
 *
 * Validates: Requirements 4.1, 4.5, 19.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "question", schema = "question_service")
public class Question extends BaseEntity {

    @Column(name = "subject", nullable = false, length = 100)
    private String subject;

    @Column(name = "topic", nullable = false, length = 200)
    private String topic;

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
    @Column(name = "embedding_vector", columnDefinition = "jsonb")
    private String embeddingVector;

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
