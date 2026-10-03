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

package com.examplatform.papergenerator.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Merkle Tree & Cryptographic Integrity Tests")
class MerkleTreeTest {

    @Test
    @DisplayName("Constructs binary Merkle tree and generates deterministic root hash for questions")
    void testMerkleTreeDeterministicRoot() {
        List<String> leaves = List.of(
                "EXAM:e1|VAR:SET-A|IDX:0|Q:q1",
                "EXAM:e1|VAR:SET-A|IDX:1|Q:q2",
                "EXAM:e1|VAR:SET-A|IDX:2|Q:q3",
                "EXAM:e1|VAR:SET-A|IDX:3|Q:q4"
        );

        MerkleTree tree1 = new MerkleTree(leaves);
        MerkleTree tree2 = new MerkleTree(leaves);

        assertThat(tree1.getRootHash()).isNotBlank();
        assertThat(tree1.getRootHash()).isEqualTo(tree2.getRootHash());
        assertThat(tree1.getLeafCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Handles odd number of leaves gracefully by pairing last leaf")
    void testOddNumberOfLeaves() {
        List<String> leaves = List.of(
                "Q1: Content 1",
                "Q2: Content 2",
                "Q3: Content 3"
        );

        MerkleTree tree = new MerkleTree(leaves);
        assertThat(tree.getRootHash()).isNotBlank();
        assertThat(tree.getLeafCount()).isEqualTo(3);

        // Verification of proof for each leaf in odd tree
        for (int i = 0; i < leaves.size(); i++) {
            MerkleTree.MerkleProof proof = tree.getProof(i);
            boolean verified = MerkleTree.verifyProof(proof.getLeafHash(), proof.getSteps(), tree.getRootHash());
            assertTrue(verified, "Proof failed for leaf index " + i);
        }
    }

    @Test
    @DisplayName("Generates valid Merkle inclusion proofs for all leaves")
    void testMerkleInclusionProofs() {
        List<String> leaves = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            leaves.add("QUESTION_PAYLOAD_" + i);
        }

        MerkleTree tree = new MerkleTree(leaves);

        for (int i = 0; i < leaves.size(); i++) {
            MerkleTree.MerkleProof proof = tree.getProof(i);
            assertThat(proof.getLeafIndex()).isEqualTo(i);
            assertThat(proof.getSteps()).hasSize(3); // log2(8) = 3 levels

            boolean isValid = MerkleTree.verifyProof(proof.getLeafHash(), proof.getSteps(), tree.getRootHash());
            assertTrue(isValid, "Merkle inclusion proof verification must succeed for leaf " + i);
        }
    }

    @Test
    @DisplayName("Detects tampering: modified question payload fails root hash match and inclusion proof")
    void testTamperDetection() {
        List<String> originalLeaves = List.of(
                "Q1: What is 2 + 2? | Options: [3, 4, 5] | Key: 4",
                "Q2: What is the capital of France? | Options: [Berlin, Paris] | Key: Paris",
                "Q3: Solve 10 / 2 | Options: [2, 5, 10] | Key: 5"
        );

        MerkleTree originalTree = new MerkleTree(originalLeaves);
        String originalRoot = originalTree.getRootHash();

        // Tampered leaf list
        List<String> tamperedLeaves = List.of(
                "Q1: What is 2 + 2? | Options: [3, 4, 5] | Key: 4",
                "Q2: What is the capital of France? | Options: [London, Paris] | Key: Paris", // altered option
                "Q3: Solve 10 / 2 | Options: [2, 5, 10] | Key: 5"
        );

        MerkleTree tamperedTree = new MerkleTree(tamperedLeaves);
        String tamperedRoot = tamperedTree.getRootHash();

        assertThat(tamperedRoot).isNotEqualTo(originalRoot);

        // Tampered leaf hash against original root proof
        MerkleTree.MerkleProof originalProofQ2 = originalTree.getProof(1);
        String tamperedLeafHashQ2 = MerkleTree.computeLeafHash(tamperedLeaves.get(1));

        boolean verified = MerkleTree.verifyProof(tamperedLeafHashQ2, originalProofQ2.getSteps(), originalRoot);
        assertFalse(verified, "Tampered leaf must fail verification against original root");
    }

    @Test
    @DisplayName("CanonicalPaperManifest builds deterministic manifest digest")
    void testCanonicalPaperManifestDigest() {
        UUID examId = UUID.randomUUID();
        List<UUID> qIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        CanonicalPaperManifest.ManifestData m1 = CanonicalPaperManifest.ManifestData.builder()
                .schemaVersion("1.0")
                .examId(examId)
                .shiftId("SHIFT-1")
                .variant("SET-A")
                .questionIds(qIds)
                .difficultyScore(2.5)
                .isPractice(false)
                .generatedAt("2026-10-03T11:45:00Z")
                .build();

        CanonicalPaperManifest.ManifestData m2 = CanonicalPaperManifest.ManifestData.builder()
                .schemaVersion("1.0")
                .examId(examId)
                .shiftId("SHIFT-1")
                .variant("SET-A")
                .questionIds(qIds)
                .difficultyScore(2.5)
                .isPractice(false)
                .generatedAt("2026-10-03T11:45:00Z")
                .build();

        String digest1 = CanonicalPaperManifest.computeManifestDigest(m1);
        String digest2 = CanonicalPaperManifest.computeManifestDigest(m2);

        assertThat(digest1).isEqualTo(digest2);
        assertThat(digest1).hasSize(64);
    }
}
