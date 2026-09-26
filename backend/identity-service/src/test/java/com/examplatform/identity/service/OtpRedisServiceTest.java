/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.service;

import com.examplatform.identity.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpRedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private OtpRedisService otpRedisService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("storeEmailOtp sets key with 10-minute TTL")
    void storeEmailOtpSetsTtl() {
        otpRedisService.storeEmailOtp("user123", "hashabc");
        verify(valueOperations).set(eq("otp:email:user123"), eq("hashabc"), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("getStoredEmailOtp returns stored hash")
    void getStoredEmailOtpReturnsHash() {
        when(valueOperations.get("otp:email:user123")).thenReturn("hashabc");
        String hash = otpRedisService.getStoredEmailOtp("user123");
        assertThat(hash).isEqualTo("hashabc");
    }

    @Test
    @DisplayName("checkAndEnforceCooldown throws when cooldown active")
    void checkAndEnforceCooldownThrowsWhenActive() {
        when(redisTemplate.hasKey("otp:cooldown:user123")).thenReturn(true);
        when(redisTemplate.getExpire("otp:cooldown:user123")).thenReturn(45L);

        assertThatThrownBy(() -> otpRedisService.checkAndEnforceCooldown("user123"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("45 seconds");
    }

    @Test
    @DisplayName("checkAndEnforceCooldown sets 60s key when no active cooldown")
    void checkAndEnforceCooldownSetsKey() {
        when(redisTemplate.hasKey("otp:cooldown:user123")).thenReturn(false);

        assertThatCode(() -> otpRedisService.checkAndEnforceCooldown("user123")).doesNotThrowAnyException();
        verify(valueOperations).set(eq("otp:cooldown:user123"), eq("1"), eq(Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("checkAndIncrementDailyResend throws when exceeding daily quota")
    void checkAndIncrementDailyResendThrowsWhenExceeded() {
        when(valueOperations.increment("otp:daily:user123")).thenReturn(6L);

        assertThatThrownBy(() -> otpRedisService.checkAndIncrementDailyResend("user123"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("Maximum limit of 5");
    }

    @Test
    @DisplayName("checkVerificationLockout throws when 5 failed attempts reached")
    void checkVerificationLockoutThrowsWhenThresholdReached() {
        when(valueOperations.get("otp:attempts:user123")).thenReturn(5L);

        assertThatThrownBy(() -> otpRedisService.checkVerificationLockout("user123"))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("locked for 15 minutes");
    }

    @Test
    @DisplayName("recordFailedAttempt increments counter and sets 15m expiry on first failure")
    void recordFailedAttemptSetsExpiryOnFirst() {
        when(valueOperations.increment("otp:attempts:user123")).thenReturn(1L);

        otpRedisService.recordFailedAttempt("user123");
        verify(redisTemplate).expire(eq("otp:attempts:user123"), eq(Duration.ofMinutes(15)));
    }
}
