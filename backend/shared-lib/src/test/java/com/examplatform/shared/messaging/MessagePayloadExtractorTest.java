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
}
