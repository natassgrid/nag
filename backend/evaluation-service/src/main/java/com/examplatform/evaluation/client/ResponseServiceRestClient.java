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

package com.examplatform.evaluation.client;

import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.exception.UpstreamServiceUnavailableException;
import com.examplatform.shared.auth.ServiceAccountTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * REST client implementation of {@link CandidateResponseClient} communicating with response-service.
 * Retrieves candidate responses with Resilience4j circuit breaker protection.
 */
@Slf4j
@Component
public class ResponseServiceRestClient implements CandidateResponseClient {

    private final RestClient restClient;
    private final String responseServiceUrl;
    private final ServiceAccountTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public ResponseServiceRestClient(
            @Value("${app.response-service.url:http://localhost:8088}") String responseServiceUrl,
            ObjectMapper objectMapper) {
        this(null, responseServiceUrl, objectMapper);
    }

    @Autowired
    public ResponseServiceRestClient(
            @Autowired(required = false) ServiceAccountTokenProvider tokenProvider,
            @Value("${app.response-service.url:http://localhost:8088}") String responseServiceUrl,
            ObjectMapper objectMapper) {
        this.tokenProvider = tokenProvider;
        this.responseServiceUrl = responseServiceUrl;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().baseUrl(responseServiceUrl).build();
    }

    @Override
    @CircuitBreaker(name = "responseService", fallbackMethod = "fetchResponsesFallback")
    public List<CandidateResponse> getCandidateResponses(UUID sessionId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        log.info("Fetching candidate responses via REST for sessionId={}, tenantId={} from {}",
                sessionId, effectiveTenant, responseServiceUrl);

        try {
            var request = restClient.get()
                    .uri("/api/v1/responses/{sessionId}/responses", sessionId)
                    .header("X-Tenant-Id", effectiveTenant)
                    .accept(MediaType.APPLICATION_JSON);

            if (tokenProvider != null) {
                try {
                    String token = tokenProvider.getServiceToken("evaluation-service");
                    if (token != null && !token.isBlank()) {
                        request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    }
                } catch (Exception e) {
                    log.debug("Could not obtain service account token for evaluation-service: {}", e.getMessage());
                }
            }

            String body = request.retrieve().body(String.class);
            if (body == null || body.isBlank()) {
                log.warn("Empty response body received from response-service for session {}", sessionId);
                return Collections.emptyList();
            }

            return parseResponsesFromBody(body);
        } catch (Exception e) {
            log.error("Failed to retrieve candidate responses for session {} from response-service: {}",
                    sessionId, e.getMessage());
            throw new UpstreamServiceUnavailableException(
                    "Failed to retrieve candidate responses for session " + sessionId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when response-service is unavailable or circuit is open.
     */
    public List<CandidateResponse> fetchResponsesFallback(UUID sessionId, String tenantId, Throwable ex) {
        log.warn("Circuit breaker open / fallback triggered for getCandidateResponses (sessionId={}): {}",
                sessionId, ex.getMessage());
        throw new UpstreamServiceUnavailableException(
                "Response service unavailable for session " + sessionId + ": " + ex.getMessage(), ex);
    }

    private List<CandidateResponse> parseResponsesFromBody(String body) {
        List<CandidateResponse> result = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode dataNode = root.has("data") ? root.get("data") : root;

            if (dataNode.isArray()) {
                for (JsonNode item : dataNode) {
                    if (item.has("questionId")) {
                        UUID qId = UUID.fromString(item.get("questionId").asText());
                        String selectedOptions = item.has("selectedOptionIds") && !item.get("selectedOptionIds").isNull()
                                ? item.get("selectedOptionIds").asText() : null;
                        String enteredValue = item.has("enteredValue") && !item.get("enteredValue").isNull()
                                ? item.get("enteredValue").asText() : null;

                        boolean attempted = (selectedOptions != null && !selectedOptions.isBlank() && !"[]".equals(selectedOptions.trim()))
                                || (enteredValue != null && !enteredValue.isBlank());

                        result.add(CandidateResponse.builder()
                                .questionId(qId)
                                .selectedOptionIds(selectedOptions)
                                .enteredValue(enteredValue)
                                .attempted(attempted)
                                .build());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse candidate responses from response-service JSON payload: {}", e.getMessage());
            throw new UpstreamServiceUnavailableException("Failed to parse candidate responses: " + e.getMessage(), e);
        }
        return result;
    }
}
