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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer that listens for audit events on the
 * {@code exam.audit.events} topic. Deserializes incoming JSON messages,
 * extracts audit fields, and delegates to {@link AuditIngestionService}
 * for SHA-256 hashing, HSM signing, and immutable persistence.
 * Supports both Kafka and RabbitMQ.
 *
 * Validates: Requirements 15.1, 15.2
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditIngestionService auditIngestionService;
    private final ObjectMapper objectMapper;

    /**
     * Listener for audit events via Kafka.
     *
     * @param message the raw event payload from Kafka
     */
    @KafkaListener(topics = "exam.audit.events", groupId = "audit-service")
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
                    key = "exam.audit.events"
            )
    )
    public void onRabbitAuditEvent(Object message) {
        log.debug("Received RabbitMQ audit event: {}", message);
        try {
            String payload;
            if (message instanceof String s) {
                payload = s;
            } else {
                payload = objectMapper.writeValueAsString(message);
            }
            processAuditEvent(payload);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ audit event: {}", e.getMessage(), e);
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
