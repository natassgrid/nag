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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

/**
 * Utility to extract raw JSON string payloads from various message broker event objects
 * such as Spring AMQP {@link Message}, byte arrays, Strings, or domain objects.
 */
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
}
