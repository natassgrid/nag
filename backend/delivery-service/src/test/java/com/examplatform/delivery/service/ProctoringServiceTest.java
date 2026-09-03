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

import com.examplatform.delivery.config.ProctoringProperties;
import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.repository.ExamSessionRepository;
import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
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

    @Mock
    DynamicConfigService dynamicConfigService;

    @InjectMocks
    ProctoringService proctoringService;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final String TENANT_ID = "default";

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
                .scheduledEndAt(Instant.now().plusSeconds(3600))
                .currentQuestionIndex(0)
                .languageCode("en")
                .fullScreenExitCount(0)
                .build();
        testSession.setTenantId(TENANT_ID);

        Mockito.lenient().when(dynamicConfigService.getBoolean(eq("delivery.tamper.detection.enabled"), anyString(), anyBoolean()))
                .thenReturn(true);
    }

    // ─────────────────────────────────────────────────────────────
    // Requirement 11.1, 11.2: captureSnapshot
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("captureSnapshot")
    class CaptureSnapshot {

        @Test
        @DisplayName("Publishes SNAPSHOT_CAPTURED event with reference to Kafka")
        void capturesAndPublishesSnapshot() {
            byte[] imageData = new byte[]{1, 2, 3, 4};
            when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(testSession));
            when(kafkaTemplate.send(eq("exam.proctoring.alerts"), eq(SESSION_ID.toString()), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            proctoringService.captureSnapshot(SESSION_ID, imageData, TENANT_ID);

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(kafkaTemplate).send(eq("exam.proctoring.alerts"), eq(SESSION_ID.toString()), captor.capture());

            Map<String, Object> event = captor.getValue();
            assertThat(event.get("eventType")).isEqualTo("SNAPSHOT_CAPTURED");
            assertThat(event.get("sessionId")).isEqualTo(SESSION_ID.toString());
            assertThat(event.get("candidateId")).isEqualTo(CANDIDATE_ID.toString());
            assertThat(event.get("snapshotRef")).asString().startsWith("snapshots/" + TENANT_ID + "/" + SESSION_ID + "/");
            assertThat(event.get("tenantId")).isEqualTo(TENANT_ID);
            assertThat(event.get("imageSize")).isEqualTo(4);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for unknown session")
        void throwsOnUnknownSession() {
            when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> proctoringService.captureSnapshot(SESSION_ID, new byte[0], TENANT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session not found");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Requirement 11.6, 11.7: recordFullScreenExit
    // ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("recordFullScreenExit")
    class RecordFullScreenExit {

        @Test
        @DisplayName("Increments exit count and saves session")
        void incrementsExitCount() {
            when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(testSession));
            when(proctoringProperties.getMaxFullScreenExits()).thenReturn(3);

            proctoringService.recordFullScreenExit(SESSION_ID);

            assertThat(testSession.getFullScreenExitCount()).isEqualTo(1);
            verify(examSessionRepository).save(testSession);
            verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Publishes SESSION_FLAGGED_FULLSCREEN_EXITS audit event when threshold reached")
        void flagsSessionWhenThresholdReached() {
            testSession.setFullScreenExitCount(2); // will become 3 == threshold
            when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.of(testSession));
            when(proctoringProperties.getMaxFullScreenExits()).thenReturn(3);
            when(kafkaTemplate.send(eq("exam.audit.events"), eq(SESSION_ID.toString()), any()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            proctoringService.recordFullScreenExit(SESSION_ID);

            assertThat(testSession.getFullScreenExitCount()).isEqualTo(3);
            verify(examSessionRepository).save(testSession);

            ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
            verify(kafkaTemplate).send(eq("exam.audit.events"), eq(SESSION_ID.toString()), captor.capture());

            Map<String, Object> event = captor.getValue();
            assertThat(event.get("eventType")).isEqualTo("SESSION_FLAGGED_FULLSCREEN_EXITS");
            assertThat(event.get("sessionId")).isEqualTo(SESSION_ID.toString());
            assertThat(event.get("candidateId")).isEqualTo(CANDIDATE_ID.toString());
            assertThat(event.get("fullScreenExitCount")).isEqualTo(3);
            assertThat(event.get("threshold")).isEqualTo(3);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException for unknown session")
        void throwsOnUnknownSession() {
            when(examSessionRepository.findBySessionId(SESSION_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> proctoringService.recordFullScreenExit(SESSION_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Session not found");
        }
    }
}
