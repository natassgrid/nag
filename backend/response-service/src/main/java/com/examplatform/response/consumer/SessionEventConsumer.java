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

package com.examplatform.response.consumer;

import com.examplatform.response.service.AutoSaveService;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;
import java.util.UUID;

/**
 * Consumer for session events.
 * Triggers auto-save on NAVIGATION events to ensure responses are persisted
 * when a candidate navigates between questions.
 * Supports Kafka, RabbitMQ, and in-memory Spring ApplicationEvents.
 *
 * Validates: Requirements 10.2, 10.3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventConsumer {

    public static final String SESSION_EVENTS_TOPIC = "exam.session.events";
    private static final String EVENT_TYPE_NAVIGATION = "NAVIGATION";

    private final AutoSaveService autoSaveService;
    private final ObjectMapper objectMapper;

    /**
     * Listens to exam.session.events topic via Kafka.
     *
     * @param event the session event payload
     */
    @KafkaListener(topics = SESSION_EVENTS_TOPIC, groupId = "response-service")
    public void handleSessionEvent(Map<String, Object> event) {
        processEventMap(event);
    }

    /**
     * Listens to exam.session.events topic via RabbitMQ.
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "response.session.events.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = SESSION_EVENTS_TOPIC
            )
    )
    public void handleRabbitSessionEvent(Object message) {
        log.info("Received RabbitMQ session event: {}", message);
        try {
            if (message instanceof Message amqpMsg) {
                String s = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(s);
                processJsonNode(node);
            } else if (message instanceof byte[] bytes) {
                String s = new String(bytes, StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(s);
                processJsonNode(node);
            } else if (message instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) map;
                processEventMap(eventMap);
            } else if (message instanceof String s) {
                JsonNode node = objectMapper.readTree(s);
                processJsonNode(node);
            } else {
                JsonNode node = objectMapper.valueToTree(message);
                processJsonNode(node);
            }
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ session event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listens to in-memory Spring session events (monolith mode).
     */
    @EventListener
    public void onSpringSessionEvent(GenericDomainEvent event) {
        if (!SESSION_EVENTS_TOPIC.equals(event.topic())) {
            return;
        }
        log.info("Received Spring in-memory session event: key={}", event.key());
        try {
            Object payload = event.payload();
            if (payload instanceof Message amqpMsg) {
                String s = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(s);
                processJsonNode(node);
            } else if (payload instanceof byte[] bytes) {
                String s = new String(bytes, StandardCharsets.UTF_8);
                JsonNode node = objectMapper.readTree(s);
                processJsonNode(node);
            } else if (payload instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> eventMap = (Map<String, Object>) map;
                processEventMap(eventMap);
            } else if (payload instanceof String s) {
                JsonNode node = objectMapper.readTree(s);
                processJsonNode(node);
            } else {
                JsonNode node = objectMapper.valueToTree(payload);
                processJsonNode(node);
            }
        } catch (Exception e) {
            log.error("Failed to process Spring in-memory session event: {}", e.getMessage(), e);
        }
    }

    private void processEventMap(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        if (EVENT_TYPE_NAVIGATION.equals(eventType)) {
            String sessionIdStr = (String) event.get("sessionId");
            if (sessionIdStr != null) {
                try {
                    UUID sessionId = UUID.fromString(sessionIdStr);
                    log.info("Navigation event received for session: {}, triggering auto-save", sessionId);
                    autoSaveService.triggerSaveForSession(sessionId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid session ID in navigation event: {}", sessionIdStr);
                }
            }
        }
    }

    private void processJsonNode(JsonNode node) {
        String eventType = node.has("eventType") ? node.get("eventType").asText() : "";
        if (EVENT_TYPE_NAVIGATION.equals(eventType)) {
            String sessionIdStr = node.has("sessionId") ? node.get("sessionId").asText() : null;
            if (sessionIdStr != null) {
                try {
                    UUID sessionId = UUID.fromString(sessionIdStr);
                    log.info("Navigation event received for session: {}, triggering auto-save", sessionId);
                    autoSaveService.triggerSaveForSession(sessionId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid session ID in navigation event: {}", sessionIdStr);
                }
            }
        }
    }
}
