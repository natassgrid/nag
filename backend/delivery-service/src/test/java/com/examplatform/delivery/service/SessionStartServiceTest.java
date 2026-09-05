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

package com.examplatform.delivery.service;

import com.examplatform.delivery.client.ShiftAssignmentClient;
import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.dto.QuestionDeliveryDto;
import com.examplatform.delivery.dto.QuestionOptionDeliveryDto;
import com.examplatform.delivery.dto.SessionStartRequest;
import com.examplatform.delivery.dto.SessionStartResponse;
import com.examplatform.delivery.dto.ShiftAssignment;
import com.examplatform.delivery.exception.ConcurrentSessionException;
import com.examplatform.delivery.repository.ExamSessionRepository;
import com.examplatform.shared.config.DynamicConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
    private ExamQuestionDeliveryService examQuestionDeliveryService;

    @Mock
    private DisabilityExtensionService disabilityExtensionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private DynamicConfigService dynamicConfigService;

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
                examQuestionDeliveryService,
                redisTemplate,
                kafkaTemplate,
                new ObjectMapper(),
                dynamicConfigService
        );

        Mockito.lenient().when(dynamicConfigService.getBoolean(anyString(), anyString(), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(2));
        Mockito.lenient().when(dynamicConfigService.getInt(anyString(), anyString(), anyInt()))
                .thenAnswer(inv -> inv.getArgument(2));
        Mockito.lenient().when(examQuestionDeliveryService.randomizeOptions(any(), any()))
                .thenAnswer(inv -> inv.getArgument(0));
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

        List<QuestionDeliveryDto> mockQuestions = List.of(
                QuestionDeliveryDto.builder()
                        .id(UUID.randomUUID().toString())
                        .text("What is 2+2?")
                        .options(List.of(
                                new QuestionOptionDeliveryDto(0, "3"),
                                new QuestionOptionDeliveryDto(1, "4")
                        ))
                        .marks(2.0)
                        .negativeMarks(0.5)
                        .build(),
                QuestionDeliveryDto.builder()
                        .id(UUID.randomUUID().toString())
                        .text("What is 3+3?")
                        .marks(2.0)
                        .negativeMarks(0.5)
                        .build(),
                QuestionDeliveryDto.builder()
                        .id(UUID.randomUUID().toString())
                        .text("What is 4+4?")
                        .marks(2.0)
                        .negativeMarks(0.5)
                        .build()
        );

        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of());
        when(shiftAssignmentClient.getShiftAssignment(CANDIDATE_ID, EXAM_ID, SHIFT_ID, TENANT_ID))
                .thenReturn(assignment);
        when(vaultCryptoService.decrypt("shift-key-" + SHIFT_ID, ENCRYPTED_PACKAGE))
                .thenReturn(DECRYPTED_PAPER);
        when(examQuestionDeliveryService.getDeliveryQuestions(eq(EXAM_ID), eq(PAPER_ID), eq(DECRYPTED_PAPER), eq(TENANT_ID)))
                .thenReturn(mockQuestions);
        when(disabilityExtensionService.getExtraTimeMinutes(CANDIDATE_ID, TENANT_ID))
                .thenReturn(0);
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When
        SessionStartResponse response = sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isNotNull();
        assertThat(response.getExamId()).isEqualTo(EXAM_ID);
        assertThat(response.getShiftId()).isEqualTo(SHIFT_ID);
        assertThat(response.getTotalQuestions()).isEqualTo(3);
        assertThat(response.getQuestions()).hasSize(3);
        assertThat(response.getFirstQuestionContent()).contains("What is 2+2?");
        assertThat(response.isKioskModeEnforced()).isTrue();
        assertThat(response.getHeartbeatIntervalSeconds()).isEqualTo(10);
        assertThat(response.getAutosaveIntervalSeconds()).isEqualTo(15);
        assertThat(response.getScheduledEndAt()).isAfter(response.getStartedAt());

        // Verify session saved in DB with ACTIVE status
        ArgumentCaptor<ExamSession> sessionCaptor = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(sessionCaptor.capture());
        ExamSession saved = sessionCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ExamSessionStatus.ACTIVE);
        assertThat(saved.getCandidateId()).isEqualTo(CANDIDATE_ID);
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);

        // Verify cached in Redis
        verify(valueOperations).set(eq("session:" + saved.getSessionId()), eq(saved), eq(Duration.ofHours(6)));
    }

    @Test
    @DisplayName("Start session with disability extension increases scheduledEndAt")
    void startSession_withDisabilityExtension_increasesDuration() {
        // Given
        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
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
        when(disabilityExtensionService.getExtraTimeMinutes(CANDIDATE_ID, TENANT_ID))
                .thenReturn(60); // 60 min disability extra
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        // When
        SessionStartResponse response = sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then: scheduled duration should be 180 + 60 = 240 minutes
        Duration duration = Duration.between(response.getStartedAt(), response.getScheduledEndAt());
        assertThat(duration.toMinutes()).isEqualTo(240);
    }

    @Test
    @DisplayName("Active session exists for the same exam resumes session seamlessly")
    void startSession_activeSessionExistsForSameExam_resumesSession() {
        // Given: candidate already has an ACTIVE session for the same exam
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant scheduledEnd = now.plus(Duration.ofMinutes(50));

        ExamSession activeSession = ExamSession.builder()
                .sessionId(sessionId)
                .candidateId(CANDIDATE_ID)
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .paperId(PAPER_ID)
                .status(ExamSessionStatus.ACTIVE)
                .startedAt(now.minus(Duration.ofMinutes(10)))
                .scheduledEndAt(scheduledEnd)
                .build();

        List<QuestionDeliveryDto> mockQuestions = List.of(
                QuestionDeliveryDto.builder()
                        .id(UUID.randomUUID().toString())
                        .text("Resumed Question 1")
                        .build()
        );

        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of(activeSession));
        when(examQuestionDeliveryService.getDeliveryQuestions(eq(EXAM_ID), eq(PAPER_ID), any(), eq(TENANT_ID)))
                .thenReturn(mockQuestions);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .build();

        // When
        SessionStartResponse response = sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(sessionId);
        assertThat(response.getExamId()).isEqualTo(EXAM_ID);
        assertThat(response.getQuestions()).hasSize(1);
    }

    @Test
    @DisplayName("Concurrent session for different exam throws ConcurrentSessionException")
    void startSession_activeSessionExistsForDifferentExam_throwsConcurrentSessionException() {
        // Given: candidate already has an ACTIVE session for a different exam
        UUID otherExamId = UUID.randomUUID();
        ExamSession activeSession = ExamSession.builder()
                .sessionId(UUID.randomUUID())
                .candidateId(CANDIDATE_ID)
                .examId(otherExamId)
                .status(ExamSessionStatus.ACTIVE)
                .scheduledEndAt(Instant.now().plus(Duration.ofHours(1)))
                .build();
        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of(activeSession));

        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .build();

        // When / Then
        assertThatThrownBy(() -> sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID))
                .isInstanceOf(ConcurrentSessionException.class)
                .hasMessageContaining("already has an active session");

        // Verify no decryption took place
        verify(vaultCryptoService, never()).decrypt(anyString(), anyString());
    }

    @Test
    @DisplayName("Completed/expired previous session allows new session")
    void startSession_completedPreviousSession_allowsNewSession() {
        // Given: candidate had a SUBMITTED session in the past
        ExamSession submittedSession = ExamSession.builder()
                .sessionId(UUID.randomUUID())
                .candidateId(CANDIDATE_ID)
                .status(ExamSessionStatus.SUBMITTED)
                .build();
        when(examSessionRepository.findByCandidateIdAndTenantId(CANDIDATE_ID, TENANT_ID))
                .thenReturn(List.of(submittedSession));

        ShiftAssignment assignment = ShiftAssignment.builder()
                .paperId(PAPER_ID)
                .encryptedPackageRef(ENCRYPTED_PACKAGE)
                .durationMinutes(120)
                .extraTimeMinutes(0)
                .build();

        when(shiftAssignmentClient.getShiftAssignment(CANDIDATE_ID, EXAM_ID, SHIFT_ID, TENANT_ID))
                .thenReturn(assignment);
        when(vaultCryptoService.decrypt("shift-key-" + SHIFT_ID, ENCRYPTED_PACKAGE))
                .thenReturn(DECRYPTED_PAPER);
        when(disabilityExtensionService.getExtraTimeMinutes(CANDIDATE_ID, TENANT_ID))
                .thenReturn(0);
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        SessionStartRequest request = SessionStartRequest.builder()
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .build();

        // When
        SessionStartResponse response = sessionStartService.startSession(request, CANDIDATE_ID, TENANT_ID);

        // Then
        assertThat(response).isNotNull();
        verify(examSessionRepository).save(any(ExamSession.class));
    }

    @Test
    @DisplayName("Resume session by ID succeeds for valid active session")
    void resumeSessionById_succeedsForActiveSession() {
        UUID sessionId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant scheduledEnd = now.plus(Duration.ofMinutes(45));

        ExamSession activeSession = ExamSession.builder()
                .sessionId(sessionId)
                .candidateId(CANDIDATE_ID)
                .examId(EXAM_ID)
                .shiftId(SHIFT_ID)
                .paperId(PAPER_ID)
                .status(ExamSessionStatus.ACTIVE)
                .startedAt(now.minus(Duration.ofMinutes(15)))
                .scheduledEndAt(scheduledEnd)
                .build();

        when(examSessionRepository.findById(sessionId)).thenReturn(Optional.of(activeSession));
        when(examQuestionDeliveryService.getDeliveryQuestions(eq(EXAM_ID), eq(PAPER_ID), any(), eq(TENANT_ID)))
                .thenReturn(List.of());
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        SessionStartResponse response = sessionStartService.resumeSessionById(sessionId, CANDIDATE_ID, TENANT_ID);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo(sessionId);
    }
}
