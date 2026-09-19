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

package com.examplatform.evaluation.consumer;

import com.examplatform.evaluation.client.AnswerKeyClient;
import com.examplatform.evaluation.client.CandidateResponseClient;
import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.service.AutoEvaluationService;
import com.examplatform.evaluation.service.ScoreAggregationService;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consumer that listens for session-submitted events on the
 * {@code exam.session.events} channel. Triggers auto-evaluation workflow
 * when a candidate submits their exam session.
 * Supports Kafka, RabbitMQ, and in-memory Spring ApplicationEvents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventConsumer {

    public static final String SESSION_EVENTS_TOPIC = "exam.session.events";
    public static final String DLQ_TOPIC = "exam.evaluation.dlq";

    private final AutoEvaluationService autoEvaluationService;
    private final ScoreAggregationService scoreAggregationService;
    private final AnswerKeyClient answerKeyClient;
    private final CandidateResponseClient candidateResponseClient;
    private final EventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /**
     * Handles session events from Kafka.
     */
    @KafkaListener(topics = SESSION_EVENTS_TOPIC, groupId = "evaluation-service")
    public void onKafkaSessionEvent(ConsumerRecord<String, String> record) {
        log.info("Received Kafka session event: key={}, partition={}", record.key(), record.partition());
        try {
            JsonNode event = objectMapper.readTree(record.value());
            processJsonEvent(event);
        } catch (Exception e) {
            log.error("Failed to process Kafka session event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles session events from RabbitMQ.
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "evaluation.session.events.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = SESSION_EVENTS_TOPIC
            )
    )
    public void onRabbitSessionEvent(Object message) {
        log.info("Received RabbitMQ session event: {}", message);
        try {
            JsonNode event;
            if (message instanceof Message amqpMsg) {
                String s = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
                event = objectMapper.readTree(s);
            } else if (message instanceof byte[] bytes) {
                String s = new String(bytes, StandardCharsets.UTF_8);
                event = objectMapper.readTree(s);
            } else if (message instanceof String s) {
                event = objectMapper.readTree(s);
            } else if (message instanceof JsonNode jn) {
                event = jn;
            } else {
                event = objectMapper.valueToTree(message);
            }
            processJsonEvent(event);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ session event: {}", e.getMessage(), e);
        }
    }

    /**
     * Handles session events from in-memory Spring ApplicationEvents (monolith mode).
     */
    @EventListener
    public void onSpringSessionEvent(GenericDomainEvent event) {
        if (!SESSION_EVENTS_TOPIC.equals(event.topic())) {
            return;
        }
        log.info("Received Spring in-memory session event: key={}", event.key());
        try {
            Object payload = event.payload();
            JsonNode jsonNode;
            if (payload instanceof Message amqpMsg) {
                String s = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
                jsonNode = objectMapper.readTree(s);
            } else if (payload instanceof byte[] bytes) {
                String s = new String(bytes, StandardCharsets.UTF_8);
                jsonNode = objectMapper.readTree(s);
            } else if (payload instanceof String s) {
                jsonNode = objectMapper.readTree(s);
            } else if (payload instanceof JsonNode jn) {
                jsonNode = jn;
            } else {
                jsonNode = objectMapper.valueToTree(payload);
            }
            processJsonEvent(jsonNode);
        } catch (Exception e) {
            log.error("Failed to process Spring in-memory session event: {}", e.getMessage(), e);
        }
    }

    public void processJsonEvent(JsonNode event) {
        UUID sessionId = null;
        UUID candidateId = null;
        String tenantId = "default";
        UUID paperId = null;
        UUID examId = null;

        try {
            String eventType = event.has("eventType") ? event.get("eventType").asText() : "";

            if (!"SESSION_SUBMITTED".equals(eventType)) {
                log.debug("Ignoring non-submission event: {}", eventType);
                return;
            }

            sessionId = UUID.fromString(event.get("sessionId").asText());
            candidateId = UUID.fromString(event.get("candidateId").asText());
            tenantId = event.has("tenantId") && !event.get("tenantId").asText().isBlank()
                    ? event.get("tenantId").asText() : "default";

            if (event.has("paperId") && !event.get("paperId").asText().isBlank()) {
                try {
                    paperId = UUID.fromString(event.get("paperId").asText());
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (event.has("examId") && !event.get("examId").asText().isBlank()) {
                try {
                    examId = UUID.fromString(event.get("examId").asText());
                } catch (IllegalArgumentException ignored) {
                }
            }

            // Fetch final candidate responses (from event or response-service)
            List<CandidateResponse> responses = fetchCandidateResponses(event, sessionId, tenantId);

            // Fetch answer keys (from event, paperId gRPC, or batch question IDs gRPC)
            List<AnswerKey> answerKeys = fetchAnswerKeys(event, paperId, responses, tenantId);

            if (answerKeys == null || answerKeys.isEmpty()) {
                log.warn("No answer keys found for session {}. Emitting DLQ event and skipping evaluation.", sessionId);
                emitDeadLetterEvent(sessionId, candidateId, tenantId, paperId, examId, "No answer keys found for session", event);
                return;
            }

            List<Evaluation> evaluations = autoEvaluationService.evaluateSession(
                    sessionId, candidateId, answerKeys, responses, tenantId);

            log.info("Auto-evaluation completed for session {}: {} evaluations created",
                    sessionId, evaluations.size());

            scoreAggregationService.aggregateScores(sessionId, candidateId, examId, tenantId);
            log.info("Score aggregation and EVALUATION_COMPLETED event published for session {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to process session event: {}", e.getMessage(), e);
            if (sessionId != null && candidateId != null) {
                emitDeadLetterEvent(sessionId, candidateId, tenantId, paperId, examId, e.getMessage(), event);
            }
        }
    }

    /**
     * Fetches candidate responses from the event payload or by querying response-service via REST.
     */
    private List<CandidateResponse> fetchCandidateResponses(JsonNode event, UUID sessionId, String tenantId) {
        if (event.has("responses") && event.get("responses").isArray() && !event.get("responses").isEmpty()) {
            try {
                return objectMapper.readValue(
                        event.get("responses").toString(),
                        new TypeReference<List<CandidateResponse>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse candidate responses from event payload: {}", e.getMessage());
            }
        }

        return candidateResponseClient.getCandidateResponses(sessionId, tenantId);
    }

    /**
     * Fetches answer keys from event payload, paperId gRPC call, or batch question IDs gRPC call.
     */
    private List<AnswerKey> fetchAnswerKeys(JsonNode event, UUID paperId, List<CandidateResponse> responses, String tenantId) {
        if (event.has("answerKeys") && event.get("answerKeys").isArray() && !event.get("answerKeys").isEmpty()) {
            try {
                return objectMapper.readValue(
                        event.get("answerKeys").toString(),
                        new TypeReference<List<AnswerKey>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse answer keys from event payload: {}", e.getMessage());
            }
        }

        if (paperId != null) {
            return answerKeyClient.getAnswerKeysForPaper(paperId, tenantId);
        }

        if (responses != null && !responses.isEmpty()) {
            List<UUID> questionIds = responses.stream()
                    .map(CandidateResponse::getQuestionId)
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (!questionIds.isEmpty()) {
                return answerKeyClient.batchGetAnswerKeys(questionIds, tenantId);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Emits a dead-letter event to {@code exam.evaluation.dlq} for retry/reprocessing when upstream fails.
     */
    private void emitDeadLetterEvent(UUID sessionId, UUID candidateId, String tenantId,
                                    UUID paperId, UUID examId, String reason, JsonNode originalEvent) {
        try {
            Map<String, Object> dlqEvent = new HashMap<>();
            dlqEvent.put("eventType", "EVALUATION_FAILED_DLQ");
            dlqEvent.put("sessionId", sessionId.toString());
            dlqEvent.put("candidateId", candidateId.toString());
            dlqEvent.put("tenantId", tenantId != null ? tenantId : "default");
            dlqEvent.put("paperId", paperId != null ? paperId.toString() : "");
            dlqEvent.put("examId", examId != null ? examId.toString() : "");
            dlqEvent.put("reason", reason != null ? reason : "Unknown evaluation failure");
            dlqEvent.put("failedAt", Instant.now().toString());
            dlqEvent.put("originalEvent", originalEvent != null ? originalEvent.toString() : "");

            eventPublisher.publish(DLQ_TOPIC, sessionId.toString(), dlqEvent);
            log.info("Published DLQ event to {} for session {}: reason={}", DLQ_TOPIC, sessionId, reason);
        } catch (Exception e) {
            log.error("Failed to publish DLQ event to {} for session {}: {}", DLQ_TOPIC, sessionId, e.getMessage());
        }
    }
}
