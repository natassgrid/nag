// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.practice.service;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeResponse;
import com.examplatform.practice.domain.PracticeSession;
import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.*;
import com.examplatform.practice.event.PracticeSessionCompletedEvent;
import com.examplatform.practice.exception.PracticeSessionNotFoundException;
import com.examplatform.practice.exception.PracticeSetNotFoundException;
import com.examplatform.practice.exception.SessionAlreadySubmittedException;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PracticeSessionServiceTest {

    @Mock
    private PracticeSessionRepository practiceSessionRepository;

    @Mock
    private PracticeSetRepository practiceSetRepository;

    @Mock
    private PracticeResponseRepository practiceResponseRepository;

    @Mock
    private PracticeEvaluationService practiceEvaluationService;

    @Mock
    private PracticeResultService practiceResultService;

    @Mock
    private QuestionBankClient questionBankClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ObjectMapper objectMapper;
    private PracticeSessionService practiceSessionService;

    private UUID candidateId;
    private UUID practiceSetId;
    private UUID sessionId;
    private PracticeSet practiceSet;
    private PracticeSession practiceSession;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        practiceSessionService = new PracticeSessionService(
                practiceSessionRepository,
                practiceSetRepository,
                practiceResponseRepository,
                practiceEvaluationService,
                practiceResultService,
                questionBankClient,
                eventPublisher,
                objectMapper
        );

        candidateId = UUID.randomUUID();
        practiceSetId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        practiceSet = PracticeSet.builder()
                .name("Calculus Practice")
                .durationMinutes(45)
                .totalQuestions(20)
                .published(true)
                .source("EXAM_CLONE")
                .build();
        ReflectionTestUtils.setField(practiceSet, "id", practiceSetId);

        practiceSession = PracticeSession.builder()
                .candidateId(candidateId)
                .practiceSetId(practiceSetId)
                .mode("TIMED")
                .status("IN_PROGRESS")
                .startedAt(Instant.now())
                .totalQuestions(20)
                .durationMinutes(45)
                .preferredLanguage("en")
                .build();
        ReflectionTestUtils.setField(practiceSession, "id", sessionId);
    }

    @Test
    @DisplayName("startSession - successfully starts practice session for published set")
    void testStartSessionSuccess() {
        StartSessionRequest req = new StartSessionRequest(practiceSetId, "TIMED", "hi");
        when(practiceSetRepository.findById(practiceSetId)).thenReturn(Optional.of(practiceSet));
        when(practiceSessionRepository.save(any(PracticeSession.class))).thenAnswer(invocation -> {
            PracticeSession ps = invocation.getArgument(0);
            ReflectionTestUtils.setField(ps, "id", sessionId);
            return ps;
        });

        PracticeSessionDto result = practiceSessionService.startSession(candidateId, req);

        assertThat(result).isNotNull();
        assertThat(result.practiceSetId()).isEqualTo(practiceSetId);
        assertThat(result.mode()).isEqualTo("TIMED");
        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.preferredLanguage()).isEqualTo("hi");
        verify(practiceSessionRepository).save(any(PracticeSession.class));
    }

    @Test
    @DisplayName("startSession - throws PracticeSetNotFoundException when set not found")
    void testStartSessionNotFound() {
        StartSessionRequest req = new StartSessionRequest(practiceSetId, "TIMED");
        when(practiceSetRepository.findById(practiceSetId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> practiceSessionService.startSession(candidateId, req))
                .isInstanceOf(PracticeSetNotFoundException.class);
    }

    @Test
    @DisplayName("saveResponse - saves question response and increments revision sequence")
    void testSaveResponse() {
        UUID questionId = UUID.randomUUID();
        SaveResponseRequest req = new SaveResponseRequest(
                questionId, "[\"opt-1\"]", null, 45000L, false
        );

        when(practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId))
                .thenReturn(Optional.of(practiceSession));
        when(practiceResponseRepository.findByPracticeSessionIdOrderByRevisionSequenceDesc(sessionId))
                .thenReturn(Collections.emptyList());

        practiceSessionService.saveResponse(sessionId, candidateId, req);

        verify(practiceResponseRepository).save(any(PracticeResponse.class));
    }

    @Test
    @DisplayName("submitSession - evaluates session, saves status SUBMITTED and publishes event")
    void testSubmitSessionSuccess() {
        practiceSession.setCorrectCount(15);
        practiceSession.setIncorrectCount(3);
        practiceSession.setSkippedCount(2);
        practiceSession.setObtainedMarks(57);
        practiceSession.setTotalMarks(80);
        practiceSession.setTopicWiseBreakdown("{}");

        when(practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId))
                .thenReturn(Optional.of(practiceSession));
        when(practiceSessionRepository.findById(sessionId))
                .thenReturn(Optional.of(practiceSession));

        PracticeResultDto mockResult = new PracticeResultDto(
                sessionId, 15, 3, 2, 57, 80, 75.0, "{}", "{}", "{}",
                Collections.emptyList(), "Calculus Practice", "TIMED"
        );
        when(practiceResultService.getResult(sessionId, candidateId)).thenReturn(mockResult);

        PracticeResultDto result = practiceSessionService.submitSession(sessionId, candidateId);

        assertThat(result).isNotNull();
        assertThat(result.correctCount()).isEqualTo(15);
        assertThat(result.obtainedMarks()).isEqualTo(57);
        verify(practiceEvaluationService).evaluateSession(sessionId);
        verify(eventPublisher).publishEvent(any(PracticeSessionCompletedEvent.class));
    }

    @Test
    @DisplayName("submitSession - throws SessionAlreadySubmittedException if already submitted")
    void testSubmitSessionAlreadySubmitted() {
        practiceSession.setStatus("SUBMITTED");
        when(practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId))
                .thenReturn(Optional.of(practiceSession));

        assertThatThrownBy(() -> practiceSessionService.submitSession(sessionId, candidateId))
                .isInstanceOf(SessionAlreadySubmittedException.class);
    }

    @Test
    @DisplayName("getSessionQuestions - resolves bilingual translation when available")
    void testGetSessionQuestionsBilingual() {
        UUID qId = UUID.randomUUID();
        practiceSet.setQuestionIds("[\"" + qId + "\"]");
        practiceSession.setPreferredLanguage("hi");

        when(practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId))
                .thenReturn(Optional.of(practiceSession));
        when(practiceSetRepository.findById(practiceSetId))
                .thenReturn(Optional.of(practiceSet));

        Map<String, Object> hiTranslation = new HashMap<>();
        hiTranslation.put("languageCode", "hi");
        hiTranslation.put("content", "कलन की परिभाषा क्या है?");

        Map<String, Map<String, Object>> transMap = new HashMap<>();
        transMap.put("hi", hiTranslation);

        AnswerKeyDto ak = new AnswerKeyDto(
                qId, "A", "SINGLE_MCQ", "top-1", "Calculus", "MEDIUM", 2,
                "What is calculus?", "[]", "Calculus is math.", "Math", transMap
        );
        when(questionBankClient.getAnswerKeys(List.of(qId)))
                .thenReturn(Map.of(qId, ak));

        List<PracticeQuestionDto> questions = practiceSessionService.getSessionQuestions(sessionId, candidateId);

        assertThat(questions).hasSize(1);
        PracticeQuestionDto q = questions.get(0);
        assertThat(q.primaryLanguage()).isEqualTo("hi");
        assertThat(q.fallbackToEnglish()).isFalse();
        assertThat(q.primaryTranslation()).isNotNull();
        assertThat(q.primaryTranslation().get("content")).isEqualTo("कलन की परिभाषा क्या है?");
        assertThat(q.content()).isEqualTo("What is calculus?");
    }

    @Test
    @DisplayName("getSessionQuestions - falls back to English when translation is unavailable")
    void testGetSessionQuestionsFallback() {
        UUID qId = UUID.randomUUID();
        practiceSet.setQuestionIds("[\"" + qId + "\"]");

        when(practiceSessionRepository.findByIdAndCandidateId(sessionId, candidateId))
                .thenReturn(Optional.of(practiceSession));
        when(practiceSetRepository.findById(practiceSetId))
                .thenReturn(Optional.of(practiceSet));

        AnswerKeyDto ak = new AnswerKeyDto(
                qId, "A", "SINGLE_MCQ", "top-1", "Calculus", "MEDIUM", 2,
                "What is calculus?", "[]", "Calculus is math.", "Math", null
        );
        when(questionBankClient.getAnswerKeys(List.of(qId)))
                .thenReturn(Map.of(qId, ak));

        List<PracticeQuestionDto> questions = practiceSessionService.getSessionQuestions(sessionId, candidateId, "ta");

        assertThat(questions).hasSize(1);
        PracticeQuestionDto q = questions.get(0);
        assertThat(q.primaryLanguage()).isEqualTo("ta");
        assertThat(q.fallbackToEnglish()).isTrue();
        assertThat(q.primaryTranslation()).isNull();
        assertThat(q.content()).isEqualTo("What is calculus?");
    }
}
