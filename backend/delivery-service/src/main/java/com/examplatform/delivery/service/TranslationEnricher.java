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

package com.examplatform.delivery.service;

import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.delivery.dto.TranslatedQuestionDeliveryDto;
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

        List<UUID> questionUuids = new ArrayList<>();
        Map<UUID, QuestionDeliveryDto> dtoMap = new HashMap<>();
        for (QuestionDeliveryDto q : questions) {
            try {
                if (q.getId() != null) {
                    UUID uid = UUID.fromString(q.getId());
                    questionUuids.add(uid);
                    dtoMap.put(uid, q);
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (questionUuids.isEmpty()) {
            return;
        }

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
                UUID qId = null;
                Object obj = rs.getObject("question_id");
                if (obj instanceof UUID) {
                    qId = (UUID) obj;
                } else if (obj != null) {
                    try {
                        qId = UUID.fromString(obj.toString());
                    } catch (Exception ignored) {}
                }
                String langCode = rs.getString("language_code");
                String payload = rs.getString("translated_payload");

                QuestionDeliveryDto qDto = dtoMap.get(qId);
                if (qDto != null && payload != null && !payload.isBlank()) {
                    try {
                        JsonNode node = objectMapper.readTree(payload);
                        String transContent = node.path("content").asText(null);
                        String transExplanation = node.path("explanation").asText(null);
                        String transImageUrl = node.has("imageUrl") && !node.get("imageUrl").isNull()
                                ? node.get("imageUrl").asText(null)
                                : qDto.getImageUrl();
                        String transImageAltText = node.has("imageAltText") && !node.get("imageAltText").isNull()
                                ? node.get("imageAltText").asText(null)
                                : qDto.getImageAltText();

                        List<QuestionOptionDeliveryDto> transOptions = new ArrayList<>();
                        JsonNode optNode = node.path("options");
                        if (optNode.isArray() && qDto.getOptions() != null) {
                            for (int i = 0; i < optNode.size(); i++) {
                                JsonNode o = optNode.get(i);
                                String optText = o.path("text").asText("");
                                String optId = o.has("id") ? o.path("id").asText("") : String.valueOf((char) ('A' + i));
                                String optImg = o.has("imageUrl") && !o.get("imageUrl").isNull()
                                        ? o.get("imageUrl").asText(null) : null;
                                String optAlt = o.has("imageAltText") && !o.get("imageAltText").isNull()
                                        ? o.get("imageAltText").asText(null) : null;

                                if (optImg == null && i < qDto.getOptions().size()) {
                                    optImg = qDto.getOptions().get(i).getImageUrl();
                                    if (optAlt == null) {
                                        optAlt = qDto.getOptions().get(i).getImageAltText();
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
                        }

                        if (transContent != null) {
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
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse translation payload for question {}: {}", qId, e.getMessage());
                    }
                }
            }, params.toArray());
        } catch (Exception e) {
            log.warn("Could not query translations for delivery questions: {}", e.getMessage());
        }
    }
}
