package com.examplatform.delivery.service;

import com.examplatform.delivery.client.ShiftAssignmentClient;
import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.dto.SessionStartRequest;
import com.examplatform.delivery.dto.SessionStartResponse;
import com.examplatform.delivery.dto.ShiftAssignment;
import com.examplatform.delivery.exception.ConcurrentSessionException;
import com.examplatform.delivery.repository.ExamSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionStartService")
class SessionStartServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ShiftAssignmentClient shiftAssignmentClient;

    @Mock
    private VaultCryptoService vaultCryptoService;

    @Mock
    private DisabilityExtensionService disabilityExtensionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private SessionStartService sessionStartService;

    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID SHIFT_ID = UUID.randomUUID();
    private static final UUID PAPER_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-001";
    private static final String ENCRYPTED_PACKAGE = "vault:v1:encrypted-paper-content";
    private static final String DECRYPTED_PAPER = """
            {"questions": [{"content": "What is 2+2?", "options": ["3","4","5","6"]}, {"content": "What is 3+3?", "options": ["5","6","7","8"]}, {"content": "What is 4+4?", "options": ["7","8","9","10"]}]}""";

    @BeforeEach
    void setUp() {
        sessionStartService = new SessionStartService(
                examSessionRepository,
                shiftAssignmentClient,
                disabilityExtensionService,
                vaultCryptoService,
                redisTemplate,
                kafkaTemplate,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("Start session creates ACTIVE session and returns first question")
    void startSession_createsActiveSession_returnsFirstQuestion() {
        // Given
        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .languageCode("en")
                .build();

        ShiftAssignment assignment = ShiftAssignment.builder()
                .paperId(PAPER_ID)
                .encryptedPackageRef(ENCRYPTED_PACKAGE)
                .durationMinutes(180)
                .extraTimeMinutes(0)
                .build();

        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of());
        when(shiftAssignmentClient.getShiftAssignment(CANDIDATE_ID, EXAM_ID, SHIFT_ID, TENANT_ID))
                .thenReturn(assignment);
        when(vaultCryptoService.decrypt("shift-key-" + SHIFT_ID, ENCRYPTED_PACKAGE))
                .thenReturn(DECRYPTED_PAPER);
        when(examSessionRepository.save(any(ExamSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        SessionStartResponse response = sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getExamId()).isEqualTo(EXAM_ID);
        assertThat(response.getShiftId()).isEqualTo(SHIFT_ID);
        assertThat(response.getStartedAt()).isNotNull();
        assertThat(response.getScheduledEndAt()).isNotNull();
        assertThat(response.getFirstQuestionContent()).isEqualTo("What is 2+2?");
        assertThat(response.getTotalQuestions()).isEqualTo(3);
    }

    @Test
    @DisplayName("Concurrent session exists throws ConcurrentSessionException")
    void startSession_concurrentSessionExists_throwsConcurrentSessionException() {
        // Given
        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .languageCode("en")
                .build();

        ExamSession activeSession = ExamSession.builder()
                .sessionId(UUID.randomUUID())
                .candidateId(CANDIDATE_ID)
                .examId(UUID.randomUUID())
                .shiftId(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .status(ExamSessionStatus.ACTIVE)
                .build();

        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of(activeSession));

        // When / Then
        assertThatThrownBy(() -> sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID))
                .isInstanceOf(ConcurrentSessionException.class)
                .hasMessageContaining(CANDIDATE_ID.toString());

        // Verify no session was created
        verify(examSessionRepository, never()).save(any());
        verify(shiftAssignmentClient, never()).getShiftAssignment(any(), any(), any(), anyString());
    }

    @Test
    @DisplayName("Session saved to repository with correct fields")
    void startSession_savesSessionWithCorrectFields() {
        // Given
        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .languageCode("hi")
                .build();

        ShiftAssignment assignment = ShiftAssignment.builder()
                .paperId(PAPER_ID)
                .encryptedPackageRef(ENCRYPTED_PACKAGE)
                .durationMinutes(120)
                .extraTimeMinutes(30)
                .build();

        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of());
        when(shiftAssignmentClient.getShiftAssignment(CANDIDATE_ID, EXAM_ID, SHIFT_ID, TENANT_ID))
                .thenReturn(assignment);
        when(vaultCryptoService.decrypt("shift-key-" + SHIFT_ID, ENCRYPTED_PACKAGE))
                .thenReturn(DECRYPTED_PAPER);
        when(examSessionRepository.save(any(ExamSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then
        ArgumentCaptor<ExamSession> sessionCaptor = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(sessionCaptor.capture());

        ExamSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getCandidateId()).isEqualTo(CANDIDATE_ID);
        assertThat(savedSession.getExamId()).isEqualTo(EXAM_ID);
        assertThat(savedSession.getShiftId()).isEqualTo(SHIFT_ID);
        assertThat(savedSession.getPaperId()).isEqualTo(PAPER_ID);
        assertThat(savedSession.getStatus()).isEqualTo(ExamSessionStatus.ACTIVE);
        assertThat(savedSession.getLanguageCode()).isEqualTo("hi");
        assertThat(savedSession.getCurrentQuestionIndex()).isEqualTo(0);
        assertThat(savedSession.getFullScreenExitCount()).isEqualTo(0);
        assertThat(savedSession.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(savedSession.getStartedAt()).isNotNull();
        assertThat(savedSession.getScheduledEndAt()).isNotNull();
        // Duration should be 120 + 30 = 150 minutes
        long durationMinutes = Duration.between(savedSession.getStartedAt(), savedSession.getScheduledEndAt()).toMinutes();
        assertThat(durationMinutes).isEqualTo(150);
    }

    @Test
    @DisplayName("Single session enforcement — SUBMITTED sessions do not block new session")
    void startSession_submittedSessionExists_allowsNewSession() {
        // Given
        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .languageCode("en")
                .build();

        ExamSession submittedSession = ExamSession.builder()
                .sessionId(UUID.randomUUID())
                .candidateId(CANDIDATE_ID)
                .examId(UUID.randomUUID())
                .shiftId(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .status(ExamSessionStatus.SUBMITTED)
                .build();

        ShiftAssignment assignment = ShiftAssignment.builder()
                .paperId(PAPER_ID)
                .encryptedPackageRef(ENCRYPTED_PACKAGE)
                .durationMinutes(180)
                .extraTimeMinutes(0)
                .build();

        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of(submittedSession));
        when(shiftAssignmentClient.getShiftAssignment(CANDIDATE_ID, EXAM_ID, SHIFT_ID, TENANT_ID))
                .thenReturn(assignment);
        when(vaultCryptoService.decrypt("shift-key-" + SHIFT_ID, ENCRYPTED_PACKAGE))
                .thenReturn(DECRYPTED_PAPER);
        when(examSessionRepository.save(any(ExamSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        SessionStartResponse response = sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then — session was created successfully
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
        verify(examSessionRepository).save(any(ExamSession.class));
    }

    @Test
    @DisplayName("Session is cached in Redis after creation")
    void startSession_cachesSessionInRedis() {
        // Given
        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .languageCode("en")
                .build();

        ShiftAssignment assignment = ShiftAssignment.builder()
                .paperId(PAPER_ID)
                .encryptedPackageRef(ENCRYPTED_PACKAGE)
                .durationMinutes(180)
                .extraTimeMinutes(0)
                .build();

        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of());
        when(shiftAssignmentClient.getShiftAssignment(CANDIDATE_ID, EXAM_ID, SHIFT_ID, TENANT_ID))
                .thenReturn(assignment);
        when(vaultCryptoService.decrypt("shift-key-" + SHIFT_ID, ENCRYPTED_PACKAGE))
                .thenReturn(DECRYPTED_PAPER);
        when(examSessionRepository.save(any(ExamSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        SessionStartResponse response = sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then
        String expectedKey = "session:" + response.getSessionId();
        verify(valueOperations).set(eq(expectedKey), any(ExamSession.class), eq(Duration.ofHours(6)));
    }
}
