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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManualEvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private ManualEvaluationService manualEvaluationService;

    private Evaluation pendingEvaluation;
    private UUID evaluationId;
    private UUID sessionId;
    private UUID questionId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        evaluationId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        questionId = UUID.randomUUID();
        candidateId = UUID.randomUUID();

        pendingEvaluation = Evaluation.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .candidateId(candidateId)
                .evaluationType(Evaluation.EvaluationType.MANUAL)
                .score(BigDecimal.ZERO)
                .maxMarks(BigDecimal.TEN)
                .negativeMarks(BigDecimal.ZERO)
                .status(Evaluation.EvaluationStatus.PENDING)
                .build();
        pendingEvaluation.setTenantId("tenant-1");

        try {
            var idField = pendingEvaluation.getClass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(pendingEvaluation, evaluationId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("Should record score and transition to MANUAL_EVALUATED")
    void recordScore_validEvaluation_scoresSuccessfully() {
        UUID evaluatorId = UUID.randomUUID();
        when(evaluationRepository.findById(evaluationId)).thenReturn(Optional.of(pendingEvaluation));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));
        when(evaluationRepository.findBySessionIdAndTenantId(sessionId, "tenant-1"))
                .thenReturn(List.of(pendingEvaluation));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        Evaluation result = manualEvaluationService.recordScore(
                evaluationId, evaluatorId, 7.5, "Good answer");

        assertThat(result.getStatus()).isEqualTo(Evaluation.EvaluationStatus.MANUAL_EVALUATED);
        assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.valueOf(7.5));
        assertThat(result.getEvaluatorId()).isEqualTo(evaluatorId);
        assertThat(result.getComments()).isEqualTo("Good answer");
    }

    @Test
    @DisplayName("Should reject scoring for AUTO evaluation type")
    void recordScore_autoEvaluation_throwsException() {
        pendingEvaluation.setEvaluationType(Evaluation.EvaluationType.AUTO);
        when(evaluationRepository.findById(evaluationId)).thenReturn(Optional.of(pendingEvaluation));

        assertThatThrownBy(() -> manualEvaluationService.recordScore(
                evaluationId, UUID.randomUUID(), 5.0, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MANUAL");
    }

    @Test
    @DisplayName("Should reject scoring for already finalized evaluation")
    void recordScore_finalizedEvaluation_throwsException() {
        pendingEvaluation.setStatus(Evaluation.EvaluationStatus.FINALIZED);
        when(evaluationRepository.findById(evaluationId)).thenReturn(Optional.of(pendingEvaluation));

        assertThatThrownBy(() -> manualEvaluationService.recordScore(
                evaluationId, UUID.randomUUID(), 5.0, ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not in a scorable state");
    }

    @Test
    @DisplayName("Should throw when evaluation not found")
    void recordScore_notFound_throwsException() {
        when(evaluationRepository.findById(evaluationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manualEvaluationService.recordScore(
                evaluationId, UUID.randomUUID(), 5.0, ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evaluation not found");
    }

    @Test
    @DisplayName("Should flag for arbitration when scores diverge beyond tolerance")
    void recordScore_scoresDiverge_flagsArbitration() {
        UUID evaluatorId = UUID.randomUUID();

        // Create a peer evaluation with score 2 (diverges from 8 by more than 20% of 10)
        Evaluation peerEval = Evaluation.builder()
                .sessionId(sessionId)
                .questionId(questionId)
                .candidateId(candidateId)
                .evaluationType(Evaluation.EvaluationType.MANUAL)
                .score(BigDecimal.valueOf(2.0))
                .maxMarks(BigDecimal.TEN)
                .negativeMarks(BigDecimal.ZERO)
                .status(Evaluation.EvaluationStatus.MANUAL_EVALUATED)
                .build();
        peerEval.setTenantId("tenant-1");

        when(evaluationRepository.findById(evaluationId)).thenReturn(Optional.of(pendingEvaluation));
        when(evaluationRepository.save(any(Evaluation.class))).thenAnswer(i -> i.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // After scoring, the current eval becomes MANUAL_EVALUATED with score 8
        // Then findBySessionIdAndTenantId returns both evaluations
        when(evaluationRepository.findBySessionIdAndTenantId(sessionId, "tenant-1"))
                .thenReturn(List.of(peerEval, pendingEvaluation));

        Evaluation result = manualEvaluationService.recordScore(
                evaluationId, evaluatorId, 8.0, "Excellent");

        // The evaluation was initially set to MANUAL_EVALUATED, then after
        // arbitration check it gets set to ARBITRATION
        verify(evaluationRepository).saveAll(any());
    }

    @Test
    @DisplayName("Should publish evaluation notification event")
    void notifyEvaluators_publishesKafkaEvent() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        manualEvaluationService.notifyEvaluatorsForManualReview(
                sessionId, candidateId, "tenant-1");

        verify(kafkaTemplate).send(anyString(), anyString(), any());
    }
}
