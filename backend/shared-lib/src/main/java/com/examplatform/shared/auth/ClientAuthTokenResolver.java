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

package com.examplatform.shared.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Shared utility for inter-service REST clients to resolve authorization headers.
 * Implements standard 4-step token resolution hierarchy:
 * 1. Propagated Authorization header from incoming HTTP request.
 * 2. SecurityContextHolder authentication (JWT or credentials).
 * 3. Service account token via ServiceAccountTokenProvider.
 * 4. Signed dev JWT fallback when a jwtSecret is configured.
 */
public final class ClientAuthTokenResolver {

    public static final List<String> DEFAULT_ROLES = List.of("SUPER_ADMIN", "EXAM_CONTROLLER");

    private ClientAuthTokenResolver() {
    }

    /**
     * Attaches Authorization header to a RestClient request spec using default roles.
     */
    public static void attachAuthHeader(RestClient.RequestHeadersSpec<?> spec, String serviceName,
                                        ServiceAccountTokenProvider tokenProvider, String jwtSecret) {
        attachAuthHeader(spec, serviceName, tokenProvider, jwtSecret, DEFAULT_ROLES);
    }

    /**
     * Attaches Authorization header to a RestClient request spec with custom roles for dev JWT fallback.
     */
    public static void attachAuthHeader(RestClient.RequestHeadersSpec<?> spec, String serviceName,
                                        ServiceAccountTokenProvider tokenProvider, String jwtSecret, List<String> roles) {
        String token = resolveAuthToken(serviceName, tokenProvider, jwtSecret, roles);
        if (token != null && !token.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, token);
        }
    }

    /**
     * Resolves the bearer or authorization header token string using default roles.
     */
    public static String resolveAuthToken(String serviceName, ServiceAccountTokenProvider tokenProvider, String jwtSecret) {
        return resolveAuthToken(serviceName, tokenProvider, jwtSecret, DEFAULT_ROLES);
    }

    /**
     * Resolves the bearer or authorization header token string with custom roles for dev JWT fallback.
     */
    public static String resolveAuthToken(String serviceName, ServiceAccountTokenProvider tokenProvider,
                                          String jwtSecret, List<String> roles) {
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
        if (tokenProvider != null && serviceName != null) {
            try {
                String token = tokenProvider.getServiceToken(serviceName);
                if (token != null && !token.isBlank()) {
                    return "Bearer " + token;
                }
            } catch (Exception ignored) {
            }
        }

        // 4. Generate signed dev JWT fallback
        if (jwtSecret != null && !jwtSecret.isBlank() && serviceName != null) {
            return "Bearer " + generateDevToken(serviceName, jwtSecret, roles != null ? roles : DEFAULT_ROLES);
        }

        return null;
    }

    /**
     * Generates an HS256-signed JWT token for local dev and testing fallback.
     */
    public static String generateDevToken(String serviceName, String jwtSecret, List<String> roles) {
        long now = System.currentTimeMillis() / 1000;
        long exp = now + 3600;
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

        StringBuilder rolesJson = new StringBuilder("[");
        if (roles != null && !roles.isEmpty()) {
            for (int i = 0; i < roles.size(); i++) {
                rolesJson.append("\"").append(roles.get(i)).append("\"");
                if (i < roles.size() - 1) {
                    rolesJson.append(",");
                }
            }
        }
        rolesJson.append("]");

        String payload = base64Url("{" +
                "\"sub\":\"" + serviceName + "\"," +
                "\"preferred_username\":\"" + serviceName + "\"," +
                "\"name\":\"" + serviceName + "\"," +
                "\"iss\":\"exam-platform-dev\"," +
                "\"aud\":\"exam-backend\"," +
                "\"iat\":" + now + "," +
                "\"exp\":" + exp + "," +
                "\"realm_access\":{\"roles\":" + rolesJson + "}" +
                "}");
        String signingInput = header + "." + payload;
        String signature = hmacSha256(signingInput, jwtSecret);
        return signingInput + "." + signature;
    }

    private static String base64Url(String input) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(input.getBytes(StandardCharsets.UTF_8));
    }

    private static String hmacSha256(String data, String jwtSecret) {
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
}
