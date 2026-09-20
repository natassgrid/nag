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
import com.examplatform.identity.domain.enums.OtpChannel;
import com.examplatform.identity.exception.SmsRateLimitExceededException;
import com.examplatform.identity.repository.OtpVerificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Msg91SmsServiceTest {

    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    private SmsProperties smsProperties;
    private Msg91SmsService msg91SmsService;

    @BeforeEach
    void setUp() {
        smsProperties = new SmsProperties();
        smsProperties.setEnabled(true);
        smsProperties.setWeeklyLimit(3);
        smsProperties.setRollingWindowDays(7);
        msg91SmsService = new Msg91SmsService(smsProperties, otpVerificationRepository);
    }

    @Test
    @DisplayName("Should normalize 10-digit Indian numbers to E.164 (+91 prefix)")
    void shouldNormalizeIndianMobileNumber() {
        assertThat(msg91SmsService.normalizeMobileNumber("9876543210")).isEqualTo("+919876543210");
        assertThat(msg91SmsService.normalizeMobileNumber("09876543210")).isEqualTo("+919876543210");
        assertThat(msg91SmsService.normalizeMobileNumber("919876543210")).isEqualTo("+919876543210");
        assertThat(msg91SmsService.normalizeMobileNumber("+919876543210")).isEqualTo("+919876543210");
        assertThat(msg91SmsService.normalizeMobileNumber("+14155552671")).isEqualTo("+14155552671");
    }

    @Test
    @DisplayName("Should enforce weekly 3-SMS rate limit and throw SmsRateLimitExceededException when reached")
    void shouldEnforceWeeklyRateLimit() {
        UUID userId = UUID.randomUUID();
        when(otpVerificationRepository.countByUserIdAndChannelAndCreatedAtAfter(eq(userId), eq(OtpChannel.SMS.name()), any(LocalDateTime.class)))
                .thenReturn(3L);
        when(otpVerificationRepository.findOldestSmsInWindow(eq(userId), eq(OtpChannel.SMS.name()), any(LocalDateTime.class)))
                .thenReturn(Optional.of(LocalDateTime.now().minusDays(3)));

        assertThatThrownBy(() -> msg91SmsService.enforceWeeklySmsRateLimit(userId, "hash", "default"))
                .isInstanceOf(SmsRateLimitExceededException.class)
                .hasMessageContaining("maximum limit of 3 SMS OTPs");
    }

    @Test
    @DisplayName("Should return accurate remaining SMS count within 7-day rolling window")
    void shouldReturnRemainingSmsCount() {
        UUID userId = UUID.randomUUID();
        when(otpVerificationRepository.countByUserIdAndChannelAndCreatedAtAfter(eq(userId), eq(OtpChannel.SMS.name()), any(LocalDateTime.class)))
                .thenReturn(1L);

        int remaining = msg91SmsService.getRemainingSmsCount(userId, "hash", "default");
        assertThat(remaining).isEqualTo(2);
    }
}
