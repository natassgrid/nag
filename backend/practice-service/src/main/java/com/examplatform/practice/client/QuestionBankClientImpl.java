// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;

import com.examplatform.practice.dto.AnswerKeyDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class QuestionBankClientImpl implements QuestionBankClient {
    private static final Logger log = LoggerFactory.getLogger(QuestionBankClientImpl.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public QuestionBankClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = (objectMapper != null) ? objectMapper : new ObjectMapper();
    }

    @Override
    public Map<UUID, AnswerKeyDto> getAnswerKeys(List<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        if (jdbcTemplate == null) {
            log.debug("No JdbcTemplate available, returning empty answer keys map");
            return Collections.emptyMap();
        }

        try {
            String inSql = String.join(",", Collections.nCopies(questionIds.size(), "?"));
            String sql = String.format("""
                SELECT id, answer_key, question_type, topic_id, topic, subject, difficulty, content, options, explanation
                FROM question_service.question
                WHERE id IN (%s)
                """, inSql);

            // Query translations if available
            Map<UUID, Map<String, Map<String, Object>>> translationsByQuestion = new HashMap<>();
            try {
                String transSql = String.format("""
                    SELECT question_id, language_code, translated_payload
                    FROM question_service.translation
                    WHERE status IN ('PUBLISHED', 'APPROVED')
                      AND question_id IN (%s)
                    """, inSql);

                jdbcTemplate.query(transSql, rs -> {
                    UUID qId = rs.getObject("question_id", UUID.class);
                    String langCode = rs.getString("language_code");
                    String payload = rs.getString("translated_payload");

                    if (qId != null && langCode != null && payload != null && !payload.isBlank()) {
                        try {
                            JsonNode node = objectMapper.readTree(payload);
                            Map<String, Object> transMap = new HashMap<>();
                            transMap.put("languageCode", langCode);
                            if (node.has("content")) {
                                transMap.put("content", node.get("content").asText());
                            }
                            if (node.has("explanation")) {
                                transMap.put("explanation", node.get("explanation").asText());
                            }
                            if (node.has("options") && node.get("options").isArray()) {
                                List<Map<String, Object>> optionsList = new ArrayList<>();
                                for (JsonNode opt : node.get("options")) {
                                    Map<String, Object> optMap = new HashMap<>();
                                    if (opt.has("id")) optMap.put("id", opt.get("id").asText());
                                    if (opt.has("text")) optMap.put("text", opt.get("text").asText());
                                    optionsList.add(optMap);
                                }
                                transMap.put("options", optionsList);
                            }
                            translationsByQuestion
                                    .computeIfAbsent(qId, k -> new HashMap<>())
                                    .put(langCode, transMap);
                        } catch (Exception parseEx) {
                            log.warn("Failed to parse translation payload for question {}: {}", qId, parseEx.getMessage());
                        }
                    }
                }, questionIds.toArray());
            } catch (Exception transEx) {
                log.debug("No translations found or table question_service.translation not present: {}", transEx.getMessage());
            }

            Map<UUID, AnswerKeyDto> map = new HashMap<>();
            jdbcTemplate.query(sql, rs -> {
                UUID qId = rs.getObject("id", UUID.class);
                String answerKey = rs.getString("answer_key");
                String questionType = rs.getString("question_type");
                String topicId = rs.getString("topic_id");
                String topicName = rs.getString("topic");
                String subject = rs.getString("subject");
                String difficulty = rs.getString("difficulty");
                String content = rs.getString("content");
                String optionsJson = rs.getString("options");
                String explanation = rs.getString("explanation");

                if (qId != null) {
                    Map<String, Map<String, Object>> qTranslations = translationsByQuestion.get(qId);
                    map.put(qId, new AnswerKeyDto(
                            qId, answerKey, questionType, topicId, topicName, difficulty, 2,
                            content, optionsJson, explanation, subject, qTranslations
                    ));
                }
            }, questionIds.toArray());

            return map;
        } catch (Exception e) {
            log.warn("Failed to query answer keys from question_service: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    @Override
    public List<UUID> findQuestionIdsBySubject(String subjectPattern, int limit) {
        if (jdbcTemplate == null || subjectPattern == null || subjectPattern.isBlank()) {
            return Collections.emptyList();
        }

        int resolvedLimit = limit > 0 ? limit : 25;
        String fullPattern = "%" + subjectPattern.replace('-', ' ').replace('_', ' ').trim() + "%";
        String firstToken = "%" + subjectPattern.split("[-_\\s]+")[0] + "%";

        try {
            String sql = """
                SELECT id FROM question_service.question
                WHERE (subject ILIKE ? OR topic ILIKE ?)
                   OR (subject ILIKE ? OR topic ILIKE ?)
                ORDER BY id
                LIMIT ?
                """;

            return jdbcTemplate.query(
                    sql,
                    (rs, rowNum) -> rs.getObject("id", UUID.class),
                    fullPattern, fullPattern, firstToken, firstToken, resolvedLimit
            );
        } catch (Exception e) {
            log.warn("Failed to find question IDs for subject pattern {}: {}", subjectPattern, e.getMessage());
            return Collections.emptyList();
        }
    }
}
