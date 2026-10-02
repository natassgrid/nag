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

package com.examplatform.analytics.consumer;

import com.examplatform.analytics.service.AnalyticsService;
import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EvaluationCompletedConsumer Unit Tests")
class EvaluationCompletedConsumerTest {

    @Mock
    private AnalyticsService analyticsService;

    private ObjectMapper objectMapper;
    private EvaluationCompletedConsumer consumer;

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        consumer = new EvaluationCompletedConsumer(analyticsService, objectMapper);
    }

    @Test
    @DisplayName("Kafka listener ingests valid EVALUATION_COMPLETED event and delegates to service")
    void onKafkaEvaluationCompleted_processesSuccessfully() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "examId", EXAM_ID.toString(),
                "candidateId", CANDIDATE_ID.toString(),
                "sessionId", SESSION_ID.toString(),
                "totalRawScore", 85.5,
                "sectionScores", Map.of("Physics", 40.0, "Chemistry", 45.5),
                "tenantId", "tenant-test",
                "evaluatedAt", Instant.now().toString()
        );
        String payload = objectMapper.writeValueAsString(event);

        consumer.onEvaluationCompleted(payload, SESSION_ID.toString());

        verify(analyticsService).processEvaluationCompleted(
                eq(EXAM_ID),
                eq(CANDIDATE_ID),
                eq(SESSION_ID),
                eq(85.5),
                eq(Map.of("Physics", 40.0, "Chemistry", 45.5)),
                any(),
                eq("tenant-test"),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("RabbitMQ listener ingests AMQP Message and delegates to service")
    void onRabbitEvaluationCompleted_processesAmqpMessage() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "examId", EXAM_ID.toString(),
                "candidateId", CANDIDATE_ID.toString(),
                "sessionId", SESSION_ID.toString(),
                "totalRawScore", 92.0,
                "tenantId", "tenant-1"
        );
        byte[] body = objectMapper.writeValueAsBytes(event);
        Message amqpMessage = new Message(body);

        consumer.onRabbitEvaluationCompleted(amqpMessage);

        verify(analyticsService).processEvaluationCompleted(
                eq(EXAM_ID),
                eq(CANDIDATE_ID),
                eq(SESSION_ID),
                eq(92.0),
                any(),
                any(),
                eq("tenant-1"),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Spring event listener processes GenericDomainEvent")
    void onSpringEvaluationCompleted_processesDomainEvent() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "EVALUATION_COMPLETED",
                "examId", EXAM_ID.toString(),
                "candidateId", CANDIDATE_ID.toString(),
                "sessionId", SESSION_ID.toString(),
                "totalRawScore", 78.0,
                "tenantId", "default"
        );
        GenericDomainEvent domainEvent = new GenericDomainEvent(
                "exam.evaluation.completed",
                SESSION_ID.toString(),
                objectMapper.writeValueAsString(event)
        );

        consumer.onSpringEvaluationCompleted(domainEvent);

        verify(analyticsService).processEvaluationCompleted(
                eq(EXAM_ID),
                eq(CANDIDATE_ID),
                eq(SESSION_ID),
                eq(78.0),
                any(),
                any(),
                eq("default"),
                any(Instant.class)
        );
    }

    @Test
    @DisplayName("Spring listener ignores non-evaluation domain event topics")
    void onSpringEvaluationCompleted_ignoresOtherTopics() {
        GenericDomainEvent domainEvent = new GenericDomainEvent(
                "exam.session.events",
                "key-1",
                "{}"
        );

        consumer.onSpringEvaluationCompleted(domainEvent);

        verify(analyticsService, never()).processEvaluationCompleted(any(), any(), any(), any(Double.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Ignores non-EVALUATION_COMPLETED event types")
    void ignoresNonEvaluationCompletedEvents() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "SESSION_STARTED",
                "examId", EXAM_ID.toString()
        );
        String payload = objectMapper.writeValueAsString(event);

        consumer.processEvaluationCompleted(payload, "key");

        verify(analyticsService, never()).processEvaluationCompleted(any(), any(), any(), any(Double.class), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Handles missing examId or candidateId gracefully")
    void handlesMissingIds() throws Exception {
        Map<String, Object> event = Map.of(
                "eventType", "EVALUATION_COMPLETED"
        );
        String payload = objectMapper.writeValueAsString(event);

        consumer.processEvaluationCompleted(payload, "key");

        verify(analyticsService, never()).processEvaluationCompleted(any(), any(), any(), any(Double.class), any(), any(), any(), any());
    }
}
