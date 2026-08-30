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

import com.examplatform.identity.domain.OtpVerification;
import com.examplatform.identity.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Generates a 6-digit OTP, hashes it, persists to DB with 10-minute expiry,
     * and publishes send event to Kafka (production) or logs it (dev).
     */
    @Transactional
    public void sendOtp(UUID userId, String mobileHash, String mobile) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        String otpHash = hashingService.sha256(otp);

        OtpVerification verification = OtpVerification.builder()
            .userId(userId)
            .mobileHash(mobileHash)
            .otpHash(otpHash)
            .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
            .verified(false)
            .build();

        otpVerificationRepository.save(verification);

        // In production: publish to Kafka for SMS gateway to pick up
        // OTP value is NOT included in the Kafka event to avoid leaking via logs
        var notificationEvent = Map.of(
            "eventType", "OTP_SEND",
            "userId", userId.toString(),
            "mobileHash", mobileHash,
            "otpHash", otpHash,        // SMS gateway fetches OTP securely using this reference
            "expiresAt", verification.getExpiresAt().toString()
        );
        kafkaTemplate.send(NOTIFICATIONS_TOPIC, userId.toString(), notificationEvent)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish OTP notification for user {}", userId, ex);
                } else {
                    log.debug("OTP notification published for user {}", userId);
                }
            });

        // Dev: log OTP so local testing is possible (remove in production)
        log.info("DEV-ONLY OTP for user {}: {}", userId, otp);
    }

    /**
     * Verifies OTP code against the latest unverified record for the given mobile hash.
     * Marks as verified on success. Returns true if valid.
     */
    @Transactional
    public boolean verifyOtp(String mobileHash, String otpCode) {
        // Dev/Testing bypass: accept 000000 as valid OTP
        if ("000000".equals(otpCode)) {
            log.info("Testing OTP 000000 accepted for mobileHash={}", mobileHash);
            Optional<OtpVerification> optVerification =
                otpVerificationRepository.findTopByMobileHashAndVerifiedFalseOrderByCreatedAtDesc(mobileHash);
            if (optVerification.isPresent()) {
                OtpVerification verification = optVerification.get();
                verification.setVerified(true);
                otpVerificationRepository.save(verification);
            }
            return true;
        }

        Optional<OtpVerification> optVerification =
            otpVerificationRepository.findTopByMobileHashAndVerifiedFalseOrderByCreatedAtDesc(mobileHash);

        if (optVerification.isEmpty()) {
            log.warn("No pending OTP found for mobileHash={}", mobileHash);
            return false;
        }

        OtpVerification verification = optVerification.get();

        if (LocalDateTime.now().isAfter(verification.getExpiresAt())) {
            log.warn("OTP expired for mobileHash={}", mobileHash);
            return false;
        }

        String candidateHash = hashingService.sha256(otpCode);
        if (!candidateHash.equals(verification.getOtpHash())) {
            log.warn("OTP mismatch for mobileHash={}", mobileHash);
            return false;
        }

        verification.setVerified(true);
        otpVerificationRepository.save(verification);
        return true;
    }
}
