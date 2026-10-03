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

package com.examplatform.result.client;

import com.examplatform.result.dto.CandidateEvaluationItemDto;
import com.examplatform.result.dto.CandidateExamResponseDto;
import com.examplatform.shared.auth.ServiceAccountTokenProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST-first implementation of EvaluationClient querying evaluation-service and response-service.
 * Falls back to direct database queries when running in single-database or monolith mode.
 */
@Slf4j
@Component
public class EvaluationClientImpl implements EvaluationClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String evaluationServiceUrl;
    private final String responseServiceUrl;
    private final ServiceAccountTokenProvider tokenProvider;
    private final String jwtSecret;
    private final ObjectMapper objectMapper;

    @Autowired
    public EvaluationClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) ServiceAccountTokenProvider tokenProvider,
            @Value("${app.evaluation.service-url:http://localhost:8086}") String evaluationServiceUrl,
            @Value("${app.response.service-url:http://localhost:8085}") String responseServiceUrl,
            @Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}") String jwtSecret,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenProvider = tokenProvider;
        this.evaluationServiceUrl = evaluationServiceUrl;
        this.responseServiceUrl = responseServiceUrl;
        this.jwtSecret = jwtSecret;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public List<CandidateEvaluationItemDto> getEvaluationsForCandidate(UUID candidateId, UUID examId, String tenantId) {
        if (candidateId == null) {
            return Collections.emptyList();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // 1. Try REST call to evaluation-service
        try {
            String url = evaluationServiceUrl + "/api/v1/evaluations/candidate/" + candidateId;
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
                    .accept(MediaType.APPLICATION_JSON);
            attachAuthHeader(spec);

            List<RemoteEvaluationDto> evaluations = spec
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<RemoteEvaluationDto>>() {});

            if (evaluations != null && !evaluations.isEmpty()) {
                log.info("Retrieved {} evaluations from evaluation-service via REST for candidate={}",
                        evaluations.size(), candidateId);

                // Fetch candidate response options if session ID is present
                UUID sessionId = evaluations.get(0).getSessionId();
                Map<UUID, CandidateResponseMetadata> responseMetaMap = fetchResponseMetadataViaRest(sessionId, effectiveTenant);

                return evaluations.stream()
                        .map(e -> toItemDto(e, responseMetaMap.get(e.getQuestionId())))
                        .toList();
            }
        } catch (Exception e) {
            log.warn("REST call to evaluation-service failed ({}), falling back to direct DB query: {}",
                    evaluationServiceUrl, e.getMessage());
        }

        // 2. Fallback to JDBC query
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String evalSql = """
                SELECT id, session_id, question_id, candidate_id, score, max_marks, negative_marks, comments, status
                FROM evaluation_service.evaluation
                WHERE candidate_id = ?
                  AND (tenant_id = ? OR tenant_id = 'default')
                ORDER BY created_at ASC
                """;

            List<CandidateEvaluationItemDto> items = new ArrayList<>();
            Map<UUID, UUID> questionToSession = new HashMap<>();

            jdbcTemplate.query(evalSql, rs -> {
                UUID id = rs.getObject("id", UUID.class);
                UUID sessId = rs.getObject("session_id", UUID.class);
                UUID qId = rs.getObject("question_id", UUID.class);
                UUID cId = rs.getObject("candidate_id", UUID.class);
                double score = rs.getDouble("score");
                double maxMarks = rs.getDouble("max_marks");
                double negMarks = rs.getDouble("negative_marks");
                String comments = rs.getString("comments");
                String status = rs.getString("status");

                if (qId != null) {
                    questionToSession.put(qId, sessId);
                }

                items.add(CandidateEvaluationItemDto.builder()
                        .evaluationId(id)
                        .sessionId(sessId)
                        .questionId(qId)
                        .candidateId(cId)
                        .score(score)
                        .maxMarks(maxMarks)
                        .negativeMarks(negMarks)
                        .comments(comments)
                        .status(status)
                        .candidateSelectedOptionIds(Collections.emptyList())
                        .timeSpentMs(0L)
                        .build());
            }, candidateId, effectiveTenant);

            if (!items.isEmpty()) {
                // Try to enrich with response data from response_service table if present
                enrichFromResponseTable(items, candidateId, effectiveTenant);
            }

            log.info("Retrieved {} evaluation items via JDBC fallback for candidate={}", items.size(), candidateId);
            return items;

        } catch (Exception e) {
            log.warn("Error querying evaluation_service table via JDBC: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<CandidateExamResponseDto> getResponsesForExam(UUID examId, String tenantId) {
        if (examId == null) {
            return Collections.emptyList();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // 1. Try JDBC query first for fastest batch computation
        if (jdbcTemplate != null) {
            try {
                // Fetch candidates and their total scores for this exam
                String resultSql = """
                    SELECT candidate_id, total_score
                    FROM result_service.result
                    WHERE exam_id = ? AND (tenant_id = ? OR tenant_id = 'default')
                    """;

                Map<UUID, Double> candidateTotalScores = new HashMap<>();
                jdbcTemplate.query(resultSql, rs -> {
                    UUID candidateId = rs.getObject("candidate_id", UUID.class);
                    double totalScore = rs.getDouble("total_score");
                    candidateTotalScores.put(candidateId, totalScore);
                }, examId, effectiveTenant);

                if (!candidateTotalScores.isEmpty()) {
                    List<UUID> candidateIds = new ArrayList<>(candidateTotalScores.keySet());
                    String inSql = String.join(",", Collections.nCopies(candidateIds.size(), "?"));

                    // Fetch evaluations for these candidates
                    String evalSql = String.format("""
                        SELECT candidate_id, question_id, score, max_marks
                        FROM evaluation_service.evaluation
                        WHERE (tenant_id = ? OR tenant_id = 'default')
                          AND candidate_id IN (%s)
                        """, inSql);

                    List<Object> evalParams = new ArrayList<>();
                    evalParams.add(effectiveTenant);
                    evalParams.addAll(candidateIds);

                    List<CandidateExamResponseDto> dtos = new ArrayList<>();
                    jdbcTemplate.query(evalSql, rs -> {
                        UUID cId = rs.getObject("candidate_id", UUID.class);
                        UUID qId = rs.getObject("question_id", UUID.class);
                        double score = rs.getDouble("score");
                        double maxMarks = rs.getDouble("max_marks");
                        double totalScore = candidateTotalScores.getOrDefault(cId, score);

                        dtos.add(CandidateExamResponseDto.builder()
                                .candidateId(cId)
                                .questionId(qId)
                                .examTotalScore(totalScore)
                                .questionScore(score)
                                .maxMarks(maxMarks)
                                .selectedOptionIds(Collections.emptyList())
                                .timeSpentMs(0L)
                                .isCorrect(score > 0 && score >= maxMarks)
                                .build());
                    }, evalParams.toArray());

                    // Enrich option selections from response_service table if available
                    try {
                        String respSql = String.format("""
                            SELECT candidate_id, question_id, selected_option_ids, cumulative_time_spent_ms
                            FROM response_service.response
                            WHERE (tenant_id = ? OR tenant_id = 'default')
                              AND is_final = TRUE
                              AND candidate_id IN (%s)
                            """, inSql);

                        Map<String, CandidateResponseMetadata> respMap = new HashMap<>();
                        jdbcTemplate.query(respSql, rs -> {
                            UUID cId = rs.getObject("candidate_id", UUID.class);
                            UUID qId = rs.getObject("question_id", UUID.class);
                            String selectedJson = rs.getString("selected_option_ids");
                            long timeSpent = rs.getLong("cumulative_time_spent_ms");
                            List<String> options = parseSelectedOptions(selectedJson);
                            respMap.put(cId + "_" + qId, new CandidateResponseMetadata(options, timeSpent));
                        }, evalParams.toArray());

                        for (CandidateExamResponseDto dto : dtos) {
                            CandidateResponseMetadata meta = respMap.get(dto.getCandidateId() + "_" + dto.getQuestionId());
                            if (meta != null) {
                                dto.setSelectedOptionIds(meta.selectedOptionIds());
                                dto.setTimeSpentMs(meta.timeSpentMs());
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Response service enrichment failed: {}", e.getMessage());
                    }

                    log.info("Retrieved {} exam responses via JDBC for exam={}", dtos.size(), examId);
                    return dtos;
                }
            } catch (Exception e) {
                log.warn("JDBC query for exam responses failed: {}", e.getMessage());
            }
        }

        return Collections.emptyList();
    }

    private void enrichFromResponseTable(List<CandidateEvaluationItemDto> items, UUID candidateId, String tenantId) {
        try {
            String respSql = """
                SELECT question_id, selected_option_ids, cumulative_time_spent_ms
                FROM response_service.response
                WHERE candidate_id = ?
                  AND (tenant_id = ? OR tenant_id = 'default')
                  AND is_final = TRUE
                """;

            Map<UUID, CandidateResponseMetadata> map = new HashMap<>();
            jdbcTemplate.query(respSql, rs -> {
                UUID qId = rs.getObject("question_id", UUID.class);
                String selectedJson = rs.getString("selected_option_ids");
                long timeSpent = rs.getLong("cumulative_time_spent_ms");
                List<String> options = parseSelectedOptions(selectedJson);
                map.put(qId, new CandidateResponseMetadata(options, timeSpent));
            }, candidateId, tenantId);

            for (CandidateEvaluationItemDto item : items) {
                CandidateResponseMetadata meta = map.get(item.getQuestionId());
                if (meta != null) {
                    item.setCandidateSelectedOptionIds(meta.selectedOptionIds());
                    item.setTimeSpentMs(meta.timeSpentMs());
                }
            }
        } catch (Exception e) {
            log.debug("Response service table not available or error during enrichment: {}", e.getMessage());
        }
    }

    private Map<UUID, CandidateResponseMetadata> fetchResponseMetadataViaRest(UUID sessionId, String tenantId) {
        if (sessionId == null) {
            return Collections.emptyMap();
        }
        try {
            String url = responseServiceUrl + "/api/v1/responses/" + sessionId + "/responses";
            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(url)
                    .header("X-Tenant-Id", tenantId)
                    .accept(MediaType.APPLICATION_JSON);
            attachAuthHeader(spec);

            ApiResponseDto<List<RemoteResponseDto>> resp = spec
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseDto<List<RemoteResponseDto>>>() {});

            if (resp != null && resp.getData() != null) {
                Map<UUID, CandidateResponseMetadata> map = new HashMap<>();
                for (RemoteResponseDto r : resp.getData()) {
                    List<String> selected = parseSelectedOptions(r.getSelectedOptionIds());
                    map.put(r.getQuestionId(), new CandidateResponseMetadata(selected, r.getCumulativeTimeSpentMs()));
                }
                return map;
            }
        } catch (Exception e) {
            log.debug("Failed to fetch response metadata via REST: {}", e.getMessage());
        }
        return Collections.emptyMap();
    }

    private List<String> parseSelectedOptions(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.debug("Failed to parse selected option JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private CandidateEvaluationItemDto toItemDto(RemoteEvaluationDto remote, CandidateResponseMetadata meta) {
        List<String> selected = meta != null ? meta.selectedOptionIds() : Collections.emptyList();
        long timeSpent = meta != null ? meta.timeSpentMs() : 0L;

        return CandidateEvaluationItemDto.builder()
                .evaluationId(remote.getId())
                .sessionId(remote.getSessionId())
                .questionId(remote.getQuestionId())
                .candidateId(remote.getCandidateId())
                .score(remote.getScore() != null ? remote.getScore() : 0.0)
                .maxMarks(remote.getMaxMarks() != null ? remote.getMaxMarks() : 1.0)
                .negativeMarks(remote.getNegativeMarks() != null ? remote.getNegativeMarks() : 0.0)
                .comments(remote.getComments())
                .status(remote.getStatus())
                .candidateSelectedOptionIds(selected)
                .timeSpentMs(timeSpent)
                .build();
    }

    private void attachAuthHeader(RestClient.RequestHeadersSpec<?> spec) {
        String token = resolveAuthToken();
        if (token != null) {
            spec.header(HttpHeaders.AUTHORIZATION, token);
        }
    }

    private String resolveAuthToken() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttrs) {
            String authHeader = servletAttrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && !authHeader.isBlank()) {
                return authHeader;
            }
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return "Bearer " + jwtAuth.getToken().getTokenValue();
        } else if (auth != null && auth.getCredentials() instanceof String cred && !cred.isBlank()) {
            return cred.startsWith("Bearer ") ? cred : "Bearer " + cred;
        }

        if (tokenProvider != null) {
            try {
                String token = tokenProvider.getServiceToken("result-service");
                if (token != null && !token.isBlank()) {
                    return "Bearer " + token;
                }
            } catch (Exception ignored) {
            }
        }

        if (jwtSecret != null && !jwtSecret.isBlank()) {
            return "Bearer " + generateDevToken();
        }

        return null;
    }

    private String generateDevToken() {
        long now = System.currentTimeMillis() / 1000;
        long exp = now + 3600;
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{" +
                "\"sub\":\"result-service\"," +
                "\"preferred_username\":\"result-service\"," +
                "\"name\":\"result-service\"," +
                "\"iss\":\"exam-platform-dev\"," +
                "\"aud\":\"exam-backend\"," +
                "\"iat\":" + now + "," +
                "\"exp\":" + exp + "," +
                "\"realm_access\":{\"roles\":[\"SUPER_ADMIN\",\"EXAM_CONTROLLER\"]}" +
                "}");
        String signingInput = header + "." + payload;
        String signature = hmacSha256(signingInput);
        return signingInput + "." + signature;
    }

    private String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }

    private record CandidateResponseMetadata(List<String> selectedOptionIds, long timeSpentMs) {}

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
    public static class RemoteEvaluationDto {
        private UUID id;
        private UUID sessionId;
        private UUID questionId;
        private UUID candidateId;
        private Double score;
        private Double maxMarks;
        private Double negativeMarks;
        private String comments;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteResponseDto {
        private UUID questionId;
        private String selectedOptionIds;
        private long cumulativeTimeSpentMs;
    }
}
