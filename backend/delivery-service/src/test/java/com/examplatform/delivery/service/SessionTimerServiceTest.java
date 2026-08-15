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

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import com.examplatform.delivery.repository.ExamSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SessionTimerService.
 * Validates: Requirements 9.3, 9.6, 9.8, 22.6
 */
@ExtendWith(MockitoExtension.class)
class SessionTimerServiceTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private SessionTimerService sessionTimerService;

    @Captor
    private ArgumentCaptor<ExamSession> sessionCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> eventCaptor;

    private ExamSession expiredSession;
    private ExamSession activeSession;

    @BeforeEach
    void setUp() {
        expiredSession = ExamSession.builder()
                .sessionId(UUID.randomUUID())
                .candidateId(UUID.randomUUID())
                .examId(UUID.randomUUID())
                .shiftId(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .status(ExamSessionStatus.ACTIVE)
                .startedAt(Instant.now().minus(Duration.ofHours(3)))
                .scheduledEndAt(Instant.now().minus(Duration.ofMinutes(5))) // past due
                .currentQuestionIndex(10)
                .languageCode("en")
                .fullScreenExitCount(0)
                .build();
        expiredSession.setTenantId("tenant-1");

        activeSession = ExamSession.builder()
                .sessionId(UUID.randomUUID())
                .candidateId(UUID.randomUUID())
                .examId(UUID.randomUUID())
                .shiftId(UUID.randomUUID())
                .paperId(UUID.randomUUID())
                .status(ExamSessionStatus.ACTIVE)
                .startedAt(Instant.now().minus(Duration.ofHours(1)))
                .scheduledEndAt(Instant.now().plus(Duration.ofHours(1))) // still active
                .currentQuestionIndex(5)
                .languageCode("en")
                .fullScreenExitCount(0)
                .build();
        activeSession.setTenantId("tenant-1");
    }

    @Test
    @DisplayName("Expired sessions get status=EXPIRED and Kafka event published")
    void expireOverdueSessions_setsExpiredAndPublishesEvent() {
        when(examSessionRepository.findByStatus(ExamSessionStatus.ACTIVE))
                .thenReturn(List.of(expiredSession));
        when(examSessionRepository.save(any(ExamSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        sessionTimerService.expireOverdueSessions();

        // Verify session was saved with EXPIRED status
        verify(examSessionRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo(ExamSessionStatus.EXPIRED);

        // Verify Kafka event was published
        verify(kafkaTemplate).send(eq("exam.session.events"),
                eq(expiredSession.getSessionId().toString()),
                eventCaptor.capture());
        Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("eventType")).isEqualTo("SESSION_EXPIRED");
        assertThat(event.get("sessionId")).isEqualTo(expiredSession.getSessionId().toString());
    }

    @Test
    @DisplayName("Active sessions that have not expired are not modified")
    void expireOverdueSessions_activeSessionNotExpired_noChange() {
        when(examSessionRepository.findByStatus(ExamSessionStatus.ACTIVE))
                .thenReturn(List.of(activeSession));

        sessionTimerService.expireOverdueSessions();

        verify(examSessionRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("getTimeRemaining returns positive duration for active session")
    void getTimeRemaining_activeSession_returnsPositive() {
        when(examSessionRepository.findBySessionId(activeSession.getSessionId()))
                .thenReturn(Optional.of(activeSession));

        Duration remaining = sessionTimerService.getTimeRemaining(activeSession.getSessionId());

        assertThat(remaining.getSeconds()).isPositive();
    }

    @Test
    @DisplayName("getTimeRemaining returns negative duration for expired session")
    void getTimeRemaining_expiredSession_returnsNegative() {
        when(examSessionRepository.findBySessionId(expiredSession.getSessionId()))
                .thenReturn(Optional.of(expiredSession));

        Duration remaining = sessionTimerService.getTimeRemaining(expiredSession.getSessionId());

        assertThat(remaining.getSeconds()).isNegative();
    }
}
