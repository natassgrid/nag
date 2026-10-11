// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeEvaluationServiceTest {

    @Mock
    private PracticeResponseRepository practiceResponseRepository;

    @Mock
    private PracticeSessionRepository practiceSessionRepository;

    @Mock
    private PracticeSetRepository practiceSetRepository;

    @Mock
    private QuestionBankClient questionBankClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PracticeEvaluationService practiceEvaluationService;

    private UUID sessionId;
    private PracticeSession practiceSession;

    @BeforeEach
    void setUp() {
        sessionId = UUID.randomUUID();
        practiceSession = PracticeSession.builder()
                .candidateId(UUID.randomUUID())
                .totalQuestions(3)
                .build();
        ReflectionTestUtils.setField(practiceSession, "id", sessionId);
    }

    @Test
    @DisplayName("evaluateSession - correctly evaluates correct, wrong, and skipped questions")
    void testEvaluateSession() {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();

        // q1: correct answer
        PracticeResponse resp1 = PracticeResponse.builder()
                .practiceSessionId(sessionId)
                .questionId(q1)
                .selectedOptionIds("[\"opt-A\"]")
                .revisionSequence(1)
                .timeSpentMs(20000L)
                .build();

        // q2: incorrect answer
        PracticeResponse resp2 = PracticeResponse.builder()
                .practiceSessionId(sessionId)
                .questionId(q2)
                .selectedOptionIds("[\"opt-B\"]")
                .revisionSequence(1)
                .timeSpentMs(30000L)
                .build();

        when(practiceSessionRepository.findById(sessionId)).thenReturn(Optional.of(practiceSession));
        when(practiceResponseRepository.findByPracticeSessionId(sessionId)).thenReturn(List.of(resp1, resp2));

        AnswerKeyDto ak1 = new AnswerKeyDto(q1, "opt-A", "MCQ", "t-1", "Algebra", "EASY", 4);
        AnswerKeyDto ak2 = new AnswerKeyDto(q2, "opt-C", "MCQ", "t-2", "Geometry", "HARD", 4);

        when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of(q1, ak1, q2, ak2));

        practiceEvaluationService.evaluateSession(sessionId);

        // 1 correct (4 marks), 1 wrong (-1 marks), 1 skipped (0 marks)
        // net obtained: 3 marks
        assertThat(practiceSession.getCorrectCount()).isEqualTo(1);
        assertThat(practiceSession.getIncorrectCount()).isEqualTo(1);
        assertThat(practiceSession.getSkippedCount()).isEqualTo(1); // 3 total - 2 answered = 1 skipped
        assertThat(practiceSession.getObtainedMarks()).isEqualTo(3);

        verify(practiceSessionRepository).save(practiceSession);
        verify(practiceResponseRepository, atLeastOnce()).save(any(PracticeResponse.class));
    }

    @Test
    @DisplayName("evaluateAnswer - evaluates MULTI_MCQ exact match and rejects partial/extra selections")
    void testEvaluateMultiMcq() {
        UUID qId = UUID.randomUUID();
        AnswerKeyDto ak = new AnswerKeyDto(qId, "[\"opt-A\", \"opt-B\"]", "MULTI_MCQ", "t-1", "Topic", "MEDIUM", 4);

        // Exact match in same order
        PracticeResponse respExact = PracticeResponse.builder().selectedOptionIds("[\"opt-A\", \"opt-B\"]").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respExact, ak)).isTrue();

        // Exact match in reversed order
        PracticeResponse respReversed = PracticeResponse.builder().selectedOptionIds("[\"opt-B\", \"opt-A\"]").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respReversed, ak)).isTrue();

        // Partial match (only A chosen) -> false
        PracticeResponse respPartial = PracticeResponse.builder().selectedOptionIds("[\"opt-A\"]").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respPartial, ak)).isFalse();

        // Extra option chosen (A, B, C) -> false
        PracticeResponse respExtra = PracticeResponse.builder().selectedOptionIds("[\"opt-A\", \"opt-B\", \"opt-C\"]").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respExtra, ak)).isFalse();
    }

    @Test
    @DisplayName("evaluateAnswer - rejects multiple selections for SINGLE_MCQ even if one matches")
    void testEvaluateSingleMcqMultipleSelectionRejected() {
        UUID qId = UUID.randomUUID();
        AnswerKeyDto ak = new AnswerKeyDto(qId, "opt-A", "SINGLE_MCQ", "t-1", "Topic", "EASY", 2);

        // Correct single selection
        PracticeResponse respSingle = PracticeResponse.builder().selectedOptionIds("[\"opt-A\"]").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respSingle, ak)).isTrue();

        // Candidate selects both opt-A and opt-B -> must be false
        PracticeResponse respMulti = PracticeResponse.builder().selectedOptionIds("[\"opt-A\", \"opt-B\"]").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respMulti, ak)).isFalse();
    }

    @Test
    @DisplayName("evaluateAnswer - evaluates NUMERICAL questions with floating point tolerance")
    void testEvaluateNumerical() {
        UUID qId = UUID.randomUUID();
        AnswerKeyDto ak = new AnswerKeyDto(qId, "3.14159", "NUMERICAL", "t-1", "Math", "MEDIUM", 2);

        // Exact string
        PracticeResponse resp1 = PracticeResponse.builder().enteredValue("3.14159").build();
        assertThat(practiceEvaluationService.evaluateAnswer(resp1, ak)).isTrue();

        // Close within epsilon (1e-6)
        PracticeResponse resp2 = PracticeResponse.builder().enteredValue("3.141590").build();
        assertThat(practiceEvaluationService.evaluateAnswer(resp2, ak)).isTrue();

        // Integer vs decimal representation (e.g. key: 42, entered: 42.0)
        AnswerKeyDto akInt = new AnswerKeyDto(qId, "42", "NUMERICAL", "t-1", "Math", "EASY", 2);
        PracticeResponse respInt = PracticeResponse.builder().enteredValue("42.0").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respInt, akInt)).isTrue();

        // Wrong numerical answer
        PracticeResponse respWrong = PracticeResponse.builder().enteredValue("43").build();
        assertThat(practiceEvaluationService.evaluateAnswer(respWrong, akInt)).isFalse();
    }

    @Test
    @DisplayName("evaluateSession - handles latest revision sequence when candidate updates response")
    void testRevisionSequenceHandling() {
        UUID q1 = UUID.randomUUID();

        // First answer: incorrect
        PracticeResponse rev1 = PracticeResponse.builder()
                .practiceSessionId(sessionId)
                .questionId(q1)
                .selectedOptionIds("[\"opt-Wrong\"]")
                .revisionSequence(1)
                .timeSpentMs(10000L)
                .build();

        // Second revision: correct
        PracticeResponse rev2 = PracticeResponse.builder()
                .practiceSessionId(sessionId)
                .questionId(q1)
                .selectedOptionIds("[\"opt-Right\"]")
                .revisionSequence(2)
                .timeSpentMs(25000L)
                .build();

        practiceSession.setTotalQuestions(1);

        when(practiceSessionRepository.findById(sessionId)).thenReturn(Optional.of(practiceSession));
        when(practiceResponseRepository.findByPracticeSessionId(sessionId)).thenReturn(List.of(rev1, rev2));

        AnswerKeyDto ak1 = new AnswerKeyDto(q1, "opt-Right", "MCQ", "t-1", "Topic", "EASY", 2);
        when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of(q1, ak1));

        practiceEvaluationService.evaluateSession(sessionId);

        // Only rev2 should count
        assertThat(practiceSession.getCorrectCount()).isEqualTo(1);
        assertThat(practiceSession.getIncorrectCount()).isEqualTo(0);
        assertThat(practiceSession.getObtainedMarks()).isEqualTo(2);
    }
}
