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

    @Column(name = "paper_definition_json", columnDefinition = "jsonb")
    private String paperDefinitionJson;

    @Column(name = "difficulty_score")
    private double difficultyScore;

    @Column(name = "topic_distribution_json", columnDefinition = "jsonb")
    private String topicDistributionJson;

    @Column(name = "encrypted_package_ref")
    private String encryptedPackageRef;

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "generated_by", columnDefinition = "uuid")
    private UUID generatedBy;
}
