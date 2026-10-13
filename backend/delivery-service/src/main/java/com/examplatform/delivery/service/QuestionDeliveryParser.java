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

import com.examplatform.delivery.dto.PaperOptionPayload;
import com.examplatform.delivery.dto.PaperPayload;
import com.examplatform.delivery.dto.PaperQuestionPayload;
import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.shared.constants.QuestionTypeConstants;
import com.examplatform.shared.constants.SharedConstants;
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
 * and decrypted examination paper structures using strongly-typed POJOs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionDeliveryParser {

    private static final String FIELD_QUESTIONS = "questions";

    private static final String[] CONTAINER_FIELDS = {
            "questionIds", FIELD_QUESTIONS, "questionGroups", "sections"
    };

    private static final String[] ID_FIELDS = {
            "id", SharedConstants.QUESTION_ID, "question_id"
    };

    private final ObjectMapper objectMapper;

    /**
     * DTO containing parsed options, correct option index, and whether any option has an image.
     */
    public record ParsedOptions(
            List<QuestionOptionDeliveryDto> options,
            Integer correctOptionIndex,
            boolean hasImages
    ) {}

    public List<QuestionDeliveryDto> parseFromPaperJson(String decryptedPaper) {
        if (decryptedPaper == null || decryptedPaper.isBlank()) {
            return Collections.emptyList();
        }
        try {
            PaperPayload paper = objectMapper.readValue(decryptedPaper, PaperPayload.class);
            if (paper.getQuestions() == null || paper.getQuestions().isEmpty()) {
                return Collections.emptyList();
            }
            List<QuestionDeliveryDto> questions = new ArrayList<>(paper.getQuestions().size());
            int index = 1;
            for (PaperQuestionPayload payload : paper.getQuestions()) {
                QuestionDeliveryDto q = convertToDeliveryDto(payload, index++);
                if (q != null) {
                    questions.add(q);
                }
            }
            return questions;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse examination paper questions JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public QuestionDeliveryDto parseSingleQuestionNode(JsonNode qNode, int sequenceNumber) {
        if (qNode == null || qNode.isNull()) {
            return null;
        }
        try {
            PaperQuestionPayload payload = objectMapper.treeToValue(qNode, PaperQuestionPayload.class);
            return convertToDeliveryDto(payload, sequenceNumber);
        } catch (Exception e) {
            log.warn("Error parsing individual question node: {}", e.getMessage());
            return null;
        }
    }

    public QuestionDeliveryDto convertToDeliveryDto(PaperQuestionPayload payload, int sequenceNumber) {
        if (payload == null) {
            return null;
        }

        String id = (payload.getId() != null && !payload.getId().isBlank())
                ? payload.getId()
                : UUID.randomUUID().toString();
        String content = payload.getContent() != null ? payload.getContent() : "";
        String subject = payload.getSubject();
        String topic = (payload.getTopic() != null && !payload.getTopic().isBlank())
                ? payload.getTopic()
                : "General";
        String explanation = payload.getExplanation();
        String questionType = (payload.getQuestionType() != null && !payload.getQuestionType().isBlank())
                ? payload.getQuestionType()
                : QuestionTypeConstants.SINGLE_MCQ;
        boolean hasImages = Boolean.TRUE.equals(payload.getHasImages());

        ParsedOptions parsedOptions = processParsedOptions(payload.getOptions(), payload.getAnswerKey());
        List<QuestionOptionDeliveryDto> options = parsedOptions.options();
        Integer correctOptionIndex = parsedOptions.correctOptionIndex();
        if (parsedOptions.hasImages()) {
            hasImages = true;
        }

        if (correctOptionIndex == null && payload.getCorrectOptionIndex() != null) {
            correctOptionIndex = payload.getCorrectOptionIndex();
        }

        String secId = resolveSectionId(subject, sequenceNumber);
        String secName = resolveSectionName(subject, sequenceNumber);
        double marks = payload.getMarks() != null ? payload.getMarks() : 2.0;
        double negativeMarks = payload.getNegativeMarks() != null ? payload.getNegativeMarks() : 0.5;

        return QuestionDeliveryDto.builder()
                .id(id)
                .text(content)
                .imageUrl(payload.getImageUrl())
                .imageAltText(payload.getImageAltText())
                .hasImages(hasImages)
                .options(options)
                .marks(marks)
                .negativeMarks(negativeMarks)
                .sectionId(secId)
                .sectionName(secName)
                .topic(topic)
                .correctOptionIndex(correctOptionIndex)
                .explanation(explanation)
                .questionType(questionType)
                .passageId(payload.getPassageId())
                .passageContent(payload.getPassageContent())
                .passageOrderIndex(payload.getPassageOrderIndex())
                .build();
    }

    /**
     * Parses an options JsonNode array into a list of QuestionOptionDeliveryDto items and resolves correct option index.
     */
    public ParsedOptions parseOptions(JsonNode optionsNode, String answerKey) {
        if (optionsNode == null || !optionsNode.isArray()) {
            return new ParsedOptions(Collections.emptyList(), null, false);
        }
        try {
            List<PaperOptionPayload> options = objectMapper.readerForListOf(PaperOptionPayload.class).readValue(optionsNode);
            return processParsedOptions(options, answerKey);
        } catch (Exception e) {
            log.warn("Failed to parse options node: {}", e.getMessage());
            return new ParsedOptions(Collections.emptyList(), null, false);
        }
    }

    public ParsedOptions processParsedOptions(List<PaperOptionPayload> optionPayloads, String answerKey) {
        if (optionPayloads == null || optionPayloads.isEmpty()) {
            return new ParsedOptions(Collections.emptyList(), null, false);
        }

        List<QuestionOptionDeliveryDto> dtos = new ArrayList<>(optionPayloads.size());
        Integer correctOptionIndex = null;
        boolean hasImages = false;

        for (int i = 0; i < optionPayloads.size(); i++) {
            PaperOptionPayload opt = optionPayloads.get(i);
            String optId = (opt.getId() != null && !opt.getId().isBlank()) ? opt.getId() : defaultOptionId(i);
            String optText = opt.getText() != null ? opt.getText() : "";
            boolean isCorrect = Boolean.TRUE.equals(opt.getIsCorrect());

            if (opt.getImageUrl() != null && !opt.getImageUrl().isBlank()) {
                hasImages = true;
            }

            if (isOptionCorrect(isCorrect, answerKey, optId, optText)) {
                correctOptionIndex = i;
            }

            dtos.add(QuestionOptionDeliveryDto.builder()
                    .id(optId)
                    .index(i)
                    .originalIndex(i)
                    .text(optText)
                    .imageUrl(opt.getImageUrl())
                    .imageAltText(opt.getImageAltText())
                    .build());
        }

        return new ParsedOptions(dtos, correctOptionIndex, hasImages);
    }

    private String defaultOptionId(int index) {
        return String.valueOf((char) ('A' + index));
    }

    private boolean isOptionCorrect(boolean isCorrect, String answerKey, String optId, String optText) {
        if (isCorrect) {
            return true;
        }
        if (answerKey == null) {
            return false;
        }
        return answerKey.equalsIgnoreCase(optId) || answerKey.equalsIgnoreCase(optText);
    }

    enum StandardSection {
        REASONING("sec-1", "General Intelligence & Reasoning", "reasoning", "intelligence"),
        AWARENESS("sec-2", "General Awareness", "awareness", "general studies", "current"),
        QUANTITATIVE("sec-3", "Quantitative Aptitude", "quantitative", "mathemat"),
        ENGLISH("sec-4", "English Comprehension", "english", "comprehension");

        final String id;
        final String name;
        final String[] keywords;

        StandardSection(String id, String name, String... keywords) {
            this.id = id;
            this.name = name;
            this.keywords = keywords;
        }

        static StandardSection findMatch(String subject) {
            if (subject == null) return null;
            String lower = subject.toLowerCase();
            for (StandardSection sec : values()) {
                for (String kw : sec.keywords) {
                    if (lower.contains(kw)) return sec;
                }
            }
            return null;
        }
    }

    public String resolveSectionId(String subject, int sequenceNumber) {
        StandardSection sec = StandardSection.findMatch(subject);
        return sec != null ? sec.id : ("sec-" + ((sequenceNumber - 1) / 25 + 1));
    }

    public String resolveSectionName(String subject, int sequenceNumber) {
        StandardSection sec = StandardSection.findMatch(subject);
        if (sec != null) {
            return sec.name;
        }
        return subject != null ? subject : ("Section " + ((sequenceNumber - 1) / 25 + 1));
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
            tryParseUuid(trimmed, list);
        }
        return list;
    }

    public void extractUuidsFromNode(JsonNode node, List<UUID> accumulator) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isTextual()) {
            tryParseUuid(node.asText(), accumulator);
            return;
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                extractUuidsFromNode(item, accumulator);
            }
            return;
        }
        if (node.isObject()) {
            extractUuidsFromObjectNode(node, accumulator);
        }
    }

    private void extractUuidsFromObjectNode(JsonNode node, List<UUID> accumulator) {
        for (String field : CONTAINER_FIELDS) {
            if (node.has(field)) {
                extractUuidsFromNode(node.get(field), accumulator);
            }
        }

        for (String field : ID_FIELDS) {
            if (node.has(field)) {
                tryParseUuid(node.get(field).asText(), accumulator);
                break;
            }
        }
    }

    private void tryParseUuid(String text, List<UUID> accumulator) {
        if (text != null && !text.isBlank()) {
            try {
                accumulator.add(UUID.fromString(text));
            } catch (IllegalArgumentException _) {
                // Ignore non-UUID strings
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
