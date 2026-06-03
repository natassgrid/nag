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

    @Column(name = "diff_json", nullable = false, columnDefinition = "JSONB")
    private String diffJson;

    @Convert(converter = EncryptedFieldConverter.class)
    @Column(name = "snapshot_json", columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;
}
