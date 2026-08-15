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

import com.examplatform.response.domain.Response;
import com.examplatform.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Handles session finalization (submission): marks all responses as final
 * and locks the response set against further modifications.
 *
 * Validates: Requirements 10.6
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SessionFinalizationService {

    private static final String TOPIC_SESSION_EVENTS = "exam.session.events";

    private final ResponseRepository responseRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Finalizes all responses for the given session: sets isFinal=true,
     * preventing further saves, and publishes a SESSION_SUBMITTED event.
     *
     * @param sessionId   the exam session UUID
     * @param candidateId the authenticated candidate's ID
     * @param tenantId    the tenant identifier
     */
    public void submitSession(UUID sessionId, UUID candidateId, String tenantId) {
        List<Response> responses = responseRepository.findBySessionIdAndTenantId(sessionId, tenantId);

        if (responses.isEmpty()) {
            log.warn("No responses found for session={} in tenant={}", sessionId, tenantId);
        }

        // Mark all responses as final — locks the response set
        for (Response response : responses) {
            response.setFinal(true);
        }
        responseRepository.saveAll(responses);

        log.info("Session finalized: sessionId={}, candidateId={}, responseCount={}",
                sessionId, candidateId, responses.size());

        // Publish SESSION_SUBMITTED event to Kafka (fire-and-forget)
        try {
            Map<String, Object> event = Map.of(
                    "eventType", "SESSION_SUBMITTED",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "responseCount", responses.size(),
                    "submittedAt", Instant.now().toString(),
                    "tenantId", tenantId
            );
            kafkaTemplate.send(TOPIC_SESSION_EVENTS, sessionId.toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish SESSION_SUBMITTED event for session [{}]: {}",
                                    sessionId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing SESSION_SUBMITTED event: {}", e.getMessage());
        }
    }
}
