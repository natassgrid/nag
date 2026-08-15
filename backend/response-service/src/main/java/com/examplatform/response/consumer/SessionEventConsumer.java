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

package com.examplatform.response.consumer;

import com.examplatform.response.service.AutoSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer for session events.
 * Triggers auto-save on NAVIGATION events to ensure responses are persisted
 * when a candidate navigates between questions.
 *
 * Validates: Requirements 10.2, 10.3
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventConsumer {

    private static final String EVENT_TYPE_NAVIGATION = "NAVIGATION";

    private final AutoSaveService autoSaveService;

    /**
     * Listens to exam.session.events topic and triggers auto-save on navigation events.
     *
     * @param event the session event payload
     */
    @KafkaListener(topics = "exam.session.events", groupId = "response-service")
    public void handleSessionEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");

        if (EVENT_TYPE_NAVIGATION.equals(eventType)) {
            String sessionIdStr = (String) event.get("sessionId");
            if (sessionIdStr != null) {
                try {
                    UUID sessionId = UUID.fromString(sessionIdStr);
                    log.info("Navigation event received for session: {}, triggering auto-save", sessionId);
                    autoSaveService.triggerSaveForSession(sessionId);
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid session ID in navigation event: {}", sessionIdStr);
                }
            }
        }
    }
}
