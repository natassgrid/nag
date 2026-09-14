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

package com.examplatform.result.consumer;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateScoreInput;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.result.service.ResultComputationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for EvaluationCompletedConsumer.
 * Validates: SPEC-RS3
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationCompletedConsumer")
class EvaluationCompletedConsumerTest {

    @Mock
    private ResultComputationService resultComputationService;

    @Mock
    private ResultRepository resultRepository;

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
    @DisplayName("SPEC-RS3-T1: Valid EVALUATION_COMPLETED event → result computed")
    void validEvent_computesResult() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "sessionId", sessionId.toString(),
                "candidateId", candidateId.toString(),
                "examId", examId.toString(),
                "totalRawScore", 145.0,
                "sectionScores", Map.of("Physics", 55.0),
                "tenantId", "default",
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
                "tenantId", "default"
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
}
