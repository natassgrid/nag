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

package com.examplatform.papergenerator.client;

import com.examplatform.papergenerator.dto.QuestionSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QuestionBankClientImplTest {

    private JdbcTemplate jdbcTemplate;
    private QuestionBankClientImpl client;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        client = new QuestionBankClientImpl(jdbcTemplate, "http://localhost:8083");

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        ReflectionTestUtils.setField(client, "restClient", restClientBuilder.build());
    }

    @Test
    @DisplayName("Successfully retrieves questions via REST when question-bank-service is available")
    void findAvailableQuestions_viaRest_success() {
        UUID qId = UUID.randomUUID();
        String jsonResponse = """
            {
                "success": true,
                "data": [
                    {
                        "id": "%s",
                        "subject": "Mathematics",
                        "topic": "Calculus",
                        "difficulty": "MEDIUM",
                        "cognitiveLevel": "APPLY",
                        "content": "What is the derivative of x^2?"
                    }
                ],
                "message": "Questions retrieved"
            }
            """.formatted(qId);

        mockServer.expect(requestTo("http://localhost:8083/api/v1/questions/blueprint-match"))
                .andExpect(method(POST))
                .andExpect(header("X-Tenant-Id", "tenant-1"))
                .andRespond(withSuccess(jsonResponse, APPLICATION_JSON));

        List<QuestionSummary> results = client.findAvailableQuestions("Mathematics", "Calculus", "MEDIUM", "APPLY", "tenant-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionId()).isEqualTo(qId);
        assertThat(results.get(0).getSubject()).isEqualTo("Mathematics");
        assertThat(results.get(0).getContent()).isEqualTo("What is the derivative of x^2?");

        mockServer.verify();
    }

    @Test
    @DisplayName("Falls back to JDBC template when REST call fails")
    @SuppressWarnings("unchecked")
    void findAvailableQuestions_restFails_fallsBackToJdbc() {
        UUID qId = UUID.randomUUID();
        mockServer.expect(requestTo("http://localhost:8083/api/v1/questions/blueprint-match"))
                .andExpect(method(POST))
                .andRespond(withServerError());

        QuestionSummary dbSummary = QuestionSummary.builder()
                .questionId(qId)
                .subject("Physics")
                .topic("Mechanics")
                .difficulty("HARD")
                .build();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(dbSummary));

        List<QuestionSummary> results = client.findAvailableQuestions("Physics", "Mechanics", "HARD", null, "tenant-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionId()).isEqualTo(qId);
        assertThat(results.get(0).getSubject()).isEqualTo("Physics");

        mockServer.verify();
    }
}
