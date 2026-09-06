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

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.delivery.repository.ExamSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Service for delivering examination questions to candidates during CBT test sessions.
 * Resolves questions from decrypted exam packages, cached question packages, or the approved question bank.
 * Supports option randomization per candidate session.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamQuestionDeliveryService {

    private static final String REDIS_QUESTIONS_PREFIX = "delivery:questions:";
    private static final Duration CACHE_DURATION = Duration.ofHours(12);

    private final JdbcTemplate jdbcTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ExamSessionRepository examSessionRepository;

    /**
     * Resolves question delivery payloads for an examination session.
     *
     * @param examId         the examination UUID
     * @param paperId        the paper UUID (if assigned)
     * @param decryptedPaper decrypted paper JSON payload (if decrypted via Vault)
     * @param tenantId       the tenant identifier
     * @return list of questions formatted for delivery interface
     */
    public List<QuestionDeliveryDto> getDeliveryQuestions(UUID examId, UUID paperId, String decryptedPaper, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // 1. First priority: parse questions embedded in the decrypted paper package
        if (decryptedPaper != null && !decryptedPaper.isBlank()) {
            List<QuestionDeliveryDto> parsedQuestions = parseFromPaperJson(decryptedPaper);
            if (!parsedQuestions.isEmpty()) {
                return parsedQuestions;
            }
        }

        // 2. Second priority: check Redis cache for this exam questions package
        String cacheKey = REDIS_QUESTIONS_PREFIX + effectiveTenant + ":" + (examId != null ? examId : "default");
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> list && !list.isEmpty()) {
                log.debug("Cache hit for delivery questions examId={}", examId);
                return convertCachedList(list);
            }
        } catch (Exception e) {
            log.warn("Redis lookup failed for questions cache: {}", e.getMessage());
        }

        // 3. Third priority: query approved questions from the question_service schema
        List<QuestionDeliveryDto> dbQuestions = fetchQuestionsFromDatabase(effectiveTenant);
        if (!dbQuestions.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, dbQuestions, CACHE_DURATION);
            } catch (Exception e) {
                log.warn("Failed to cache questions in Redis: {}", e.getMessage());
            }
            return dbQuestions;
        }

        return Collections.emptyList();
    }

    /**
     * Retrieves questions for an ongoing exam session.
     */
    public List<QuestionDeliveryDto> getQuestionsForSession(UUID sessionId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        ExamSession session = examSessionRepository.findBySessionIdAndTenantId(sessionId, effectiveTenant)
                .orElse(null);

        if (session == null) {
            return Collections.emptyList();
        }

        List<QuestionDeliveryDto> baseQuestions = getDeliveryQuestions(session.getExamId(), session.getPaperId(), null, effectiveTenant);
        return randomizeOptions(baseQuestions, sessionId);
    }

    /**
     * Randomizes option order for each question deterministically using a session seed.
     * Preserves originalIndex and option ID while updating the display index and correctOptionIndex.
     *
     * @param questions original list of questions
     * @param seedId    UUID used as randomization seed (e.g. sessionId or candidateId)
     * @return new list of questions with randomized option order
     */
    public List<QuestionDeliveryDto> randomizeOptions(List<QuestionDeliveryDto> questions, UUID seedId) {
        if (questions == null || questions.isEmpty()) {
            return Collections.emptyList();
        }

        long baseSeed = seedId != null
                ? (seedId.getMostSignificantBits() ^ seedId.getLeastSignificantBits())
                : System.currentTimeMillis();

        List<QuestionDeliveryDto> randomizedList = new ArrayList<>(questions.size());

        for (QuestionDeliveryDto q : questions) {
            if (q.getOptions() == null || q.getOptions().size() <= 1) {
                randomizedList.add(q);
                continue;
            }

            long qSeed = baseSeed ^ (q.getId() != null ? q.getId().hashCode() : 0);
            Random random = new Random(qSeed);

            List<QuestionOptionDeliveryDto> shuffled = new ArrayList<>(q.getOptions());
            Collections.shuffle(shuffled, random);

            Integer newCorrectOptionIndex = null;
            List<QuestionOptionDeliveryDto> reindexedOptions = new ArrayList<>(shuffled.size());

            for (int i = 0; i < shuffled.size(); i++) {
                QuestionOptionDeliveryDto original = shuffled.get(i);
                if (q.getCorrectOptionIndex() != null && original.getOriginalIndex() == q.getCorrectOptionIndex()) {
                    newCorrectOptionIndex = i;
                }

                reindexedOptions.add(QuestionOptionDeliveryDto.builder()
                        .id(original.getId())
                        .index(i)
                        .originalIndex(original.getOriginalIndex())
                        .text(original.getText())
                        .build());
            }

            randomizedList.add(QuestionDeliveryDto.builder()
                    .id(q.getId())
                    .text(q.getText())
                    .options(reindexedOptions)
                    .marks(q.getMarks())
                    .negativeMarks(q.getNegativeMarks())
                    .sectionId(q.getSectionId())
                    .sectionName(q.getSectionName())
                    .topic(q.getTopic())
                    .correctOptionIndex(newCorrectOptionIndex != null ? newCorrectOptionIndex : q.getCorrectOptionIndex())
                    .explanation(q.getExplanation())
                    .build());
        }

        return randomizedList;
    }

    private List<QuestionDeliveryDto> parseFromPaperJson(String decryptedPaper) {
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

    private QuestionDeliveryDto parseSingleQuestionNode(JsonNode qNode, int sequenceNumber) {
        try {
            String id = qNode.has("id") ? qNode.get("id").asText() : UUID.randomUUID().toString();
            String content = qNode.has("content") ? qNode.get("content").asText() :
                    (qNode.has("text") ? qNode.get("text").asText() : "");
            String subject = qNode.has("subject") ? qNode.get("subject").asText() : null;
            String topic = qNode.has("topic") ? qNode.get("topic").asText() : "General";
            String explanation = qNode.has("explanation") ? qNode.get("explanation").asText() : null;
            String answerKey = qNode.has("answerKey") ? qNode.get("answerKey").asText() :
                    (qNode.has("answer_key") ? qNode.get("answer_key").asText() : null);

            List<QuestionOptionDeliveryDto> options = new ArrayList<>();
            Integer correctOptionIndex = null;
            JsonNode optionsNode = qNode.get("options");

            if (optionsNode != null && optionsNode.isArray()) {
                for (int i = 0; i < optionsNode.size(); i++) {
                    JsonNode optNode = optionsNode.get(i);
                    String optText;
                    String optId = "";
                    boolean isCorrect = false;

                    if (optNode.isObject()) {
                        optText = optNode.has("text") ? optNode.get("text").asText() : optNode.asText();
                        optId = optNode.has("id") ? optNode.get("id").asText() : String.valueOf((char) ('A' + i));
                        isCorrect = optNode.has("isCorrect") && optNode.get("isCorrect").asBoolean();
                    } else {
                        optText = optNode.asText();
                        optId = String.valueOf((char) ('A' + i));
                    }

                    if (isCorrect || (answerKey != null && (answerKey.equalsIgnoreCase(optId) || answerKey.equalsIgnoreCase(optText)))) {
                        correctOptionIndex = i;
                    }

                    options.add(QuestionOptionDeliveryDto.builder()
                            .id(optId)
                            .index(i)
                            .originalIndex(i)
                            .text(optText)
                            .build());
                }
            }

            if (correctOptionIndex == null && qNode.has("correctOptionIndex")) {
                correctOptionIndex = qNode.get("correctOptionIndex").asInt();
            }

            SectionInfo sectionInfo = resolveSectionInfo(subject, sequenceNumber);

            return QuestionDeliveryDto.builder()
                    .id(id)
                    .text(content)
                    .options(options)
                    .marks(qNode.has("marks") ? qNode.get("marks").asDouble() : 2.0)
                    .negativeMarks(qNode.has("negativeMarks") ? qNode.get("negativeMarks").asDouble() : 0.5)
                    .sectionId(sectionInfo.id)
                    .sectionName(sectionInfo.name)
                    .topic(topic)
                    .correctOptionIndex(correctOptionIndex)
                    .explanation(explanation)
                    .build();
        } catch (Exception e) {
            log.warn("Error parsing individual question node: {}", e.getMessage());
            return null;
        }
    }

    private List<QuestionDeliveryDto> fetchQuestionsFromDatabase(String tenantId) {
        try {
            String sql = """
                SELECT id, subject, topic, subtopic, difficulty, cognitive_level, question_type,
                       content, options, answer_key, explanation
                FROM question_service.question
                WHERE (tenant_id = ? OR tenant_id = 'default')
                  AND state = 'APPROVED'
                ORDER BY CASE 
                  WHEN subject ILIKE '%Reasoning%' OR subject ILIKE '%Intelligence%' THEN 1
                  WHEN subject ILIKE '%Awareness%' OR subject ILIKE '%General Studies%' THEN 2
                  WHEN subject ILIKE '%Quantitative%' OR subject ILIKE '%Math%' THEN 3
                  WHEN subject ILIKE '%English%' THEN 4
                  ELSE 5 END, id
                LIMIT 100
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                UUID id = rs.getObject("id", UUID.class);
                String subject = rs.getString("subject");
                String topic = rs.getString("topic");
                String content = rs.getString("content");
                String optionsJson = rs.getString("options");
                String answerKey = rs.getString("answer_key");
                String explanation = rs.getString("explanation");

                List<QuestionOptionDeliveryDto> options = new ArrayList<>();
                Integer correctOptionIndex = null;

                if (optionsJson != null && !optionsJson.isBlank()) {
                    try {
                        JsonNode optArr = objectMapper.readTree(optionsJson);
                        if (optArr.isArray()) {
                            for (int i = 0; i < optArr.size(); i++) {
                                JsonNode optNode = optArr.get(i);
                                String optText;
                                String optId = "";
                                boolean isCorrect = false;

                                if (optNode.isObject()) {
                                    optText = optNode.has("text") ? optNode.get("text").asText() : optNode.asText();
                                    optId = optNode.has("id") ? optNode.get("id").asText() : String.valueOf((char) ('A' + i));
                                    isCorrect = optNode.has("isCorrect") && optNode.get("isCorrect").asBoolean();
                                } else {
                                    optText = optNode.asText();
                                    optId = String.valueOf((char) ('A' + i));
                                }

                                if (isCorrect || (answerKey != null && (answerKey.equalsIgnoreCase(optId) || answerKey.equalsIgnoreCase(optText)))) {
                                    correctOptionIndex = i;
                                }

                                options.add(QuestionOptionDeliveryDto.builder()
                                        .id(optId)
                                        .index(i)
                                        .originalIndex(i)
                                        .text(optText)
                                        .build());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("Error parsing options JSON for question {}: {}", id, e.getMessage());
                    }
                }

                SectionInfo sectionInfo = resolveSectionInfo(subject, rowNum + 1);

                return QuestionDeliveryDto.builder()
                        .id(id != null ? id.toString() : UUID.randomUUID().toString())
                        .text(content)
                        .options(options)
                        .marks(2.0)
                        .negativeMarks(0.5)
                        .sectionId(sectionInfo.id)
                        .sectionName(sectionInfo.name)
                        .topic(topic != null ? topic : "General")
                        .correctOptionIndex(correctOptionIndex)
                        .explanation(explanation)
                        .build();
            }, tenantId);
        } catch (Exception e) {
            log.error("Failed to query questions from question_service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private SectionInfo resolveSectionInfo(String subject, int sequenceNumber) {
        if (subject != null) {
            String lower = subject.toLowerCase();
            if (lower.contains("reasoning") || lower.contains("intelligence")) {
                return new SectionInfo("sec-1", "General Intelligence & Reasoning");
            } else if (lower.contains("awareness") || lower.contains("general studies") || lower.contains("current")) {
                return new SectionInfo("sec-2", "General Awareness");
            } else if (lower.contains("quantitative") || lower.contains("mathemat")) {
                return new SectionInfo("sec-3", "Quantitative Aptitude");
            } else if (lower.contains("english") || lower.contains("comprehension")) {
                return new SectionInfo("sec-4", "English Comprehension");
            } else {
                return new SectionInfo("sec-" + Math.abs(subject.hashCode() % 1000), subject);
            }
        }

        // Fallback based on question number ranges (25 per section)
        if (sequenceNumber <= 25) {
            return new SectionInfo("sec-1", "General Intelligence & Reasoning");
        } else if (sequenceNumber <= 50) {
            return new SectionInfo("sec-2", "General Awareness");
        } else if (sequenceNumber <= 75) {
            return new SectionInfo("sec-3", "Quantitative Aptitude");
        } else {
            return new SectionInfo("sec-4", "English Comprehension");
        }
    }

    @SuppressWarnings("unchecked")
    private List<QuestionDeliveryDto> convertCachedList(List<?> rawList) {
        try {
            return objectMapper.convertValue(rawList,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, QuestionDeliveryDto.class));
        } catch (Exception e) {
            log.warn("Failed to cast cached questions list: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private record SectionInfo(String id, String name) {}
}
