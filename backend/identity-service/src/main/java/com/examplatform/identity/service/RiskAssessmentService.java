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

import com.examplatform.identity.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Assesses risk signals during authentication and determines whether
 * step-up authentication (MFA/OTP) should be required even if the
 * account does not have {@code mfaEnabled=true}.
 *
 * <p>Risk signals evaluated:
 * <ul>
 *   <li>New device — device fingerprint differs from stored value</li>
 *   <li>Unusual login time — login outside 06:00–23:00 local time</li>
 *   <li>IP change — reserved for future geo/IP-based risk (placeholder)</li>
 * </ul>
 *
 * <p><strong>Validates: Requirements 2.6</strong>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    /**
     * Determines if step-up authentication should be required based on
     * contextual risk signals.
     *
     * @param account           the user account being authenticated
     * @param deviceFingerprint the device fingerprint from the current request
     * @param ipAddress         the originating IP address
     * @param loginTime         the timestamp of the login attempt
     * @return {@code true} if step-up authentication is required
     */
    public boolean isStepUpRequired(UserAccount account, String deviceFingerprint,
                                    String ipAddress, LocalDateTime loginTime) {
        // Risk signal 1: new device
        if (account.getDeviceFingerprint() != null
                && deviceFingerprint != null
                && !account.getDeviceFingerprint().equals(deviceFingerprint)) {
            log.info("Risk signal: new device detected for user {}", account.getId());
            return true;
        }

        // Risk signal 2: unusual time (before 6 AM or after 11 PM)
        int hour = loginTime.getHour();
        if (hour < 6 || hour >= 23) {
            log.info("Risk signal: unusual login time ({}) for user {}", hour, account.getId());
            return true;
        }

        // Risk signal 3: IP/geo change — placeholder for future enhancement
        // Could compare ipAddress against last successful login IP stored in Redis

        return false;
    }
}
