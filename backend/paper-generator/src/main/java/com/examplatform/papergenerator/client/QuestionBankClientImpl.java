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

import com.examplatform.papergenerator.dto.BatchTranslationJobResponseDto;
import com.examplatform.papergenerator.dto.PaperTranslateRequest;
import com.examplatform.papergenerator.dto.QuestionSummary;
import com.examplatform.questionbank.grpc.BatchFindQuestionsGrpcRequest;
import com.examplatform.questionbank.grpc.BatchFindQuestionsGrpcResponse;
import com.examplatform.questionbank.grpc.BlueprintMatchGrpcRequest;
import com.examplatform.questionbank.grpc.BlueprintMatchGrpcResponse;
import com.examplatform.questionbank.grpc.QuestionBankGrpcServiceGrpc;
import com.examplatform.questionbank.grpc.QuestionSummaryGrpc;
import com.examplatform.shared.auth.ClientAuthTokenResolver;
import com.examplatform.shared.auth.ServiceAccountTokenProvider;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    public List<QuestionSummary> findAvailableQuestions(String subject, String topic,
                                                        String difficulty, String cognitiveLevel, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String cleanSubject = (subject != null) ? subject.trim() : "";
        String cleanTopic = (topic != null) ? topic.trim() : "";
        String cleanDifficulty = (difficulty != null && !difficulty.isBlank()) ? difficulty.trim() : null;
        String cleanCognitiveLevel = (cognitiveLevel != null && !cognitiveLevel.isBlank()) ? cognitiveLevel.trim() : null;

        log.debug("Finding questions: subject='{}', topic='{}', difficulty='{}', cognitiveLevel='{}', tenant='{}'",
                cleanSubject, cleanTopic, cleanDifficulty, cleanCognitiveLevel, effectiveTenant);

        // 1. Try gRPC first if enabled
        if (grpcEnabled) {
            try {
                ManagedChannel channel = GrpcChannelFactory.getChannel(grpcHost, grpcPort);
                QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceBlockingStub stub =
                        QuestionBankGrpcServiceGrpc.newBlockingStub(channel)
                                .withDeadlineAfter(grpcTimeoutMs, TimeUnit.MILLISECONDS);

                BlueprintMatchGrpcRequest grpcRequest = BlueprintMatchGrpcRequest.newBuilder()
                        .setSubject(cleanSubject)
                        .setTopic(cleanTopic)
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
            String url = questionBankServiceUrl + "/api/v1/questions/blueprint-match";
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("subject", cleanSubject);
            requestBody.put("topic", cleanTopic);
            if (cleanDifficulty != null) requestBody.put("difficulty", cleanDifficulty);
            if (cleanCognitiveLevel != null) requestBody.put("cognitiveLevel", cleanCognitiveLevel);

            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
                    .contentType(MediaType.APPLICATION_JSON);
            attachAuthHeader(spec);

            ApiResponseDto<List<QuestionResponseDto>> apiResponse = spec
                    .body(requestBody)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseDto<List<QuestionResponseDto>>>() {});

            if (apiResponse != null && apiResponse.getData() != null) {
                log.info("Retrieved {} questions from question-bank-service via REST", apiResponse.getData().size());
                return apiResponse.getData().stream()
                        .map(this::toSummary)
                        .toList();
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
            // First attempt: exact match on subject, topic, difficulty, and cognitive level
            String sql = """
                SELECT id, subject, topic, difficulty, cognitive_level, usage_count, last_used_at, content, passage_id, passage_order_index
                FROM question_service.question
                WHERE tenant_id = ?
                  AND UPPER(TRIM(subject)) = UPPER(?)
                  AND UPPER(TRIM(topic)) = UPPER(?)
                  AND state = 'PUBLISHED'
                  AND (? IS NULL OR UPPER(TRIM(difficulty)) = UPPER(?))
                  AND (? IS NULL OR UPPER(TRIM(cognitive_level)) = UPPER(?))
                ORDER BY RANDOM()
                """;

            List<QuestionSummary> questions = jdbcTemplate.query(sql, (rs, rowNum) -> {
                Timestamp ts = rs.getTimestamp("last_used_at");
                Instant lastUsedAt = ts != null ? ts.toInstant() : null;
                UUID passageId = rs.getObject("passage_id", UUID.class);
                Integer passageOrderIndex = (Integer) rs.getObject("passage_order_index");
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
                        .passageId(passageId)
                        .passageOrderIndex(passageOrderIndex)
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
                    SELECT id, subject, topic, difficulty, cognitive_level, usage_count, last_used_at, content, passage_id, passage_order_index
                    FROM question_service.question
                    WHERE tenant_id = ?
                      AND UPPER(TRIM(subject)) = UPPER(?)
                      AND UPPER(TRIM(topic)) = UPPER(?)
                      AND state = 'PUBLISHED'
                      AND (? IS NULL OR UPPER(TRIM(difficulty)) = UPPER(?))
                    ORDER BY RANDOM()
                    """;

                List<QuestionSummary> fallbackQuestions = jdbcTemplate.query(fallbackSql, (rs, rowNum) -> {
                    Timestamp ts = rs.getTimestamp("last_used_at");
                    Instant lastUsedAt = ts != null ? ts.toInstant() : null;
                    UUID passageId = rs.getObject("passage_id", UUID.class);
                    Integer passageOrderIndex = (Integer) rs.getObject("passage_order_index");
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
                            .passageId(passageId)
                            .passageOrderIndex(passageOrderIndex)
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
            String url = questionBankServiceUrl + "/api/v1/questions/batch-find";
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
                    .contentType(MediaType.APPLICATION_JSON);
            attachAuthHeader(spec);

            ApiResponseDto<List<QuestionResponseDto>> apiResponse = spec
                    .body(questionIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseDto<List<QuestionResponseDto>>>() {});

            if (apiResponse != null && apiResponse.getData() != null) {
                log.info("Retrieved {} questions by IDs from question-bank-service via REST", apiResponse.getData().size());
                return apiResponse.getData().stream()
                        .map(this::toSummary)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("REST call for batch-find failed ({}), falling back to direct DB query: {}",
                    questionBankServiceUrl, e.getMessage());
        }

        // 3. Fallback to JDBC query
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String inSql = String.join(",", Collections.nCopies(questionIds.size(), "?"));
            String sql = String.format("""
                SELECT id, subject, topic, difficulty, cognitive_level, usage_count, last_used_at, content, passage_id, passage_order_index
                FROM question_service.question
                WHERE (tenant_id = ? OR tenant_id = 'default')
                  AND id IN (%s)
                """, inSql);

            List<Object> params = new ArrayList<>();
            params.add(effectiveTenant);
            params.addAll(questionIds);

            Map<UUID, QuestionSummary> map = new HashMap<>();
            jdbcTemplate.query(sql, rs -> {
                UUID qId = rs.getObject("id", UUID.class);
                Timestamp ts = rs.getTimestamp("last_used_at");
                Instant lastUsedAt = ts != null ? ts.toInstant() : null;
                UUID passageId = rs.getObject("passage_id", UUID.class);
                Integer passageOrderIndex = (Integer) rs.getObject("passage_order_index");
                map.put(qId, QuestionSummary.builder()
                        .questionId(qId)
                        .subject(rs.getString("subject"))
                        .topic(rs.getString("topic"))
                        .difficulty(rs.getString("difficulty"))
                        .cognitiveLevel(rs.getString("cognitive_level"))
                        .usageCount(rs.getInt("usage_count"))
                        .lastUsedAt(lastUsedAt)
                        .reusePolicy("1_YEAR")
                        .content(rs.getString("content"))
                        .passageId(passageId)
                        .passageOrderIndex(passageOrderIndex)
                        .build());
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

        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String url = questionBankServiceUrl + "/api/v1/translations/batch/auto-translate";

        Map<String, Object> body = new HashMap<>();
        body.put("paperId", paperId);
        body.put("questionIds", questionIds);
        body.put("sourceLanguage", request != null && request.getSourceLanguage() != null ? request.getSourceLanguage() : "en");
        body.put("targetLanguage", request != null && request.getTargetLanguage() != null ? request.getTargetLanguage() : "hi");
        body.put("targetStatus", request != null && request.getTargetStatus() != null ? request.getTargetStatus() : "PUBLISHED");
        body.put("overwriteExisting", request == null || request.getOverwriteExisting() == null || request.getOverwriteExisting());
        body.put("batchSize", request != null && request.getBatchSize() != null ? request.getBatchSize() : 50);
        body.put("throttleDelayMs", request != null && request.getThrottleDelayMs() != null ? request.getThrottleDelayMs() : 50);
        body.put("maxConcurrency", request != null && request.getMaxConcurrency() != null ? request.getMaxConcurrency() : 2);

        log.info("Sending batch translation request to {} for paperId={}, questionCount={}",
                url, paperId, questionIds != null ? questionIds.size() : 0);

        try {
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
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
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String url = questionBankServiceUrl + "/api/v1/translations/batch/" + jobId;

        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant);
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
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String url = questionBankServiceUrl + "/api/v1/translations/batch/paper/" + paperId;

        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant);
            attachAuthHeader(spec);

            List<BatchTranslationJobResponseDto> res = spec
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BatchTranslationJobResponseDto>>() {});
            return res != null ? res : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to get batch translation jobs for paperId={} from {}: {}", paperId, url, e.getMessage());
            return Collections.emptyList();
        }
    }

    private static final List<String> CLIENT_ROLES = List.of(
            "SUPER_ADMIN", "EXAM_CONTROLLER", "QUESTION_AUTHOR", "REVIEWER");

    private void attachAuthHeader(RestClient.RequestBodySpec spec) {
        ClientAuthTokenResolver.attachAuthHeader(spec, "paper-generator", tokenProvider, jwtSecret, CLIENT_ROLES);
    }

    private void attachAuthHeader(RestClient.RequestHeadersSpec<?> spec) {
        ClientAuthTokenResolver.attachAuthHeader(spec, "paper-generator", tokenProvider, jwtSecret, CLIENT_ROLES);
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
                .passageId(dto.getPassageId())
                .passageOrderIndex(dto.getPassageOrderIndex())
                .build();
    }

    private QuestionSummary toSummaryFromGrpc(QuestionSummaryGrpc grpc) {
        UUID passageId = null;
        if (grpc.getPassageId() != null && !grpc.getPassageId().isBlank()) {
            try {
                passageId = UUID.fromString(grpc.getPassageId());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return QuestionSummary.builder()
                .questionId(UUID.fromString(grpc.getId()))
                .subject(grpc.getSubject())
                .topic(grpc.getTopic())
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
        private String subject;
        private String topic;
        private String difficulty;
        private String cognitiveLevel;
        private String content;
        private UUID passageId;
        private Integer passageOrderIndex;
    }
}
