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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MessagePayloadExtractor Tests")
class MessagePayloadExtractorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("extracts payload from Spring AMQP Message")
    void extractsFromAmqpMessage() {
        Message message = new Message("{\"eventType\":\"TEST\"}".getBytes(StandardCharsets.UTF_8));
        String result = MessagePayloadExtractor.extractPayload(message, objectMapper);
        assertThat(result).isEqualTo("{\"eventType\":\"TEST\"}");
    }

    @Test
    @DisplayName("extracts payload from byte array")
    void extractsFromByteArray() {
        byte[] bytes = "{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8);
        String result = MessagePayloadExtractor.extractPayload(bytes, objectMapper);
        assertThat(result).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    @DisplayName("extracts payload from String")
    void extractsFromString() {
        String input = "simple string payload";
        String result = MessagePayloadExtractor.extractPayload(input, objectMapper);
        assertThat(result).isEqualTo(input);
    }

    @Test
    @DisplayName("serializes Object payload using ObjectMapper")
    void serializesObject() {
        Map<String, String> map = Map.of("testKey", "testVal");
        String result = MessagePayloadExtractor.extractPayload(map, objectMapper);
        assertThat(result).contains("\"testKey\":\"testVal\"");
    }

    @Test
    @DisplayName("returns null for null input")
    void returnsNullForNull() {
        String result = MessagePayloadExtractor.extractPayload(null, objectMapper);
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("handleRabbitEvent extracts payload and invokes handler")
    void handleRabbitEventSuccess() {
        AtomicReference<String> handled = new AtomicReference<>();
        MessagePayloadExtractor.handleRabbitEvent(
                "{\"hello\":\"world\"}",
                objectMapper,
                "test.topic",
                handled::set
        );
        assertThat(handled.get()).isEqualTo("{\"hello\":\"world\"}");
    }

    @Test
    @DisplayName("handleSpringEvent extracts payload for matching topic")
    void handleSpringEventMatching() {
        AtomicReference<String> handledPayload = new AtomicReference<>();
        AtomicReference<String> handledKey = new AtomicReference<>();

        GenericDomainEvent event = new GenericDomainEvent("test.topic", "key123", "{\"score\":100}");
        MessagePayloadExtractor.handleSpringEvent(
                event,
                objectMapper,
                "test.topic",
                (payload, key) -> {
                    handledPayload.set(payload);
                    handledKey.set(key);
                }
        );

        assertThat(handledPayload.get()).isEqualTo("{\"score\":100}");
        assertThat(handledKey.get()).isEqualTo("key123");
    }

    @Test
    @DisplayName("handleSpringEvent ignores non-matching topic")
    void handleSpringEventNonMatching() {
        AtomicReference<String> handled = new AtomicReference<>();
        GenericDomainEvent event = new GenericDomainEvent("other.topic", "key123", "{\"score\":100}");
        MessagePayloadExtractor.handleSpringEvent(
                event,
                objectMapper,
                "test.topic",
                (payload, key) -> handled.set(payload)
        );
        assertThat(handled.get()).isNull();
    }

    @Test
    @DisplayName("parseEventIfMatching returns payload map when eventType matches")
    void parseEventIfMatchingSuccess() {
        String json = "{\"eventType\":\"EVAL_DONE\",\"value\":42}";
        Optional<Map<String, Object>> result = MessagePayloadExtractor.parseEventIfMatching(json, objectMapper, "EVAL_DONE");
        assertThat(result).isPresent();
        assertThat(result.get().get("eventType")).isEqualTo("EVAL_DONE");
        assertThat(result.get().get("value")).isEqualTo(42);

        Optional<Map<String, Object>> mismatch = MessagePayloadExtractor.parseEventIfMatching(json, objectMapper, "OTHER");
        assertThat(mismatch).isEmpty();

        assertThat(MessagePayloadExtractor.parseEventIfMatching(null, objectMapper, "EVAL_DONE")).isEmpty();
        assertThat(MessagePayloadExtractor.parseEventIfMatching("invalid-json", objectMapper, "EVAL_DONE")).isEmpty();
    }
}
