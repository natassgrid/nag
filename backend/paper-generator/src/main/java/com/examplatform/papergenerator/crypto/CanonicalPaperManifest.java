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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.papergenerator.crypto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic Canonical Manifest generator for examination papers.
 * Orders keys, normalizes whitespace, formats question payloads deterministically,
 * and computes the canonical manifest SHA-256 digest.
 *
 * Validates: Issue #156
 */
public class CanonicalPaperManifest {

    private static final ObjectMapper CANONICAL_MAPPER = JsonMapper.builder()
            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .build();

    /**
     * Creates a deterministic JSON manifest string.
     */
    public static String toCanonicalJson(ManifestData data) {
        try {
            return CANONICAL_MAPPER.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to produce canonical manifest JSON", e);
        }
    }

    /**
     * Computes the SHA-256 digest of the canonical manifest string.
     */
    public static String computeManifestDigest(ManifestData data) {
        String canonicalJson = toCanonicalJson(data);
        return MerkleTree.sha256Hex(canonicalJson);
    }

    /**
     * Converts a list of question UUIDs or question items into normalized leaf payloads for Merkle Tree creation.
     */
    public static List<String> buildQuestionLeafPayloads(List<UUID> questionIds, UUID examId, String variant) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> leaves = new ArrayList<>();
        String var = (variant != null && !variant.isBlank()) ? variant : "STANDARD";
        for (int i = 0; i < questionIds.size(); i++) {
            UUID qId = questionIds.get(i);
            String leafPayload = String.format("EXAM:%s|VAR:%s|IDX:%d|Q:%s",
                    examId != null ? examId.toString() : "NONE",
                    var,
                    i,
                    qId.toString());
            leaves.add(leafPayload);
        }
        return leaves;
    }

    @Data
    @Builder
    public static class ManifestData {
        private String schemaVersion;
        private UUID examId;
        private String shiftId;
        private String variant;
        private List<UUID> questionIds;
        private Map<String, Integer> topicDistribution;
        private double difficultyScore;
        private boolean isPractice;
        private String generatedAt;
    }
}
