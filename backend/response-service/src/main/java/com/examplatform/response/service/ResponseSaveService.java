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

import com.examplatform.response.config.MetricsConfig;
import com.examplatform.response.domain.Response;
import com.examplatform.response.dto.SaveResponseRequest;
import com.examplatform.response.dto.SaveResponseResponse;
import com.examplatform.response.repository.ResponseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Service responsible for persisting candidate responses and publishing save events.
 * Enforces Kafka acks=all before acknowledging to the client (synchronous send).
 *
 * Validates: Requirements 10.1, 20.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResponseSaveService {

    private static final String TOPIC_RESPONSE_SAVED = "exam.response.saved";
    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String AUDIT_THROTTLE_PREFIX = "audit:response:";
    private static final Duration AUDIT_THROTTLE_TTL = Duration.ofSeconds(60);

    private final ResponseRepository responseRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MetricsConfig metricsConfig;

    /**
     * Saves a candidate response, publishes a Kafka event with acks=all, and returns the result.
     *
     * @param sessionId   the exam session ID
     * @param request     the save request payload
     * @param candidateId the candidate's user ID (from JWT sub)
     * @param tenantId    the tenant identifier (from X-Tenant-Id header)
     * @return the save confirmation response
     */
    public SaveResponseResponse saveResponse(UUID sessionId, SaveResponseRequest request,
                                              UUID candidateId, String tenantId) {
        // 1. Determine revision sequence
        List<Response> previousResponses = responseRepository
                .findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(sessionId, request.getQuestionId());

        int newRevision = previousResponses.isEmpty() ? 1 : previousResponses.get(0).getRevisionSequence() + 1;

        // 2. Build Response entity
        Response response = Response.builder()
                .sessionId(sessionId)
                .questionId(request.getQuestionId())
                .candidateId(candidateId)
                .selectedOptionIds(request.getSelectedOptionIds())
                .enteredValue(request.getEnteredValue())
                .timestamp(request.getTimestamp())
                .cumulativeTimeSpentMs(request.getCumulativeTimeSpentMs())
                .revisionSequence(newRevision)
                .saveSource(request.getSaveSource())
                .isFinal(false)
                .build();
        response.setTenantId(tenantId);

        // 3. Save to DB
        Response saved = responseRepository.save(response);

        // 4. Publish save confirmation to Kafka with acks=all (blocking)
        Map<String, Object> savedEvent = Map.of(
                "responseId", saved.getId(),
                "sessionId", sessionId,
                "questionId", request.getQuestionId(),
                "candidateId", candidateId,
                "revisionSequence", newRevision,
                "saveSource", request.getSaveSource(),
                "savedAt", Instant.now().toString(),
                "tenantId", tenantId
        );

        try {
            kafkaTemplate.send(TOPIC_RESPONSE_SAVED, sessionId.toString(), savedEvent)
                    .get(); // BLOCKING — ensures acks=all before returning to client
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Kafka send interrupted", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Kafka send failed", e);
        }

        // 5. Increment the response_save_rate counter
        metricsConfig.getResponseSaveCounter().increment();

        // 6. Publish sampled audit event (max once per candidate per 60 seconds)
        publishSampledAuditEvent(sessionId, candidateId, request.getQuestionId(), tenantId);

        // 7. Return SaveResponseResponse
        log.info("Saved response: sessionId={}, questionId={}, revision={}, saveSource={}",
                sessionId, request.getQuestionId(), newRevision, request.getSaveSource());

        return SaveResponseResponse.builder()
                .responseId(saved.getId())
                .sessionId(sessionId)
                .questionId(request.getQuestionId())
                .revisionSequence(newRevision)
                .saveSource(request.getSaveSource())
                .savedAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Publishes a sampled RESPONSE_SAVED audit event to Kafka, throttled to max once
     * per candidate per 60 seconds using a Redis key with TTL.
     */
    private void publishSampledAuditEvent(UUID sessionId, UUID candidateId, UUID questionId, String tenantId) {
        try {
            String throttleKey = AUDIT_THROTTLE_PREFIX + candidateId;
            Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(throttleKey, "1", AUDIT_THROTTLE_TTL);

            if (Boolean.TRUE.equals(wasAbsent)) {
                // No audit event published for this candidate in last 60s — publish now
                Map<String, Object> auditEvent = Map.of(
                        "eventType", "RESPONSE_SAVED",
                        "sessionId", sessionId.toString(),
                        "candidateId", candidateId.toString(),
                        "questionId", questionId.toString(),
                        "tenantId", tenantId,
                        "occurredAt", Instant.now().toString()
                );
                kafkaTemplate.send(AUDIT_TOPIC, candidateId.toString(), auditEvent)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish sampled RESPONSE_SAVED audit event for candidate [{}]: {}",
                                        candidateId, ex.getMessage());
                            }
                        });
            }
        } catch (Exception e) {
            // Never block main save operation for audit failures
            log.error("Error publishing sampled audit event: {}", e.getMessage());
        }
    }
}
