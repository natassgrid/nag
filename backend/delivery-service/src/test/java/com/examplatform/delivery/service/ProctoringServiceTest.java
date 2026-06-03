package com.examplatform.delivery.service;

import com.examplatform.delivery.config.ProctoringProperties;
import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.repository.ExamSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProctoringService}.
 *
 * Validates: Requirements 11.1, 11.2, 11.6, 11.7
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProctoringService")
class ProctoringServiceTest {

    @Mock
    ExamSessionRepository examSessionRepository;

    @Mock
    KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    ProctoringProperties proctoringProperties;

    @InjectMocks
    ProctoringService proctoringService;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final String TENANT_ID = "exam-authority-1";

    private ExamSession testSession;

    @BeforeEach
    void setUp() {
        testSession = ExamSession.builder()
                .sessionId(SESSION_ID)
                .candidateId(CANDIDATE_ID)
                .examId(UUID.randomUUID())
                .shiftId(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .status(ExamSession.ExamSessionStatus.ACTIVE)
                .startedAt(Instant.now())
                .scheduledEndAt(Instant.now().plusSeconds(7200))
                .currentQuestionIndex(0)
                .languageCode("en")
                .fullScreenExitCount(0)
                .build();
    }

    @Test
    @DisplayName("recordFullScreenExit increments exit count")
    void incrementsExitCount() {
        testSession.setFullScreenExitCount(1);

        when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(testSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proctoringProperties.getMaxFullScreenExits()).thenReturn(3);

        proctoringService.recordFullScreenExit(SESSION_ID);

        ArgumentCaptor<ExamSession> captor = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(captor.capture());
        assertThat(captor.getValue().getFullScreenExitCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("recordFullScreenExit flags session after 3 exits")
    @SuppressWarnings("unchecked")
    void flagsSessionAfterThreeExits() {
        testSession.setFullScreenExitCount(2); // Will become 3 after increment

        when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(testSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proctoringProperties.getMaxFullScreenExits()).thenReturn(3);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        proctoringService.recordFullScreenExit(SESSION_ID);

        // Verify audit event is published when threshold is reached
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("exam.audit.events"), eq(SESSION_ID.toString()), eventCaptor.capture());

        Map<String, Object> event = (Map<String, Object>) eventCaptor.getValue();
        assertThat(event.get("eventType")).isEqualTo("SESSION_FLAGGED_FULLSCREEN_EXITS");
        assertThat(event.get("sessionId")).isEqualTo(SESSION_ID.toString());
        assertThat(event.get("fullScreenExitCount")).isEqualTo(3);
    }

    @Test
    @DisplayName("recordFullScreenExit does not flag below threshold")
    void doesNotFlagBelowThreshold() {
        testSession.setFullScreenExitCount(0); // Will become 1 after increment

        when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(testSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(proctoringProperties.getMaxFullScreenExits()).thenReturn(3);

        proctoringService.recordFullScreenExit(SESSION_ID);

        // No Kafka publish for audit since we're below threshold
        verify(kafkaTemplate, never()).send(eq("exam.audit.events"), anyString(), any());
    }

    @Test
    @DisplayName("recordFullScreenExit throws when session not found")
    void throwsWhenSessionNotFound() {
        when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proctoringService.recordFullScreenExit(SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    @DisplayName("captureSnapshot publishes event to proctoring topic")
    @SuppressWarnings("unchecked")
    void captureSnapshotPublishesEvent() {
        byte[] imageData = new byte[]{1, 2, 3, 4, 5};

        when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(testSession));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        proctoringService.captureSnapshot(SESSION_ID, imageData, TENANT_ID);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("exam.proctoring.alerts"), eq(SESSION_ID.toString()), eventCaptor.capture());

        Map<String, Object> event = (Map<String, Object>) eventCaptor.getValue();
        assertThat(event.get("eventType")).isEqualTo("SNAPSHOT_CAPTURED");
        assertThat(event.get("sessionId")).isEqualTo(SESSION_ID.toString());
        assertThat(event.get("tenantId")).isEqualTo(TENANT_ID);
        assertThat(event.get("imageSize")).isEqualTo(5);
    }
}
