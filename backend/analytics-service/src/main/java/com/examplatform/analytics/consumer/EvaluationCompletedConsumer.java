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

package com.examplatform.analytics.consumer;

import com.examplatform.analytics.service.AnalyticsService;
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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Ingests evaluation completed messages and updates analytics aggregations.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluationCompletedConsumer {

    public static final String EVALUATION_COMPLETED_TOPIC = "exam.evaluation.completed";

    private final AnalyticsService analyticsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = EVALUATION_COMPLETED_TOPIC,
            groupId = "analytics-service-evaluation-consumer",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onEvaluationCompleted(
            @Payload String payload,
            @Header(value = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        log.info("Ingesting analytics for evaluation completed: key={}", key);
        processEvaluationCompleted(payload, key);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "analytics.evaluation.events.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = EVALUATION_COMPLETED_TOPIC
            )
    )
    public void onRabbitEvaluationCompleted(Object message) {
        log.debug("Ingesting analytics via RabbitMQ: {}", message);
        MessagePayloadExtractor.handleRabbitEvent(message, objectMapper, EVALUATION_COMPLETED_TOPIC,
                payload -> processEvaluationCompleted(payload, null));
    }

    @EventListener
    public void onSpringEvaluationCompleted(GenericDomainEvent event) {
        log.debug("Ingesting analytics via Spring event: key={}", event.key());
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

            UUID examId = DataConversionUtils.parseUUID(event.get("examId"));
            UUID candidateId = DataConversionUtils.parseUUID(event.get("candidateId"));
            UUID sessionId = DataConversionUtils.parseUUID(event.get("sessionId"));

            if (examId == null || candidateId == null) {
                log.warn("Missing required examId or candidateId in event payload: {}", payload);
                return;
            }

            double totalRawScore = DataConversionUtils.toDouble(event.get("totalRawScore"));
            String tenantId = (String) event.getOrDefault("tenantId", "default");

            @SuppressWarnings("unchecked")
            Map<String, Object> sectionScores = (Map<String, Object>) event.getOrDefault("sectionScores", Map.of());

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionLevelScores = (List<Map<String, Object>>) event.getOrDefault("questionLevelScores", List.of());

            Instant evaluatedAt = DataConversionUtils.parseInstant(event.get("evaluatedAt"));

            analyticsService.processEvaluationCompleted(
                    examId,
                    candidateId,
                    sessionId,
                    totalRawScore,
                    sectionScores,
                    questionLevelScores,
                    tenantId,
                    evaluatedAt
            );

            log.info("Successfully ingested evaluation completed for candidate={}, exam={}", candidateId, examId);

        } catch (Exception e) {
            log.error("Failed to ingest EVALUATION_COMPLETED event: payload={}", payload, e);
        }
    }
}
