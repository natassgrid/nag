// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;

import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.shared.ai.latex.LatexPreservationUtil;
import com.examplatform.shared.constants.SharedConstants;
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
                SELECT id, answer_key, question_type, subject_id, subject, topic_id, topic, subtopic_id, subtopic, difficulty, content, options, explanation, benchmark_time_seconds
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
                            if (node.has(SharedConstants.CONTENT)) {
                                transMap.put(SharedConstants.CONTENT,
                                        LatexPreservationUtil.cleanStrayUnderscores(node.get(SharedConstants.CONTENT).asText()));
                            }
                            if (node.has("explanation")) {
                                transMap.put("explanation",
                                        LatexPreservationUtil.cleanStrayUnderscores(node.get("explanation").asText()));
                            }
                            if (node.has("options") && node.get("options").isArray()) {
                                List<Map<String, Object>> optionsList = new ArrayList<>();
                                for (JsonNode opt : node.get("options")) {
                                    Map<String, Object> optMap = new HashMap<>();
                                    if (opt.has("id")) optMap.put("id", opt.get("id").asText());
                                    if (opt.has("text")) optMap.put("text",
                                            LatexPreservationUtil.cleanStrayUnderscores(opt.get("text").asText()));
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
                Long subjectId = rs.getObject("subject_id", Long.class);
                String subject = rs.getString(SharedConstants.SUBJECT);
                String topicId = rs.getString("topic_id");
                String topicName = rs.getString("topic");
                Long subtopicId = rs.getObject("subtopic_id", Long.class);
                String subtopicName = rs.getString("subtopic");
                String difficulty = rs.getString(SharedConstants.DIFFICULTY);
                String content = rs.getString(SharedConstants.CONTENT);
                String optionsJson = rs.getString("options");
                String explanation = rs.getString("explanation");
                Integer benchmarkTime = rs.getObject("benchmark_time_seconds", Integer.class);
                int benchmarkTimeSec = (benchmarkTime != null && benchmarkTime > 0) ? benchmarkTime : 90;

                if (qId != null) {
                    Map<String, Map<String, Object>> qTranslations = translationsByQuestion.get(qId);
                    map.put(qId, new AnswerKeyDto(
                            qId, answerKey, questionType, topicId, topicName, difficulty, 2,
                            content, optionsJson, explanation, subject, qTranslations,
                            subjectId, subtopicId, subtopicName, benchmarkTimeSec
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
        try {
            String sql = """
                SELECT id FROM question_service.question
                WHERE state = 'PUBLISHED'
                  AND LOWER(subject) LIKE LOWER(?)
                ORDER BY created_at DESC
                LIMIT ?
                """;
            return jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getObject("id", UUID.class),
                    "%" + subjectPattern.trim() + "%",
                    Math.max(1, limit)
            );
        } catch (Exception e) {
            log.warn("Failed to query question IDs by subject from question_service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<UUID> findQuestionsForAdaptiveQuiz(List<String> subtopics, String difficulty, int limit) {
        if (jdbcTemplate == null || limit <= 0) {
            return Collections.emptyList();
        }
        try {
            if (subtopics != null && !subtopics.isEmpty()) {
                String inSql = String.join(",", Collections.nCopies(subtopics.size(), "?"));
                String sql;
                Object[] params;
                if (difficulty != null && !difficulty.isBlank()) {
                    sql = String.format("""
                        SELECT id FROM question_service.question
                        WHERE state = 'PUBLISHED'
                          AND UPPER(difficulty) = ?
                          AND LOWER(subtopic) IN (%s)
                        ORDER BY created_at DESC
                        LIMIT ?
                        """, inSql);
                    List<Object> pList = new ArrayList<>();
                    pList.add(difficulty.trim().toUpperCase());
                    for (String s : subtopics) pList.add(s.trim().toLowerCase());
                    pList.add(limit);
                    params = pList.toArray();
                } else {
                    sql = String.format("""
                        SELECT id FROM question_service.question
                        WHERE state = 'PUBLISHED'
                          AND LOWER(subtopic) IN (%s)
                        ORDER BY created_at DESC
                        LIMIT ?
                        """, inSql);
                    List<Object> pList = new ArrayList<>();
                    for (String s : subtopics) pList.add(s.trim().toLowerCase());
                    pList.add(limit);
                    params = pList.toArray();
                }
                List<UUID> result = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject("id", UUID.class), params);
                if (!result.isEmpty()) return result;
            }

            // Fallback: select questions by difficulty
            String fallbackSql = (difficulty != null && !difficulty.isBlank())
                    ? "SELECT id FROM question_service.question WHERE state = 'PUBLISHED' AND UPPER(difficulty) = ? ORDER BY created_at DESC LIMIT ?"
                    : "SELECT id FROM question_service.question WHERE state = 'PUBLISHED' ORDER BY created_at DESC LIMIT ?";
            Object[] fallbackParams = (difficulty != null && !difficulty.isBlank())
                    ? new Object[]{difficulty.trim().toUpperCase(), limit}
                    : new Object[]{limit};

            return jdbcTemplate.query(fallbackSql, (rs, rowNum) -> rs.getObject("id", UUID.class), fallbackParams);
        } catch (Exception e) {
            log.warn("Failed to query questions for adaptive quiz: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
