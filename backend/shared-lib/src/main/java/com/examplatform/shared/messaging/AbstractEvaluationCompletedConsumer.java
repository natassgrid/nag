/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.shared.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Base consumer contract and lifecycle handlers for evaluation-completed domain events.
 * Handles dispatching across Spring ApplicationEvent and RabbitMQ integrations,
 * and encapsulates payload validation and deserialization.
 *
 * @author Natassia Grid Development Team
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractEvaluationCompletedConsumer {

    public static final String EVALUATION_COMPLETED_TOPIC = "exam.evaluation.completed";
    public static final String EVALUATION_COMPLETED_EVENT_TYPE = "EVALUATION_COMPLETED";

    protected final ObjectMapper objectMapper;

    protected AbstractEvaluationCompletedConsumer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Common listener for in-process Spring GenericDomainEvents.
     */
    @EventListener
    public void onSpringEvaluationCompleted(GenericDomainEvent event) {
        log.debug("Dispatching evaluation completed via Spring event: key={}", event.key());
        MessagePayloadExtractor.handleSpringEvent(event, objectMapper, EVALUATION_COMPLETED_TOPIC,
                this::processEvaluationCompleted);
    }

    /**
     * Common listener processing for RabbitMQ AMQP messages.
     */
    public void onRabbitEvaluationCompleted(Object message) {
        log.debug("Dispatching evaluation completed via RabbitMQ: {}", message);
        MessagePayloadExtractor.handleRabbitEvent(message, objectMapper, EVALUATION_COMPLETED_TOPIC,
                payload -> processEvaluationCompleted(payload, null));
    }

    /**
     * Validates and parses the payload for EVALUATION_COMPLETED event type,
     * delegating to the consumer action if matching.
     *
     * @param payload the JSON event payload string
     * @param eventConsumer the consumer for the parsed event map
     */
    protected void handleEvaluationCompleted(String payload, Consumer<Map<String, Object>> eventConsumer) {
        try {
            var eventOpt = MessagePayloadExtractor.parseEventIfMatching(
                    payload, objectMapper, EVALUATION_COMPLETED_EVENT_TYPE);
            if (eventOpt.isEmpty()) {
                log.debug("Ignoring non-matching or invalid payload: {}", payload);
                return;
            }
            eventConsumer.accept(eventOpt.get());
        } catch (Exception e) {
            log.error("Failed to ingest {} event: payload={}", EVALUATION_COMPLETED_EVENT_TYPE, payload, e);
        }
    }

    /**
     * Subclass-specific evaluation completed processing logic.
     *
     * @param payload the raw payload
     * @param key the message key or partition key
     */
    public abstract void processEvaluationCompleted(String payload, String key);
}
