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

package com.examplatform.evaluation.client;

import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.exception.UpstreamServiceUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResponseServiceRestClientTest {

    private ObjectMapper objectMapper;
    private ResponseServiceRestClient client;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        client = new ResponseServiceRestClient(null, "http://localhost:19998", objectMapper);
    }

    @Test
    @DisplayName("Throws UpstreamServiceUnavailableException when response-service is unreachable")
    void shouldThrowWhenResponseServiceUnreachable() {
        UUID sessionId = UUID.randomUUID();

        assertThatThrownBy(() -> client.getCandidateResponses(sessionId, "tenant-1"))
                .isInstanceOf(UpstreamServiceUnavailableException.class)
                .hasMessageContaining("Failed to retrieve candidate responses for session");
    }

    @Test
    @DisplayName("Fallback method throws UpstreamServiceUnavailableException")
    void shouldThrowFromFallbackMethod() {
        UUID sessionId = UUID.randomUUID();
        Throwable cause = new RuntimeException("Circuit open");

        assertThatThrownBy(() -> client.fetchResponsesFallback(sessionId, "tenant-1", cause))
                .isInstanceOf(UpstreamServiceUnavailableException.class)
                .hasMessageContaining("Response service unavailable for session");
    }
}
