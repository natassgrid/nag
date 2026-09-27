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
import com.examplatform.identity.domain.enums.OtpChannel;
import com.examplatform.identity.exception.SmsRateLimitExceededException;
import com.examplatform.identity.repository.OtpVerificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class Msg91SmsService {

    private final SmsProperties smsProperties;
    private final OtpVerificationRepository otpVerificationRepository;
    private final RestClient restClient = RestClient.create();

    /**
     * Normalizes a mobile number to E.164 format (+91 prefix for Indian numbers).
     */
    public String normalizeMobileNumber(String rawMobile) {
        if (rawMobile == null || rawMobile.isBlank()) {
            throw new IllegalArgumentException("Mobile number cannot be blank");
        }

        // Strip all whitespace, hyphens, and non-digit characters except leading '+'
        String cleaned = rawMobile.trim().replaceAll("[\\s\\-\\(\\)]", "");

        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return "+" + cleaned;
        }

        if (cleaned.startsWith("0") && cleaned.length() == 11) {
            return "+91" + cleaned.substring(1);
        }

        if (cleaned.length() == 10 && cleaned.matches("^[6-9]\\d{9}$")) {
            return "+91" + cleaned;
        }

        // Default fallback with plus
        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
    }

    public void enforceWeeklyRateLimit(UUID userId, String mobileHash) {
        enforceWeeklySmsRateLimit(userId, mobileHash, null);
    }

    /**
     * Checks whether the user has exceeded the weekly SMS rate limit (Issue #141: max 3 per week).
     */
    public void enforceWeeklySmsRateLimit(UUID userId, String mobileHash, String tenantId) {
        if (!smsProperties.isEnabled()) {
            return;
        }

        int rollingDays = smsProperties.getRollingWindowDays();
        int maxAllowed = smsProperties.getWeeklyLimit();
        LocalDateTime windowStart = LocalDateTime.now().minusDays(rollingDays);

        long smsCount;
        if (userId != null) {
            smsCount = otpVerificationRepository.countByUserIdAndChannelAndCreatedAtAfter(userId, OtpChannel.SMS.name(), windowStart);
        } else if (mobileHash != null) {
            smsCount = otpVerificationRepository.countByMobileHashAndChannelAndCreatedAtAfter(mobileHash, OtpChannel.SMS.name(), windowStart);
        } else {
            return;
        }

        if (smsCount >= maxAllowed) {
            LocalDateTime oldestInWindow = Optional.ofNullable(userId)
                    .flatMap(uid -> otpVerificationRepository.findOldestSmsInWindow(uid, OtpChannel.SMS.name(), windowStart))
                    .orElse(LocalDateTime.now().minusDays(rollingDays).plusDays(1));
            LocalDateTime retryAfter = oldestInWindow.plusDays(rollingDays);

            log.warn("Candidate [{}] exceeded weekly SMS OTP limit ({} sent in last {} days). Next available: {}",
                    userId != null ? userId : mobileHash, smsCount, rollingDays, retryAfter);

            throw new SmsRateLimitExceededException(
                    "You have reached the maximum limit of " + maxAllowed + " SMS OTPs for this week. " +
                            "Please use email verification or try SMS again on " + retryAfter.toLocalDate() + ".",
                    retryAfter
            );
        }
    }

    public int getRemainingSmsCount(UUID userId, String mobileHash) {
        return getRemainingSmsCount(userId, mobileHash, null);
    }

    /**
     * Returns the number of remaining SMS OTP requests available in the current rolling window.
     */
    public int getRemainingSmsCount(UUID userId, String mobileHash, String tenantId) {
        if (!smsProperties.isEnabled()) {
            return smsProperties.getWeeklyLimit();
        }

        LocalDateTime windowStart = LocalDateTime.now().minusDays(smsProperties.getRollingWindowDays());
        long sentCount;
        if (userId != null) {
            sentCount = otpVerificationRepository.countByUserIdAndChannelAndCreatedAtAfter(userId, OtpChannel.SMS.name(), windowStart);
        } else if (mobileHash != null) {
            sentCount = otpVerificationRepository.countByMobileHashAndChannelAndCreatedAtAfter(mobileHash, OtpChannel.SMS.name(), windowStart);
        } else {
            return smsProperties.getWeeklyLimit();
        }

        return Math.max(0, smsProperties.getWeeklyLimit() - (int) sentCount);
    }

    public LocalDateTime getNextSmsAvailableAt(UUID userId, String mobileHash) {
        if (!smsProperties.isEnabled()) {
            return null;
        }
        if (getRemainingSmsCount(userId, mobileHash) > 0) {
            return null;
        }
        LocalDateTime windowStart = LocalDateTime.now().minusDays(smsProperties.getRollingWindowDays());
        return Optional.ofNullable(userId)
                .flatMap(uid -> otpVerificationRepository.findOldestSmsInWindow(uid, OtpChannel.SMS.name(), windowStart))
                .map(oldest -> oldest.plusDays(smsProperties.getRollingWindowDays()))
                .orElse(LocalDateTime.now().plusDays(1));
    }

    /**
     * Delivers OTP code via MSG91 API or logs in mock/dev mode.
     */
    public boolean sendSmsOtp(String rawMobile, String otpCode) {
        String normalizedMobile = normalizeMobileNumber(rawMobile);
        String mobileWithoutPlus = normalizedMobile.replaceFirst("^\\+", "");

        String authKey = smsProperties.getMsg91().getAuthKey();
        String templateId = smsProperties.getMsg91().getTemplateId();

        // If credentials are not configured or dev mode, log mock SMS
        if (authKey == null || authKey.isBlank() || templateId == null || templateId.isBlank()) {
            log.info("[MSG91 MOCK] Sending OTP [{}] to mobile [{}] (E.164: {})", otpCode, rawMobile, normalizedMobile);
            return true;
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(smsProperties.getMsg91().getApiUrl())
                    .queryParam("template_id", templateId)
                    .queryParam("mobile", mobileWithoutPlus)
                    .queryParam("authkey", authKey)
                    .queryParam("otp", otpCode);

            if (smsProperties.getMsg91().getSenderId() != null && !smsProperties.getMsg91().getSenderId().isBlank()) {
                uriBuilder.queryParam("sender", smsProperties.getMsg91().getSenderId());
            }

            String url = uriBuilder.toUriString();
            log.debug("Dispatching MSG91 OTP request to URL: {}", url.replaceAll("authkey=[^&]+", "authkey=***"));

            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .retrieve()
                    .body(Map.class);

            log.info("MSG91 API response for mobile [{}]: {}", normalizedMobile, response);
            return response != null && "success".equalsIgnoreCase(String.valueOf(response.get("type")));
        } catch (Exception e) {
            log.error("Failed to send MSG91 SMS OTP to mobile [{}]: {}", normalizedMobile, e.getMessage());
            return false;
        }
    }
}
