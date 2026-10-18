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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Paper entity representing a generated examination paper.
 * Stores question selection (IDs + ordering) as JSONB, along with
 * statistical metadata, shift-specific AES-256 paper encryption,
 * binary SHA-256 Merkle tree root hash, and public DLT ledger anchoring proofs.
 *
 * Validates: Requirements 8.7, Issue #156
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true, exclude = {"sections"})
@ToString(callSuper = true, exclude = {"sections"})
@Entity
@Table(name = "paper", schema = "paper_generator")
public class Paper extends BaseEntity {

    /** Meaningful human-readable name of the generated paper. */
    @Column(name = SharedConstants.NAME)
    private String name;

    @Column(name = "exam_id", nullable = false, columnDefinition = "uuid")
    private UUID examId;

    @Column(name = "blueprint_id", columnDefinition = "uuid")
    private UUID blueprintId;

    @Builder.Default
    @Column(name = "blueprint_version", nullable = false)
    private int blueprintVersion = 1;

    @Builder.Default
    @OneToMany(mappedBy = "paper", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private java.util.List<PaperSection> sections = new java.util.ArrayList<>();

    public void addSection(PaperSection section) {
        if (sections == null) sections = new java.util.ArrayList<>();
        sections.add(section);
        section.setPaper(this);
    }

    @Column(name = "shift_id", nullable = false)
    private String shiftId;

    @Column(name = SharedConstants.STATUS, nullable = false, length = 20)
    private String status;

    /** Indicates whether this paper is generated specifically for candidate practice and learning. */
    @Column(name = "is_practice", nullable = false)
    private boolean isPractice;

    /** Variant designation for multi-set examinations (e.g., SET-A, SET-B, STANDARD). */
    @Column(name = "variant", length = 50)
    private String variant;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "paper_definition_json", columnDefinition = "jsonb")
    private String paperDefinitionJson;

    @Column(name = "difficulty_score")
    private double difficultyScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "topic_distribution_json", columnDefinition = "jsonb")
    private String topicDistributionJson;

    @Column(name = "encrypted_package_ref", columnDefinition = "TEXT")
    private String encryptedPackageRef;

    @Column(name = "encryption_key_id")
    private String encryptionKeyId;

    @Column(name = "generated_by", columnDefinition = "uuid")
    private UUID generatedBy;

    // --- Cryptographic Merkle Tree & Public Ledger Anchoring (Issue #156) ---

    /** Binary SHA-256 Merkle root hash of all normalized question leaves in this paper. */
    @Column(name = "paper_root_hash", length = 64)
    private String paperRootHash;

    /** SHA-256 hash of the canonical sorted paper manifest JSON. */
    @Column(name = "manifest_digest", length = 64)
    private String manifestDigest;

    /** Public DLT / Blockchain transaction hash confirming immutable attestation. */
    @Column(name = "ledger_tx_hash")
    private String ledgerTxHash;

    /** Hedera Consensus Timestamp or DLT consensus time string. */
    @Column(name = "ledger_consensus_timestamp", length = 64)
    private String ledgerConsensusTimestamp;

    /** Ledger block or sequence number. */
    @Column(name = "ledger_block_number")
    private Long ledgerBlockNumber;

    /** Direct public explorer link to inspect the consensus transaction. */
    @Column(name = "ledger_explorer_url", columnDefinition = "TEXT")
    private String ledgerExplorerUrl;

    /** Network name where paper is anchored (e.g. HEDERA_TESTNET, NAG_MOCK_LEDGER). */
    @Column(name = "ledger_network", length = 100)
    private String ledgerNetwork;

    /** Merkle audit proof JSON mapping question IDs to proof steps. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "merkle_proof_json", columnDefinition = "jsonb")
    private String merkleProofJson;

    /** Timestamp when ledger attestation was confirmed. */
    @Column(name = "anchored_at")
    private Instant anchoredAt;

    /** Timestamp when time-locked encryption key is authorized for distribution. */
    @Column(name = "time_lock_release_at")
    private Instant timeLockReleaseAt;

    /** Whether the paper is currently locked under envelope encryption. */
    @Column(name = "is_time_locked")
    private Boolean isTimeLocked;
}
