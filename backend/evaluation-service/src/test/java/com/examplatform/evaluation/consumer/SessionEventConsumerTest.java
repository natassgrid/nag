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

package com.examplatform.evaluation.consumer;

import com.examplatform.evaluation.client.AnswerKeyClient;
import com.examplatform.evaluation.client.CandidateResponseClient;
import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.exception.UpstreamServiceUnavailableException;
import com.examplatform.evaluation.service.AutoEvaluationService;
import com.examplatform.evaluation.service.ScoreAggregationService;
import com.examplatform.shared.messaging.EventPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionEventConsumerTest {

    @Mock
    private AutoEvaluationService autoEvaluationService;

    @Mock
    private ScoreAggregationService scoreAggregationService;

    @Mock
    private AnswerKeyClient answerKeyClient;

    @Mock
    private CandidateResponseClient candidateResponseClient;

    @Mock
    private EventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private SessionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new SessionEventConsumer(
                autoEvaluationService,
                scoreAggregationService,
                answerKeyClient,
                candidateResponseClient,
                eventPublisher,
                objectMapper
        );
    }

    @Nested
    @DisplayName("Happy Path & Inter-service Fetching")
    class HappyPathTests {

        @Test
        @DisplayName("Fetches answer keys and responses via clients when not embedded in event")
        void shouldFetchViaClientsAndEvaluateSuccessfully() throws Exception {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();
            UUID examId = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();

            String eventJson = String.format("""
                    {
                        "eventType": "SESSION_SUBMITTED",
                        "sessionId": "%s",
                        "candidateId": "%s",
                        "paperId": "%s",
                        "examId": "%s",
                        "tenantId": "tenant-xyz"
                    }
                    """, sessionId, candidateId, paperId, examId);

            JsonNode eventNode = objectMapper.readTree(eventJson);

            List<CandidateResponse> mockResponses = List.of(
                    CandidateResponse.builder()
                            .questionId(questionId)
                            .selectedOptionIds("[\"opt-1\"]")
                            .attempted(true)
                            .build()
            );
            List<AnswerKey> mockAnswerKeys = List.of(
                    AnswerKey.builder()
                            .questionId(questionId)
                            .questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-1\"]")
                            .marksPerQuestion(4.0)
                            .negativeMarks(1.0)
                            .build()
            );

            when(candidateResponseClient.getCandidateResponses(sessionId, "tenant-xyz"))
                    .thenReturn(mockResponses);
            when(answerKeyClient.getAnswerKeysForPaper(paperId, "tenant-xyz"))
                    .thenReturn(mockAnswerKeys);

            Evaluation mockEvaluation = Evaluation.builder()
                    .sessionId(sessionId)
                    .candidateId(candidateId)
                    .questionId(questionId)
                    .score(BigDecimal.valueOf(4.0))
                    .build();
            when(autoEvaluationService.evaluateSession(sessionId, candidateId, mockAnswerKeys, mockResponses, "tenant-xyz"))
                    .thenReturn(List.of(mockEvaluation));

            consumer.processJsonEvent(eventNode);

            verify(candidateResponseClient).getCandidateResponses(sessionId, "tenant-xyz");
            verify(answerKeyClient).getAnswerKeysForPaper(paperId, "tenant-xyz");
            verify(autoEvaluationService).evaluateSession(sessionId, candidateId, mockAnswerKeys, mockResponses, "tenant-xyz");
            verify(scoreAggregationService).aggregateScores(sessionId, candidateId, examId, "tenant-xyz");
            verify(eventPublisher, never()).publish(eq(SessionEventConsumer.DLQ_TOPIC), anyString(), any());
        }

        @Test
        @DisplayName("Batch-fetches answer keys using question IDs if paperId is omitted")
        void shouldBatchFetchAnswerKeysWhenPaperIdAbsent() throws Exception {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();

            String eventJson = String.format("""
                    {
                        "eventType": "SESSION_SUBMITTED",
                        "sessionId": "%s",
                        "candidateId": "%s",
                        "tenantId": "default"
                    }
                    """, sessionId, candidateId);

            JsonNode eventNode = objectMapper.readTree(eventJson);

            List<CandidateResponse> mockResponses = List.of(
                    CandidateResponse.builder()
                            .questionId(questionId)
                            .selectedOptionIds("[\"opt-2\"]")
                            .attempted(true)
                            .build()
            );
            List<AnswerKey> mockAnswerKeys = List.of(
                    AnswerKey.builder()
                            .questionId(questionId)
                            .questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-2\"]")
                            .marksPerQuestion(2.0)
                            .build()
            );

            when(candidateResponseClient.getCandidateResponses(sessionId, "default"))
                    .thenReturn(mockResponses);
            when(answerKeyClient.batchGetAnswerKeys(List.of(questionId), "default"))
                    .thenReturn(mockAnswerKeys);

            consumer.processJsonEvent(eventNode);

            verify(answerKeyClient).batchGetAnswerKeys(List.of(questionId), "default");
            verify(autoEvaluationService).evaluateSession(sessionId, candidateId, mockAnswerKeys, mockResponses, "default");
            verify(scoreAggregationService).aggregateScores(sessionId, candidateId, null, "default");
        }

        @Test
        @DisplayName("Uses embedded answer keys and responses directly when available")
        void shouldRespectEmbeddedPayloads() throws Exception {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();

            String eventJson = String.format("""
                    {
                        "eventType": "SESSION_SUBMITTED",
                        "sessionId": "%s",
                        "candidateId": "%s",
                        "tenantId": "default",
                        "answerKeys": [
                            {
                                "questionId": "%s",
                                "questionType": "SINGLE_MCQ",
                                "correctAnswer": "[\\"opt-1\\"]",
                                "marksPerQuestion": 1.0,
                                "negativeMarks": 0.0
                            }
                        ],
                        "responses": [
                            {
                                "questionId": "%s",
                                "selectedOptionIds": "[\\"opt-1\\"]",
                                "attempted": true
                            }
                        ]
                    }
                    """, sessionId, candidateId, questionId, questionId);

            JsonNode eventNode = objectMapper.readTree(eventJson);

            consumer.processJsonEvent(eventNode);

            verify(candidateResponseClient, never()).getCandidateResponses(any(), any());
            verify(answerKeyClient, never()).getAnswerKeysForPaper(any(), any());
            verify(answerKeyClient, never()).batchGetAnswerKeys(anyList(), any());
            verify(autoEvaluationService).evaluateSession(eq(sessionId), eq(candidateId), anyList(), anyList(), eq("default"));
        }
    }

    @Nested
    @DisplayName("Upstream Failures & DLQ Emission")
    class UpstreamFailureTests {

        @Test
        @DisplayName("Emits DLQ event when QuestionBank service is unreachable")
        void shouldPublishDlqWhenQuestionBankFails() throws Exception {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();

            String eventJson = String.format("""
                    {
                        "eventType": "SESSION_SUBMITTED",
                        "sessionId": "%s",
                        "candidateId": "%s",
                        "paperId": "%s",
                        "tenantId": "test-tenant"
                    }
                    """, sessionId, candidateId, paperId);

            JsonNode eventNode = objectMapper.readTree(eventJson);

            when(candidateResponseClient.getCandidateResponses(sessionId, "test-tenant"))
                    .thenReturn(Collections.emptyList());
            when(answerKeyClient.getAnswerKeysForPaper(paperId, "test-tenant"))
                    .thenThrow(new UpstreamServiceUnavailableException("QuestionBank gRPC connection refused"));

            consumer.processJsonEvent(eventNode);

            verify(autoEvaluationService, never()).evaluateSession(any(), any(), anyList(), anyList(), anyString());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dlqCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher).publish(eq(SessionEventConsumer.DLQ_TOPIC), eq(sessionId.toString()), dlqCaptor.capture());

            Map<String, Object> dlqEvent = dlqCaptor.getValue();
            assertThat(dlqEvent.get("eventType")).isEqualTo("EVALUATION_FAILED_DLQ");
            assertThat(dlqEvent.get("sessionId")).isEqualTo(sessionId.toString());
            assertThat(dlqEvent.get("candidateId")).isEqualTo(candidateId.toString());
            assertThat(dlqEvent.get("tenantId")).isEqualTo("test-tenant");
            assertThat(dlqEvent.get("reason").toString()).contains("QuestionBank gRPC connection refused");
        }

        @Test
        @DisplayName("Emits DLQ event when ResponseService is unreachable")
        void shouldPublishDlqWhenResponseServiceFails() throws Exception {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();

            String eventJson = String.format("""
                    {
                        "eventType": "SESSION_SUBMITTED",
                        "sessionId": "%s",
                        "candidateId": "%s",
                        "paperId": "%s"
                    }
                    """, sessionId, candidateId, paperId);

            JsonNode eventNode = objectMapper.readTree(eventJson);

            when(candidateResponseClient.getCandidateResponses(sessionId, "default"))
                    .thenThrow(new UpstreamServiceUnavailableException("Response service timeout 504"));

            consumer.processJsonEvent(eventNode);

            verify(autoEvaluationService, never()).evaluateSession(any(), any(), anyList(), anyList(), anyString());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dlqCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher).publish(eq(SessionEventConsumer.DLQ_TOPIC), eq(sessionId.toString()), dlqCaptor.capture());

            Map<String, Object> dlqEvent = dlqCaptor.getValue();
            assertThat(dlqEvent.get("eventType")).isEqualTo("EVALUATION_FAILED_DLQ");
            assertThat(dlqEvent.get("sessionId")).isEqualTo(sessionId.toString());
            assertThat(dlqEvent.get("reason").toString()).contains("Response service timeout 504");
        }

        @Test
        @DisplayName("Emits DLQ event when answer keys are empty")
        void shouldPublishDlqWhenAnswerKeysEmpty() throws Exception {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();

            String eventJson = String.format("""
                    {
                        "eventType": "SESSION_SUBMITTED",
                        "sessionId": "%s",
                        "candidateId": "%s",
                        "paperId": "%s"
                    }
                    """, sessionId, candidateId, paperId);

            JsonNode eventNode = objectMapper.readTree(eventJson);

            when(candidateResponseClient.getCandidateResponses(sessionId, "default"))
                    .thenReturn(Collections.emptyList());
            when(answerKeyClient.getAnswerKeysForPaper(paperId, "default"))
                    .thenReturn(Collections.emptyList());

            consumer.processJsonEvent(eventNode);

            verify(autoEvaluationService, never()).evaluateSession(any(), any(), anyList(), anyList(), anyString());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dlqCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher).publish(eq(SessionEventConsumer.DLQ_TOPIC), eq(sessionId.toString()), dlqCaptor.capture());

            Map<String, Object> dlqEvent = dlqCaptor.getValue();
            assertThat(dlqEvent.get("eventType")).isEqualTo("EVALUATION_FAILED_DLQ");
            assertThat(dlqEvent.get("reason")).isEqualTo("No answer keys found for session");
        }

        @Test
        @DisplayName("Ignores non-SESSION_SUBMITTED events without triggering evaluation or DLQ")
        void shouldIgnoreNonSubmittedEvents() throws Exception {
            String eventJson = """
                    {
                        "eventType": "SESSION_STARTED",
                        "sessionId": "00000000-0000-0000-0000-000000000001",
                        "candidateId": "00000000-0000-0000-0000-000000000002"
                    }
                    """;
            JsonNode eventNode = objectMapper.readTree(eventJson);

            consumer.processJsonEvent(eventNode);

            verify(candidateResponseClient, never()).getCandidateResponses(any(), any());
            verify(answerKeyClient, never()).getAnswerKeysForPaper(any(), any());
            verify(autoEvaluationService, never()).evaluateSession(any(), any(), anyList(), anyList(), anyString());
            verify(eventPublisher, never()).publish(anyString(), anyString(), any());
        }
    }
}
