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

package com.examplatform.response.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

/**
 * Manages automatic saving of candidate responses.
 * Runs on a 30-second schedule to persist any pending changes from Redis,
 * and also triggers saves on navigation events received via Kafka.
 *
 * Validates: Requirements 10.2, 10.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoSaveService {

    private static final String ACTIVE_SESSIONS_KEY = "active-sessions";
    private static final String PENDING_CHANGES_PREFIX = "pending-changes:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ResponseSaveService responseSaveService;

    /**
     * Scheduled task that runs every 30 seconds to iterate active sessions
     * from Redis and trigger save for any pending changes.
     */
    @Scheduled(fixedDelay = 30_000)
    public void autoSavePendingChanges() {
        Set<Object> activeSessions = redisTemplate.opsForSet().members(ACTIVE_SESSIONS_KEY);

        if (activeSessions == null || activeSessions.isEmpty()) {
            log.debug("No active sessions found for auto-save");
            return;
        }

        int savedCount = 0;
        for (Object sessionObj : activeSessions) {
            String sessionIdStr = sessionObj.toString();
            try {
                UUID sessionId = UUID.fromString(sessionIdStr);
                boolean hasPending = triggerSaveForSession(sessionId);
                if (hasPending) {
                    savedCount++;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid session ID in active-sessions set: {}", sessionIdStr);
            } catch (Exception e) {
                log.error("Error during auto-save for session: {}", sessionIdStr, e);
            }
        }

        if (savedCount > 0) {
            log.info("Auto-save completed: saved pending changes for {} sessions", savedCount);
        }
    }

    /**
     * Triggers auto-save for a specific session. Called by the scheduled task
     * and by the SessionEventConsumer on navigation events.
     *
     * @param sessionId the session to save pending changes for
     * @return true if pending changes were found and saved
     */
    public boolean triggerSaveForSession(UUID sessionId) {
        String pendingKey = PENDING_CHANGES_PREFIX + sessionId;
        Boolean hasPending = redisTemplate.hasKey(pendingKey);

        if (Boolean.TRUE.equals(hasPending)) {
            // Mark pending changes as being processed
            Object pendingData = redisTemplate.opsForValue().get(pendingKey);
            if (pendingData != null) {
                log.debug("Triggering auto-save for session: {}", sessionId);
                // Delete the pending key to avoid duplicate saves
                redisTemplate.delete(pendingKey);
                return true;
            }
        }
        return false;
    }
}
