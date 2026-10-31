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

package com.examplatform.papergenerator.client;

import com.examplatform.papergenerator.dto.BatchTranslationJobResponseDto;
import com.examplatform.papergenerator.dto.PaperTranslateRequest;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.questionbank.grpc.*;
import com.examplatform.shared.auth.ClientAuthTokenResolver;
import com.examplatform.shared.auth.ServiceAccountTokenProvider;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.grpc.GrpcChannelFactory;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.grpc.ManagedChannel;
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Multi-tiered implementation of QuestionBankClient querying question-bank-service.
 * Tier 1: Fast gRPC RPC call (when enabled).
 * Tier 2: REST API call with token propagation.
 * Tier 3: Direct database query fallback when remote service is unavailable.
 *
 * Validates: Requirements 8.1, 8.3
 */
@Slf4j
@Component
public class QuestionBankClientImpl implements QuestionBankClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String questionBankServiceUrl;
    private final ServiceAccountTokenProvider tokenProvider;
    private final String jwtSecret;
    private final boolean grpcEnabled;
    private final String grpcHost;
    private final int grpcPort;
    private final long grpcTimeoutMs;

    public QuestionBankClientImpl(
            JdbcTemplate jdbcTemplate,
            String questionBankServiceUrl) {
        this(jdbcTemplate, null, null, questionBankServiceUrl, "dev-jwt-secret-key-for-local-testing-minimum-32-chars", false, "localhost", 9083, 5000);
    }

    public QuestionBankClientImpl(
            JdbcTemplate jdbcTemplate,
            String questionBankServiceUrl,
            boolean grpcEnabled,
            String grpcHost,
            int grpcPort) {
        this(jdbcTemplate, null, null, questionBankServiceUrl, "dev-jwt-secret-key-for-local-testing-minimum-32-chars", grpcEnabled, grpcHost, grpcPort, 5000);
    }

    @Autowired
    public QuestionBankClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) ServiceAccountTokenProvider tokenProvider,
            @Autowired(required = false) RestClient.Builder restClientBuilder,
            @Value("${app.question-bank.service-url:http://localhost:8083}") String questionBankServiceUrl,
            @Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}") String jwtSecret,
            @Value("${app.question-bank.grpc-enabled:true}") boolean grpcEnabled,
            @Value("${grpc.client.questionbank.host:localhost}") String grpcHost,
            @Value("${grpc.client.questionbank.port:9083}") int grpcPort,
            @Value("${grpc.client.questionbank.timeout-ms:5000}") long grpcTimeoutMs) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenProvider = tokenProvider;
        this.questionBankServiceUrl = questionBankServiceUrl;
        this.jwtSecret = jwtSecret;
        this.grpcEnabled = grpcEnabled;
        this.grpcHost = grpcHost;
        this.grpcPort = grpcPort;
        this.grpcTimeoutMs = grpcTimeoutMs;
        this.restClient = (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build();
    }

    @Override
    public List<QuestionSummary> findAvailableQuestions(Long subjectId, Long topicId, Long subtopicId,
                                                        String difficulty, String cognitiveLevel, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String cleanDifficulty = (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null;
        String cleanCognitiveLevel = (cognitiveLevel != null && !cognitiveLevel.isBlank()) ? cognitiveLevel.trim() : null;

        log.debug("Finding questions by IDs: subjectId={}, topicId={}, subtopicId={}, difficulty='{}', cognitiveLevel='{}', tenant='{}'",
                subjectId, topicId, subtopicId, cleanDifficulty, cleanCognitiveLevel, effectiveTenant);

        // 1. Try gRPC first if enabled
        if (grpcEnabled) {
            try {
                ManagedChannel channel = GrpcChannelFactory.getChannel(grpcHost, grpcPort);
                QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceBlockingStub stub =
                        QuestionBankGrpcServiceGrpc.newBlockingStub(channel)
                                .withDeadlineAfter(grpcTimeoutMs, TimeUnit.MILLISECONDS);

                BlueprintMatchGrpcRequest grpcRequest = BlueprintMatchGrpcRequest.newBuilder()
                        .setSubjectId(subjectId != null ? subjectId : 0L)
                        .setTopicId(topicId != null ? topicId : 0L)
                        .setSubtopicId(subtopicId != null ? subtopicId : 0L)
                        .setDifficulty(cleanDifficulty != null ? cleanDifficulty : "")
                        .setCognitiveLevel(cleanCognitiveLevel != null ? cleanCognitiveLevel : "")
                        .setTenantId(effectiveTenant)
                        .build();

                BlueprintMatchGrpcResponse grpcResponse = stub.matchBlueprint(grpcRequest);
                if (grpcResponse != null) {
                    log.info("Retrieved {} questions from question-bank-service via gRPC", grpcResponse.getQuestionsCount());
                    return grpcResponse.getQuestionsList().stream()
                            .map(this::toSummaryFromGrpc)
                            .toList();
                }
            } catch (Exception e) {
                log.debug("gRPC call to question-bank-service failed ({}:{}), attempting REST fallback: {}",
                        grpcHost, grpcPort, e.getMessage());
            }
        }

        // 2. Try REST call to question-bank-service
        try {
            String url = questionBankServiceUrl + "/api/v1/questions/match-blueprint";
            Map<String, Object> requestBody = new HashMap<>();
            if (subjectId != null) requestBody.put("subjectId", subjectId);
            if (topicId != null) requestBody.put("topicId", topicId);
            if (subtopicId != null) requestBody.put("subtopicId", subtopicId);
            if (cleanDifficulty != null) requestBody.put(SharedConstants.DIFFICULTY, cleanDifficulty);
            if (cleanCognitiveLevel != null) requestBody.put("cognitiveLevel", cleanCognitiveLevel);

            List<QuestionSummary> questions = fetchQuestionsViaRest(url, effectiveTenant, requestBody);
            if (questions != null) {
                return questions;
            }
        } catch (Exception e) {
            log.warn("REST call to question-bank-service failed ({}), falling back to direct DB query: {}",
                    questionBankServiceUrl, e.getMessage());
        }

        // 3. Fallback to direct JDBC query if available
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String sql = """
                SELECT q.id, q.subject_id, q.topic_id, q.subtopic_id, s.name AS subject, t.name AS topic, st.name AS subtopic,
                       q.difficulty, q.cognitive_level, q.usage_count, q.last_used_at, q.content, q.passage_id, q.passage_order_index
                FROM question_service.question q
                LEFT JOIN question_service.subject s ON q.subject_id = s.id
                LEFT JOIN question_service.topic t ON q.topic_id = t.id
                LEFT JOIN question_service.subtopic st ON q.subtopic_id = st.id
                WHERE (q.tenant_id = ? OR q.tenant_id IS NULL OR q.tenant_id = 'default')
                  AND (? IS NULL OR q.subject_id = ?)
                  AND (? IS NULL OR q.topic_id = ?)
                  AND (? IS NULL OR q.subtopic_id = ?)
                  AND q.state IN ('APPROVED', 'PUBLISHED')
                  AND (? IS NULL OR UPPER(TRIM(q.difficulty)) = UPPER(?))
                  AND (? IS NULL OR UPPER(TRIM(q.cognitive_level)) = UPPER(?))
                ORDER BY RANDOM()
                """;

            List<QuestionSummary> questions = jdbcTemplate.query(sql, (rs, _) -> mapQuestionSummary(rs),
                    effectiveTenant,
                    subjectId, subjectId,
                    topicId, topicId,
                    subtopicId, subtopicId,
                    cleanDifficulty, cleanDifficulty,
                    cleanCognitiveLevel, cleanCognitiveLevel);

            if (!questions.isEmpty()) {
                log.info("Found {} questions via DB fallback for subjectId={}, topicId={}, subtopicId={}",
                        questions.size(), subjectId, topicId, subtopicId);
                return questions;
            }

            if (cleanCognitiveLevel != null) {
                String fallbackSql = """
                    SELECT q.id, q.subject_id, q.topic_id, q.subtopic_id, s.name AS subject, t.name AS topic, st.name AS subtopic,
                           q.difficulty, q.cognitive_level, q.usage_count, q.last_used_at, q.content, q.passage_id, q.passage_order_index
                    FROM question_service.question q
                    LEFT JOIN question_service.subject s ON q.subject_id = s.id
                    LEFT JOIN question_service.topic t ON q.topic_id = t.id
                    LEFT JOIN question_service.subtopic st ON q.subtopic_id = st.id
                    WHERE (q.tenant_id = ? OR q.tenant_id IS NULL OR q.tenant_id = 'default')
                      AND (? IS NULL OR q.subject_id = ?)
                      AND (? IS NULL OR q.topic_id = ?)
                      AND (? IS NULL OR q.subtopic_id = ?)
                      AND q.state IN ('APPROVED', 'PUBLISHED')
                      AND (? IS NULL OR UPPER(TRIM(q.difficulty)) = UPPER(?))
                    ORDER BY RANDOM()
                    """;

                return jdbcTemplate.query(fallbackSql, (rs, _) -> mapQuestionSummary(rs),
                        effectiveTenant,
                        subjectId, subjectId,
                        topicId, topicId,
                        subtopicId, subtopicId,
                        cleanDifficulty, cleanDifficulty);
            }

            return questions;
        } catch (Exception e) {
            log.error("Error querying questions from question_service for subjectId={}, topicId={}: {}",
                    subjectId, topicId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<QuestionSummary> findAvailableQuestions(String subject, String topic,
                                                        String difficulty, String cognitiveLevel, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String cleanSubject = (subject != null) ? subject.trim() : "";
        String cleanTopic = (topic != null) ? topic.trim() : "";
        String cleanDifficulty = (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null;
        String cleanCognitiveLevel = (cognitiveLevel != null && !cognitiveLevel.isBlank()) ? cognitiveLevel.trim() : null;

        // 1. Try REST call to question-bank-service
        try {
            String url = questionBankServiceUrl + "/api/v1/questions/match-blueprint";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put(SharedConstants.SUBJECT, cleanSubject);
            requestBody.put("topic", cleanTopic);
            if (cleanDifficulty != null) requestBody.put(SharedConstants.DIFFICULTY, cleanDifficulty);
            if (cleanCognitiveLevel != null) requestBody.put("cognitiveLevel", cleanCognitiveLevel);

            List<QuestionSummary> questions = fetchQuestionsViaRest(url, effectiveTenant, requestBody);
            if (questions != null) {
                return questions;
            }
        } catch (Exception e) {
            log.debug("REST match-blueprint failed for subject='{}': {}", cleanSubject, e.getMessage());
        }

        // 2. Fallback to direct JDBC query
        if (jdbcTemplate != null) {
            try {
                String sql = """
                    SELECT q.id, q.subject_id, q.topic_id, q.subtopic_id, s.name AS subject, t.name AS topic, st.name AS subtopic,
                           q.difficulty, q.cognitive_level, q.usage_count, q.last_used_at, q.content, q.passage_id, q.passage_order_index
                    FROM question_service.question q
                    LEFT JOIN question_service.subject s ON q.subject_id = s.id
                    LEFT JOIN question_service.topic t ON q.topic_id = t.id
                    LEFT JOIN question_service.subtopic st ON q.subtopic_id = st.id
                    WHERE (q.tenant_id = ? OR q.tenant_id = 'default' OR q.tenant_id IS NULL)
                      AND UPPER(TRIM(s.name)) = UPPER(?)
                      AND UPPER(TRIM(t.name)) = UPPER(?)
                      AND q.state IN ('APPROVED', 'PUBLISHED')
                      AND (? IS NULL OR UPPER(TRIM(q.difficulty)) = UPPER(?))
                      AND (? IS NULL OR UPPER(TRIM(q.cognitive_level)) = UPPER(?))
                    ORDER BY RANDOM()
                    """;
                List<QuestionSummary> questions = jdbcTemplate.query(sql, (rs, _) -> mapQuestionSummary(rs),
                        effectiveTenant, cleanSubject, cleanTopic, cleanDifficulty, cleanDifficulty, cleanCognitiveLevel, cleanCognitiveLevel);
                if (!questions.isEmpty()) {
                    return questions;
                }
                if (cleanCognitiveLevel != null) {
                    String fallbackSql = """
                        SELECT q.id, q.subject_id, q.topic_id, q.subtopic_id, s.name AS subject, t.name AS topic, st.name AS subtopic,
                               q.difficulty, q.cognitive_level, q.usage_count, q.last_used_at, q.content, q.passage_id, q.passage_order_index
                        FROM question_service.question q
                        LEFT JOIN question_service.subject s ON q.subject_id = s.id
                        LEFT JOIN question_service.topic t ON q.topic_id = t.id
                        LEFT JOIN question_service.subtopic st ON q.subtopic_id = st.id
                        WHERE (q.tenant_id = ? OR q.tenant_id = 'default' OR q.tenant_id IS NULL)
                          AND UPPER(TRIM(s.name)) = UPPER(?)
                          AND UPPER(TRIM(t.name)) = UPPER(?)
                          AND q.state IN ('APPROVED', 'PUBLISHED')
                          AND (? IS NULL OR UPPER(TRIM(q.difficulty)) = UPPER(?))
                        ORDER BY RANDOM()
                        """;
                    return jdbcTemplate.query(fallbackSql, (rs, _) -> mapQuestionSummary(rs),
                            effectiveTenant, cleanSubject, cleanTopic, cleanDifficulty, cleanDifficulty);
                }
            } catch (Exception e) {
                log.warn("JDBC match failed for subject='{}': {}", cleanSubject, e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    @Override
    public List<QuestionSummary> findQuestionsByIds(List<UUID> questionIds, String tenantId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;

        // 1. Try gRPC first if enabled
        if (grpcEnabled) {
            try {
                ManagedChannel channel = GrpcChannelFactory.getChannel(grpcHost, grpcPort);
                QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceBlockingStub stub =
                        QuestionBankGrpcServiceGrpc.newBlockingStub(channel)
                                .withDeadlineAfter(grpcTimeoutMs, TimeUnit.MILLISECONDS);

                BatchFindQuestionsGrpcRequest.Builder reqBuilder = BatchFindQuestionsGrpcRequest.newBuilder()
                        .setTenantId(effectiveTenant);
                for (UUID id : questionIds) {
                    reqBuilder.addQuestionIds(id.toString());
                }

                BatchFindQuestionsGrpcResponse grpcResponse = stub.batchFindQuestions(reqBuilder.build());
                if (grpcResponse != null) {
                    Map<UUID, QuestionSummary> map = new HashMap<>();
                    for (QuestionSummaryGrpc q : grpcResponse.getQuestionsList()) {
                        try {
                            UUID qId = UUID.fromString(q.getId());
                            map.put(qId, toSummaryFromGrpc(q));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    List<QuestionSummary> ordered = new ArrayList<>();
                    for (UUID qId : questionIds) {
                        QuestionSummary qs = map.get(qId);
                        if (qs != null) {
                            ordered.add(qs);
                        } else {
                            ordered.add(QuestionSummary.builder().questionId(qId).build());
                        }
                    }
                    log.info("Retrieved {} questions by IDs via gRPC from question-bank-service", ordered.size());
                    return ordered;
                }
            } catch (Exception e) {
                log.debug("gRPC batchFindQuestions failed ({}:{}), attempting REST fallback: {}",
                        grpcHost, grpcPort, e.getMessage());
            }
        }

        // 2. Try REST call to question-bank-service
        try {
            String url = questionBankServiceUrl + "/api/v1/questions/by-ids";
            List<QuestionSummary> questions = fetchQuestionsViaRest(url, effectiveTenant, questionIds);
            if (questions != null) {
                return questions;
            }
        } catch (Exception e) {
            log.warn("REST call for by-ids failed ({}), falling back to direct DB query: {}",
                    questionBankServiceUrl, e.getMessage());
        }

        // 3. Fallback to JDBC query
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String inSql = String.join(",", Collections.nCopies(questionIds.size(), "?"));
            String sql = String.format("""
                SELECT q.id, q.subject_id, q.topic_id, q.subtopic_id, s.name AS subject, t.name AS topic, st.name AS subtopic,
                       q.difficulty, q.cognitive_level, q.usage_count, q.last_used_at, q.content, q.passage_id, q.passage_order_index
                FROM question_service.question q
                LEFT JOIN question_service.subject s ON q.subject_id = s.id
                LEFT JOIN question_service.topic t ON q.topic_id = t.id
                LEFT JOIN question_service.subtopic st ON q.subtopic_id = st.id
                WHERE (q.tenant_id = ? OR q.tenant_id = 'default')
                  AND q.id IN (%s)
                """, inSql);

            List<Object> params = new ArrayList<>();
            params.add(effectiveTenant);
            params.addAll(questionIds);

            Map<UUID, QuestionSummary> map = new HashMap<>();
            jdbcTemplate.query(sql, rs -> {
                try {
                    QuestionSummary summary = mapQuestionSummary(rs);
                    map.put(summary.getQuestionId(), summary);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
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

    @Override
    public BatchTranslationJobResponseDto triggerBatchTranslation(
            UUID paperId,
            List<UUID> questionIds,
            PaperTranslateRequest request,
            UUID initiatedBy,
            String tenantId) {

        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String url = questionBankServiceUrl + "/api/v1/translations/batch/auto-translate";

        Map<String, Object> body = new HashMap<>();
        body.put("paperId", paperId);
        body.put("questionIds", questionIds);
        body.put("sourceLanguage", request != null && request.getSourceLanguage() != null ? request.getSourceLanguage() : "en");
        body.put("targetLanguage", request != null && request.getTargetLanguage() != null ? request.getTargetLanguage() : "hi");
        body.put("targetStatus", request != null && request.getTargetStatus() != null ? request.getTargetStatus() :
            QuestionStateConstants.PUBLISHED);
        body.put("overwriteExisting", request == null || request.getOverwriteExisting() == null || request.getOverwriteExisting());
        body.put("batchSize", request != null && request.getBatchSize() != null ? request.getBatchSize() : 50);
        body.put("throttleDelayMs", request != null && request.getThrottleDelayMs() != null ? request.getThrottleDelayMs() : 50);
        body.put("maxConcurrency", request != null && request.getMaxConcurrency() != null ? request.getMaxConcurrency() : 2);

        log.info("Sending batch translation request to {} for paperId={}, questionCount={}",
                url, paperId, questionIds != null ? questionIds.size() : 0);

        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .header(SharedConstants.HEADER_TENANT_ID, effectiveTenant)
                    .contentType(MediaType.APPLICATION_JSON);
            attachAuthHeader(spec);

            return spec
                    .body(body)
                    .retrieve()
                    .body(BatchTranslationJobResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to connect to question-bank-service at {}: {}", url, e.getMessage());
            throw new IllegalStateException("Unable to connect to question-bank-service at " + questionBankServiceUrl +
                    ". Please ensure question-bank-service is running. Details: " + e.getMessage(), e);
        }
    }

    @Override
    public BatchTranslationJobResponseDto getBatchTranslationStatus(UUID jobId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String url = questionBankServiceUrl + "/api/v1/translations/batch/" + jobId;

        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(url)
                    .header(SharedConstants.HEADER_TENANT_ID, effectiveTenant);
            attachAuthHeader(spec);

            return spec
                    .retrieve()
                    .body(BatchTranslationJobResponseDto.class);
        } catch (Exception e) {
            log.error("Failed to get batch translation status for jobId={} from {}: {}", jobId, url, e.getMessage());
            throw new IllegalStateException("Unable to get batch translation status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<BatchTranslationJobResponseDto> listBatchJobsByPaper(UUID paperId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : SharedConstants.DEFAULT_TENANT;
        String url = questionBankServiceUrl + "/api/v1/translations/batch/paper/" + paperId;

        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(url)
                    .header(SharedConstants.HEADER_TENANT_ID, effectiveTenant);
            attachAuthHeader(spec);

            List<BatchTranslationJobResponseDto> res = spec
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get batch translation jobs for paperId={} from {}: {}", paperId, url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static final List<String> CLIENT_ROLES = List.of(
            "SUPER_ADMIN", "EXAM_CONTROLLER", "QUESTION_AUTHOR", "REVIEWER");

    private static QuestionSummary mapQuestionSummary(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("last_used_at");
        Instant lastUsedAt = ts != null ? ts.toInstant() : null;
        UUID passageId = rs.getObject("passage_id", UUID.class);
        Integer passageOrderIndex = (Integer) rs.getObject("passage_order_index");

        Long subjectId = null;
        try { subjectId = rs.getObject("subject_id", Long.class); } catch (Exception ignored) {}
        Long topicId = null;
        try { topicId = rs.getObject("topic_id", Long.class); } catch (Exception ignored) {}
        Long subtopicId = null;
        try { subtopicId = rs.getObject("subtopic_id", Long.class); } catch (Exception ignored) {}
        String subtopic = null;
        try { subtopic = rs.getString("subtopic"); } catch (Exception ignored) {}

        return QuestionSummary.builder()
                .questionId(rs.getObject("id", UUID.class))
                .subjectId(subjectId)
                .topicId(topicId)
                .subtopicId(subtopicId)
                .subject(rs.getString(SharedConstants.SUBJECT))
                .topic(rs.getString("topic"))
                .subtopic(subtopic)
                .difficulty(rs.getString(SharedConstants.DIFFICULTY))
                .cognitiveLevel(rs.getString("cognitive_level"))
                .usageCount(rs.getInt("usage_count"))
                .lastUsedAt(lastUsedAt)
                .reusePolicy("1_YEAR")
                .content(rs.getString(SharedConstants.CONTENT))
                .passageId(passageId)
                .passageOrderIndex(passageOrderIndex)
                .build();
    }

    private List<QuestionSummary> fetchQuestionsViaRest(String url, String effectiveTenant, Object requestBody) {
        RestClient.RequestBodySpec spec = restClient.post()
                .uri(url)
                .header(SharedConstants.HEADER_TENANT_ID, effectiveTenant)
                .contentType(MediaType.APPLICATION_JSON);
        attachAuthHeader(spec);

        ApiResponseDto<List<QuestionResponseDto>> apiResponse = spec
                .body(requestBody)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (apiResponse != null && apiResponse.getData() != null) {
            log.info("Retrieved {} questions from question-bank-service via REST", apiResponse.getData().size());
            return apiResponse.getData().stream()
                    .map(this::toSummary)
                    .toList();
        }
        return null;
    }

    private void attachAuthHeader(RestClient.RequestBodySpec spec) {
        ClientAuthTokenResolver.attachAuthHeader(spec, "paper-generator", tokenProvider, jwtSecret, CLIENT_ROLES);
    }

    private void attachAuthHeader(RestClient.RequestHeadersSpec<?> spec) {
        ClientAuthTokenResolver.attachAuthHeader(spec, "paper-generator", tokenProvider, jwtSecret, CLIENT_ROLES);
    }

    private QuestionSummary toSummary(QuestionResponseDto dto) {
        return QuestionSummary.builder()
                .questionId(dto.getId())
                .subjectId(dto.getSubjectId())
                .topicId(dto.getTopicId())
                .subtopicId(dto.getSubtopicId())
                .subject(dto.getSubject())
                .topic(dto.getTopic())
                .subtopic(dto.getSubtopic())
                .difficulty(dto.getDifficulty())
                .cognitiveLevel(dto.getCognitiveLevel())
                .usageCount(0)
                .reusePolicy("1_YEAR")
                .content(dto.getContent())
                .passageId(dto.getPassageId())
                .passageOrderIndex(dto.getPassageOrderIndex())
                .build();
    }

    private QuestionSummary toSummaryFromGrpc(QuestionSummaryGrpc grpc) {
        UUID passageId = null;
        if (!grpc.getPassageId().isBlank()) {
            try {
                passageId = UUID.fromString(grpc.getPassageId());
            } catch (IllegalArgumentException ignored) {
            }
        }

        Long subjectId = null;
        if (grpc.getSubjectId() != null && !grpc.getSubjectId().isBlank()) {
            try { subjectId = Long.parseLong(grpc.getSubjectId()); } catch (NumberFormatException ignored) {}
        }
        Long topicId = null;
        if (grpc.getTopicId() != null && !grpc.getTopicId().isBlank()) {
            try { topicId = Long.parseLong(grpc.getTopicId()); } catch (NumberFormatException ignored) {}
        }
        Long subtopicId = null;
        if (grpc.getSubtopicId() != null && !grpc.getSubtopicId().isBlank()) {
            try { subtopicId = Long.parseLong(grpc.getSubtopicId()); } catch (NumberFormatException ignored) {}
        }

        return QuestionSummary.builder()
                .questionId(UUID.fromString(grpc.getId()))
                .subjectId(subjectId)
                .topicId(topicId)
                .subtopicId(subtopicId)
                .subject(grpc.getSubject())
                .topic(grpc.getTopic())
                .subtopic(grpc.getSubtopic())
                .difficulty(grpc.getDifficulty())
                .cognitiveLevel(grpc.getCognitiveLevel())
                .usageCount(grpc.getUsageCount())
                .reusePolicy("1_YEAR")
                .content(grpc.getContent())
                .passageId(passageId)
                .passageOrderIndex(grpc.getPassageOrderIndex())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApiResponseDto<T> {
        private String status;
        private Boolean success;
        private T data;
        private String message;
        private Object timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QuestionResponseDto {
        private UUID id;
        private Long subjectId;
        private Long topicId;
        private Long subtopicId;
        private String subject;
        private String topic;
        private String subtopic;
        private String difficulty;
        private String cognitiveLevel;
        private String content;
        private UUID passageId;
        private Integer passageOrderIndex;
    }
}
