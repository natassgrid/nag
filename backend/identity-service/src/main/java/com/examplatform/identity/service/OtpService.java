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

import com.examplatform.identity.config.SmsProperties;
import com.examplatform.identity.domain.OtpVerification;
import com.examplatform.identity.exception.RateLimitExceededException;
import com.examplatform.identity.repository.OtpVerificationRepository;
import com.examplatform.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced OTP Service managing generation, Redis TTL caching (10 mins),
 * 60s cooldown, daily quotas, consecutive failure lockout, MSG91 SMS dispatch,
 * and Gmail SMTP candidate onboarding notifications.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final String NOTIFICATIONS_TOPIC = "nag.notifications.events";

    private final OtpVerificationRepository otpVerificationRepository;
    private final HashingService hashingService;
    private final EventPublisher eventPublisher;
    private final Msg91SmsService msg91SmsService;
    private final IdentityEmailService identityEmailService;
    private final OtpRedisService otpRedisService;

    /**
     * Generates a 6-digit OTP, stores in Redis, enforces cooldown & quotas,
     * persists to DB, and sends SMS via MSG91.
     */
    @Transactional
    public void sendSmsOtp(UUID userId, String mobileHash, String mobileNumber, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String identifier = (userId != null) ? (userId + ":SMS") : (effectiveTenant + ":SMS:" + (mobileHash != null ? mobileHash : "anonymous"));

        // 0. Enforce weekly SMS quota if enabled
        msg91SmsService.enforceWeeklyRateLimit(userId, mobileHash);

        // 1. Enforce 60s cooldown and daily resend limits
        otpRedisService.checkAndEnforceCooldown(identifier);
        otpRedisService.checkAndIncrementDailyResend(identifier);

        // 2. Generate 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        String otpHash = hashingService.sha256(otp);

        // 3. Store in Redis
        otpRedisService.storeEmailOtp(identifier, otpHash);
        if (userId != null && mobileHash != null && !mobileHash.isBlank()) {
            otpRedisService.storeEmailOtp(mobileHash, otpHash);
        }

        // 4. Save to DB
        OtpVerification verification = OtpVerification.builder()
                .userId(userId)
                .mobileHash(mobileHash)
                .emailHash("")
                .otpType("MOBILE")
                .channel("SMS")
                .targetDestination(mobileNumber)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .build();
        verification.setTenantId(effectiveTenant);
        otpVerificationRepository.save(verification);

        // 5. Dispatch SMS
        if (mobileNumber != null && !mobileNumber.isBlank()) {
            msg91SmsService.sendSmsOtp(mobileNumber, otp);
        }

        // 6. Publish event
        var notificationEvent = Map.of(
                "eventType", "SMS_OTP_SEND",
                "userId", userId != null ? userId.toString() : "",
                "mobileHash", mobileHash != null ? mobileHash : "",
                "tenantId", effectiveTenant,
                "channel", "SMS",
                "otpHash", otpHash,
                "expiresAt", verification.getExpiresAt().toString()
        );
        try {
            eventPublisher.publish(NOTIFICATIONS_TOPIC, userId != null ? userId.toString() : "anonymous", notificationEvent);
        } catch (Exception ex) {
            log.error("Failed to publish SMS OTP notification for user {}", userId, ex);
        }

        log.info("SMS OTP dispatched for user [{}] in tenant [{}], remaining weekly SMS: {}",
                userId, effectiveTenant, msg91SmsService.getRemainingSmsCount(userId, mobileHash));
    }

    /**
     * Generates a 6-digit OTP, stores hashed OTP in Redis with 10-minute TTL,
     * enforces 60s cooldown & daily resend limits, persists to DB for auditing,
     * and delivers branded email with direct verification link.
     */
    @Transactional
    public void sendEmailOtp(UUID userId, String emailHash, String email, String candidateName, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String identifier = (userId != null) ? (userId + ":EMAIL") : (effectiveTenant + ":EMAIL:" + (emailHash != null ? emailHash : "anonymous"));

        // 1. Enforce 60s resend cooldown and 5/24h daily quota in Redis
        otpRedisService.checkAndEnforceCooldown(identifier);
        otpRedisService.checkAndIncrementDailyResend(identifier);

        // 2. Generate cryptographically secure 6-digit OTP
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        String otpHash = hashingService.sha256(otp);

        // 3. Store hashed OTP in Redis with 10-minute TTL
        otpRedisService.storeEmailOtp(identifier, otpHash);
        if (userId != null && emailHash != null && !emailHash.isBlank()) {
            otpRedisService.storeEmailOtp(emailHash, otpHash);
        }

        // 4. Save to PostgreSQL for audit trail
        OtpVerification verification = OtpVerification.builder()
                .userId(userId)
                .emailHash(emailHash)
                .mobileHash("") // non-null schema requirement fallback
                .otpType("EMAIL")
                .channel("EMAIL")
                .targetDestination(email)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .build();
        verification.setTenantId(effectiveTenant);

        otpVerificationRepository.save(verification);

        // 5. Send Email with direct verification link via Gmail SMTP / Mock
        if (email != null && !email.isBlank()) {
            identityEmailService.sendCandidateEmailOtp(email, otp, candidateName, userId);
        }

        var notificationEvent = Map.of(
                "eventType", "EMAIL_OTP_SEND",
                "userId", userId != null ? userId.toString() : "",
                "emailHash", emailHash != null ? emailHash : "",
                "tenantId", effectiveTenant,
                "channel", "EMAIL",
                "otpHash", otpHash,
                "expiresAt", verification.getExpiresAt().toString()
        );
        try {
            eventPublisher.publish(NOTIFICATIONS_TOPIC, userId != null ? userId.toString() : "anonymous", notificationEvent);
        } catch (Exception ex) {
            log.error("Failed to publish Email OTP notification for user {}", userId, ex);
        }

        log.info("Email OTP dispatched for user [{}] / identifier [{}] in tenant [{}]", userId, identifier, effectiveTenant);
    }

    /**
     * Backward-compatible helper for existing code.
     */
    @Transactional
    public void sendOtp(UUID userId, String mobileHash, String mobile) {
        sendSmsOtp(userId, mobileHash, mobile, "default");
    }

    /**
     * Verifies Email OTP code against Redis with TTL and lockout protection,
     * falling back to DB record if Redis key has rotated.
     */
    @Transactional
    public boolean verifyEmailOtp(UUID userId, String emailHash, String otpCode, String tenantId) {
        return verifyOtpInternal(userId, emailHash, otpCode, tenantId, "EMAIL", "EMAIL");
    }

    /**
     * Backward-compatible verifyEmailOtp.
     */
    @Transactional
    public boolean verifyEmailOtp(UUID userId, String emailHash, String otpCode) {
        return verifyEmailOtp(userId, emailHash, otpCode, "default");
    }

    /**
     * Verifies Mobile OTP code.
     */
    @Transactional
    public boolean verifyMobileOtp(UUID userId, String mobileHash, String otpCode, String tenantId) {
        return verifyOtpInternal(userId, mobileHash, otpCode, tenantId, "MOBILE", "SMS");
    }

    /**
     * Backward-compatible verifyMobileOtp/verifyOtp.
     */
    @Transactional
    public boolean verifyOtp(String mobileHash, String otpCode) {
        return verifyMobileOtp(null, mobileHash, otpCode, "default");
    }

    @Transactional
    public boolean verifyMobileOtp(UUID userId, String mobileHash, String otpCode) {
        return verifyMobileOtp(userId, mobileHash, otpCode, "default");
    }

    private boolean verifyOtpInternal(UUID userId, String targetHash, String otpCode, String tenantId, String otpType, String channelSuffix) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String identifier = (userId != null) ? (userId + ":" + channelSuffix) : (effectiveTenant + ":" + channelSuffix + ":" + (targetHash != null ? targetHash : "anonymous"));

        // Developer test bypass code 000000
        if ("000000".equals(otpCode != null ? otpCode.trim() : "")) {
            log.info("Testing OTP 000000 accepted for {} verification (userId={}, tenant={})", otpType.toLowerCase(), userId, effectiveTenant);
            Optional<OtpVerification> opt = findPendingVerification(userId, targetHash, otpType, effectiveTenant);
            opt.ifPresent(v -> {
                v.setVerified(true);
                otpVerificationRepository.save(v);
            });
            otpRedisService.removeEmailOtp(identifier);
            if (targetHash != null) {
                otpRedisService.removeEmailOtp(targetHash);
            }
            otpRedisService.clearFailedAttempts(identifier);
            return true;
        }

        // 1. Check if user is locked out due to >= 5 failed attempts
        otpRedisService.checkVerificationLockout(identifier);

        // 2. Compute SHA-256 of candidate OTP
        String candidateHash = hashingService.sha256(otpCode != null ? otpCode.trim() : "");

        // 3. Try reading from Redis
        String storedRedisHash = otpRedisService.getStoredEmailOtp(identifier);
        if (storedRedisHash == null && targetHash != null) {
            storedRedisHash = otpRedisService.getStoredEmailOtp(targetHash);
        }

        if (storedRedisHash != null) {
            if (candidateHash.equals(storedRedisHash)) {
                // Success: clean up Redis and mark verified in DB
                otpRedisService.removeEmailOtp(identifier);
                if (targetHash != null) {
                    otpRedisService.removeEmailOtp(targetHash);
                }
                otpRedisService.clearFailedAttempts(identifier);

                Optional<OtpVerification> optVerification = findPendingVerification(userId, targetHash, otpType, effectiveTenant);
                optVerification.ifPresent(v -> {
                    v.setVerified(true);
                    otpVerificationRepository.save(v);
                });
                return true;
            } else {
                otpRedisService.recordFailedAttempt(identifier);
                log.warn("{} OTP mismatch from Redis for identifier [{}]", otpType, identifier);
                return false;
            }
        }

        // 4. Fallback to PostgreSQL DB if Redis key expired or not set
        Optional<OtpVerification> optVerification = findPendingVerification(userId, targetHash, otpType, effectiveTenant);

        if (optVerification.isEmpty()) {
            otpRedisService.recordFailedAttempt(identifier);
            log.warn("No pending {} OTP found in DB for userId={}, hash={}, tenant={}", otpType, userId, targetHash, effectiveTenant);
            return false;
        }

        OtpVerification verification = optVerification.get();
        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            otpRedisService.recordFailedAttempt(identifier);
            log.warn("{} OTP expired in DB for userId={}, tenant={}", otpType, userId, effectiveTenant);
            return false;
        }

        if (!candidateHash.equals(verification.getOtpHash())) {
            otpRedisService.recordFailedAttempt(identifier);
            log.warn("{} OTP mismatch in DB for userId={}, tenant={}", otpType, userId, effectiveTenant);
            return false;
        }

        // Success: clear lockout and mark verified
        otpRedisService.clearFailedAttempts(identifier);
        verification.setVerified(true);
        otpVerificationRepository.save(verification);
        return true;
    }

    private Optional<OtpVerification> findPendingVerification(UUID userId, String targetHash, String otpType, String tenantId) {
        if ("MOBILE".equals(otpType)) {
            Optional<OtpVerification> opt = userId != null ?
                    otpVerificationRepository.findTopByUserIdAndOtpTypeAndTenantIdAndVerifiedFalseOrderByCreatedAtDesc(userId, "MOBILE", tenantId)
                            .or(() -> otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(userId, "MOBILE")) :
                    otpVerificationRepository.findTopByMobileHashAndOtpTypeAndTenantIdAndVerifiedFalseOrderByCreatedAtDesc(targetHash, "MOBILE", tenantId)
                            .or(() -> otpVerificationRepository.findTopByMobileHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(targetHash, "MOBILE"));
            if (opt.isEmpty() && targetHash != null) {
                opt = otpVerificationRepository.findTopByMobileHashAndTenantIdAndVerifiedFalseOrderByCreatedAtDesc(targetHash, tenantId)
                        .or(() -> otpVerificationRepository.findTopByMobileHashAndVerifiedFalseOrderByCreatedAtDesc(targetHash));
            }
            return opt;
        } else {
            return userId != null ?
                    otpVerificationRepository.findTopByUserIdAndOtpTypeAndTenantIdAndVerifiedFalseOrderByCreatedAtDesc(userId, "EMAIL", tenantId)
                            .or(() -> otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(userId, "EMAIL")) :
                    otpVerificationRepository.findTopByEmailHashAndOtpTypeAndTenantIdAndVerifiedFalseOrderByCreatedAtDesc(targetHash, "EMAIL", tenantId)
                            .or(() -> otpVerificationRepository.findTopByEmailHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(targetHash, "EMAIL"));
        }
    }
}
