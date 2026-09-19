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

package com.examplatform.result.consumer;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateScoreInput;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.result.service.ResultComputationService;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumer for the exam.evaluation.completed topic.
 * Triggered after evaluation-service completes scoring for a candidate's session.
 * Computes and persists the full diagnostic result record.
 * Supports Kafka, RabbitMQ, and in-memory Spring events across deployment modes.
 *
 * Validates: SPEC-RS3 (EVALUATION_COMPLETED Consumer)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationCompletedConsumer {

    public static final String EVALUATION_COMPLETED_TOPIC = "exam.evaluation.completed";

    private final ResultComputationService resultComputationService;
    private final ResultRepository resultRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes EVALUATION_COMPLETED events via Kafka.
     *
     * @param payload  the event payload as JSON string
     * @param key      the Kafka message key (sessionId)
     */
    @KafkaListener(
            topics = EVALUATION_COMPLETED_TOPIC,
            groupId = "result-service-evaluation-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEvaluationCompleted(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        log.info("Received Kafka EVALUATION_COMPLETED event: key={}", key);
        processEvaluationCompleted(payload, key);
    }

    /**
     * Consumes EVALUATION_COMPLETED events via RabbitMQ.
     *
     * @param message the event payload (String or Object map)
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "result.evaluation.events.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = EVALUATION_COMPLETED_TOPIC
            )
    )
    public void onRabbitEvaluationCompleted(Object message) {
        log.info("Received RabbitMQ EVALUATION_COMPLETED event: {}", message);
        try {
            String payload;
            if (message instanceof String s) {
                payload = s;
            } else {
                payload = objectMapper.writeValueAsString(message);
            }
            processEvaluationCompleted(payload, null);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ EVALUATION_COMPLETED event: {}", e.getMessage(), e);
        }
    }

    /**
     * Consumes EVALUATION_COMPLETED events via in-memory Spring events (monolith mode).
     *
     * @param event the in-memory generic domain event
     */
    @EventListener
    public void onSpringEvaluationCompleted(GenericDomainEvent event) {
        if (!EVALUATION_COMPLETED_TOPIC.equals(event.topic())) {
            return;
        }
        log.info("Received Spring in-memory EVALUATION_COMPLETED event: key={}", event.key());
        try {
            Object rawPayload = event.payload();
            String payload;
            if (rawPayload instanceof String s) {
                payload = s;
            } else {
                payload = objectMapper.writeValueAsString(rawPayload);
            }
            processEvaluationCompleted(payload, event.key());
        } catch (Exception e) {
            log.error("Failed to process Spring in-memory EVALUATION_COMPLETED event: {}", e.getMessage(), e);
        }
    }

    /**
     * Core processing logic for EVALUATION_COMPLETED events.
     * Idempotent: if a result already exists for the candidate+exam, it is skipped.
     *
     * @param payload the JSON string payload
     * @param key     the event key (optional)
     */
    public void processEvaluationCompleted(String payload, String key) {
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
            String eventType = (String) event.get("eventType");

            if (!"EVALUATION_COMPLETED".equals(eventType)) {
                log.debug("Ignoring non-EVALUATION_COMPLETED event: {}", eventType);
                return;
            }

            UUID candidateId = UUID.fromString((String) event.get("candidateId"));
            UUID examId      = parseUUID(event.get("examId"));
            String tenantId  = (String) event.getOrDefault("tenantId", "default");
            double totalRawScore = toDouble(event.get("totalRawScore"));

            // Idempotency: skip if result already exists
            if (examId != null) {
                Optional<Result> existing = resultRepository
                        .findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId);
                if (existing.isPresent()) {
                    log.info("Result already exists for candidate={}, exam={} — skipping", candidateId, examId);
                    return;
                }
            }

            // Extract section scores
            @SuppressWarnings("unchecked")
            Map<String, Object> rawSectionScores = (Map<String, Object>) event.getOrDefault("sectionScores", Map.of());
            Map<String, Double> sectionScores = new HashMap<>();
            rawSectionScores.forEach((k, v) -> sectionScores.put(k, toDouble(v)));

            // Build score input
            CandidateScoreInput scoreInput = CandidateScoreInput.builder()
                    .candidateId(candidateId)
                    .totalRawScore(totalRawScore)
                    .sectionScores(sectionScores)
                    .shiftMean(0)
                    .shiftStdDev(0)
                    .build();

            // Compute and save result (single-candidate batch)
            if (examId != null) {
                List<Result> results = resultComputationService.computeResults(
                        examId, List.of(scoreInput), false, tenantId);

                // Enrich with diagnostic data from event
                enrichWithDiagnostics(results, event, tenantId);

                log.info("Result computed for candidate={}, exam={}, score={}",
                        candidateId, examId, totalRawScore);
            }

        } catch (Exception e) {
            log.error("Failed to process EVALUATION_COMPLETED event key={}: {}", key, e.getMessage(), e);
            // Do not rethrow — prevents poison-pill message from blocking the consumer
        }
    }

    /**
     * Enriches saved results with diagnostic data from the evaluation event.
     * Computes accuracy rate and time analysis from question-level scores.
     */
    private void enrichWithDiagnostics(List<Result> results, Map<String, Object> event, String tenantId) {
        if (results.isEmpty()) return;

        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionLevelScores =
                    (List<Map<String, Object>>) event.getOrDefault("questionLevelScores", List.of());

            if (questionLevelScores.isEmpty()) return;

            // Compute accuracy: questions with score > 0 / total attempted
            long totalAttempted = questionLevelScores.size();
            long correct = questionLevelScores.stream()
                    .filter(q -> toDouble(q.get("score")) > 0)
                    .count();

            BigDecimal accuracyRate = totalAttempted > 0
                    ? BigDecimal.valueOf((double) correct / totalAttempted * 100).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // Compute time analysis (if timeSpentMs present)
            long totalTimeMs = questionLevelScores.stream()
                    .mapToLong(q -> toLong(q.get("timeSpentMs")))
                    .sum();
            long avgTimeMs = totalAttempted > 0 ? totalTimeMs / totalAttempted : 0;

            long timeOnCorrect = questionLevelScores.stream()
                    .filter(q -> toDouble(q.get("score")) > 0)
                    .mapToLong(q -> toLong(q.get("timeSpentMs")))
                    .sum();
            long timeOnIncorrect = totalTimeMs - timeOnCorrect;

            Map<String, Object> timeAnalysis = Map.of(
                    "avgTimePerQuestionMs", avgTimeMs,
                    "timeOnCorrectMs", timeOnCorrect,
                    "timeOnIncorrectMs", timeOnIncorrect,
                    "totalQuestions", totalAttempted
            );

            String timeAnalysisJson = objectMapper.writeValueAsString(timeAnalysis);

            // Update the result records
            for (Result result : results) {
                result.setAccuracyRate(accuracyRate);
                result.setTimeAnalysisJson(timeAnalysisJson);
            }

            resultRepository.saveAll(results);
            log.debug("Enriched {} result(s) with diagnostic data", results.size());

        } catch (Exception e) {
            log.warn("Failed to enrich result with diagnostics: {}", e.getMessage());
            // Non-fatal — result is still saved without diagnostics
        }
    }

    private UUID parseUUID(Object value) {
        if (value == null) return null;
        try {
            return UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(value.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number n) return n.longValue();
        try { return Long.parseLong(value.toString()); } catch (NumberFormatException e) { return 0L; }
    }
}
