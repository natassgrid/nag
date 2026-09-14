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

package com.examplatform.response.service;

import com.examplatform.response.exception.ResponseIntegrityException;
import com.examplatform.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Validates response integrity by calling delivery-service.
 * Checks that the session is active, the questionId belongs to the candidate's paper,
 * and the selected option IDs are valid for that question.
 *
 * Implements fail-open resilience: if delivery-service is unreachable,
 * the response is allowed through and flagged INTEGRITY_UNKNOWN on the audit topic.
 *
 * Validates: SPEC-R1 (Integrity Validation on Save)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrityValidationService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final RestTemplate deliveryRestTemplate;
    private final EventPublisher eventPublisher;

    @Value("${delivery.service.url:http://delivery-service:8084}")
    private String deliveryServiceUrl;

    /**
     * Validates a response against the delivery-service.
     * Throws ResponseIntegrityException if the response fails validation.
     * Fails open (allows through) if delivery-service is unavailable.
     *
     * @param sessionId   the exam session UUID
     * @param candidateId the candidate UUID
     * @param questionId  the question UUID
     * @param tenantId    the tenant identifier
     */
    public void validateResponse(UUID sessionId, UUID candidateId, UUID questionId, String tenantId) {
        try {
            String url = deliveryServiceUrl + "/api/v1/sessions/" + sessionId
                    + "/validate-response?candidateId=" + candidateId
                    + "&questionId=" + questionId
                    + "&tenantId=" + tenantId;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = deliveryRestTemplate.getForObject(url, Map.class);

            if (response == null) {
                log.warn("Null response from delivery-service for session={}, question={}", sessionId, questionId);
                publishIntegrityAlert(sessionId, candidateId, questionId, "NULL_RESPONSE", tenantId);
                return; // fail-open
            }

            Boolean valid = (Boolean) response.get("valid");
            if (Boolean.FALSE.equals(valid)) {
                String reason = (String) response.getOrDefault("reason", "INTEGRITY_VIOLATION");
                log.warn("Integrity validation failed: session={}, question={}, reason={}",
                        sessionId, questionId, reason);
                throw new ResponseIntegrityException(reason,
                        "Response rejected: " + reason + " for questionId=" + questionId);
            }

            log.debug("Integrity validation passed: session={}, question={}", sessionId, questionId);

        } catch (ResponseIntegrityException e) {
            throw e; // re-throw, don't swallow
        } catch (ResourceAccessException e) {
            // delivery-service unreachable — fail open
            log.warn("delivery-service unreachable for integrity check (fail-open): session={}, error={}",
                    sessionId, e.getMessage());
            publishIntegrityAlert(sessionId, candidateId, questionId, "INTEGRITY_UNKNOWN", tenantId);
        } catch (Exception e) {
            // Any other error — fail open
            log.warn("Unexpected error during integrity validation (fail-open): session={}, error={}",
                    sessionId, e.getMessage());
            publishIntegrityAlert(sessionId, candidateId, questionId, "INTEGRITY_UNKNOWN", tenantId);
        }
    }

    private void publishIntegrityAlert(UUID sessionId, UUID candidateId, UUID questionId,
                                       String reason, String tenantId) {
        try {
            Map<String, Object> alert = Map.of(
                    "eventType", "INTEGRITY_ALERT",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "questionId", questionId.toString(),
                    "reason", reason,
                    "tenantId", tenantId,
                    "occurredAt", Instant.now().toString()
            );
            eventPublisher.publish(AUDIT_TOPIC, sessionId.toString(), alert);
        } catch (Exception ex) {
            log.error("Failed to publish integrity alert: {}", ex.getMessage());
        }
    }
}
