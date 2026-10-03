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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Component responsible for parsing and normalizing question payloads from JSON, JsonNodes,
 * and decrypted examination paper structures.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionDeliveryParser {

    private final ObjectMapper objectMapper;

    public List<QuestionDeliveryDto> parseFromPaperJson(String decryptedPaper) {
        List<QuestionDeliveryDto> questions = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(decryptedPaper);
            JsonNode questionsNode = root.path("questions");
            if (questionsNode.isArray()) {
                int index = 1;
                for (JsonNode qNode : questionsNode) {
                    QuestionDeliveryDto q = parseSingleQuestionNode(qNode, index++);
                    if (q != null) {
                        questions.add(q);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not parse decrypted paper JSON: {}", e.getMessage());
        }
        return questions;
    }

    public QuestionDeliveryDto parseSingleQuestionNode(JsonNode qNode, int sequenceNumber) {
        try {
            String id = qNode.has("id") ? qNode.get("id").asText() : UUID.randomUUID().toString();
            String content = qNode.has("content") ? qNode.get("content").asText() :
                    (qNode.has("text") ? qNode.get("text").asText() : "");
            String subject = qNode.has("subject") ? qNode.get("subject").asText() : null;
            String topic = qNode.has("topic") ? qNode.get("topic").asText() : "General";
            String explanation = qNode.has("explanation") ? qNode.get("explanation").asText() : null;
            String answerKey = qNode.has("answerKey") ? qNode.get("answerKey").asText() :
                    (qNode.has("answer_key") ? qNode.get("answer_key").asText() : null);
            String imageUrl = qNode.has("imageUrl") && !qNode.get("imageUrl").isNull() ? qNode.get("imageUrl").asText(null) : null;
            String imageAltText = qNode.has("imageAltText") && !qNode.get("imageAltText").isNull() ? qNode.get("imageAltText").asText(null) : null;
            boolean hasImages = qNode.has("hasImages") && qNode.get("hasImages").asBoolean();

            String questionType = qNode.has("questionType") ? qNode.get("questionType").asText() :
                    (qNode.has("question_type") ? qNode.get("question_type").asText() : "SINGLE_MCQ");
            String passageId = qNode.has("passageId") ? qNode.get("passageId").asText() :
                    (qNode.has("passage_id") ? qNode.get("passage_id").asText() : null);
            String passageContent = qNode.has("passageContent") ? qNode.get("passageContent").asText() :
                    (qNode.has("passage_content") ? qNode.get("passage_content").asText() : null);
            Integer passageOrderIndex = qNode.has("passageOrderIndex") ? qNode.get("passageOrderIndex").asInt() :
                    (qNode.has("passage_order_index") ? qNode.get("passage_order_index").asInt() : null);

            List<QuestionOptionDeliveryDto> options = new ArrayList<>();
            Integer correctOptionIndex = null;
            JsonNode optionsNode = qNode.get("options");

            if (optionsNode != null && optionsNode.isArray()) {
                for (int i = 0; i < optionsNode.size(); i++) {
                    JsonNode optNode = optionsNode.get(i);
                    String optText;
                    String optId = "";
                    String optImageUrl = null;
                    String optImageAltText = null;
                    boolean isCorrect = false;

                    if (optNode.isObject()) {
                        optText = optNode.has("text") ? optNode.get("text").asText() : optNode.asText();
                        optId = optNode.has("id") ? optNode.get("id").asText() : String.valueOf((char) ('A' + i));
                        isCorrect = optNode.has("isCorrect") && optNode.get("isCorrect").asBoolean();
                        optImageUrl = optNode.has("imageUrl") && !optNode.get("imageUrl").isNull() ? optNode.get("imageUrl").asText(null) : null;
                        optImageAltText = optNode.has("imageAltText") && !optNode.get("imageAltText").isNull() ? optNode.get("imageAltText").asText(null) : null;
                    } else {
                        optText = optNode.asText();
                        optId = String.valueOf((char) ('A' + i));
                    }

                    if (optImageUrl != null && !optImageUrl.isBlank()) {
                        hasImages = true;
                    }

                    if (isCorrect || (answerKey != null && (answerKey.equalsIgnoreCase(optId) || answerKey.equalsIgnoreCase(optText)))) {
                        correctOptionIndex = i;
                    }

                    options.add(QuestionOptionDeliveryDto.builder()
                            .id(optId)
                            .index(i)
                            .originalIndex(i)
                            .text(optText)
                            .imageUrl(optImageUrl)
                            .imageAltText(optImageAltText)
                            .build());
                }
            }

            if (correctOptionIndex == null && qNode.has("correctOptionIndex")) {
                correctOptionIndex = qNode.get("correctOptionIndex").asInt();
            }

            String secId = resolveSectionId(subject, sequenceNumber);
            String secName = resolveSectionName(subject, sequenceNumber);

            return QuestionDeliveryDto.builder()
                    .id(id)
                    .text(content)
                    .imageUrl(imageUrl)
                    .imageAltText(imageAltText)
                    .hasImages(hasImages)
                    .options(options)
                    .marks(qNode.has("marks") ? qNode.get("marks").asDouble() : 2.0)
                    .negativeMarks(qNode.has("negativeMarks") ? qNode.get("negativeMarks").asDouble() : 0.5)
                    .sectionId(secId)
                    .sectionName(secName)
                    .topic(topic)
                    .correctOptionIndex(correctOptionIndex)
                    .explanation(explanation)
                    .questionType(questionType)
                    .passageId(passageId)
                    .passageContent(passageContent)
                    .passageOrderIndex(passageOrderIndex)
                    .build();
        } catch (Exception e) {
            log.warn("Error parsing individual question node: {}", e.getMessage());
            return null;
        }
    }

    public String resolveSectionId(String subject, int sequenceNumber) {
        if (subject != null) {
            String lower = subject.toLowerCase();
            if (lower.contains("reasoning") || lower.contains("intelligence")) return "sec-1";
            if (lower.contains("awareness") || lower.contains("general studies") || lower.contains("current")) return "sec-2";
            if (lower.contains("quantitative") || lower.contains("mathemat")) return "sec-3";
            if (lower.contains("english") || lower.contains("comprehension")) return "sec-4";
        }
        return "sec-" + ((sequenceNumber - 1) / 25 + 1);
    }

    public String resolveSectionName(String subject, int sequenceNumber) {
        if (subject != null) {
            String lower = subject.toLowerCase();
            if (lower.contains("reasoning") || lower.contains("intelligence")) return "General Intelligence & Reasoning";
            if (lower.contains("awareness") || lower.contains("general studies") || lower.contains("current")) return "General Awareness";
            if (lower.contains("quantitative") || lower.contains("mathemat")) return "Quantitative Aptitude";
            if (lower.contains("english") || lower.contains("comprehension")) return "English Comprehension";
            return subject;
        }
        return "Section " + ((sequenceNumber - 1) / 25 + 1);
    }

    public List<UUID> extractQuestionUuidsFromJson(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        List<UUID> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            extractUuidsFromNode(root, result);
        } catch (Exception e) {
            log.warn("Failed to parse question UUIDs from JSON: {}", e.getMessage());
        }
        return result;
    }

    public List<UUID> extractQuestionUuidsFromJsonOrString(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        raw = raw.trim();
        if (raw.startsWith("[") || raw.startsWith("{")) {
            return extractQuestionUuidsFromJson(raw);
        }
        // Fallback to comma-separated UUID strings
        List<UUID> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim().replace("\"", "").replace("'", "");
            try {
                if (!trimmed.isBlank()) {
                    list.add(UUID.fromString(trimmed));
                }
            } catch (IllegalArgumentException ignored) {}
        }
        return list;
    }

    public void extractUuidsFromNode(JsonNode node, List<UUID> accumulator) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            try {
                accumulator.add(UUID.fromString(node.asText()));
            } catch (IllegalArgumentException ignored) {}
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                extractUuidsFromNode(item, accumulator);
            }
            return;
        }
        if (node.isObject()) {
            if (node.has("questionIds")) {
                extractUuidsFromNode(node.get("questionIds"), accumulator);
            }
            if (node.has("questions")) {
                extractUuidsFromNode(node.get("questions"), accumulator);
            }
            if (node.has("questionGroups")) {
                extractUuidsFromNode(node.get("questionGroups"), accumulator);
            }
            if (node.has("sections")) {
                extractUuidsFromNode(node.get("sections"), accumulator);
            }
            if (node.has("id")) {
                try {
                    accumulator.add(UUID.fromString(node.get("id").asText()));
                } catch (IllegalArgumentException ignored) {}
            } else if (node.has("questionId")) {
                try {
                    accumulator.add(UUID.fromString(node.get("questionId").asText()));
                } catch (IllegalArgumentException ignored) {}
            } else if (node.has("question_id")) {
                try {
                    accumulator.add(UUID.fromString(node.get("question_id").asText()));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<QuestionDeliveryDto> convertCachedList(List<?> rawList) {
        try {
            return objectMapper.convertValue(rawList,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionDeliveryDto.class));
        } catch (Exception e) {
            log.warn("Failed to cast cached questions list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
