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

package com.examplatform.papergenerator.client;

import com.examplatform.papergenerator.dto.QuestionSummary;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST-first implementation of QuestionBankClient querying question-bank-service.
 * Selects approved questions matching blueprint criteria (subject, topic, difficulty, cognitive level).
 * Gracefully falls back to direct database query if the remote service is unavailable.
 *
 * Validates: Requirements 8.1, 8.3
 */
@Slf4j
@Component
public class QuestionBankClientImpl implements QuestionBankClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String questionBankServiceUrl;

    @Autowired
    public QuestionBankClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Value("${app.question-bank.service-url:http://localhost:8083}") String questionBankServiceUrl) {
        this.jdbcTemplate = jdbcTemplate;
        this.questionBankServiceUrl = questionBankServiceUrl;
        this.restClient = RestClient.create();
    }

    @Override
    public List<QuestionSummary> findAvailableQuestions(String subject, String topic,
                                                        String difficulty, String cognitiveLevel, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String cleanSubject = (subject != null) ? subject.trim() : "";
        String cleanTopic = (topic != null) ? topic.trim() : "";
        String cleanDifficulty = (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null;
        String cleanCognitiveLevel = (cognitiveLevel != null && !cognitiveLevel.isBlank()) ? cognitiveLevel.trim() : null;

        log.debug("Finding questions via REST: subject='{}', topic='{}', difficulty='{}', cognitiveLevel='{}', tenant='{}'",
                cleanSubject, cleanTopic, cleanDifficulty, cleanCognitiveLevel, effectiveTenant);

        // 1. Try REST call to question-bank-service
        try {
            String url = questionBankServiceUrl + "/api/v1/questions/blueprint-match";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("subject", cleanSubject);
            requestBody.put("topic", cleanTopic);
            if (cleanDifficulty != null) requestBody.put("difficulty", cleanDifficulty);
            if (cleanCognitiveLevel != null) requestBody.put("cognitiveLevel", cleanCognitiveLevel);

            ApiResponseDto<List<QuestionResponseDto>> apiResponse = restClient.post()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseDto<List<QuestionResponseDto>>>() {});

            if (apiResponse != null && apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                log.info("Retrieved {} questions from question-bank-service via REST", apiResponse.getData().size());
                return apiResponse.getData().stream()
                        .map(this::toSummary)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("REST call to question-bank-service failed ({}), falling back to direct DB query: {}",
                    questionBankServiceUrl, e.getMessage());
        }

        // 2. Fallback to direct JDBC query if available
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            // First attempt: exact match on subject, topic, difficulty, and cognitive level
            String sql = """
                SELECT id, subject, topic, difficulty, cognitive_level, usage_count, last_used_at, content
                FROM question_service.question
                WHERE tenant_id = ?
                  AND UPPER(TRIM(subject)) = UPPER(?)
                  AND UPPER(TRIM(topic)) = UPPER(?)
                  AND state = 'APPROVED'
                  AND (? IS NULL OR UPPER(TRIM(difficulty)) = UPPER(?))\
                  AND (? IS NULL OR UPPER(TRIM(cognitive_level)) = UPPER(?))\
                ORDER BY RANDOM()
                """;

            List<QuestionSummary> questions = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Timestamp ts = rs.getTimestamp("last_used_at");
                Instant lastUsedAt = ts != null ? ts.toInstant() : null;
                return QuestionSummary.builder()
                        .questionId(rs.getObject("id", UUID.class))
                        .subject(rs.getString("subject"))
                        .topic(rs.getString("topic"))
                        .difficulty(rs.getString("difficulty"))
                        .cognitiveLevel(rs.getString("cognitive_level"))
                        .usageCount(rs.getInt("usage_count"))
                        .lastUsedAt(lastUsedAt)
                        .reusePolicy("1_YEAR")
                        .content(rs.getString("content"))
                        .build();
            }, effectiveTenant, cleanSubject, cleanTopic, cleanDifficulty, cleanDifficulty, cleanCognitiveLevel, cleanCognitiveLevel);

            if (!questions.isEmpty()) {
                log.info("Found {} questions via DB fallback for subject='{}', topic='{}', difficulty='{}', cognitiveLevel='{}'",
                        questions.size(), cleanSubject, cleanTopic, cleanDifficulty, cleanCognitiveLevel);
                return questions;
            }

            // Fallback: match by difficulty if specific cognitive level produces no results
            if (cleanCognitiveLevel != null) {
                log.info("No questions with cognitiveLevel='{}'; falling back to difficulty-only for subject='{}', topic='{}'",
                        cleanCognitiveLevel, cleanSubject, cleanTopic);
                String fallbackSql = """
                    SELECT id, subject, topic, difficulty, cognitive_level, usage_count, last_used_at, content
                    FROM question_service.question
                    WHERE tenant_id = ?
                      AND UPPER(TRIM(subject)) = UPPER(?)
                      AND UPPER(TRIM(topic)) = UPPER(?)
                      AND state = 'APPROVED'
                      AND (? IS NULL OR UPPER(TRIM(difficulty)) = UPPER(?))\
                    ORDER BY RANDOM()
                    """;

                List<QuestionSummary> fallbackQuestions = jdbcTemplate.query(fallbackSql, (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("last_used_at");
                    Instant lastUsedAt = ts != null ? ts.toInstant() : null;
                    return QuestionSummary.builder()
                            .questionId(rs.getObject("id", UUID.class))
                            .subject(rs.getString("subject"))
                            .topic(rs.getString("topic"))
                            .difficulty(rs.getString("difficulty"))
                            .cognitiveLevel(rs.getString("cognitive_level"))
                            .usageCount(rs.getInt("usage_count"))
                            .lastUsedAt(lastUsedAt)
                            .reusePolicy("1_YEAR")
                            .content(rs.getString("content"))
                            .build();
                }, effectiveTenant, cleanSubject, cleanTopic, cleanDifficulty, cleanDifficulty);

                return fallbackQuestions;
            }

            return questions;
        } catch (Exception e) {
            log.error("Error querying questions from question_service for subject='{}', topic='{}': {}",
                    cleanSubject, cleanTopic, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<QuestionSummary> findQuestionsByIds(List<UUID> questionIds, String tenantId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // 1. Try REST call to question-bank-service
        try {
            String url = questionBankServiceUrl + "/api/v1/questions/batch-find";
            ApiResponseDto<List<QuestionResponseDto>> apiResponse = restClient.post()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(questionIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseDto<List<QuestionResponseDto>>>() {});

            if (apiResponse != null && apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                log.info("Retrieved {} questions by IDs from question-bank-service via REST", apiResponse.getData().size());
                return apiResponse.getData().stream()
                        .map(this::toSummary)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("REST call for batch-find failed ({}), falling back to direct DB query: {}",
                    questionBankServiceUrl, e.getMessage());
        }

        // 2. Fallback to JDBC query
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String inSql = String.join(",", Collections.nCopies(questionIds.size(), "?"));
            String sql = String.format("""
                SELECT id, subject, topic, difficulty, cognitive_level, usage_count, last_used_at, content
                FROM question_service.question
                WHERE (tenant_id = ? OR tenant_id = 'default')
                  AND id IN (%s)
                """, inSql);

            List<Object> params = new ArrayList<>();
            params.add(effectiveTenant);
            params.addAll(questionIds);

            Map<UUID, QuestionSummary> map = new HashMap<>();
            jdbcTemplate.query(sql, rs -> {
                UUID id = rs.getObject("id", UUID.class);
                Timestamp ts = rs.getTimestamp("last_used_at");
                Instant lastUsedAt = ts != null ? ts.toInstant() : null;
                QuestionSummary qs = QuestionSummary.builder()
                        .questionId(id)
                        .subject(rs.getString("subject"))
                        .topic(rs.getString("topic"))
                        .difficulty(rs.getString("difficulty"))
                        .cognitiveLevel(rs.getString("cognitive_level"))
                        .usageCount(rs.getInt("usage_count"))
                        .lastUsedAt(lastUsedAt)
                        .content(rs.getString("content"))
                        .build();
                map.put(id, qs);
            }, params.toArray());

            List<QuestionSummary> ordered = new ArrayList<>();
            for (UUID qId : questionIds) {
                QuestionSummary qs = map.get(qId);
                if (qs != null) {
                    ordered.add(qs);
                } else {
                    ordered.add(QuestionSummary.builder().questionId(qId).build());
                }
            }
            return ordered;
        } catch (Exception e) {
            log.error("Error finding questions by ids for paper review: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private QuestionSummary toSummary(QuestionResponseDto dto) {
        return QuestionSummary.builder()
                .questionId(dto.getId())
                .subject(dto.getSubject())
                .topic(dto.getTopic())
                .difficulty(dto.getDifficulty())
                .cognitiveLevel(dto.getCognitiveLevel())
                .usageCount(0)
                .reusePolicy("1_YEAR")
                .content(dto.getContent())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiResponseDto<T> {
        private boolean success;
        private T data;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionResponseDto {
        private UUID id;
        private String subject;
        private String topic;
        private String difficulty;
        private String cognitiveLevel;
        private String content;
    }
}
