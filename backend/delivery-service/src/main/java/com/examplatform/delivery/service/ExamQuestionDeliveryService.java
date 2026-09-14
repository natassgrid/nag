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
import com.examplatform.delivery.dto.TranslatedQuestionDeliveryDto;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Service for delivering examination questions to candidates during CBT test sessions.
 * Resolves questions from decrypted exam packages, cached question packages, or the approved question bank.
 * Enriches questions with approved/published multi-language translations (e.g. Hindi).
 * Supports option randomization per candidate session across English and regional translations.
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
                enrichWithTranslations(parsedQuestions, effectiveTenant);
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

        // 3. Third priority: query approved questions from the database matching the exam specification
        List<QuestionDeliveryDto> dbQuestions = fetchQuestionsForExam(examId, effectiveTenant);
        if (!dbQuestions.isEmpty()) {
            enrichWithTranslations(dbQuestions, effectiveTenant);
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
     * Also synchronizes the option ordering in any attached regional language translations.
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
            Map<String, Integer> idToNewIndexMap = new HashMap<>();

            for (int i = 0; i < shuffled.size(); i++) {
                QuestionOptionDeliveryDto original = shuffled.get(i);
                if (q.getCorrectOptionIndex() != null && original.getOriginalIndex() == q.getCorrectOptionIndex()) {
                    newCorrectOptionIndex = i;
                }
                idToNewIndexMap.put(original.getId(), i);

                reindexedOptions.add(QuestionOptionDeliveryDto.builder()
                        .id(original.getId())
                        .index(i)
                        .originalIndex(original.getOriginalIndex())
                        .text(original.getText())
                        .build());
            }

            // Also re-index translations to match the exact option IDs
            Map<String, TranslatedQuestionDeliveryDto> randomizedTranslations = new HashMap<>();
            if (q.getTranslations() != null) {
                for (Map.Entry<String, TranslatedQuestionDeliveryDto> entry : q.getTranslations().entrySet()) {
                    TranslatedQuestionDeliveryDto trans = entry.getValue();
                    List<QuestionOptionDeliveryDto> transOptions = new ArrayList<>();
                    if (trans.getOptions() != null && !trans.getOptions().isEmpty()) {
                        Map<String, QuestionOptionDeliveryDto> transOptMap = new HashMap<>();
                        for (QuestionOptionDeliveryDto opt : trans.getOptions()) {
                            transOptMap.put(opt.getId(), opt);
                        }

                        for (int i = 0; i < reindexedOptions.size(); i++) {
                            QuestionOptionDeliveryDto baseOpt = reindexedOptions.get(i);
                            QuestionOptionDeliveryDto transOpt = transOptMap.get(baseOpt.getId());
                            if (transOpt != null) {
                                transOptions.add(QuestionOptionDeliveryDto.builder()
                                        .id(baseOpt.getId())
                                        .index(i)
                                        .originalIndex(transOpt.getOriginalIndex())
                                        .text(transOpt.getText())
                                        .build());
                            }
                        }
                    }

                    randomizedTranslations.put(entry.getKey(), TranslatedQuestionDeliveryDto.builder()
                            .languageCode(trans.getLanguageCode())
                            .content(trans.getContent())
                            .options(transOptions)
                            .explanation(trans.getExplanation())
                            .build());
                }
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
                    .translations(randomizedTranslations)
                    .build());
        }

        return randomizedList;
    }

    /**
     * Enriches delivered questions with published and approved regional language translations.
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
                UUID qId = rs.getObject("question_id", UUID.class);
                String langCode = rs.getString("language_code");
                String payload = rs.getString("translated_payload");

                QuestionDeliveryDto qDto = dtoMap.get(qId);
                if (qDto != null && payload != null && !payload.isBlank()) {
                    try {
                        JsonNode node = objectMapper.readTree(payload);
                        String transContent = node.path("content").asText(null);
                        String transExplanation = node.path("explanation").asText(null);
                        List<QuestionOptionDeliveryDto> transOptions = new ArrayList<>();

                        JsonNode optionsNode = node.path("options");
                        if (optionsNode.isArray()) {
                            for (int i = 0; i < optionsNode.size(); i++) {
                                JsonNode optNode = optionsNode.get(i);
                                String optText = optNode.path("text").asText("");
                                String optId = optNode.path("id").asText(String.valueOf((char) ('A' + i)));
                                transOptions.add(QuestionOptionDeliveryDto.builder()
                                        .id(optId)
                                        .index(i)
                                        .originalIndex(i)
                                        .text(optText)
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

            String secId = resolveSectionId(subject, sequenceNumber);
            String secName = resolveSectionName(subject, sequenceNumber);

            return QuestionDeliveryDto.builder()
                    .id(id)
                    .text(content)
                    .options(options)
                    .marks(qNode.has("marks") ? qNode.get("marks").asDouble() : 2.0)
                    .negativeMarks(qNode.has("negativeMarks") ? qNode.get("negativeMarks").asDouble() : 0.5)
                    .sectionId(secId)
                    .sectionName(secName)
                    .topic(topic)
                    .correctOptionIndex(correctOptionIndex)
                    .explanation(explanation)
                    .build();
        } catch (Exception e) {
            log.warn("Error parsing individual question node: {}", e.getMessage());
            return null;
        }
    }

    private List<QuestionDeliveryDto> fetchQuestionsForExam(UUID examId, String tenantId) {
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        String sectionsJson = null;
        if (examId != null) {
            try {
                sectionsJson = jdbcTemplate.queryForObject(
                        "SELECT sections_json FROM examination_service.examination WHERE id = ?",
                        String.class,
                        examId
                );
            } catch (Exception e) {
                log.debug("No sections_json found for examId {}: {}", examId, e.getMessage());
            }
        }

        List<QuestionDeliveryDto> result = new ArrayList<>();
        Set<UUID> usedQuestionIds = new HashSet<>();

        if (sectionsJson != null && !sectionsJson.isBlank()) {
            try {
                JsonNode sectionsArr = objectMapper.readTree(sectionsJson);
                if (sectionsArr.isArray() && !sectionsArr.isEmpty()) {
                    int secIdx = 1;
                    for (JsonNode secNode : sectionsArr) {
                        String secName = secNode.path("name").asText("Section " + secIdx);
                        String subject = secNode.path("subject").asText("");
                        int count = secNode.path("questionCount").asInt(25);
                        double marks = secNode.path("marksPerQuestion").asDouble(2.0);
                        double negMarks = secNode.path("negativeMarksPerQuestion").asDouble(0.5);
                        String secId = "sec-" + secIdx;

                        List<QuestionDeliveryDto> secQuestions = fetchQuestionsForSubject(
                                subject, count, secId, secName, marks, negMarks, usedQuestionIds, tenantId);
                        result.addAll(secQuestions);
                        secIdx++;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse sections_json for exam {}: {}", examId, e.getMessage());
            }
        }

        if (result.isEmpty()) {
            result = fetchDefaultApprovedQuestions(tenantId);
        }

        return result;
    }

    private List<QuestionDeliveryDto> fetchQuestionsForSubject(
            String subject,
            int limit,
            String secId,
            String secName,
            double marks,
            double negMarks,
            Set<UUID> usedIds,
            String tenantId
    ) {
        String keyword = "%" + (subject != null && !subject.isBlank() ? subject.trim() : "") + "%";
        String sql = """
            SELECT id, subject, topic, subtopic, difficulty, cognitive_level, question_type,
                   content, options, answer_key, explanation
            FROM question_service.question
            WHERE (tenant_id = ? OR tenant_id = 'default')
              AND state = 'APPROVED'
              AND (subject ILIKE ? OR topic ILIKE ? OR subtopic ILIKE ?)
            ORDER BY id
            LIMIT ?
            """;

        List<QuestionDeliveryDto> matched = queryQuestions(sql, new Object[]{tenantId, keyword, keyword, keyword, limit * 2}, secId, secName, marks, negMarks);

        List<QuestionDeliveryDto> filtered = new ArrayList<>();
        for (QuestionDeliveryDto q : matched) {
            try {
                UUID qUuid = UUID.fromString(q.getId());
                if (!usedIds.contains(qUuid)) {
                    usedIds.add(qUuid);
                    filtered.add(q);
                    if (filtered.size() >= limit) {
                        break;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (filtered.size() < limit) {
            String fallbackSql = """
                SELECT id, subject, topic, subtopic, difficulty, cognitive_level, question_type,
                       content, options, answer_key, explanation
                FROM question_service.question
                WHERE (tenant_id = ? OR tenant_id = 'default')
                  AND state = 'APPROVED'
                ORDER BY id
                LIMIT 200
                """;
            List<QuestionDeliveryDto> extra = queryQuestions(fallbackSql, new Object[]{tenantId}, secId, secName, marks, negMarks);
            for (QuestionDeliveryDto q : extra) {
                try {
                    UUID qUuid = UUID.fromString(q.getId());
                    if (!usedIds.contains(qUuid)) {
                        usedIds.add(qUuid);
                        filtered.add(q);
                        if (filtered.size() >= limit) {
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }

        return filtered;
    }

    private List<QuestionDeliveryDto> fetchDefaultApprovedQuestions(String tenantId) {
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
        return queryQuestions(sql, new Object[]{tenantId}, null, null, 2.0, 0.5);
    }

    private List<QuestionDeliveryDto> queryQuestions(String sql, Object[] params, String defaultSecId, String defaultSecName, double marks, double negMarks) {
        try {
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

                String sId = defaultSecId != null ? defaultSecId : resolveSectionId(subject, rowNum + 1);
                String sName = defaultSecName != null ? defaultSecName : resolveSectionName(subject, rowNum + 1);

                return QuestionDeliveryDto.builder()
                        .id(id != null ? id.toString() : UUID.randomUUID().toString())
                        .text(content)
                        .options(options)
                        .marks(marks)
                        .negativeMarks(negMarks)
                        .sectionId(sId)
                        .sectionName(sName)
                        .topic(topic != null ? topic : "General")
                        .correctOptionIndex(correctOptionIndex)
                        .explanation(explanation)
                        .build();
            }, params);
        } catch (Exception e) {
            log.error("Failed to query questions from question_service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private String resolveSectionId(String subject, int sequenceNumber) {
        if (subject != null) {
            String lower = subject.toLowerCase();
            if (lower.contains("reasoning") || lower.contains("intelligence")) return "sec-1";
            if (lower.contains("awareness") || lower.contains("general studies") || lower.contains("current")) return "sec-2";
            if (lower.contains("quantitative") || lower.contains("mathemat")) return "sec-3";
            if (lower.contains("english") || lower.contains("comprehension")) return "sec-4";
        }
        return "sec-" + ((sequenceNumber - 1) / 25 + 1);
    }

    private String resolveSectionName(String subject, int sequenceNumber) {
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
