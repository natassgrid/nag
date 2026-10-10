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

package com.examplatform.practice.util;

import com.examplatform.practice.domain.PracticeResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utility methods for parsing question IDs and filtering latest practice responses.
 */
public final class PracticeQuestionUtils {

    private PracticeQuestionUtils() {
    }

    public static List<UUID> extractQuestionIds(String raw, ObjectMapper objectMapper) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        List<UUID> list = new ArrayList<>();
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(trimmed);
                if (root.isArray()) {
                    for (JsonNode n : root) {
                        try {
                            list.add(UUID.fromString(n.asText()));
                        } catch (Exception ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        } else {
            for (String part : trimmed.split(",")) {
                String clean = part.trim().replace("\"", "").replace("'", "");
                try {
                    list.add(UUID.fromString(clean));
                } catch (Exception ignored) {
                }
            }
        }
        return list;
    }

    public static Map<UUID, PracticeResponse> getLatestResponsesByQuestion(List<PracticeResponse> responses) {
        Map<UUID, PracticeResponse> latest = new LinkedHashMap<>();
        for (PracticeResponse r : responses) {
            latest.merge(r.getQuestionId(), r,
                    (existing, incoming) -> incoming.getRevisionSequence() > existing.getRevisionSequence() ? incoming : existing);
        }
        return latest;
    }
    public static List<UUID> resolveOrderedQuestionIds(String rawQuestionIds, Map<UUID, PracticeResponse> latestResponses, ObjectMapper objectMapper) {
        List<UUID> orderedQuestionIds = new ArrayList<>();
        if (rawQuestionIds != null && !rawQuestionIds.isBlank()) {
            orderedQuestionIds.addAll(extractQuestionIds(rawQuestionIds, objectMapper));
        }
        for (UUID qId : latestResponses.keySet()) {
            if (!orderedQuestionIds.contains(qId)) {
                orderedQuestionIds.add(qId);
            }
        }
        return orderedQuestionIds;
    }
}
