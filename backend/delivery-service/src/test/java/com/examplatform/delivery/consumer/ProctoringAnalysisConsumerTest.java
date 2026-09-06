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

package com.examplatform.delivery.consumer;

import com.examplatform.shared.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProctoringAnalysisConsumer}.
 *
 * Validates: Requirements 11.3, 11.4, 11.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProctoringAnalysisConsumer")
class ProctoringAnalysisConsumerTest {

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Random random;

    private ProctoringAnalysisConsumer proctoringAnalysisConsumer;

    @BeforeEach
    void setUp() {
        proctoringAnalysisConsumer = new ProctoringAnalysisConsumer(eventPublisher, random);
    }

    @Test
    @DisplayName("analyze processes event and publishes audit events on detection")
    @SuppressWarnings("unchecked")
    void publishesAuditEventsOnDetection() {
        String sessionId = UUID.randomUUID().toString();
        String candidateId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("sessionId", sessionId);
        event.put("candidateId", candidateId);
        event.put("snapshotRef", "snapshots/tenant/session/123456");

        // First call triggers detection (< 0.10), second is confidence (0.85 + x * 0.15), next 2 don't trigger
        when(random.nextDouble()).thenReturn(0.05, 0.50, 0.50, 0.50);

        proctoringAnalysisConsumer.analyze(event);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(1)).publish(eq("exam.audit.events"), eq(sessionId), eventCaptor.capture());

        List<Object> allEvents = eventCaptor.getAllValues();
        assertThat(allEvents).isNotEmpty();

        Map<String, Object> publishedEvent = (Map<String, Object>) allEvents.get(0);
        assertThat(publishedEvent.get("sessionId")).isEqualTo(sessionId);
        assertThat(publishedEvent.get("candidateId")).isEqualTo(candidateId);
        assertThat(publishedEvent.get("snapshotRef")).isEqualTo("snapshots/tenant/session/123456");
        assertThat(publishedEvent.get("source")).isEqualTo("ai-proctoring-analysis");
        assertThat(publishedEvent.get("occurredAt")).isNotNull();
        assertThat(publishedEvent.get("eventType")).isEqualTo("no-face-detected");
        assertThat((Double) publishedEvent.get("confidence")).isEqualTo(0.85 + 0.50 * 0.15);
    }

    @Test
    @DisplayName("analyze processes event and does not publish audit events when no detection")
    void noAuditEventsWhenBelowThreshold() {
        String sessionId = UUID.randomUUID().toString();
        String candidateId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("sessionId", sessionId);
        event.put("candidateId", candidateId);
        event.put("snapshotRef", "snapshots/tenant/session/123456");

        when(random.nextDouble()).thenReturn(0.50, 0.50, 0.50);

        proctoringAnalysisConsumer.analyze(event);

        verify(eventPublisher, never()).publish(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("analyzeRabbit processes event correctly")
    void analyzeRabbitProcessesEvent() {
        String sessionId = UUID.randomUUID().toString();
        String candidateId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("sessionId", sessionId);
        event.put("candidateId", candidateId);
        event.put("snapshotRef", "snapshots/tenant/session/123456");

        when(random.nextDouble()).thenReturn(0.05, 0.50, 0.50, 0.50);

        proctoringAnalysisConsumer.analyzeRabbit(event);

        verify(eventPublisher, times(1)).publish(eq("exam.audit.events"), eq(sessionId), any());
    }

    @Test
    @DisplayName("analyze handles exception during publish gracefully")
    void handlesPublishExceptionGracefully() {
        String sessionId = UUID.randomUUID().toString();
        String candidateId = UUID.randomUUID().toString();

        Map<String, Object> event = new HashMap<>();
        event.put("sessionId", sessionId);
        event.put("candidateId", candidateId);
        event.put("snapshotRef", "snapshots/tenant/session/123456");

        when(random.nextDouble()).thenReturn(0.05, 0.50, 0.50, 0.50);
        doThrow(new RuntimeException("Kafka error")).when(eventPublisher).publish(anyString(), anyString(), any());

        // Should not throw exception
        proctoringAnalysisConsumer.analyze(event);

        verify(eventPublisher, times(1)).publish(eq("exam.audit.events"), eq(sessionId), any());
    }
}
