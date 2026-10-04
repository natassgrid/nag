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
import com.examplatform.questionbank.grpc.BatchFindQuestionsGrpcRequest;
import com.examplatform.questionbank.grpc.BatchFindQuestionsGrpcResponse;
import com.examplatform.questionbank.grpc.BlueprintMatchGrpcRequest;
import com.examplatform.questionbank.grpc.BlueprintMatchGrpcResponse;
import com.examplatform.questionbank.grpc.QuestionBankGrpcServiceGrpc;
import com.examplatform.questionbank.grpc.QuestionSummaryGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
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
    private Server grpcServer;
    private int grpcPort;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        client = new QuestionBankClientImpl(jdbcTemplate, "http://localhost:8083");

        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        ReflectionTestUtils.setField(client, "restClient", restClientBuilder.build());
    }

    @AfterEach
    void tearDown() {
        if (grpcServer != null) {
            grpcServer.shutdownNow();
        }
    }

    @Test
    @DisplayName("Successfully retrieves questions via REST when question-bank-service is available")
    void findAvailableQuestions_viaRest_success() {
        UUID qId = UUID.randomUUID();
        String jsonResponse = """
            {
                "status": "success",
                "message": "Questions retrieved",
                "timestamp": "2026-10-04T04:25:15.932623270Z",
                "data": [
                    {
                        "id": "%s",
                        "subject": "Mathematics",
                        "topic": "Calculus",
                        "difficulty": "MEDIUM",
                        "cognitiveLevel": "APPLY",
                        "content": "What is the derivative of x^2?",
                        "authorId": "00000000-0000-0000-0000-000000000001",
                        "createdAt": "2026-09-19T08:18:08.483802"
                    }
                ]
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
    @DisplayName("Successfully handles REST response with empty data list")
    void findAvailableQuestions_viaRest_emptyData_success() {
        String jsonResponse = """
            {
                "status": "success",
                "message": "Blueprint questions retrieved successfully",
                "timestamp": "2026-10-04T04:25:15.932623270Z",
                "data": []
            }
            """;

        mockServer.expect(requestTo("http://localhost:8083/api/v1/questions/blueprint-match"))
                .andExpect(method(POST))
                .andRespond(withSuccess(jsonResponse, APPLICATION_JSON));

        List<QuestionSummary> results = client.findAvailableQuestions("Computer Knowledge", "Computer Basics", "EASY", "REMEMBER", "tenant-1");

        assertThat(results).isEmpty();
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

    @Test
    @DisplayName("Successfully retrieves questions via gRPC when gRPC server is enabled and available")
    void findAvailableQuestions_viaGrpc_success() throws IOException {
        UUID qId = UUID.randomUUID();

        QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase mockGrpcService =
                new QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase() {
                    @Override
                    public void matchBlueprint(BlueprintMatchGrpcRequest request,
                                               StreamObserver<BlueprintMatchGrpcResponse> responseObserver) {
                        BlueprintMatchGrpcResponse response = BlueprintMatchGrpcResponse.newBuilder()
                                .addQuestions(QuestionSummaryGrpc.newBuilder()
                                        .setId(qId.toString())
                                        .setSubject("Chemistry")
                                        .setTopic("Organic")
                                        .setDifficulty("EASY")
                                        .setCognitiveLevel("REMEMBER")
                                        .setContent("Structure of Benzene?")
                                        .build())
                                .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }
                };

        grpcServer = ServerBuilder.forPort(0)
                .addService(mockGrpcService)
                .build()
                .start();
        grpcPort = grpcServer.getPort();

        QuestionBankClientImpl grpcClient = new QuestionBankClientImpl(
                jdbcTemplate, "http://localhost:8083", true, "localhost", grpcPort);

        List<QuestionSummary> results = grpcClient.findAvailableQuestions(
                "Chemistry", "Organic", "EASY", "REMEMBER", "tenant-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionId()).isEqualTo(qId);
        assertThat(results.get(0).getSubject()).isEqualTo("Chemistry");
        assertThat(results.get(0).getTopic()).isEqualTo("Organic");
        assertThat(results.get(0).getContent()).isEqualTo("Structure of Benzene?");
    }

    @Test
    @DisplayName("gRPC returning 0 questions is authoritative and does not fall through to REST or DB")
    void findAvailableQuestions_viaGrpc_emptyResponse_returnsEmptyAuthoritatively() throws IOException {
        QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase mockGrpcService =
                new QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase() {
                    @Override
                    public void matchBlueprint(BlueprintMatchGrpcRequest request,
                                               StreamObserver<BlueprintMatchGrpcResponse> responseObserver) {
                        BlueprintMatchGrpcResponse response = BlueprintMatchGrpcResponse.newBuilder().build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }
                };

        grpcServer = ServerBuilder.forPort(0)
                .addService(mockGrpcService)
                .build()
                .start();
        grpcPort = grpcServer.getPort();

        QuestionBankClientImpl grpcClient = new QuestionBankClientImpl(
                jdbcTemplate, "http://localhost:8083", true, "localhost", grpcPort);

        List<QuestionSummary> results = grpcClient.findAvailableQuestions(
                "Computer Knowledge", "Computer Basics", "EASY", "REMEMBER", "tenant-1");

        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Successfully retrieves questions by IDs via gRPC when enabled")
    void findQuestionsByIds_viaGrpc_success() throws IOException {
        UUID qId = UUID.randomUUID();

        QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase mockGrpcService =
                new QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase() {
                    @Override
                    public void batchFindQuestions(BatchFindQuestionsGrpcRequest request,
                                                   StreamObserver<BatchFindQuestionsGrpcResponse> responseObserver) {
                        BatchFindQuestionsGrpcResponse response = BatchFindQuestionsGrpcResponse.newBuilder()
                                .addQuestions(QuestionSummaryGrpc.newBuilder()
                                        .setId(qId.toString())
                                        .setSubject("History")
                                        .setTopic("Modern India")
                                        .setDifficulty("MEDIUM")
                                        .setContent("Year of Independence?")
                                        .build())
                                .build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }
                };

        grpcServer = ServerBuilder.forPort(0)
                .addService(mockGrpcService)
                .build()
                .start();
        grpcPort = grpcServer.getPort();

        QuestionBankClientImpl grpcClient = new QuestionBankClientImpl(
                jdbcTemplate, "http://localhost:8083", true, "localhost", grpcPort);

        List<QuestionSummary> results = grpcClient.findQuestionsByIds(List.of(qId), "tenant-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionId()).isEqualTo(qId);
        assertThat(results.get(0).getSubject()).isEqualTo("History");
        assertThat(results.get(0).getContent()).isEqualTo("Year of Independence?");
    }

    @Test
    @DisplayName("gRPC batchFindQuestions returning 0 questions is authoritative and does not fall through to REST or DB")
    void findQuestionsByIds_viaGrpc_emptyResponse_returnsEmptyAuthoritatively() throws IOException {
        UUID qId = UUID.randomUUID();
        QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase mockGrpcService =
                new QuestionBankGrpcServiceGrpc.QuestionBankGrpcServiceImplBase() {
                    @Override
                    public void batchFindQuestions(BatchFindQuestionsGrpcRequest request,
                                                   StreamObserver<BatchFindQuestionsGrpcResponse> responseObserver) {
                        BatchFindQuestionsGrpcResponse response = BatchFindQuestionsGrpcResponse.newBuilder().build();
                        responseObserver.onNext(response);
                        responseObserver.onCompleted();
                    }
                };

        grpcServer = ServerBuilder.forPort(0)
                .addService(mockGrpcService)
                .build()
                .start();
        grpcPort = grpcServer.getPort();

        QuestionBankClientImpl grpcClient = new QuestionBankClientImpl(
                jdbcTemplate, "http://localhost:8083", true, "localhost", grpcPort);

        List<QuestionSummary> results = grpcClient.findQuestionsByIds(List.of(qId), "tenant-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionId()).isEqualTo(qId);
        assertThat(results.get(0).getSubject()).isNull();
    }

    @Test
    @DisplayName("Successfully retrieves questions by IDs via REST when question-bank-service is available")
    void findQuestionsByIds_viaRest_success() {
        UUID qId = UUID.randomUUID();
        String jsonResponse = """
            {
                "status": "success",
                "message": "Questions retrieved",
                "timestamp": "2026-10-04T04:25:15.932623270Z",
                "data": [
                    {
                        "id": "%s",
                        "subject": "History",
                        "topic": "Modern India",
                        "difficulty": "MEDIUM",
                        "content": "Year of Independence?"
                    }
                ]
            }
            """.formatted(qId);

        mockServer.expect(requestTo("http://localhost:8083/api/v1/questions/batch-find"))
                .andExpect(method(POST))
                .andExpect(header("X-Tenant-Id", "tenant-1"))
                .andRespond(withSuccess(jsonResponse, APPLICATION_JSON));

        List<QuestionSummary> results = client.findQuestionsByIds(List.of(qId), "tenant-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionId()).isEqualTo(qId);
        assertThat(results.get(0).getSubject()).isEqualTo("History");
        assertThat(results.get(0).getContent()).isEqualTo("Year of Independence?");

        mockServer.verify();
    }

    @Test
    @DisplayName("Falls back to REST when gRPC fails")
    void findAvailableQuestions_grpcFails_fallsBackToRest() {
        UUID qId = UUID.randomUUID();
        String jsonResponse = """
            {
                "status": "success",
                "message": "Questions retrieved",
                "timestamp": "2026-10-04T04:25:15.932623270Z",
                "data": [
                    {
                        "id": "%s",
                        "subject": "Geography",
                        "topic": "Rivers",
                        "difficulty": "EASY",
                        "content": "Longest river in India?"
                    }
                ]
            }
            """.formatted(qId);

        // Point to dead gRPC port (e.g. 19999) but valid REST mock
        QuestionBankClientImpl fallbackClient = new QuestionBankClientImpl(
                jdbcTemplate, "http://localhost:8083", true, "localhost", 19999);

        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer localMockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        ReflectionTestUtils.setField(fallbackClient, "restClient", restClientBuilder.build());

        localMockServer.expect(requestTo("http://localhost:8083/api/v1/questions/blueprint-match"))
                .andExpect(method(POST))
                .andRespond(withSuccess(jsonResponse, APPLICATION_JSON));

        List<QuestionSummary> results = fallbackClient.findAvailableQuestions(
                "Geography", "Rivers", "EASY", null, "tenant-1");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getQuestionId()).isEqualTo(qId);
        assertThat(results.get(0).getSubject()).isEqualTo("Geography");
        assertThat(results.get(0).getContent()).isEqualTo("Longest river in India?");

        localMockServer.verify();
    }
}
