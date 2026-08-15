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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterService")
class RateLimiterServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    private static final String TEST_IP = "192.168.1.1";
    private static final int MAX_ATTEMPTS = 10;

    @Nested
    @DisplayName("isAllowed()")
    class IsAllowed {

        @Test
        @DisplayName("returns true when count is below max")
        void returnsTrueWhenCountIsBelowMax() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment("rate:auth:ip:" + TEST_IP)).thenReturn(5L);

            boolean result = rateLimiterService.isAllowed(TEST_IP, MAX_ATTEMPTS);

            assertAll(
                () -> assertThat(result).isTrue()
            );
        }

        @Test
        @DisplayName("returns false when count equals max + 1")
        void returnsFalseWhenCountExceedsMax() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment("rate:auth:ip:" + TEST_IP)).thenReturn((long) MAX_ATTEMPTS + 1);

            boolean result = rateLimiterService.isAllowed(TEST_IP, MAX_ATTEMPTS);

            assertAll(
                () -> assertThat(result).isFalse()
            );
        }

        @Test
        @DisplayName("uses the correct key prefix 'rate:auth:ip:'")
        void usesCorrectKeyPrefix() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            when(valueOperations.increment(keyCaptor.capture())).thenReturn(3L);

            rateLimiterService.isAllowed(TEST_IP, MAX_ATTEMPTS);

            assertAll(
                () -> assertThat(keyCaptor.getValue()).isEqualTo("rate:auth:ip:" + TEST_IP),
                () -> assertThat(keyCaptor.getValue()).startsWith("rate:auth:ip:")
            );
        }

        @Test
        @DisplayName("sets TTL on first request (count == 1)")
        void setsTtlOnFirstRequest() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment("rate:auth:ip:" + TEST_IP)).thenReturn(1L);

            rateLimiterService.isAllowed(TEST_IP, MAX_ATTEMPTS);

            verify(redisTemplate).expire(eq("rate:auth:ip:" + TEST_IP), eq(Duration.ofSeconds(60)));
        }

        @Test
        @DisplayName("does NOT set TTL on subsequent requests (count > 1)")
        void doesNotSetTtlOnSubsequentRequests() {
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.increment("rate:auth:ip:" + TEST_IP)).thenReturn(2L);

            rateLimiterService.isAllowed(TEST_IP, MAX_ATTEMPTS);

            verify(redisTemplate, never()).expire(any(), any(Duration.class));
        }
    }
}
