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

package com.examplatform.evaluation.service;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.repository.EvaluationRepository;
import com.examplatform.shared.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScoreAggregationService.
 * Validates: SPEC-E2 (EVALUATION_COMPLETED Kafka event)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScoreAggregationService")
class ScoreAggregationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ScoreAggregationService service;

    private UUID sessionId;
    private UUID candidateId;
    private UUID examId;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        examId = UUID.randomUUID();
    }

    @Test
    @DisplayName("SPEC-E2-T1: After aggregation, EVALUATION_COMPLETED event published")
    void afterAggregation_evaluationCompletedEventPublished() {
        // Given
        Evaluation eval = buildEvaluation(4.0, 4.0);
        when(evaluationRepository.findBySessionIdAndTenantId(sessionId, "default"))
                .thenReturn(List.of(eval));
        doNothing().when(eventPublisher).publish(anyString(), anyString(), any());

        // When
        service.aggregateScores(sessionId, candidateId, examId, "default");

        // Then: at minimum 2 events published (SCORES_AGGREGATED + EVALUATION_COMPLETED)
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(eventPublisher, atLeastOnce()).publish(topicCaptor.capture(), anyString(), any());
        assertThat(topicCaptor.getAllValues())
                .contains("exam.evaluation.completed");
    }

    @Test
    @DisplayName("SPEC-E2-T3: Event payload includes questionLevelScores")
    void eventPayload_includesQuestionLevelScores() {
        // Given
        Evaluation eval = buildEvaluation(4.0, 4.0);
        when(evaluationRepository.findBySessionIdAndTenantId(sessionId, "default"))
                .thenReturn(List.of(eval));
        ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
        doNothing().when(eventPublisher).publish(anyString(), anyString(), payloadCaptor.capture());

        // When
        service.aggregateScores(sessionId, candidateId, examId, "default");

        // Then: find the EVALUATION_COMPLETED payload
        boolean found = payloadCaptor.getAllValues().stream()
                .anyMatch(p -> "EVALUATION_COMPLETED".equals(p.get("eventType"))
                        && p.containsKey("questionLevelScores"));
        assertThat(found).isTrue();
    }

    private Evaluation buildEvaluation(double score, double maxMarks) {
        Evaluation eval = new Evaluation();
        eval.setSessionId(sessionId);
        eval.setQuestionId(UUID.randomUUID());
        eval.setCandidateId(candidateId);
        eval.setEvaluationType(Evaluation.EvaluationType.AUTO);
        eval.setScore(BigDecimal.valueOf(score));
        eval.setMaxMarks(BigDecimal.valueOf(maxMarks));
        eval.setNegativeMarks(BigDecimal.valueOf(0.25));
        eval.setStatus(Evaluation.EvaluationStatus.AUTO_EVALUATED);
        return eval;
    }
}
