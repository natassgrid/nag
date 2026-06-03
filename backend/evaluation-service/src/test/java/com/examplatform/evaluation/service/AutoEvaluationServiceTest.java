package com.examplatform.evaluation.service;

import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.repository.EvaluationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutoEvaluationService")
class AutoEvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Captor
    private ArgumentCaptor<List<Evaluation>> evaluationsCaptor;

    private AutoEvaluationService service;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-1";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new AutoEvaluationService(evaluationRepository, objectMapper, kafkaTemplate);
        when(evaluationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    @DisplayName("Single MCQ Evaluation")
    class SingleMcqTests {

        @Test
        @DisplayName("correct answer → positive marks")
        void correctSingleMcq_awardsPositiveMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("SINGLE_MCQ")
                    .correctAnswer("[\"opt-2\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-2\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            Evaluation eval = result.get(0);
            assertThat(eval.getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
            assertThat(eval.getEvaluationType()).isEqualTo(Evaluation.EvaluationType.AUTO);
            assertThat(eval.getStatus()).isEqualTo(Evaluation.EvaluationStatus.AUTO_EVALUATED);
        }

        @Test
        @DisplayName("wrong answer → negative marks")
        void wrongSingleMcq_awardsNegativeMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("SINGLE_MCQ")
                    .correctAnswer("[\"opt-2\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-3\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(-1.0));
        }
    }

    @Nested
    @DisplayName("Multi MCQ Evaluation")
    class MultiMcqTests {

        @Test
        @DisplayName("fully correct answer → full marks")
        void correctMultiMcq_awardsFullMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("MULTI_MCQ")
                    .correctAnswer("[\"opt-1\",\"opt-3\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-3\",\"opt-1\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        }

        @Test
        @DisplayName("wrong answer (contains incorrect option) → zero marks (partial marking)")
        void wrongMultiMcq_awardsZeroMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("MULTI_MCQ")
                    .correctAnswer("[\"opt-1\",\"opt-3\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds("[\"opt-1\",\"opt-2\"]")
                    .attempted(true)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            // With partial marking: contains incorrect opt-2 → zero marks (not negative)
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Numerical Evaluation")
    class NumericalTests {

        @Test
        @DisplayName("exact correct numerical → positive marks")
        void correctNumerical_awardsPositiveMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("NUMERICAL")
                    .correctAnswer("3.14")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .enteredValue("3.14")
                    .attempted(true)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));
        }

        @Test
        @DisplayName("wrong numerical → negative marks")
        void wrongNumerical_awardsNegativeMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("NUMERICAL")
                    .correctAnswer("3.14")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .enteredValue("2.71")
                    .attempted(true)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(-1.0));
        }

        @Test
        @DisplayName("numerical within tolerance (0.0009 difference) → positive marks")
        void numericalWithinTolerance_awardsPositiveMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("NUMERICAL")
                    .correctAnswer("5.000")
                    .marksPerQuestion(2.0)
                    .negativeMarks(0.5)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .enteredValue("5.0009")
                    .attempted(true)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        }
    }

    @Nested
    @DisplayName("Unattempted Questions")
    class UnattemptedTests {

        @Test
        @DisplayName("unattempted question → zero marks")
        void unattemptedQuestion_awardsZeroMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("SINGLE_MCQ")
                    .correctAnswer("[\"opt-1\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            CandidateResponse response = CandidateResponse.builder()
                    .questionId(questionId)
                    .selectedOptionIds(null)
                    .attempted(false)
                    .build();

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(response), TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("question with no response in list → zero marks")
        void missingResponse_awardsZeroMarks() {
            UUID questionId = UUID.randomUUID();
            AnswerKey key = AnswerKey.builder()
                    .questionId(questionId)
                    .questionType("SINGLE_MCQ")
                    .correctAnswer("[\"opt-1\"]")
                    .marksPerQuestion(4.0)
                    .negativeMarks(1.0)
                    .build();

            // No response provided for this question
            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, List.of(key), List.of(), TENANT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Mixed Session Evaluation")
    class MixedSessionTests {

        @Test
        @DisplayName("mixed session produces correct evaluation count")
        void mixedSession_producesCorrectEvaluationCount() {
            UUID q1 = UUID.randomUUID();
            UUID q2 = UUID.randomUUID();
            UUID q3 = UUID.randomUUID();
            UUID q4 = UUID.randomUUID();

            List<AnswerKey> keys = List.of(
                    AnswerKey.builder().questionId(q1).questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-1\"]").marksPerQuestion(4.0).negativeMarks(1.0).build(),
                    AnswerKey.builder().questionId(q2).questionType("MULTI_MCQ")
                            .correctAnswer("[\"opt-a\",\"opt-b\"]").marksPerQuestion(4.0).negativeMarks(1.0).build(),
                    AnswerKey.builder().questionId(q3).questionType("NUMERICAL")
                            .correctAnswer("42").marksPerQuestion(4.0).negativeMarks(0.0).build(),
                    AnswerKey.builder().questionId(q4).questionType("SINGLE_MCQ")
                            .correctAnswer("[\"opt-2\"]").marksPerQuestion(4.0).negativeMarks(1.0).build()
            );

            List<CandidateResponse> responses = List.of(
                    CandidateResponse.builder().questionId(q1).selectedOptionIds("[\"opt-1\"]").attempted(true).build(),
                    CandidateResponse.builder().questionId(q2).selectedOptionIds("[\"opt-a\",\"opt-b\"]").attempted(true).build(),
                    CandidateResponse.builder().questionId(q3).enteredValue("42").attempted(true).build()
                    // q4 not attempted — no response entry
            );

            List<Evaluation> result = service.evaluateSession(
                    SESSION_ID, CANDIDATE_ID, keys, responses, TENANT_ID);

            verify(evaluationRepository).saveAll(evaluationsCaptor.capture());
            List<Evaluation> saved = evaluationsCaptor.getValue();

            assertThat(saved).hasSize(4);

            // q1: correct single MCQ → +4
            Evaluation e1 = saved.stream().filter(e -> e.getQuestionId().equals(q1)).findFirst().orElseThrow();
            assertThat(e1.getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));

            // q2: correct multi MCQ → +4
            Evaluation e2 = saved.stream().filter(e -> e.getQuestionId().equals(q2)).findFirst().orElseThrow();
            assertThat(e2.getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));

            // q3: correct numerical → +4
            Evaluation e3 = saved.stream().filter(e -> e.getQuestionId().equals(q3)).findFirst().orElseThrow();
            assertThat(e3.getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.0));

            // q4: unattempted → 0
            Evaluation e4 = saved.stream().filter(e -> e.getQuestionId().equals(q4)).findFirst().orElseThrow();
            assertThat(e4.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }
}
