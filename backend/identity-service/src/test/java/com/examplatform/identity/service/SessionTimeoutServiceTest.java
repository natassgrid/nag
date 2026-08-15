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

package com.examplatform.identity.service;

import com.examplatform.identity.domain.ActiveSession;
import com.examplatform.identity.repository.ActiveSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionTimeoutService")
class SessionTimeoutServiceTest {

    @Mock
    private ActiveSessionRepository activeSessionRepository;

    @InjectMocks
    private SessionTimeoutService sessionTimeoutService;

    @Test
    @DisplayName("deletes expired sessions when found")
    void deletesExpiredSessionsWhenFound() {
        ActiveSession expired1 = ActiveSession.builder()
                .userId(UUID.randomUUID())
                .sessionToken("token-1")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();
        ActiveSession expired2 = ActiveSession.builder()
                .userId(UUID.randomUUID())
                .sessionToken("token-2")
                .expiresAt(LocalDateTime.now().minusMinutes(10))
                .build();

        List<ActiveSession> expiredSessions = List.of(expired1, expired2);
        when(activeSessionRepository.findAllByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(expiredSessions);

        sessionTimeoutService.invalidateExpiredSessions();

        verify(activeSessionRepository).deleteAll(expiredSessions);
    }

    @Test
    @DisplayName("does nothing when no sessions are expired")
    void doesNothingWhenNoSessionsExpired() {
        when(activeSessionRepository.findAllByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        sessionTimeoutService.invalidateExpiredSessions();

        verify(activeSessionRepository, never()).deleteAll(any());
    }

    @Test
    @DisplayName("handles empty list gracefully")
    void handlesEmptyListGracefully() {
        when(activeSessionRepository.findAllByExpiresAtBefore(any(LocalDateTime.class)))
                .thenReturn(List.of());

        sessionTimeoutService.invalidateExpiredSessions();

        verify(activeSessionRepository, never()).deleteAll(any());
    }
}
