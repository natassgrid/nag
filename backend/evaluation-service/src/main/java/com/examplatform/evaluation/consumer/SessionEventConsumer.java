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

package com.examplatform.evaluation.consumer;

import com.examplatform.evaluation.dto.AnswerKey;
import com.examplatform.evaluation.dto.CandidateResponse;
import com.examplatform.evaluation.domain.Evaluation;
import com.examplatform.evaluation.service.AutoEvaluationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Kafka consumer that listens for session-submitted events on the
 * {@code exam.session.events} topic. Triggers auto-evaluation workflow
 * when a candidate submits their exam session.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventConsumer {

    private final AutoEvaluationService autoEvaluationService;
    private final ObjectMapper objectMapper;

    /**
     * Handles session events from Kafka. When a SESSION_SUBMITTED event is received,
     * triggers the auto-evaluation pipeline for objective questions.
     *
     * @param record the Kafka consumer record containing the event payload
     */
    @KafkaListener(topics = "exam.session.events", groupId = "evaluation-service")
    public void onSessionEvent(ConsumerRecord<String, String> record) {
        log.info("Received session event: key={}, partition={}", record.key(), record.partition());

        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.has("eventType") ? event.get("eventType").asText() : "";

            if (!"SESSION_SUBMITTED".equals(eventType)) {
                log.debug("Ignoring non-submission event: {}", eventType);
                return;
            }

            UUID sessionId = UUID.fromString(event.get("sessionId").asText());
            UUID candidateId = UUID.fromString(event.get("candidateId").asText());
            String tenantId = event.has("tenantId") ? event.get("tenantId").asText() : "default";

            // Fetch answer keys (from question-bank-service or local cache — stub for now)
            List<AnswerKey> answerKeys = fetchAnswerKeys(event);

            // Fetch final responses (from response-service — stub for now)
            List<CandidateResponse> responses = fetchCandidateResponses(event);

            if (answerKeys.isEmpty()) {
                log.warn("No answer keys found for session {}. Skipping auto-evaluation.", sessionId);
                return;
            }

            List<Evaluation> evaluations = autoEvaluationService.evaluateSession(
                    sessionId, candidateId, answerKeys, responses, tenantId);

            log.info("Auto-evaluation completed for session {}: {} evaluations created",
                    sessionId, evaluations.size());

        } catch (Exception e) {
            log.error("Failed to process session event: {}", e.getMessage(), e);
        }
    }

    /**
     * Fetches answer keys for the submitted session.
     * TODO: Replace with actual call to question-bank-service or paper definition lookup.
     */
    private List<AnswerKey> fetchAnswerKeys(JsonNode event) {
        if (event.has("answerKeys")) {
            try {
                return objectMapper.readValue(
                        event.get("answerKeys").toString(),
                        new TypeReference<List<AnswerKey>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse answer keys from event", e);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Fetches candidate responses for the submitted session.
     * TODO: Replace with actual call to response-service.
     */
    private List<CandidateResponse> fetchCandidateResponses(JsonNode event) {
        if (event.has("responses")) {
            try {
                return objectMapper.readValue(
                        event.get("responses").toString(),
                        new TypeReference<List<CandidateResponse>>() {});
            } catch (Exception e) {
                log.warn("Failed to parse candidate responses from event", e);
            }
        }
        return Collections.emptyList();
    }
}
