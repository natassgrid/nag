/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.delivery.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Custom Jackson deserializer for PaperOptionPayload supporting both object and string representations:
 * 1. Object: {"id": "A", "text": "...", "isCorrect": true, ...}
 * 2. String: "Option text"
 */
public class PaperOptionPayloadDeserializer extends JsonDeserializer<PaperOptionPayload> {

    @Override
    public PaperOptionPayload deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return PaperOptionPayload.builder()
                    .text(p.getText())
                    .build();
        }

        JsonNode node = p.getCodec().readTree(p);
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isTextual()) {
            return PaperOptionPayload.builder()
                    .text(node.asText())
                    .build();
        }

        String id = node.has("id") ? node.get("id").asText() : null;
        String text = node.has("text") ? node.get("text").asText() : node.asText();
        boolean isCorrect = node.has("isCorrect") && node.get("isCorrect").asBoolean();
        String imageUrl = node.has("imageUrl") && !node.get("imageUrl").isNull()
                ? node.get("imageUrl").asText(null) : null;
        String imageAltText = node.has("imageAltText") && !node.get("imageAltText").isNull()
                ? node.get("imageAltText").asText(null) : null;

        return PaperOptionPayload.builder()
                .id(id)
                .text(text)
                .isCorrect(isCorrect)
                .imageUrl(imageUrl)
                .imageAltText(imageAltText)
                .build();
    }
}
