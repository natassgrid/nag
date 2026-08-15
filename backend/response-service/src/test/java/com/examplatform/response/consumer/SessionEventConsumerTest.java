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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for SessionEventConsumer.
 * Validates: Requirements 10.2, 10.3
 */
@ExtendWith(MockitoExtension.class)
class SessionEventConsumerTest {

    @Mock
    private AutoSaveService autoSaveService;

    @InjectMocks
    private SessionEventConsumer sessionEventConsumer;

    @Test
    @DisplayName("Navigation event triggers auto-save for session")
    void handleSessionEvent_navigationEvent_triggersAutoSave() {
        UUID sessionId = UUID.randomUUID();
        Map<String, Object> event = Map.of(
                "eventType", "NAVIGATION",
                "sessionId", sessionId.toString()
        );

        sessionEventConsumer.handleSessionEvent(event);

        verify(autoSaveService).triggerSaveForSession(sessionId);
    }

    @Test
    @DisplayName("Non-navigation event does not trigger auto-save")
    void handleSessionEvent_nonNavigationEvent_doesNotTrigger() {
        Map<String, Object> event = Map.of(
                "eventType", "SESSION_EXPIRED",
                "sessionId", UUID.randomUUID().toString()
        );

        sessionEventConsumer.handleSessionEvent(event);

        verify(autoSaveService, never()).triggerSaveForSession(any());
    }

    @Test
    @DisplayName("Event with missing sessionId is handled gracefully")
    void handleSessionEvent_missingSessionId_handlesGracefully() {
        Map<String, Object> event = Map.of(
                "eventType", "NAVIGATION"
        );

        // Should not throw
        sessionEventConsumer.handleSessionEvent(event);

        verify(autoSaveService, never()).triggerSaveForSession(any());
    }
}
