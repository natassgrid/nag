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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

/**
 * High-performance Binary SHA-256 Merkle Tree Engine for Examination Papers.
 * Implements deterministic leaf node hashing, pairwise tree aggregation, root calculation,
 * inclusion proof generation (audit path), and zero-knowledge verification.
 *
 * Validates: Issue #156, Requirement 8.7 (Cryptographic Merkle Tree Anchoring)
 */
public class MerkleTree {

    public static final String LEAF_PREFIX = "NAG_LEAF_V1:";
    public static final String NODE_PREFIX = "NAG_NODE_V1:";

    private final List<String> leafHashes;
    private final List<List<String>> levels;
    private final String rootHash;

    public MerkleTree(List<String> rawLeafPayloads) {
        if (rawLeafPayloads == null || rawLeafPayloads.isEmpty()) {
            this.leafHashes = Collections.emptyList();
            this.levels = Collections.emptyList();
            this.rootHash = sha256Hex(LEAF_PREFIX + "EMPTY_TREE");
            return;
        }

        List<String> leaves = new ArrayList<>();
        for (String payload : rawLeafPayloads) {
            leaves.add(computeLeafHash(payload));
        }
        this.leafHashes = Collections.unmodifiableList(leaves);

        List<List<String>> treeLevels = new ArrayList<>();
        treeLevels.add(new ArrayList<>(leaves));

        List<String> currentLevel = leaves;
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                nextLevel.add(combineNodes(left, right));
            }
            treeLevels.add(nextLevel);
            currentLevel = nextLevel;
        }

        this.levels = Collections.unmodifiableList(treeLevels);
        this.rootHash = treeLevels.get(treeLevels.size() - 1).get(0);
    }

    /**
     * Creates a Merkle Tree from already hashed leaf nodes.
     */
    public static MerkleTree fromLeafHashes(List<String> leafHashes) {
        if (leafHashes == null || leafHashes.isEmpty()) {
            return new MerkleTree(Collections.emptyList());
        }
        return new MerkleTree(leafHashes, true);
    }

    private MerkleTree(List<String> leafHashes, boolean alreadyHashed) {
        this.leafHashes = Collections.unmodifiableList(new ArrayList<>(leafHashes));

        List<List<String>> treeLevels = new ArrayList<>();
        treeLevels.add(new ArrayList<>(leafHashes));

        List<String> currentLevel = leafHashes;
        while (currentLevel.size() > 1) {
            List<String> nextLevel = new ArrayList<>();
            for (int i = 0; i < currentLevel.size(); i += 2) {
                String left = currentLevel.get(i);
                String right = (i + 1 < currentLevel.size()) ? currentLevel.get(i + 1) : left;
                nextLevel.add(combineNodes(left, right));
            }
            treeLevels.add(nextLevel);
            currentLevel = nextLevel;
        }

        this.levels = Collections.unmodifiableList(treeLevels);
        this.rootHash = treeLevels.get(treeLevels.size() - 1).get(0);
    }

    public String getRootHash() {
        return rootHash;
    }

    public List<String> getLeafHashes() {
        return leafHashes;
    }

    public int getLeafCount() {
        return leafHashes.size();
    }

    /**
     * Generates a compact audit proof (Merkle path) for the leaf at leafIndex.
     */
    public MerkleProof getProof(int leafIndex) {
        if (leafIndex < 0 || leafIndex >= leafHashes.size()) {
            throw new IndexOutOfBoundsException("Leaf index " + leafIndex + " out of bounds for size " + leafHashes.size());
        }

        String targetLeaf = leafHashes.get(leafIndex);
        List<MerkleProofStep> proofSteps = new ArrayList<>();

        int currentIndex = leafIndex;
        for (int level = 0; level < levels.size() - 1; level++) {
            List<String> currentLevel = levels.get(level);
            boolean isRightNode = (currentIndex % 2 == 1);
            int siblingIndex = isRightNode ? currentIndex - 1 : currentIndex + 1;

            if (siblingIndex < currentLevel.size()) {
                String siblingHash = currentLevel.get(siblingIndex);
                proofSteps.add(new MerkleProofStep(siblingHash, isRightNode ? "LEFT" : "RIGHT"));
            } else {
                // If odd leaf with no right sibling, sibling is itself on the right
                proofSteps.add(new MerkleProofStep(currentLevel.get(currentIndex), "RIGHT"));
            }

            currentIndex /= 2;
        }

        return MerkleProof.builder()
                .leafIndex(leafIndex)
                .leafHash(targetLeaf)
                .rootHash(rootHash)
                .steps(proofSteps)
                .build();
    }

    /**
     * Verifies if a given leaf hash and Merkle proof reconstructs the expected root hash.
     */
    public static boolean verifyProof(String leafHash, List<MerkleProofStep> steps, String expectedRootHash) {
        if (leafHash == null || expectedRootHash == null || steps == null) {
            return false;
        }

        String currentHash = leafHash;
        for (MerkleProofStep step : steps) {
            if ("LEFT".equalsIgnoreCase(step.getPosition())) {
                currentHash = combineNodes(step.getHash(), currentHash);
            } else {
                currentHash = combineNodes(currentHash, step.getHash());
            }
        }

        return currentHash.equalsIgnoreCase(expectedRootHash);
    }

    public static String computeLeafHash(String rawContent) {
        return sha256Hex(LEAF_PREFIX + (rawContent != null ? rawContent : ""));
    }

    public static String combineNodes(String leftHex, String rightHex) {
        return sha256Hex(NODE_PREFIX + leftHex + ":" + rightHex);
    }

    public static String sha256Hex(String input) {
        return sha256Hex(input.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest algorithm not found", e);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerkleProof {
        private int leafIndex;
        private String leafHash;
        private String rootHash;
        private List<MerkleProofStep> steps;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MerkleProofStep {
        private String hash;
        /** "LEFT" if sibling is to the left of the path node, "RIGHT" otherwise. */
        private String position;
    }
}
