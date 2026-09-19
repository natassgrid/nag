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
import com.examplatform.evaluation.repository.EvaluationRepository;
import com.examplatform.evaluation.support.AbstractIntegrationTest;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.messaging.GenericDomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    @DisplayName("End-to-end: Session submitted event triggers answer key & response fetching, persists evaluations, and emits EVALUATION_COMPLETED event")
    void shouldProcessSessionSubmissionEndToEnd() {
        UUID sessionId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID paperId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID question1 = UUID.randomUUID();
        UUID question2 = UUID.randomUUID();

        // Mock answer keys from question-bank-service
        List<AnswerKey> mockAnswerKeys = List.of(
                AnswerKey.builder()
                        .questionId(question1)
                        .questionType("SINGLE_MCQ")
                        .correctAnswer("[\"opt-A\"]")
                        .marksPerQuestion(4.0)
                        .negativeMarks(1.0)
                        .build(),
                AnswerKey.builder()
                        .questionId(question2)
                        .questionType("NUMERICAL")
                        .correctAnswer("42.0")
                        .marksPerQuestion(4.0)
                        .negativeMarks(0.0)
                        .build()
        );
        when(answerKeyClient.getAnswerKeysForPaper(paperId, "default"))
                .thenReturn(mockAnswerKeys);

        // Mock responses from response-service
        List<CandidateResponse> mockResponses = List.of(
                CandidateResponse.builder()
                        .questionId(question1)
                        .selectedOptionIds("[\"opt-A\"]")
                        .attempted(true)
                        .build(),
                CandidateResponse.builder()
                        .questionId(question2)
                        .enteredValue("42.0")
                        .attempted(true)
                        .build()
        );
        when(candidateResponseClient.getCandidateResponses(sessionId, "default"))
                .thenReturn(mockResponses);

        // Submit in-memory domain event (monolith/macro event bus)
        Map<String, Object> eventPayload = Map.of(
                "eventType", "SESSION_SUBMITTED",
                "sessionId", sessionId.toString(),
                "candidateId", candidateId.toString(),
                "paperId", paperId.toString(),
                "examId", examId.toString(),
                "tenantId", "default"
        );
        sessionEventConsumer.onSpringSessionEvent(new GenericDomainEvent(
                SessionEventConsumer.SESSION_EVENTS_TOPIC,
                sessionId.toString(),
                eventPayload
        ));

        // Verify evaluations saved in database
        List<Evaluation> savedEvaluations = evaluationRepository.findBySessionIdAndTenantId(sessionId, "default");
        assertThat(savedEvaluations).hasSize(2);
        assertThat(savedEvaluations).allMatch(e -> e.getStatus() == Evaluation.EvaluationStatus.AUTO_EVALUATED);
        assertThat(savedEvaluations).allMatch(e -> e.getScore().compareTo(BigDecimal.valueOf(4.0)) == 0);

        // Verify EVALUATION_COMPLETED published to exam.evaluation.completed
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> completedEventCaptor = ArgumentCaptor.forClass(Map.class);
        verify(eventPublisher).publish(eq("exam.evaluation.completed"), eq(sessionId.toString()), completedEventCaptor.capture());

        Map<String, Object> completedEvent = completedEventCaptor.getValue();
        assertThat(completedEvent.get("eventType")).isEqualTo("EVALUATION_COMPLETED");
        assertThat(completedEvent.get("sessionId")).isEqualTo(sessionId.toString());
        assertThat(completedEvent.get("candidateId")).isEqualTo(candidateId.toString());
        assertThat(completedEvent.get("examId")).isEqualTo(examId.toString());
        assertThat(completedEvent.get("totalRawScore")).isEqualTo(new BigDecimal("8.00"));
    }
}
