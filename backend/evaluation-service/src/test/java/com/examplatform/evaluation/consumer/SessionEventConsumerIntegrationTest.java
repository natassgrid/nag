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
import com.examplatform.evaluation.dto.MarkingScheme;
import com.examplatform.evaluation.exception.UpstreamServiceUnavailableException;
import com.examplatform.evaluation.repository.EvaluationRepository;
import com.examplatform.evaluation.support.AbstractIntegrationTest;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.messaging.GenericDomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Evaluation Pipeline End-to-End Integration Tests (#140 / #108)")
class SessionEventConsumerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AnswerKeyClient answerKeyClient;

    @MockitoBean
    private CandidateResponseClient candidateResponseClient;

    @MockitoBean
    private EventPublisher eventPublisher;

    @Autowired
    private SessionEventConsumer sessionEventConsumer;

    @Autowired
    private EvaluationRepository evaluationRepository;

    @BeforeEach
    void setUp() {
        evaluationRepository.deleteAll();
    }

    @Nested
    @DisplayName("1. Live Multi-Service Pipeline Flow")
    class LiveMultiServiceFlow {

        @Test
        @DisplayName("Full evaluation pipeline: evaluates mixed question types, persists to DB, aggregates scores, and emits EVALUATION_COMPLETED event")
        void shouldProcessComprehensiveEvaluationPipelineEndToEnd() {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();
            UUID examId = UUID.randomUUID();
            String tenantId = "tenant-engineering";

            UUID q1SingleMcqCorrect = UUID.randomUUID();
            UUID q2SingleMcqWrong = UUID.randomUUID();
            UUID q3MultiMcqPartial = UUID.randomUUID();
            UUID q4MultiMcqWrong = UUID.randomUUID();
            UUID q5NumericalCorrect = UUID.randomUUID();
            UUID q6NumericalWrong = UUID.randomUUID();
            UUID q7Unattempted = UUID.randomUUID();
            UUID q8ZeroNegativeScheme = UUID.randomUUID();

            List<AnswerKey> mockAnswerKeys = List.of(
                    AnswerKey.builder()
                            .questionId(q1SingleMcqCorrect)
                            .questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-A\"]")
                            .marksPerQuestion(4.0)
                            .negativeMarks(1.0)
                            .markingScheme(MarkingScheme.STANDARD)
                            .build(),
                    AnswerKey.builder()
                            .questionId(q2SingleMcqWrong)
                            .questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-B\"]")
                            .marksPerQuestion(4.0)
                            .negativeMarks(1.0)
                            .markingScheme(MarkingScheme.STANDARD)
                            .build(),
                    AnswerKey.builder()
                            .questionId(q3MultiMcqPartial)
                            .questionType("MULTI_MCQ")
                            .correctAnswer("[\"opt-A\", \"opt-B\", \"opt-C\", \"opt-D\"]")
                            .marksPerQuestion(4.0)
                            .negativeMarks(1.0)
                            .markingScheme(MarkingScheme.STANDARD)
                            .build(),
                    AnswerKey.builder()
                            .questionId(q4MultiMcqWrong)
                            .questionType("MULTI_MCQ")
                            .correctAnswer("[\"opt-A\", \"opt-B\"]")
                            .marksPerQuestion(4.0)
                            .negativeMarks(1.0)
                            .markingScheme(MarkingScheme.STANDARD)
                            .build(),
                    AnswerKey.builder()
                            .questionId(q5NumericalCorrect)
                            .questionType("NUMERICAL")
                            .correctAnswer("42.0")
                            .marksPerQuestion(4.0)
                            .negativeMarks(0.5)
                            .markingScheme(MarkingScheme.STANDARD)
                            .build(),
                    AnswerKey.builder()
                            .questionId(q6NumericalWrong)
                            .questionType("NUMERICAL")
                            .correctAnswer("100.0")
                            .marksPerQuestion(4.0)
                            .negativeMarks(0.5)
                            .markingScheme(MarkingScheme.STANDARD)
                            .build(),
                    AnswerKey.builder()
                            .questionId(q7Unattempted)
                            .questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-A\"]")
                            .marksPerQuestion(4.0)
                            .negativeMarks(1.0)
                            .markingScheme(MarkingScheme.STANDARD)
                            .build(),
                    AnswerKey.builder()
                            .questionId(q8ZeroNegativeScheme)
                            .questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-A\"]")
                            .marksPerQuestion(4.0)
                            .negativeMarks(1.0)
                            .markingScheme(MarkingScheme.ZERO_NEGATIVE)
                            .build()
            );
            when(answerKeyClient.getAnswerKeysForPaper(paperId, tenantId))
                    .thenReturn(mockAnswerKeys);

            List<CandidateResponse> mockResponses = List.of(
                    CandidateResponse.builder()
                            .questionId(q1SingleMcqCorrect)
                            .selectedOptionIds("[\"opt-A\"]")
                            .attempted(true)
                            .build(),
                    CandidateResponse.builder()
                            .questionId(q2SingleMcqWrong)
                            .selectedOptionIds("[\"opt-C\"]")
                            .attempted(true)
                            .build(),
                    CandidateResponse.builder()
                            .questionId(q3MultiMcqPartial)
                            .selectedOptionIds("[\"opt-A\", \"opt-B\"]") // 2 of 4 correct -> +2.0 marks
                            .attempted(true)
                            .build(),
                    CandidateResponse.builder()
                            .questionId(q4MultiMcqWrong)
                            .selectedOptionIds("[\"opt-A\", \"opt-wrong\"]") // wrong option included -> -1.0
                            .attempted(true)
                            .build(),
                    CandidateResponse.builder()
                            .questionId(q5NumericalCorrect)
                            .enteredValue("42.0000001") // within 1e-6 epsilon -> +4.0
                            .attempted(true)
                            .build(),
                    CandidateResponse.builder()
                            .questionId(q6NumericalWrong)
                            .enteredValue("95.0") // outside epsilon -> -0.5
                            .attempted(true)
                            .build(),
                    CandidateResponse.builder()
                            .questionId(q7Unattempted)
                            .attempted(false)
                            .build(),
                    CandidateResponse.builder()
                            .questionId(q8ZeroNegativeScheme)
                            .selectedOptionIds("[\"opt-C\"]") // wrong but ZERO_NEGATIVE scheme -> 0.0
                            .attempted(true)
                            .build()
            );
            when(candidateResponseClient.getCandidateResponses(sessionId, tenantId))
                    .thenReturn(mockResponses);

            // Trigger session submitted event
            Map<String, Object> eventPayload = Map.of(
                    "eventType", "SESSION_SUBMITTED",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "paperId", paperId.toString(),
                    "examId", examId.toString(),
                    "tenantId", tenantId
            );
            sessionEventConsumer.onSpringSessionEvent(new GenericDomainEvent(
                    SessionEventConsumer.SESSION_EVENTS_TOPIC,
                    sessionId.toString(),
                    eventPayload
            ));

            // Verify clients were invoked with correct tenant context
            verify(answerKeyClient).getAnswerKeysForPaper(paperId, tenantId);
            verify(candidateResponseClient).getCandidateResponses(sessionId, tenantId);

            // Verify evaluations persisted to database
            List<Evaluation> savedEvaluations = evaluationRepository.findBySessionIdAndTenantId(sessionId, tenantId);
            assertThat(savedEvaluations).hasSize(8);
            assertThat(savedEvaluations).allMatch(e -> tenantId.equals(e.getTenantId()));
            assertThat(savedEvaluations).allMatch(e -> e.getStatus() == Evaluation.EvaluationStatus.AUTO_EVALUATED);

            Map<UUID, BigDecimal> scoresByQuestion = savedEvaluations.stream()
                    .collect(java.util.stream.Collectors.toMap(Evaluation::getQuestionId, Evaluation::getScore));

            assertThat(scoresByQuestion.get(q1SingleMcqCorrect)).isEqualByComparingTo("4.0");
            assertThat(scoresByQuestion.get(q2SingleMcqWrong)).isEqualByComparingTo("-1.0");
            assertThat(scoresByQuestion.get(q3MultiMcqPartial)).isEqualByComparingTo("2.0");
            assertThat(scoresByQuestion.get(q4MultiMcqWrong)).isEqualByComparingTo("-1.0");
            assertThat(scoresByQuestion.get(q5NumericalCorrect)).isEqualByComparingTo("4.0");
            assertThat(scoresByQuestion.get(q6NumericalWrong)).isEqualByComparingTo("-0.5");
            assertThat(scoresByQuestion.get(q7Unattempted)).isEqualByComparingTo("0.0");
            assertThat(scoresByQuestion.get(q8ZeroNegativeScheme)).isEqualByComparingTo("0.0");

            // Total: 4.0 - 1.0 + 2.0 - 1.0 + 4.0 - 0.5 + 0.0 + 0.0 = 7.50
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> completedEventCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher, atLeastOnce()).publish(eq("exam.evaluation.completed"), eq(sessionId.toString()), completedEventCaptor.capture());

            Map<String, Object> completedEvent = completedEventCaptor.getValue();
            assertThat(completedEvent.get("eventType")).isEqualTo("EVALUATION_COMPLETED");
            assertThat(completedEvent.get("sessionId")).isEqualTo(sessionId.toString());
            assertThat(completedEvent.get("candidateId")).isEqualTo(candidateId.toString());
            assertThat(completedEvent.get("examId")).isEqualTo(examId.toString());
            assertThat(completedEvent.get("tenantId")).isEqualTo(tenantId);
            assertThat(completedEvent.get("totalRawScore")).isEqualTo(new BigDecimal("7.50"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> questionLevelScores = (List<Map<String, Object>>) completedEvent.get("questionLevelScores");
            assertThat(questionLevelScores).hasSize(8);

            // Verify audit event emitted
            verify(eventPublisher, atLeastOnce()).publish(eq("exam.audit.events"), eq(sessionId.toString()), any());
        }
    }

    @Nested
    @DisplayName("2. Resilience & DLQ Fallback")
    class ResilienceAndDlqFallback {

        @Test
        @DisplayName("Routes to DLQ topic when question-bank-service gRPC is unavailable")
        void shouldRouteToDlqWhenQuestionBankUnavailable() {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();
            UUID examId = UUID.randomUUID();
            String tenantId = "tenant-resilience";

            when(candidateResponseClient.getCandidateResponses(sessionId, tenantId))
                    .thenReturn(Collections.emptyList());
            when(answerKeyClient.getAnswerKeysForPaper(paperId, tenantId))
                    .thenThrow(new UpstreamServiceUnavailableException("Connection to question-bank-service refused: 9083"));

            Map<String, Object> eventPayload = Map.of(
                    "eventType", "SESSION_SUBMITTED",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "paperId", paperId.toString(),
                    "examId", examId.toString(),
                    "tenantId", tenantId
            );
            sessionEventConsumer.onSpringSessionEvent(new GenericDomainEvent(
                    SessionEventConsumer.SESSION_EVENTS_TOPIC,
                    sessionId.toString(),
                    eventPayload
            ));

            // Verify no partial evaluations persisted
            List<Evaluation> evaluations = evaluationRepository.findBySessionIdAndTenantId(sessionId, tenantId);
            assertThat(evaluations).isEmpty();

            // Verify dead-letter event published to exam.evaluation.dlq
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dlqCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher).publish(eq(SessionEventConsumer.DLQ_TOPIC), eq(sessionId.toString()), dlqCaptor.capture());

            Map<String, Object> dlqEvent = dlqCaptor.getValue();
            assertThat(dlqEvent.get("eventType")).isEqualTo("EVALUATION_FAILED_DLQ");
            assertThat(dlqEvent.get("sessionId")).isEqualTo(sessionId.toString());
            assertThat(dlqEvent.get("candidateId")).isEqualTo(candidateId.toString());
            assertThat(dlqEvent.get("paperId")).isEqualTo(paperId.toString());
            assertThat(dlqEvent.get("examId")).isEqualTo(examId.toString());
            assertThat(dlqEvent.get("tenantId")).isEqualTo(tenantId);
            assertThat(dlqEvent.get("reason").toString()).contains("Connection to question-bank-service refused");
            assertThat(dlqEvent).containsKey("failedAt");
            assertThat(dlqEvent).containsKey("originalEvent");

            // Verify completed event was NEVER published
            verify(eventPublisher, never()).publish(eq("exam.evaluation.completed"), anyString(), any());
        }

        @Test
        @DisplayName("Routes to DLQ topic when response-service REST is unavailable")
        void shouldRouteToDlqWhenResponseServiceUnavailable() {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();
            String tenantId = "tenant-resilience-rest";

            when(candidateResponseClient.getCandidateResponses(sessionId, tenantId))
                    .thenThrow(new UpstreamServiceUnavailableException("Response-service 503 Service Unavailable"));

            Map<String, Object> eventPayload = Map.of(
                    "eventType", "SESSION_SUBMITTED",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "paperId", paperId.toString(),
                    "tenantId", tenantId
            );
            sessionEventConsumer.onSpringSessionEvent(new GenericDomainEvent(
                    SessionEventConsumer.SESSION_EVENTS_TOPIC,
                    sessionId.toString(),
                    eventPayload
            ));

            assertThat(evaluationRepository.findBySessionIdAndTenantId(sessionId, tenantId)).isEmpty();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dlqCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher).publish(eq(SessionEventConsumer.DLQ_TOPIC), eq(sessionId.toString()), dlqCaptor.capture());

            Map<String, Object> dlqEvent = dlqCaptor.getValue();
            assertThat(dlqEvent.get("eventType")).isEqualTo("EVALUATION_FAILED_DLQ");
            assertThat(dlqEvent.get("reason").toString()).contains("Response-service 503 Service Unavailable");
        }

        @Test
        @DisplayName("Routes to DLQ topic when answer keys return empty list")
        void shouldRouteToDlqWhenAnswerKeysEmpty() {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();

            when(candidateResponseClient.getCandidateResponses(sessionId, "default"))
                    .thenReturn(Collections.emptyList());
            when(answerKeyClient.getAnswerKeysForPaper(paperId, "default"))
                    .thenReturn(Collections.emptyList());

            Map<String, Object> eventPayload = Map.of(
                    "eventType", "SESSION_SUBMITTED",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "paperId", paperId.toString()
            );
            sessionEventConsumer.onSpringSessionEvent(new GenericDomainEvent(
                    SessionEventConsumer.SESSION_EVENTS_TOPIC,
                    sessionId.toString(),
                    eventPayload
            ));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> dlqCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher).publish(eq(SessionEventConsumer.DLQ_TOPIC), eq(sessionId.toString()), dlqCaptor.capture());

            Map<String, Object> dlqEvent = dlqCaptor.getValue();
            assertThat(dlqEvent.get("eventType")).isEqualTo("EVALUATION_FAILED_DLQ");
            assertThat(dlqEvent.get("reason")).isEqualTo("No answer keys found for session");
        }
    }

    @Nested
    @DisplayName("3. Multi-Tenancy Context Preservation")
    class MultiTenancyContextPreservation {

        @Test
        @DisplayName("Preserves custom tenant ID through client queries, database persistence, and completed event publication")
        void shouldPreserveCustomTenantContext() {
            UUID sessionId = UUID.randomUUID();
            UUID candidateId = UUID.randomUUID();
            UUID paperId = UUID.randomUUID();
            UUID examId = UUID.randomUUID();
            UUID questionId = UUID.randomUUID();
            String customTenantId = "tenant-medical-council-2026";

            when(answerKeyClient.getAnswerKeysForPaper(paperId, customTenantId))
                    .thenReturn(List.of(
                            AnswerKey.builder()
                                    .questionId(questionId)
                                    .questionType("SINGLE_MCQ")
                                    .correctAnswer("[\"opt-1\"]")
                                    .marksPerQuestion(2.0)
                                    .negativeMarks(0.5)
                                    .markingScheme(MarkingScheme.STANDARD)
                                    .build()
                    ));

            when(candidateResponseClient.getCandidateResponses(sessionId, customTenantId))
                    .thenReturn(List.of(
                            CandidateResponse.builder()
                                    .questionId(questionId)
                                    .selectedOptionIds("[\"opt-1\"]")
                                    .attempted(true)
                                    .build()
                    ));

            Map<String, Object> eventPayload = Map.of(
                    "eventType", "SESSION_SUBMITTED",
                    "sessionId", sessionId.toString(),
                    "candidateId", candidateId.toString(),
                    "paperId", paperId.toString(),
                    "examId", examId.toString(),
                    "tenantId", customTenantId
            );
            sessionEventConsumer.onSpringSessionEvent(new GenericDomainEvent(
                    SessionEventConsumer.SESSION_EVENTS_TOPIC,
                    sessionId.toString(),
                    eventPayload
            ));

            List<Evaluation> evals = evaluationRepository.findBySessionIdAndTenantId(sessionId, customTenantId);
            assertThat(evals).hasSize(1);
            assertThat(evals.get(0).getTenantId()).isEqualTo(customTenantId);
            assertThat(evals.get(0).getScore()).isEqualByComparingTo("2.0");

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> completedEventCaptor = ArgumentCaptor.forClass(Map.class);
            verify(eventPublisher, atLeastOnce()).publish(eq("exam.evaluation.completed"), eq(sessionId.toString()), completedEventCaptor.capture());

            Map<String, Object> completedEvent = completedEventCaptor.getValue();
            assertThat(completedEvent.get("tenantId")).isEqualTo(customTenantId);
        }
    }
}
