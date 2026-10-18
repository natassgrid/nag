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

package com.examplatform.result.consumer;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateScoreInput;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.result.service.QuestionAnalyticsService;
import com.examplatform.result.service.ResultComputationService;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EvaluationCompletedConsumer.
 * Validates: SPEC-RS3, Issue #111
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationCompletedConsumer")
class EvaluationCompletedConsumerTest {

    @Mock
    private ResultComputationService resultComputationService;

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private QuestionAnalyticsService questionAnalyticsService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private EvaluationCompletedConsumer consumer;

    private UUID candidateId;
    private UUID examId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        candidateId = UUID.randomUUID();
        examId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
    }

    @Test
    @DisplayName("SPEC-RS3-T1: Valid EVALUATION_COMPLETED event via Kafka → result computed and cache invalidated")
    void validEvent_computesResult() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "sessionId", sessionId.toString(),
                "candidateId", candidateId.toString(),
                "examId", examId.toString(),
                "totalRawScore", 145.0,
                "sectionScores", Map.of("Physics", 55.0),
            SharedConstants.TENANT_ID, "default",
                "questionLevelScores", List.of()
        ));

        Result mockResult = Result.builder()
                .candidateId(candidateId)
                .examId(examId)
                .totalScore(BigDecimal.valueOf(145.0))
                .digiLockerPushed(false)
                .build();
        mockResult.setTenantId("default");

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, "default"))
                .thenReturn(Optional.empty());
        when(resultComputationService.computeResults(eq(examId), any(), anyBoolean(), eq("default")))
                .thenReturn(List.of(mockResult));

        consumer.onEvaluationCompleted(payload, sessionId.toString());

        verify(resultComputationService).computeResults(eq(examId), any(), anyBoolean(), eq("default"));
        verify(questionAnalyticsService).invalidateCache(examId);
    }

    @Test
    @DisplayName("SPEC-RS3-T1b: Valid EVALUATION_COMPLETED event via RabbitMQ → result computed")
    void validRabbitEvent_computesResult() throws Exception {
        Map<String, Object> eventMap = Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "sessionId", sessionId.toString(),
                "candidateId", candidateId.toString(),
                "examId", examId.toString(),
                "totalRawScore", 145.0,
                "sectionScores", Map.of("Physics", 55.0),
            SharedConstants.TENANT_ID, "default",
                "questionLevelScores", List.of()
        );

        Result mockResult = Result.builder()
                .candidateId(candidateId)
                .examId(examId)
                .totalScore(BigDecimal.valueOf(145.0))
                .digiLockerPushed(false)
                .build();
        mockResult.setTenantId("default");

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, "default"))
                .thenReturn(Optional.empty());
        when(resultComputationService.computeResults(eq(examId), any(), anyBoolean(), eq("default")))
                .thenReturn(List.of(mockResult));

        consumer.onRabbitEvaluationCompleted(eventMap);

        verify(resultComputationService).computeResults(eq(examId), any(), anyBoolean(), eq("default"));
        verify(questionAnalyticsService).invalidateCache(examId);
    }

    @Test
    @DisplayName("SPEC-RS3-T1c: Valid EVALUATION_COMPLETED event via Spring in-memory → result computed")
    void validSpringEvent_computesResult() throws Exception {
        Map<String, Object> eventMap = Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "sessionId", sessionId.toString(),
                "candidateId", candidateId.toString(),
                "examId", examId.toString(),
                "totalRawScore", 145.0,
                "sectionScores", Map.of("Physics", 55.0),
            SharedConstants.TENANT_ID, "default",
                "questionLevelScores", List.of()
        );

        GenericDomainEvent domainEvent = new GenericDomainEvent(
                EvaluationCompletedConsumer.EVALUATION_COMPLETED_TOPIC,
                sessionId.toString(),
                eventMap
        );

        Result mockResult = Result.builder()
                .candidateId(candidateId)
                .examId(examId)
                .totalScore(BigDecimal.valueOf(145.0))
                .digiLockerPushed(false)
                .build();
        mockResult.setTenantId("default");

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, "default"))
                .thenReturn(Optional.empty());
        when(resultComputationService.computeResults(eq(examId), any(), anyBoolean(), eq("default")))
                .thenReturn(List.of(mockResult));

        consumer.onSpringEvaluationCompleted(domainEvent);

        verify(resultComputationService).computeResults(eq(examId), any(), anyBoolean(), eq("default"));
        verify(questionAnalyticsService).invalidateCache(examId);
    }

    @Test
    @DisplayName("SPEC-RS3-T2: Duplicate event (result already exists) → skipped")
    void duplicateEvent_isSkipped() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "sessionId", sessionId.toString(),
                "candidateId", candidateId.toString(),
                "examId", examId.toString(),
                "totalRawScore", 145.0,
            SharedConstants.TENANT_ID, "default"
        ));

        Result existingResult = Result.builder().candidateId(candidateId).examId(examId)
                .totalScore(BigDecimal.valueOf(145.0)).digiLockerPushed(false).build();
        existingResult.setTenantId("default");

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, "default"))
                .thenReturn(Optional.of(existingResult));

        consumer.onEvaluationCompleted(payload, sessionId.toString());

        verify(resultComputationService, never()).computeResults(any(), any(), anyBoolean(), anyString());
    }

    @Test
    @DisplayName("SPEC-RS3-T3: Malformed JSON payload → no exception thrown")
    void malformedPayload_handledGracefully() {
        // Should not throw — consumer must not crash on bad messages
        consumer.onEvaluationCompleted("not-valid-json", "some-key");
    }

    @Test
    @DisplayName("SPEC-RS3-T4: Event with section and taxonomy breakdowns passes them to CandidateScoreInput")
    void eventWithBreakdowns_propagatesToScoreInput() throws Exception {
        UUID secId = UUID.randomUUID();
        Map<String, Object> secMap = Map.of(
                "sectionId", secId.toString(),
                "sectionName", "Chemistry Section 1",
                "subjectId", 1003L,
                "subject", "Chemistry",
                "score", 30.0,
                "maxMarks", 40.0,
                "accuracyRate", 75.0,
                "questionCount", 10,
                "attemptedCount", 8,
                "correctCount", 6
        );

        Map<String, Object> subMap = Map.ofEntries(
                Map.entry("subjectId", 1003L),
                Map.entry("subject", "Chemistry"),
                Map.entry("topicId", 2003L),
                Map.entry("topic", "Organic Chemistry"),
                Map.entry("subtopicId", 3003L),
                Map.entry("subtopic", "Aldehydes & Ketones"),
                Map.entry("score", 12.0),
                Map.entry("maxMarks", 16.0),
                Map.entry("accuracyRate", 75.0),
                Map.entry("questionCount", 4),
                Map.entry("attemptedCount", 4),
                Map.entry("correctCount", 3),
                Map.entry("totalTimeSpentMs", 120000L)
        );

        String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "sessionId", sessionId.toString(),
                "candidateId", candidateId.toString(),
                "examId", examId.toString(),
                "totalRawScore", 30.0,
                "sectionScores", Map.of("Chemistry Section 1", 30.0),
                "sectionBreakdowns", List.of(secMap),
                "subtopicBreakdowns", List.of(subMap),
                SharedConstants.TENANT_ID, "default",
                "questionLevelScores", List.of()
        ));

        Result mockResult = Result.builder()
                .candidateId(candidateId)
                .examId(examId)
                .totalScore(BigDecimal.valueOf(30.0))
                .digiLockerPushed(false)
                .build();
        mockResult.setTenantId("default");

        when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, "default"))
                .thenReturn(Optional.empty());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CandidateScoreInput>> captor = ArgumentCaptor.forClass(List.class);
        when(resultComputationService.computeResults(eq(examId), captor.capture(), anyBoolean(), eq("default")))
                .thenReturn(List.of(mockResult));

        consumer.onEvaluationCompleted(payload, sessionId.toString());

        List<CandidateScoreInput> capturedInputs = captor.getValue();
        assertThat(capturedInputs).hasSize(1);
        CandidateScoreInput input = capturedInputs.get(0);

        assertThat(input.getSectionBreakdowns()).hasSize(1);
        assertThat(input.getSectionBreakdowns().get(0).getSectionId()).isEqualTo(secId);
        assertThat(input.getSectionBreakdowns().get(0).getSubjectId()).isEqualTo(1003L);

        assertThat(input.getSubtopicBreakdowns()).hasSize(1);
        assertThat(input.getSubtopicBreakdowns().get(0).getSubtopicId()).isEqualTo(3003L);
        assertThat(input.getSubtopicBreakdowns().get(0).getSubtopic()).isEqualTo("Aldehydes & Ketones");
    }
}
