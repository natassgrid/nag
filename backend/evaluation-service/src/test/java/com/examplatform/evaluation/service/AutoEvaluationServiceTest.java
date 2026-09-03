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
import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.repository.EvaluationRepository;
import com.examplatform.shared.config.DynamicConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoEvaluationService")
class AutoEvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private DynamicConfigService dynamicConfigService;

    @Captor
    private ArgumentCaptor<List<Evaluation>> evaluationsCaptor;

    private AutoEvaluationService service;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-1";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AutoEvaluationService(evaluationRepository, objectMapper, kafkaTemplate, dynamicConfigService);

        Mockito.lenient().when(dynamicConfigService.getBoolean(anyString(), anyString(), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(2));
        when(evaluationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Single MCQ Evaluation")
    class SingleMcqTests {

        @Test
        @DisplayName("correct answer -> positive marks")
        void correctSingleMcq_awardsPositiveMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("SINGLE_MCQ")
                    .correctAnswer("[\"opt-2\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-2\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
            assertThat(results.get(0).getStatus()).isEqualTo(Evaluation.EvaluationStatus.AUTO_EVALUATED);
        }

        @Test
        @DisplayName("incorrect answer -> negative marks deduction")
        void incorrectSingleMcq_deductsNegativeMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("SINGLE_MCQ")
                    .correctAnswer("[\"opt-2\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-3\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(-1.0));
        }

        @Test
        @DisplayName("unattempted single MCQ -> zero marks")
        void unattemptedSingleMcq_awardsZero() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("SINGLE_MCQ")
                    .correctAnswer("[\"opt-2\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .attempted(false)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Multi MCQ Evaluation")
    class MultiMcqTests {

        @Test
        @DisplayName("all correct options selected -> full positive marks")
        void allCorrectMultiMcq_awardsFullMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("MULTI_MCQ")
                    .correctAnswer("[\"opt-1\", \"opt-3\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-1\", \"opt-3\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        }

        @Test
        @DisplayName("partial correct chosen without incorrect -> proportional marks")
        void partialCorrectMultiMcq_awardsProportionalMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("MULTI_MCQ")
                    .correctAnswer("[\"opt-1\", \"opt-2\", \"opt-3\", \"opt-4\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            // Candidate selected 2 of 4 correct options
            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-1\", \"opt-2\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            // 2/4 * 4.0 = 2.0 marks
            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        }

        @Test
        @DisplayName("incorrect option included -> -2.0 negative marks")
        void incorrectOptionIncludedMultiMcq_deductsNegativeMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("MULTI_MCQ")
                    .correctAnswer("[\"opt-1\", \"opt-2\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-1\", \"opt-wrong\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(-2.0));
        }
    }

    @Nested
    @DisplayName("Numerical Evaluation")
    class NumericalTests {

        @Test
        @DisplayName("exact numeric answer -> positive marks")
        void exactNumerical_awardsPositiveMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("NUMERICAL")
                    .correctAnswer("3.14159")
                    .marksPerQuestion(4.0)
                    .negativeMarks(0.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .enteredValue("3.14159")
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        }

        @Test
        @DisplayName("answer within tolerance -> positive marks")
        void answerWithinTolerance_awardsPositiveMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("NUMERICAL")
                    .correctAnswer("10.0")
                    .marksPerQuestion(3.0)
                    .negativeMarks(0.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .enteredValue("10.00000001") // Within 1e-6 tolerance
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(3.0));
        }

        @Test
        @DisplayName("answer outside tolerance -> negative marks deduction")
        void answerOutsideTolerance_deductsNegativeMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("NUMERICAL")
                    .correctAnswer("10.0")
                    .marksPerQuestion(3.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse resp = CandidateResponse.builder()
                    .questionId(questionId)
                    .enteredValue("10.5")
                    .attempted(true)
                    .build();

            List<Evaluation> results = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(resp), TENANT_ID);

            assertThat(results.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(-1.0));
        }
    }
}
