/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.service;

import com.examplatform.identity.exception.RateLimitExceededException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redis-backed service for OTP storage, 10-minute TTL, resend cooldown (60s),
 * daily resend quota (max 5/24h), and verification attempt lockouts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpRedisService {

    private static final String OTP_PREFIX = "otp:email:";
    private static final String COOLDOWN_PREFIX = "otp:cooldown:";
    private static final String DAILY_PREFIX = "otp:daily:";
    private static final String ATTEMPTS_PREFIX = "otp:attempts:";

    public static final Duration OTP_EXPIRY = Duration.ofMinutes(10);
    public static final Duration COOLDOWN_DURATION = Duration.ofSeconds(60);
    public static final Duration DAILY_WINDOW = Duration.ofHours(24);
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    public static final int MAX_DAILY_RESENDS = 5;
    public static final int MAX_FAILED_ATTEMPTS = 5;

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * Stores hashed OTP in Redis with a 10-minute TTL.
     */
    public void storeEmailOtp(String identifier, String otpHash) {
        if (identifier == null || identifier.isBlank() || otpHash == null) {
            return;
        }
        String key = OTP_PREFIX + identifier.trim();
        try {
            redisTemplate.opsForValue().set(key, otpHash, OTP_EXPIRY);
            log.debug("Hashed OTP stored in Redis for [{}] with TTL {}", identifier, OTP_EXPIRY);
        } catch (Exception ex) {
            log.warn("Failed to store OTP in Redis for [{}]: {}", identifier, ex.getMessage());
        }
    }

    /**
     * Retrieves the stored OTP hash from Redis.
     */
    public String getStoredEmailOtp(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        String key = OTP_PREFIX + identifier.trim();
        try {
            Object val = redisTemplate.opsForValue().get(key);
            return val != null ? val.toString() : null;
        } catch (Exception ex) {
            log.warn("Failed to read OTP from Redis for [{}]: {}", identifier, ex.getMessage());
            return null;
        }
    }

    /**
     * Removes active OTP from Redis upon successful verification.
     */
    public void removeEmailOtp(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(OTP_PREFIX + identifier.trim());
        } catch (Exception ex) {
            log.warn("Failed to delete OTP from Redis for [{}]: {}", identifier, ex.getMessage());
        }
    }

    /**
     * Enforces the 60-second cooldown timer between OTP generation/resend requests.
     */
    public void checkAndEnforceCooldown(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        String key = COOLDOWN_PREFIX + identifier.trim();
        try {
            Boolean hasKey = redisTemplate.hasKey(key);
            if (Boolean.TRUE.equals(hasKey)) {
                Long expire = redisTemplate.getExpire(key);
                long seconds = (expire != null && expire > 0) ? expire : 60;
                throw new RateLimitExceededException(
                    "Please wait " + seconds + " seconds before requesting a new verification code."
                );
            }
            redisTemplate.opsForValue().set(key, "1", COOLDOWN_DURATION);
        } catch (RateLimitExceededException rle) {
            throw rle;
        } catch (Exception ex) {
            log.warn("Cooldown check failed on Redis for [{}]: {}", identifier, ex.getMessage());
        }
    }

    /**
     * Enforces the maximum 5 resends per 24 hours quota.
     */
    public void checkAndIncrementDailyResend(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        String key = DAILY_PREFIX + identifier.trim();
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, DAILY_WINDOW);
            }
            if (count != null && count > MAX_DAILY_RESENDS) {
                throw new RateLimitExceededException(
                    "Maximum limit of " + MAX_DAILY_RESENDS + " verification codes reached for today. Please try again in 24 hours."
                );
            }
        } catch (RateLimitExceededException rle) {
            throw rle;
        } catch (Exception ex) {
            log.warn("Daily resend check failed on Redis for [{}]: {}", identifier, ex.getMessage());
        }
    }

    /**
     * Checks if the user is currently locked out from verification due to 5 consecutive failures.
     */
    public void checkVerificationLockout(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        String key = ATTEMPTS_PREFIX + identifier.trim();
        try {
            Object val = redisTemplate.opsForValue().get(key);
            if (val != null) {
                long count = (val instanceof Number n) ? n.longValue() : Long.parseLong(val.toString());
                if (count >= MAX_FAILED_ATTEMPTS) {
                    throw new RateLimitExceededException(
                        "Too many failed verification attempts. Verification is locked for 15 minutes."
                    );
                }
            }
        } catch (RateLimitExceededException rle) {
            throw rle;
        } catch (Exception ex) {
            log.warn("Lockout check failed on Redis for [{}]: {}", identifier, ex.getMessage());
        }
    }

    /**
     * Increments the failed verification attempt counter. Locks out for 15 minutes on >= 5 failures.
     */
    public void recordFailedAttempt(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        String key = ATTEMPTS_PREFIX + identifier.trim();
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, LOCKOUT_DURATION);
            }
            log.debug("Recorded failed verification attempt {} for [{}]", count, identifier);
        } catch (Exception ex) {
            log.warn("Failed to record failed attempt in Redis for [{}]: {}", identifier, ex.getMessage());
        }
    }

    /**
     * Clears failed attempt lockout on successful verification.
     */
    public void clearFailedAttempts(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        try {
            redisTemplate.delete(ATTEMPTS_PREFIX + identifier.trim());
        } catch (Exception ex) {
            log.warn("Failed to clear failed attempts from Redis for [{}]: {}", identifier, ex.getMessage());
        }
    }
}
