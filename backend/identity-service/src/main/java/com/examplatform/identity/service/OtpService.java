/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.\n *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.identity.service;

import com.examplatform.identity.domain.OtpVerification;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final String NOTIFICATIONS_TOPIC = "exam.notifications.outbound";

    private final OtpVerificationRepository otpVerificationRepository;
    private final HashingService hashingService;
    private final EventPublisher eventPublisher;
    private final Msg91SmsService msg91SmsService;
    private final IdentityEmailService identityEmailService;
    private final OtpRedisService otpRedisService;

    /**
     * Generates a 6-digit OTP, hashes it, persists to DB with 10-minute expiry,
     * sends via MSG91 SMS, and publishes notification event.
     */
    @Transactional
    public void sendSmsOtp(UUID userId, String mobileHash, String mobile, String tenantId) {
        // Enforce weekly SMS quota
        msg91SmsService.enforceWeeklyRateLimit(userId, mobileHash);

        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        String otpHash = hashingService.sha256(otp);

        OtpVerification verification = OtpVerification.builder()
                .userId(userId)
                .mobileHash(mobileHash)
                .otpType("MOBILE")
                .channel("SMS")
                .targetDestination(mobile)
                .otpHash(otpHash)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .verified(false)
                .build();
        verification.setTenantId(tenantId != null ? tenantId : "default");

        otpVerificationRepository.save(verification);

        // Send SMS via MSG91
        if (mobile != null && !mobile.isBlank()) {
            msg91SmsService.sendSmsOtp(mobile, otp);
        }

        // Publish event for outbound notifications / auditing
        var notificationEvent = Map.of(
                "eventType", "OTP_SEND",
                "userId", userId != null ? userId.toString() : "",
                "mobileHash", mobileHash != null ? mobileHash : "",
                "channel", "SMS",
                "otpHash", otpHash,
                "expiresAt", verification.getExpiresAt().toString()
        );
        try {
            eventPublisher.publish(NOTIFICATIONS_TOPIC, userId != null ? userId.toString() : "anonymous", notificationEvent);
        } catch (Exception ex) {
            log.error("Failed to publish SMS OTP notification for user {}", userId, ex);
        }

        log.info("SMS OTP dispatched for user [{}], remaining weekly SMS: {}",
                userId, msg91SmsService.getRemainingSmsCount(userId, mobileHash));
    }

    /**
     * Generates a 6-digit OTP, stores hashed OTP in Redis with 10-minute TTL,
     * enforces 60s cooldown & daily resend limits, persists to DB for auditing,
     * and delivers branded email with direct verification link.
     */
    @Transactional
    public void sendEmailOtp(UUID userId, String emailHash, String email, String candidateName, String tenantId) {
        String identifier = (userId != null) ? userId.toString() : emailHash;

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
        verification.setTenantId(tenantId != null ? tenantId : "default");

        otpVerificationRepository.save(verification);

        // 5. Send Email with direct verification link via Gmail SMTP / Mock
        if (email != null && !email.isBlank()) {
            identityEmailService.sendCandidateEmailOtp(email, otp, candidateName, userId);
        }

        var notificationEvent = Map.of(
                "eventType", "EMAIL_OTP_SEND",
                "userId", userId != null ? userId.toString() : "",
                "emailHash", emailHash != null ? emailHash : "",
                "channel", "EMAIL",
                "otpHash", otpHash,
                "expiresAt", verification.getExpiresAt().toString()
        );
        try {
            eventPublisher.publish(NOTIFICATIONS_TOPIC, userId != null ? userId.toString() : "anonymous", notificationEvent);
        } catch (Exception ex) {
            log.error("Failed to publish Email OTP notification for user {}", userId, ex);
        }

        log.info("Email OTP dispatched for user [{}] / identifier [{}]", userId, identifier);
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
    public boolean verifyEmailOtp(UUID userId, String emailHash, String otpCode) {
        String identifier = (userId != null) ? userId.toString() : emailHash;

        // 1. Check if user is locked out due to consecutive failed attempts (5 failures = 15m lock)
        otpRedisService.checkVerificationLockout(identifier);

        // 2. Test bypass code 000000 support
        if ("000000".equals(otpCode != null ? otpCode.trim() : "")) {
            log.info("Testing OTP 000000 accepted for email verification (userId={})", userId);
            otpRedisService.clearFailedAttempts(identifier);
            otpRedisService.removeEmailOtp(identifier);
            if (emailHash != null) {
                otpRedisService.removeEmailOtp(emailHash);
            }
            Optional<OtpVerification> opt = userId != null ?
                    otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(userId, "EMAIL") :
                    otpVerificationRepository.findTopByEmailHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(emailHash, "EMAIL");
            opt.ifPresent(v -> {
                v.setVerified(true);
                otpVerificationRepository.save(v);
            });
            return true;
        }

        String candidateHash = hashingService.sha256(otpCode != null ? otpCode.trim() : "");

        // 3. Fast Redis TTL check
        String storedRedisHash = otpRedisService.getStoredEmailOtp(identifier);
        if (storedRedisHash == null && emailHash != null) {
            storedRedisHash = otpRedisService.getStoredEmailOtp(emailHash);
        }

        if (storedRedisHash != null) {
            if (storedRedisHash.equals(candidateHash)) {
                // Success: clear lockout and remove OTP
                otpRedisService.clearFailedAttempts(identifier);
                otpRedisService.removeEmailOtp(identifier);
                if (emailHash != null) {
                    otpRedisService.removeEmailOtp(emailHash);
                }

                // Update DB audit record
                Optional<OtpVerification> opt = userId != null ?
                        otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(userId, "EMAIL") :
                        otpVerificationRepository.findTopByEmailHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(emailHash, "EMAIL");
                opt.ifPresent(v -> {
                    v.setVerified(true);
                    otpVerificationRepository.save(v);
                });
                return true;
            } else {
                // Mismatch: record failure
                otpRedisService.recordFailedAttempt(identifier);
                log.warn("Email OTP mismatch from Redis for identifier [{}]", identifier);
                return false;
            }
        }

        // 4. Fallback to PostgreSQL DB if Redis key expired or not set
        Optional<OtpVerification> optVerification = userId != null ?
                otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(userId, "EMAIL") :
                otpVerificationRepository.findTopByEmailHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(emailHash, "EMAIL");

        if (optVerification.isEmpty()) {
            otpRedisService.recordFailedAttempt(identifier);
            log.warn("No pending Email OTP found in DB for userId={}, emailHash={}", userId, emailHash);
            return false;
        }

        OtpVerification verification = optVerification.get();
        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            otpRedisService.recordFailedAttempt(identifier);
            log.warn("Email OTP expired in DB for userId={}", userId);
            return false;
        }

        if (!candidateHash.equals(verification.getOtpHash())) {
            otpRedisService.recordFailedAttempt(identifier);
            log.warn("Email OTP mismatch in DB for userId={}", userId);
            return false;
        }

        // Success: clear lockout
        otpRedisService.clearFailedAttempts(identifier);
        verification.setVerified(true);
        otpVerificationRepository.save(verification);
        return true;
    }

    /**
     * Verifies Mobile OTP code.
     */
    @Transactional
    public boolean verifyMobileOtp(UUID userId, String mobileHash, String otpCode) {
        if ("000000".equals(otpCode != null ? otpCode.trim() : "")) {
            log.info("Testing OTP 000000 accepted for mobile verification (userId={})", userId);
            Optional<OtpVerification> opt = userId != null ?
                    otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(userId, "MOBILE") :
                    otpVerificationRepository.findTopByMobileHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(mobileHash, "MOBILE");
            if (opt.isEmpty() && mobileHash != null) {
                opt = otpVerificationRepository.findTopByMobileHashAndVerifiedFalseOrderByCreatedAtDesc(mobileHash);
            }
            opt.ifPresent(v -> {
                v.setVerified(true);
                otpVerificationRepository.save(v);
            });
            return true;
        }

        Optional<OtpVerification> optVerification = userId != null ?
                otpVerificationRepository.findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(userId, "MOBILE") :
                otpVerificationRepository.findTopByMobileHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(mobileHash, "MOBILE");

        if (optVerification.isEmpty() && mobileHash != null) {
            optVerification = otpVerificationRepository.findTopByMobileHashAndVerifiedFalseOrderByCreatedAtDesc(mobileHash);
        }

        if (optVerification.isEmpty()) {
            log.warn("No pending Mobile OTP found for userId={}, mobileHash={}", userId, mobileHash);
            return false;
        }

        OtpVerification verification = optVerification.get();
        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            log.warn("Mobile OTP expired for userId={}", userId);
            return false;
        }

        String candidateHash = hashingService.sha256(otpCode != null ? otpCode.trim() : "");
        if (!candidateHash.equals(verification.getOtpHash())) {
            log.warn("Mobile OTP mismatch for userId={}", userId);
            return false;
        }

        verification.setVerified(true);
        otpVerificationRepository.save(verification);
        return true;
    }

    /**
     * Backward-compatible verifyOtp method.
     */
    @Transactional
    public boolean verifyOtp(String mobileHash, String otpCode) {
        return verifyMobileOtp(null, mobileHash, otpCode);
    }
}
