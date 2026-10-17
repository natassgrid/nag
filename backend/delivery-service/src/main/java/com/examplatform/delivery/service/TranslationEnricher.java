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

package com.examplatform.delivery.service;

import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.delivery.dto.TranslatedQuestionDeliveryDto;
import com.examplatform.shared.ai.latex.LatexPreservationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Component responsible for querying and attaching published/approved regional language translations
 * to delivery questions while preserving image assets and structure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslationEnricher {

    private static final String FIELD_IMAGE_URL = "imageUrl";
    private static final String FIELD_IMAGE_ALT_TEXT = "imageAltText";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Enriches delivered questions with published and approved regional language translations.
     * Preserves and falls back to question stem images and option images when present.
     */
    public void enrichWithTranslations(List<QuestionDeliveryDto> questions, String tenantId) {
        if (questions == null || questions.isEmpty() || jdbcTemplate == null) {
            return;
        }

        Map<UUID, QuestionDeliveryDto> dtoMap = buildQuestionDtoMap(questions);
        if (dtoMap.isEmpty()) {
            return;
        }

        queryAndAttachTranslations(dtoMap, tenantId);
    }

    private Map<UUID, QuestionDeliveryDto> buildQuestionDtoMap(List<QuestionDeliveryDto> questions) {
        Map<UUID, QuestionDeliveryDto> dtoMap = new HashMap<>();
        for (QuestionDeliveryDto q : questions) {
            if (q.getId() != null) {
                try {
                    dtoMap.put(UUID.fromString(q.getId()), q);
                } catch (IllegalArgumentException _) {
                    // Ignore questions with invalid UUID format
                }
            }
        }
        return dtoMap;
    }

    @SuppressWarnings("java:S2077") // Dynamic inSql only contains '?' placeholders safely parameterized
    private void queryAndAttachTranslations(Map<UUID, QuestionDeliveryDto> dtoMap, String tenantId) {
        List<UUID> questionUuids = new ArrayList<>(dtoMap.keySet());
        try {
            String inSql = String.join(",", Collections.nCopies(questionUuids.size(), "?"));
            String sql = String.format("""
                SELECT question_id, language_code, translated_payload
                FROM question_service.translation
                WHERE (tenant_id = ? OR tenant_id = 'default')
                  AND status IN ('PUBLISHED', 'APPROVED')
                  AND question_id IN (%s)
                """, inSql);

            List<Object> params = new ArrayList<>();
            params.add(tenantId != null ? tenantId : "default");
            params.addAll(questionUuids);

            jdbcTemplate.query(sql, rs -> {
                UUID qId = extractQuestionId(rs.getObject("question_id"));
                String langCode = rs.getString("language_code");
                String payload = rs.getString("translated_payload");
                processTranslationRow(dtoMap.get(qId), qId, langCode, payload);
            }, params.toArray());
        } catch (Exception e) {
            log.warn("Could not query translations for delivery questions: {}", e.getMessage());
        }
    }

    private UUID extractQuestionId(Object obj) {
        if (obj instanceof UUID uuid) {
            return uuid;
        }
        if (obj != null) {
            try {
                return UUID.fromString(obj.toString());
            } catch (Exception _) {
                // Ignore unparseable question ID object
            }
        }
        return null;
    }

    private void processTranslationRow(QuestionDeliveryDto qDto, UUID qId, String langCode, String payload) {
        if (qDto == null || payload == null || payload.isBlank()) {
            return;
        }

        try {
            JsonNode node = objectMapper.readTree(payload);
            String rawContent = node.path("content").asText(null);
            if (rawContent == null) {
                return;
            }
            String transContent = LatexPreservationUtil.cleanStrayUnderscores(rawContent);

            String rawExplanation = node.path("explanation").asText(null);
            String transExplanation = rawExplanation != null ? LatexPreservationUtil.cleanStrayUnderscores(rawExplanation) : null;
            String transImageUrl = getTextFieldOrDefault(node, FIELD_IMAGE_URL, qDto.getImageUrl());
            String transImageAltText = getTextFieldOrDefault(node, FIELD_IMAGE_ALT_TEXT, qDto.getImageAltText());
            List<QuestionOptionDeliveryDto> transOptions = parseTranslatedOptions(node.path("options"), qDto.getOptions());

            if (qDto.getTranslations() == null) {
                qDto.setTranslations(new HashMap<>());
            }
            qDto.getTranslations().put(langCode, TranslatedQuestionDeliveryDto.builder()
                    .languageCode(langCode)
                    .content(transContent)
                    .imageUrl(transImageUrl)
                    .imageAltText(transImageAltText)
                    .options(transOptions)
                    .explanation(transExplanation)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to parse translation payload for question {}: {}", qId, e.getMessage());
        }
    }

    private List<QuestionOptionDeliveryDto> parseTranslatedOptions(
            JsonNode optNode, List<QuestionOptionDeliveryDto> baseOptions) {
        List<QuestionOptionDeliveryDto> transOptions = new ArrayList<>();
        if (!optNode.isArray() || baseOptions == null) {
            return transOptions;
        }

        for (int i = 0; i < optNode.size(); i++) {
            JsonNode o = optNode.get(i);
            String rawOptText = o.path("text").asText("");
            String optText = LatexPreservationUtil.cleanStrayUnderscores(rawOptText);
            String optId = o.has("id") ? o.path("id").asText("") : String.valueOf((char) ('A' + i));
            String optImg = getTextFieldOrDefault(o, FIELD_IMAGE_URL, null);
            String optAlt = getTextFieldOrDefault(o, FIELD_IMAGE_ALT_TEXT, null);

            if (optImg == null && i < baseOptions.size()) {
                optImg = baseOptions.get(i).getImageUrl();
                if (optAlt == null) {
                    optAlt = baseOptions.get(i).getImageAltText();
                }
            }

            transOptions.add(QuestionOptionDeliveryDto.builder()
                    .id(optId)
                    .index(i)
                    .originalIndex(i)
                    .text(optText)
                    .imageUrl(optImg)
                    .imageAltText(optAlt)
                    .build());
        }
        return transOptions;
    }

    private static String getTextFieldOrDefault(JsonNode node, String fieldName, String defaultValue) {
        if (node != null && node.has(fieldName) && !node.get(fieldName).isNull()) {
            return node.get(fieldName).asText(null);
        }
        return defaultValue;
    }
}
