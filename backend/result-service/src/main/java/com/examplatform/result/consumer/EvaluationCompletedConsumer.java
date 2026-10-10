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
import com.examplatform.result.service.QuestionAnalyticsService;
import com.examplatform.result.service.ResultComputationService;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.examplatform.shared.messaging.MessagePayloadExtractor;
import com.examplatform.shared.util.DataConversionUtils;
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
 * Computes and persists diagnostic scorecards and invalidates question analytics cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationCompletedConsumer {

    public static final String EVALUATION_COMPLETED_TOPIC = "exam.evaluation.completed";

    private final ResultComputationService resultComputationService;
    private final ResultRepository resultRepository;
    private final QuestionAnalyticsService questionAnalyticsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = EVALUATION_COMPLETED_TOPIC,
            groupId = "result-service-evaluation-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEvaluationCompleted(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        log.info("Computing result scorecard for evaluation completed: key={}", key);
        processEvaluationCompleted(payload, key);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "result.evaluation.events.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = EVALUATION_COMPLETED_TOPIC
            )
    )
    public void onRabbitEvaluationCompleted(Object message) {
        log.debug("Computing result scorecard via RabbitMQ: {}", message);
        MessagePayloadExtractor.handleRabbitEvent(message, objectMapper, EVALUATION_COMPLETED_TOPIC,
                payload -> processEvaluationCompleted(payload, null));
    }

    @EventListener
    public void onSpringEvaluationCompleted(GenericDomainEvent event) {
        log.debug("Computing result scorecard via Spring event: key={}", event.key());
        MessagePayloadExtractor.handleSpringEvent(event, objectMapper, EVALUATION_COMPLETED_TOPIC,
                this::processEvaluationCompleted);
    }

    public void processEvaluationCompleted(String payload, String key) {
        try {
            var eventOpt = MessagePayloadExtractor.parseEventIfMatching(payload, objectMapper, "EVALUATION_COMPLETED");
            if (eventOpt.isEmpty()) {
                log.debug("Ignoring non-matching or invalid payload: {}", payload);
                return;
            }
            Map<String, Object> event = eventOpt.get();

            UUID candidateId = UUID.fromString((String) event.get("candidateId"));
            UUID examId      = DataConversionUtils.parseUUID(event.get("examId"));
            String tenantId  = (String) event.getOrDefault("tenantId", "default");
            double totalRawScore = DataConversionUtils.toDouble(event.get("totalRawScore"));

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
            rawSectionScores.forEach((k, v) -> sectionScores.put(k, DataConversionUtils.toDouble(v)));

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

                // Invalidate analytics cache for this exam
                questionAnalyticsService.invalidateCache(examId);

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
                    .filter(q -> DataConversionUtils.toDouble(q.get("score")) > 0)
                    .count();

            BigDecimal accuracyRate = BigDecimal.valueOf(correct * 100.0 / totalAttempted)
                    .setScale(2, RoundingMode.HALF_UP);

            for (Result r : results) {
                r.setAccuracyRate(accuracyRate);
                r.setTenantId(tenantId);
            }
            resultRepository.saveAll(results);

        } catch (Exception e) {
            log.warn("Failed to enrich results with diagnostic data: {}", e.getMessage());
        }
    }
}
