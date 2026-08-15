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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed rate limiter for authentication endpoints.
 * Uses an increment-and-TTL counter per IP address with a fixed 60-second window.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private static final String KEY_PREFIX = "rate:auth:ip:";
    private static final Duration WINDOW_DURATION = Duration.ofSeconds(60);

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Check whether a request from the given IP is within the allowed rate limit.
     * Increments the counter and sets a 60-second TTL on the first request in the window.
     *
     * @param ipAddress   the client IP address
     * @param maxAttempts maximum number of attempts allowed in the window
     * @return {@code true} if the request is within the limit, {@code false} if the limit is exceeded
     */
    public boolean isAllowed(String ipAddress, int maxAttempts) {
        String key = KEY_PREFIX + ipAddress;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                log.warn("Redis increment returned null for key [{}]; allowing request by default", key);
                return true;
            }
            if (count == 1L) {
                // First request in this window — set the expiry
                redisTemplate.expire(key, WINDOW_DURATION);
            }
            boolean allowed = count <= maxAttempts;
            if (!allowed) {
                log.debug("Rate limit exceeded for IP [{}]: count={}, max={}", ipAddress, count, maxAttempts);
            }
            return allowed;
        } catch (Exception e) {
            log.error("Rate limiter Redis error for IP [{}]: {}", ipAddress, e.getMessage(), e);
            // Fail open — allow the request to avoid blocking legitimate users on Redis failure
            return true;
        }
    }

    /**
     * Return the current request count for the given IP within the active window.
     *
     * @param ipAddress the client IP address
     * @return current counter value, or {@code 0} if no window is active or on Redis error
     */
    public long getCurrentCount(String ipAddress) {
        String key = KEY_PREFIX + ipAddress;
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return 0L;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.parseLong(value.toString());
        } catch (Exception e) {
            log.error("Rate limiter Redis read error for IP [{}]: {}", ipAddress, e.getMessage(), e);
            return 0L;
        }
    }
}
