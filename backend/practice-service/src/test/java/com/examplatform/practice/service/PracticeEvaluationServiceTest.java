// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
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
        // net obtained: 3 marks, total marks: 2 questions answered * 4 = 8
        assertThat(practiceSession.getCorrectCount()).isEqualTo(1);
        assertThat(practiceSession.getIncorrectCount()).isEqualTo(1);
        assertThat(practiceSession.getSkippedCount()).isEqualTo(1); // 3 total - 2 answered = 1 skipped
        assertThat(practiceSession.getObtainedMarks()).isEqualTo(3);

        verify(practiceSessionRepository).save(practiceSession);
        verify(practiceResponseRepository, atLeastOnce()).save(any(PracticeResponse.class));
    }
}
