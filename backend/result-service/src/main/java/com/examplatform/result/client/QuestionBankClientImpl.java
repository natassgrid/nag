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

import com.examplatform.result.dto.QuestionDetailDto;
import com.examplatform.result.dto.ReviewOptionDto;
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
 * REST-first implementation of QuestionBankClient querying question-bank-service.
 * Falls back to direct database queries when running in single-database or monolith mode.
 */
@Slf4j
@Component
public class QuestionBankClientImpl implements QuestionBankClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String questionBankServiceUrl;
    private final ServiceAccountTokenProvider tokenProvider;
    private final String jwtSecret;
    private final ObjectMapper objectMapper;

    @Autowired
    public QuestionBankClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) ServiceAccountTokenProvider tokenProvider,
            @Value("${app.question-bank.service-url:http://localhost:8083}") String questionBankServiceUrl,
            @Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}") String jwtSecret,
            ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenProvider = tokenProvider;
        this.questionBankServiceUrl = questionBankServiceUrl;
        this.jwtSecret = jwtSecret;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public List<QuestionDetailDto> findQuestionsByIds(List<UUID> questionIds, String tenantId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        // 1. Try REST call to question-bank-service
        try {
            String url = questionBankServiceUrl + "/api/v1/questions/by-ids";
            RestClient.RequestBodySpec spec = restClient.post()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
                    .contentType(MediaType.APPLICATION_JSON);
            attachAuthHeader(spec);

            ApiResponseDto<List<RemoteQuestionDto>> apiResponse = spec
                    .body(questionIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseDto<List<RemoteQuestionDto>>>() {});

            if (apiResponse != null && apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                log.info("Retrieved {} questions from question-bank-service via REST", apiResponse.getData().size());
                return apiResponse.getData().stream()
                        .map(this::toDetailDto)
                        .toList();
            }
        } catch (Exception e) {
            log.warn("REST call to question-bank-service failed ({}), falling back to direct DB query: {}",
                    questionBankServiceUrl, e.getMessage());
        }

        // 2. Fallback to direct JDBC query
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String inSql = String.join(",", Collections.nCopies(questionIds.size(), "?"));
            String sql = String.format("""
                SELECT id, subject, topic, difficulty, cognitive_level, content, options, explanation, answer_key
                FROM question_service.question
                WHERE (tenant_id = ? OR tenant_id = 'default')
                  AND id IN (%s)
                """, inSql);

            List<Object> params = new ArrayList<>();
            params.add(effectiveTenant);
            params.addAll(questionIds);

            Map<UUID, QuestionDetailDto> map = new HashMap<>();
            jdbcTemplate.query(sql, rs -> {
                UUID id = rs.getObject("id", UUID.class);
                String optionsJson = rs.getString("options");
                List<ReviewOptionDto> options = parseOptions(optionsJson);

                QuestionDetailDto dto = QuestionDetailDto.builder()
                        .id(id)
                        .subject(rs.getString("subject"))
                        .topic(rs.getString("topic"))
                        .difficulty(rs.getString("difficulty"))
                        .cognitiveLevel(rs.getString("cognitive_level"))
                        .content(rs.getString("content"))
                        .options(options)
                        .explanation(rs.getString("explanation"))
                        .answerKey(rs.getString("answer_key"))
                        .build();
                map.put(id, dto);
            }, params.toArray());

            List<QuestionDetailDto> result = new ArrayList<>();
            for (UUID qId : questionIds) {
                QuestionDetailDto dto = map.get(qId);
                if (dto != null) {
                    result.add(dto);
                }
            }
            log.info("Retrieved {} questions via JDBC fallback for tenant={}", result.size(), effectiveTenant);
            return result;
        } catch (Exception e) {
            log.warn("Error querying question_service table via JDBC: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<ReviewOptionDto> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<List<ReviewOptionDto>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse options JSON: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private QuestionDetailDto toDetailDto(RemoteQuestionDto remote) {
        List<ReviewOptionDto> options = Collections.emptyList();
        if (remote.getOptions() != null) {
            options = remote.getOptions().stream()
                    .map(opt -> ReviewOptionDto.builder()
                            .id(opt.getId())
                            .text(opt.getText())
                            .isCorrect(opt.isCorrect())
                            .build())
                    .toList();
        }
        return QuestionDetailDto.builder()
                .id(remote.getId())
                .subject(remote.getSubject())
                .topic(remote.getTopic())
                .difficulty(remote.getDifficulty())
                .cognitiveLevel(remote.getCognitiveLevel())
                .content(remote.getContent())
                .options(options)
                .explanation(remote.getExplanation())
                .answerKey(remote.getAnswerKey())
                .build();
    }

    private void attachAuthHeader(RestClient.RequestBodySpec spec) {
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
    public static class RemoteQuestionDto {
        private UUID id;
        private String subject;
        private String topic;
        private String difficulty;
        private String cognitiveLevel;
        private String content;
        private String answerKey;
        private String explanation;
        private List<RemoteOptionDto> options;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RemoteOptionDto {
        private String id;
        private String text;
        private boolean isCorrect;
    }
}
