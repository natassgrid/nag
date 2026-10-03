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

package com.examplatform.shared.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiErrorResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("ApiErrorResponse builds and serializes standard properties correctly")
    void testStandardResponse() throws Exception {
        ApiErrorResponse response = ApiErrorResponse.of(404, "Not Found", "Item not found");
        assertThat(response.status()).isEqualTo(404);
        assertThat(response.error()).isEqualTo("Not Found");
        assertThat(response.message()).isEqualTo("Item not found");
        assertThat(response.timestamp()).isNotNull();

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"status\":404")
                .contains("\"error\":\"Not Found\"")
                .contains("\"message\":\"Item not found\"")
                .contains("\"timestamp\":");
    }

    @Test
    @DisplayName("ApiErrorResponse builder supports fieldErrors and extra dynamic properties")
    void testBuilderWithExtraProperties() throws Exception {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .status(422)
                .error("Unprocessable Entity")
                .message("Validation failed")
                .code("INVALID_PAYLOAD")
                .fieldErrors(Map.of("name", "Name cannot be blank"))
                .property("expectedTotalMarks", 100)
                .property("actualTotalMarks", 90)
                .build();

        String json = objectMapper.writeValueAsString(response);
        assertThat(json).contains("\"status\":422")
                .contains("\"code\":\"INVALID_PAYLOAD\"")
                .contains("\"fieldErrors\":{\"name\":\"Name cannot be blank\"}")
                .contains("\"expectedTotalMarks\":100")
                .contains("\"actualTotalMarks\":90");
    }

    @Test
    @DisplayName("ErrorEnvelope serializes nested error structure correctly")
    void testErrorEnvelope() throws Exception {
        ErrorEnvelope envelope = ErrorEnvelope.of("CONCURRENT_SESSION", "Session active on another device", 409, "exam-1", "sess-1");
        assertThat(envelope.status()).isEqualTo("error");
        assertThat(envelope.httpStatus()).isEqualTo(409);
        assertThat(envelope.error().code()).isEqualTo("CONCURRENT_SESSION");
        assertThat(envelope.error().activeExamId()).isEqualTo("exam-1");
        assertThat(envelope.error().activeSessionId()).isEqualTo("sess-1");

        String json = objectMapper.writeValueAsString(envelope);
        assertThat(json).contains("\"status\":\"error\"")
                .contains("\"httpStatus\":409")
                .contains("\"activeExamId\":\"exam-1\"")
                .contains("\"activeSessionId\":\"sess-1\"");
    }

    @Test
    @DisplayName("BaseGlobalExceptionHandler handles DomainException and builds responses")
    void testBaseGlobalExceptionHandler() {
        BaseGlobalExceptionHandler handler = new BaseGlobalExceptionHandler() {};

        ResponseEntity<ApiErrorResponse> notFoundRes = handler.handleDomainException(new ResourceNotFoundException("Entity 123"));
        assertThat(notFoundRes.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(notFoundRes.getBody()).isNotNull();
        assertThat(notFoundRes.getBody().status()).isEqualTo(404);
        assertThat(notFoundRes.getBody().message()).isEqualTo("Entity 123");

        ResponseEntity<ApiErrorResponse> genericRes = handler.handleGeneric(new RuntimeException("Oops"));
        assertThat(genericRes.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(genericRes.getBody().message()).isEqualTo("An unexpected error occurred");
    }
}
