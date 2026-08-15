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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service that invalidates idle sessions whose {@code expiresAt}
 * timestamp has passed. Runs every 60 seconds.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SessionTimeoutService {

    private final ActiveSessionRepository activeSessionRepository;

    /**
     * Runs every 60 seconds. Finds and deletes all sessions whose expiresAt
     * is before the current time (i.e., idle sessions that have timed out).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void invalidateExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<ActiveSession> expired = activeSessionRepository.findAllByExpiresAtBefore(now);
        if (!expired.isEmpty()) {
            log.info("Invalidating {} expired sessions", expired.size());
            activeSessionRepository.deleteAll(expired);
        }
    }
}
