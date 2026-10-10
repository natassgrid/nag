/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.identity.service;

import com.examplatform.shared.crypto.HashingService;

import com.examplatform.identity.domain.OtpVerification;
import com.examplatform.identity.exception.RateLimitExceededException;
import com.examplatform.identity.repository.OtpVerificationRepository;
import com.examplatform.shared.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OTP Verification, Failure Recovery & ResetOTP Lifecycle Integration Tests")
class OtpLifecycleIntegrationTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Msg91SmsService msg91SmsService;

    @Mock
    private IdentityEmailService identityEmailService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private HashingService hashingService;
    private OtpRedisService otpRedisService;
    private OtpService otpService;

    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "candidate@example.com";
    private static final String TENANT_ID = "default";
    private String emailHash;

    @BeforeEach
    void setUp() {
        hashingService = new HashingService();
        emailHash = hashingService.sha256(EMAIL);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        otpRedisService = new OtpRedisService(redisTemplate);
        otpService = new OtpService(
                otpVerificationRepository,
                hashingService,
                eventPublisher,
                msg91SmsService,
                identityEmailService,
                otpRedisService
        );
    }

    // =========================================================================
    // 1. ResetOTP & Overwrite Lifecycle Tests
    // =========================================================================
    @Nested
    @DisplayName("ResetOTP & Overwrite Lifecycle")
    class ResetOtpLifecycle {

        @Test
        @DisplayName("+ve: Overwriting old OTP on resend: Old OTP is invalidated in Redis, new OTP succeeds")
        void resetOtpOverwritesPreviousCode() {
            String emailIdentifier = USER_ID + ":EMAIL";
            // First OTP dispatch
            when(redisTemplate.hasKey("otp:cooldown:" + emailIdentifier)).thenReturn(false);
            when(valueOperations.increment("otp:daily:" + emailIdentifier)).thenReturn(1L);

            ArgumentCaptor<String> firstOtpCaptor = ArgumentCaptor.forClass(String.class);
            otpService.sendEmailOtp(USER_ID, emailHash, EMAIL, "Candidate", TENANT_ID);

            verify(identityEmailService, times(1))
                    .sendCandidateEmailOtp(eq(EMAIL), firstOtpCaptor.capture(), eq("Candidate"), eq(USER_ID));
            String firstOtp = firstOtpCaptor.getValue();
            String firstOtpHash = hashingService.sha256(firstOtp);

            // Second OTP dispatch (Resend / Reset OTP)
            when(redisTemplate.hasKey("otp:cooldown:" + emailIdentifier)).thenReturn(false);
            when(valueOperations.increment("otp:daily:" + emailIdentifier)).thenReturn(2L);

            ArgumentCaptor<String> secondOtpCaptor = ArgumentCaptor.forClass(String.class);
            otpService.sendEmailOtp(USER_ID, emailHash, EMAIL, "Candidate", TENANT_ID);

            verify(identityEmailService, times(2))
                    .sendCandidateEmailOtp(eq(EMAIL), secondOtpCaptor.capture(), eq("Candidate"), eq(USER_ID));
            String secondOtp = secondOtpCaptor.getValue();
            String secondOtpHash = hashingService.sha256(secondOtp);

            // Redis now holds secondOtpHash
            when(valueOperations.get("otp:email:" + emailIdentifier)).thenReturn(secondOtpHash);

            // 1. Verifying with old (first) OTP must FAIL
            boolean firstOtpValid = otpService.verifyEmailOtp(USER_ID, emailHash, firstOtp);
            assertThat(firstOtpValid).isFalse();
            verify(valueOperations).increment("otp:attempts:" + emailIdentifier);

            // 2. Verifying with new (second) OTP must SUCCEED
            OtpVerification dbVerification = OtpVerification.builder()
                    .userId(USER_ID)
                    .emailHash(emailHash)
                    .otpHash(secondOtpHash)
                    .verified(false)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            when(otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(USER_ID, "EMAIL"))
                    .thenReturn(Optional.of(dbVerification));

            boolean secondOtpValid = otpService.verifyEmailOtp(USER_ID, emailHash, secondOtp);
            assertThat(secondOtpValid).isTrue();

            // Assert Redis cleanup on verification success
            verify(redisTemplate).delete("otp:email:" + emailIdentifier);
            verify(redisTemplate).delete("otp:attempts:" + emailIdentifier);
            assertThat(dbVerification.isVerified()).isTrue();
            verify(otpVerificationRepository).save(dbVerification);
        }
    }

    // =========================================================================
    // 2. Failure Recovery & Lockout Tests
    // =========================================================================
    @Nested
    @DisplayName("Failure Recovery & Consecutive Lockout")
    class FailureAndLockout {

        @Test
        @DisplayName("-ve: 5 consecutive failed attempts trigger 15-minute verification lockout")
        void consecutiveFailuresTriggerLockout() {
            String emailIdentifier = USER_ID + ":EMAIL";
            when(valueOperations.get("otp:attempts:" + emailIdentifier)).thenReturn(5L);

            assertThatThrownBy(() -> otpService.verifyEmailOtp(USER_ID, emailHash, "123456"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("locked for 15 minutes");
        }

        @Test
        @DisplayName("+ve: Developer test bypass code 000000 succeeds and cleans up lockout keys")
        void devBypassOtpSucceeds() {
            String emailIdentifier = USER_ID + ":EMAIL";
            OtpVerification dbVerification = OtpVerification.builder()
                    .userId(USER_ID)
                    .emailHash(emailHash)
                    .otpHash("someHash")
                    .verified(false)
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            when(otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(USER_ID, "EMAIL"))
                    .thenReturn(Optional.of(dbVerification));

            boolean result = otpService.verifyEmailOtp(USER_ID, emailHash, "000000");

            assertThat(result).isTrue();
            verify(redisTemplate).delete("otp:attempts:" + emailIdentifier);
            verify(redisTemplate).delete("otp:email:" + emailIdentifier);
            assertThat(dbVerification.isVerified()).isTrue();
        }
    }

    // =========================================================================
    // 3. Cooldown & Daily Quota Rate Limiting Tests
    // =========================================================================
    @Nested
    @DisplayName("Cooldown & Daily Quota Rate Limits")
    class RateLimits {

        @Test
        @DisplayName("-ve: Resend request during 60s cooldown throws RateLimitExceededException")
        void cooldownThrowsException() {
            String emailIdentifier = USER_ID + ":EMAIL";
            when(redisTemplate.hasKey("otp:cooldown:" + emailIdentifier)).thenReturn(true);
            when(redisTemplate.getExpire("otp:cooldown:" + emailIdentifier)).thenReturn(42L);

            assertThatThrownBy(() -> otpService.sendEmailOtp(USER_ID, emailHash, EMAIL, "Candidate", TENANT_ID))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("42 seconds");
        }

        @Test
        @DisplayName("-ve: Exceeding 5 resends per 24 hours throws RateLimitExceededException")
        void dailyLimitThrowsException() {
            String emailIdentifier = USER_ID + ":EMAIL";
            when(redisTemplate.hasKey("otp:cooldown:" + emailIdentifier)).thenReturn(false);
            when(valueOperations.increment("otp:daily:" + emailIdentifier)).thenReturn(6L);

            assertThatThrownBy(() -> otpService.sendEmailOtp(USER_ID, emailHash, EMAIL, "Candidate", TENANT_ID))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessageContaining("Maximum limit of 5");
        }
    }

    // =========================================================================
    // 4. Mobile SMS OTP Tests
    // =========================================================================
    @Nested
    @DisplayName("Mobile SMS OTP Lifecycle")
    class MobileSmsOtpLifecycle {

        @Test
        @DisplayName("+ve: sendSmsOtp enforces rate limits and verifyMobileOtp verifies successfully")
        void sendAndVerifyMobileOtp() {
            ArgumentCaptor<OtpVerification> captor = ArgumentCaptor.forClass(OtpVerification.class);
            String mobile = "9876543210";
            String mobileHash = hashingService.sha256(mobile);

            otpService.sendSmsOtp(USER_ID, mobileHash, mobile, TENANT_ID);

            verify(msg91SmsService).enforceWeeklyRateLimit(USER_ID, mobileHash);
            verify(otpVerificationRepository).save(captor.capture());
            OtpVerification saved = captor.getValue();
            assertThat(saved.getOtpType()).isEqualTo("MOBILE");
            assertThat(saved.getChannel()).isEqualTo("SMS");

            // Verify with mock DB return
            when(otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(USER_ID, "MOBILE"))
                    .thenReturn(Optional.of(saved));

            // Dev bypass verification
            boolean valid = otpService.verifyMobileOtp(USER_ID, mobileHash, "000000");
            assertThat(valid).isTrue();
            assertThat(saved.isVerified()).isTrue();
        }
    }
}
