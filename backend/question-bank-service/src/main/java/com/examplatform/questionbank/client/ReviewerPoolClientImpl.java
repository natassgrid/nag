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

package com.examplatform.questionbank.client;

import com.examplatform.questionbank.dto.ReviewerDto;
import com.examplatform.shared.auth.ServiceAccountTokenProvider;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST-first client implementation for retrieving reviewers from identity-service.
 * Falls back to direct database queries if identity-service REST endpoint is unreachable.
 */
@Slf4j
@Component
public class ReviewerPoolClientImpl implements ReviewerPoolClient {

    private final JdbcTemplate jdbcTemplate;
    private final RestClient restClient;
    private final String identityServiceUrl;
    private final ServiceAccountTokenProvider tokenProvider;
    private final String jwtSecret;

    public ReviewerPoolClientImpl(
            JdbcTemplate jdbcTemplate,
            String identityServiceUrl) {
        this(jdbcTemplate, null, identityServiceUrl, "dev-jwt-secret-key-for-local-testing-minimum-32-chars");
    }

    @Autowired
    public ReviewerPoolClientImpl(
            @Autowired(required = false) JdbcTemplate jdbcTemplate,
            @Autowired(required = false) ServiceAccountTokenProvider tokenProvider,
            @Value("${app.identity.service-url:http://localhost:8081}") String identityServiceUrl,
            @Value("${app.jwt.secret:dev-jwt-secret-key-for-local-testing-minimum-32-chars}") String jwtSecret) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenProvider = tokenProvider;
        this.identityServiceUrl = identityServiceUrl;
        this.jwtSecret = jwtSecret;
        this.restClient = RestClient.create();
    }

    @Override
    public List<ReviewerDto> getReviewers(String subject, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String cleanSubject = (subject != null) ? subject.trim() : "";

        // 1. Try REST call to identity-service
        try {
            String url = identityServiceUrl + "/api/v1/identity/reviewers";
            if (!cleanSubject.isBlank()) {
                url += "?subject=" + java.net.URLEncoder.encode(cleanSubject, StandardCharsets.UTF_8);
            }

            RestClient.RequestHeadersSpec<?> spec = restClient.get()
                    .uri(url)
                    .header("X-Tenant-Id", effectiveTenant)
                    .accept(MediaType.APPLICATION_JSON);
            attachAuthHeader(spec);

            ApiResponseDto<List<ReviewerDto>> apiResponse = spec
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponseDto<List<ReviewerDto>>>() {});

            if (apiResponse != null && apiResponse.getData() != null && !apiResponse.getData().isEmpty()) {
                log.info("Retrieved {} reviewers from identity-service via REST for subject='{}'",
                        apiResponse.getData().size(), cleanSubject);
                return apiResponse.getData();
            }
        } catch (Exception e) {
            log.warn("REST call to identity-service failed ({}), falling back to direct DB query: {}",
                    identityServiceUrl, e.getMessage());
        }

        // 2. Fallback to direct JDBC query
        if (jdbcTemplate == null) {
            return Collections.emptyList();
        }

        try {
            String sql = """
                SELECT u.id, u.username, u.specialization, u.account_status,
                       COALESCE(STRING_AGG(DISTINCT r.role, ','), '') AS roles_csv
                FROM identity_service.user_account u
                JOIN identity_service.user_role_assignment r ON r.user_id = u.id AND r.tenant_id = u.tenant_id
                WHERE (u.tenant_id = ? OR u.tenant_id = 'default')
                  AND u.account_status = 'ACTIVE'
                  AND r.role IN ('REVIEWER', 'SUBJECT_MATTER_EXPERT', 'EXAM_CONTROLLER', 'QUESTION_REVIEWER')
                GROUP BY u.id, u.username, u.specialization, u.account_status
                """;

            List<ReviewerDto> all = jdbcTemplate.query(sql, (rs, rowNum) -> {
                String rolesCsv = rs.getString("roles_csv");
                List<String> roles = (rolesCsv != null && !rolesCsv.isBlank())
                        ? Arrays.asList(rolesCsv.split(","))
                        : Collections.emptyList();

                return ReviewerDto.builder()
                        .id(rs.getObject("id", UUID.class))
                        .username(rs.getString("username"))
                        .specialization(rs.getString("specialization"))
                        .accountStatus(rs.getString("account_status"))
                        .roles(roles)
                        .build();
            }, effectiveTenant);

            if (all.isEmpty() || cleanSubject.isBlank()) {
                return all;
            }

            // Filter for subject specialists
            String target = cleanSubject.toLowerCase();
            List<ReviewerDto> specialists = all.stream()
                    .filter(r -> r.getSpecialization() != null && matchesSubject(r.getSpecialization(), target))
                    .toList();

            if (!specialists.isEmpty()) {
                return specialists;
            }

            return all;
        } catch (Exception e) {
            log.error("Error querying reviewers directly from identity_service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private boolean matchesSubject(String specialization, String subject) {
        if (specialization == null || subject == null) return false;
        String s1 = specialization.trim().toLowerCase();
        String s2 = subject.trim().toLowerCase();
        return s1.equals(s2) || s1.contains(s2) || s2.contains(s1);
    }

    private void attachAuthHeader(RestClient.RequestHeadersSpec<?> spec) {
        String authHeader = resolveAuthorizationHeader();
        if (authHeader != null && !authHeader.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, authHeader);
        }
    }

    private String resolveAuthorizationHeader() {
        // 1. Check incoming HTTP request
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletAttrs) {
            String authHeader = servletAttrs.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && !authHeader.isBlank()) {
                return authHeader;
            }
        }

        // 2. Check SecurityContextHolder
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            return "Bearer " + jwtAuth.getToken().getTokenValue();
        } else if (auth != null && auth.getCredentials() instanceof String cred && !cred.isBlank()) {
            return cred.startsWith("Bearer ") ? cred : "Bearer " + cred;
        }

        // 3. Service account token provider
        if (tokenProvider != null) {
            try {
                String token = tokenProvider.getServiceToken("question-bank-service");
                if (token != null && !token.isBlank()) {
                    return "Bearer " + token;
                }
            } catch (Exception ignored) {}
        }

        // 4. Generate signed dev JWT fallback
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
                "\"sub\":\"question-bank-service\"," +
                "\"preferred_username\":\"question-bank-service\"," +
                "\"name\":\"question-bank-service\"," +
                "\"iss\":\"exam-platform-dev\"," +
                "\"aud\":\"exam-backend\"," +
                "\"iat\":" + now + "," +
                "\"exp\":" + exp + "," +
                "\"realm_access\":{\"roles\":[\"SUPER_ADMIN\",\"EXAM_CONTROLLER\",\"QUESTION_AUTHOR\",\"REVIEWER\",\"SUBJECT_MATTER_EXPERT\"]}" +
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
            log.error("Failed to generate service JWT signature: {}", e.getMessage());
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
}
