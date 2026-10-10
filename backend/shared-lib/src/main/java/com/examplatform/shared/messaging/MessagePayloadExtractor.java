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

package com.examplatform.shared.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility to extract raw JSON string payloads and dispatch domain events from various message broker
 * sources such as Spring AMQP {@link Message}, byte arrays, Strings, or domain objects.
 */
@Slf4j
public final class MessagePayloadExtractor {

    private MessagePayloadExtractor() {
    }

    /**
     * Extracts a UTF-8 string payload from an event object.
     *
     * @param message      the event payload (AMQP Message, byte[], String, or Object)
     * @param objectMapper the Jackson ObjectMapper used when object serialization is required
     * @return the string payload, or {@code null} if message is null
     */
    public static String extractPayload(Object message, ObjectMapper objectMapper) {
        if (message == null) {
            return null;
        }
        if (message instanceof Message amqpMsg) {
            return new String(amqpMsg.getBody(), StandardCharsets.UTF_8);
        }
        if (message instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (message instanceof String s) {
            return s;
        }
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize message payload to JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Safely processes an AMQP RabbitMQ message by extracting its JSON payload and passing it to a consumer.
     *
     * @param message        the AMQP message object
     * @param objectMapper   Jackson object mapper
     * @param eventTopic     name of the topic for logging
     * @param payloadHandler consumer receiving the extracted JSON payload
     */
    public static void handleRabbitEvent(
            Object message,
            ObjectMapper objectMapper,
            String eventTopic,
            Consumer<String> payloadHandler
    ) {
        try {
            String payload = extractPayload(message, objectMapper);
            payloadHandler.accept(payload);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ {} event: {}", eventTopic, e.getMessage(), e);
        }
    }

    /**
     * Safely processes an in-memory Spring event by verifying the topic and passing payload and key to a consumer.
     *
     * @param event          generic domain event
     * @param objectMapper   Jackson object mapper
     * @param expectedTopic  expected event topic
     * @param payloadHandler consumer receiving (payload, key)
     */
    public static void handleSpringEvent(
            GenericDomainEvent event,
            ObjectMapper objectMapper,
            String expectedTopic,
            BiConsumer<String, String> payloadHandler
    ) {
        if (event == null || !expectedTopic.equals(event.topic())) {
            return;
        }
        try {
            String payload = extractPayload(event.payload(), objectMapper);
            payloadHandler.accept(payload, event.key());
        } catch (Exception e) {
            log.error("Failed to process Spring in-memory {} event: {}", expectedTopic, e.getMessage(), e);
        }
    }

    /**
     * Parses a JSON payload and verifies that the {@code eventType} field matches the expected type.
     *
     * @param payload           JSON payload string
     * @param objectMapper      Jackson object mapper
     * @param expectedEventType expected eventType string (e.g. "EVALUATION_COMPLETED")
     * @return an Optional containing the parsed Map if valid and matching, otherwise Optional.empty()
     */
    public static Optional<Map<String, Object>> parseEventIfMatching(
            String payload,
            ObjectMapper objectMapper,
            String expectedEventType
    ) {
        if (payload == null || payload.isBlank()) {
            return Optional.empty();
        }
        try {
            Map<String, Object> event = objectMapper.readValue(payload, new TypeReference<>() {});
            String eventType = (String) event.get("eventType");
            if (expectedEventType.equals(eventType)) {
                return Optional.of(event);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
