// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.client;

import com.examplatform.practice.dto.AnswerKeyDto;
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

    public QuestionBankClientImpl(@Autowired(required = false) JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
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
                    map.put(qId, new AnswerKeyDto(
                            qId, answerKey, questionType, topicId, topicName, difficulty, 2,
                            content, optionsJson, explanation, subject
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
