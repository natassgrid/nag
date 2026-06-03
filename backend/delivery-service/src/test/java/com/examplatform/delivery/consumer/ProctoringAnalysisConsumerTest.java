package com.examplatform.delivery.consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
    KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    ProctoringAnalysisConsumer proctoringAnalysisConsumer;

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

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        // Run analyze multiple times to trigger at least one detection (stub is random ~10%)
        // We call it enough times that statistically at least one detection triggers
        for (int i = 0; i < 50; i++) {
            proctoringAnalysisConsumer.analyze(event);
        }

        // Verify at least one audit event was published to the audit topic
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, atLeastOnce()).send(eq("exam.audit.events"), eq(sessionId), eventCaptor.capture());

        // Verify the event structure
        List<Object> allEvents = eventCaptor.getAllValues();
        assertThat(allEvents).isNotEmpty();

        Map<String, Object> publishedEvent = (Map<String, Object>) allEvents.get(0);
        assertThat(publishedEvent.get("sessionId")).isEqualTo(sessionId);
        assertThat(publishedEvent.get("candidateId")).isEqualTo(candidateId);
        assertThat(publishedEvent.get("source")).isEqualTo("ai-proctoring-analysis");
        assertThat(publishedEvent.get("occurredAt")).isNotNull();

        // Verify event type is one of the expected detection types
        String eventType = (String) publishedEvent.get("eventType");
        assertThat(eventType).isIn("no-face-detected", "multiple-faces-detected", "prohibited-object-detected");
    }
}
