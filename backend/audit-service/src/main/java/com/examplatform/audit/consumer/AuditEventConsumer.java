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

package com.examplatform.audit.consumer;

import com.examplatform.audit.service.AuditIngestionService;
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

/**
 * Consumes audit event messages from Kafka, RabbitMQ, and in-memory Spring events.
 * Deserializes JSON payloads and delegates to {@link AuditIngestionService}
 * for cryptographic signing and persistent storage.
 *
 * Requirements: 12.1, 12.2, 12.3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    public static final String AUDIT_TOPIC = "exam.audit.events";

    private final AuditIngestionService auditIngestionService;
    private final ObjectMapper objectMapper;

    /**
     * Listener for audit events via Kafka.
     *
     * @param message the raw JSON string payload from Kafka
     */
    @KafkaListener(topics = AUDIT_TOPIC, groupId = "audit-service")
    public void onKafkaAuditEvent(String message) {
        log.debug("Received Kafka audit event: {}", message);
        processAuditEvent(message);
    }

    /**
     * Listener for audit events via RabbitMQ.
     *
     * @param message the event payload from RabbitMQ
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "audit.events.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = AUDIT_TOPIC
            )
    )
    public void onRabbitAuditEvent(Object message) {
        log.debug("Received RabbitMQ audit event: {}", message);
        try {
            String payload;
            if (message instanceof Message amqpMsg) {
                payload = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
            } else if (message instanceof byte[] bytes) {
                payload = new String(bytes, StandardCharsets.UTF_8);
            } else if (message instanceof String s) {
                payload = s;
            } else {
                payload = objectMapper.writeValueAsString(message);
            }
            processAuditEvent(payload);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ audit event: {}", e.getMessage(), e);
        }
    }

    /**
     * Listener for in-memory Spring events (monolith mode).
     *
     * @param event the in-process domain event
     */
    @EventListener
    public void onSpringAuditEvent(GenericDomainEvent event) {
        if (!AUDIT_TOPIC.equals(event.topic())) {
            return;
        }
        log.debug("Received Spring in-memory audit event: key={}", event.key());
        try {
            Object payload = event.payload();
            String message;
            if (payload instanceof Message amqpMsg) {
                message = new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
            } else if (payload instanceof byte[] bytes) {
                message = new String(bytes, StandardCharsets.UTF_8);
            } else if (payload instanceof String s) {
                message = s;
            } else {
                message = objectMapper.writeValueAsString(payload);
            }
            processAuditEvent(message);
        } catch (Exception e) {
            log.error("Failed to process in-memory audit event: {}", e.getMessage(), e);
        }
    }

    private void processAuditEvent(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);

            String eventType = getTextOrDefault(root, "eventType", "UNKNOWN");
            String actorId = getTextOrDefault(root, "actorId", "00000000-0000-0000-0000-000000000000");
            String resource = getTextOrDefault(root, "resource", "unknown");
            String ipAddress = getTextOrNull(root, "ipAddress");
            String deviceFingerprint = getTextOrNull(root, "deviceFingerprint");
            String tenantId = getTextOrDefault(root, "tenantId", "default");

            auditIngestionService.ingest(message, eventType, actorId, resource,
                    ipAddress, deviceFingerprint, tenantId);

            log.info("Successfully ingested audit event type=[{}] actor=[{}]", eventType, actorId);
        } catch (Exception e) {
            log.error("Failed to process audit event: {}", e.getMessage(), e);
            // In production, this would be sent to a dead-letter topic
        }
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        JsonNode node = root.get(field);
        return (node != null && !node.isNull()) ? node.asText() : defaultValue;
    }

    private String getTextOrNull(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return (node != null && !node.isNull()) ? node.asText() : null;
    }
}
