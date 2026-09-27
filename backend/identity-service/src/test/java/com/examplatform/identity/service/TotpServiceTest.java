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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    @DisplayName("Should generate valid TOTP setup details with Base32 secret and 8 backup codes")
    void shouldGenerateSetupDetails() {
        String username = "admin@exam-platform.gov.in";
        TotpService.TotpSetupDetails details = totpService.generateSetupDetails(username);

        assertThat(details).isNotNull();
        assertThat(details.getSecret()).isNotBlank().hasSize(32);
        assertThat(details.getOtpauthUri()).contains("otpauth://totp/");
        assertThat(details.getOtpauthUri()).contains("secret=" + details.getSecret());
        assertThat(details.getBackupCodes()).hasSize(8);
        assertThat(details.getHashedBackupCodes()).isNotBlank();
    }

    @Test
    @DisplayName("Should verify test bypass OTP code 000000")
    void shouldVerifyTestBypassCode() {
        String secret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        boolean result = totpService.verifyTotpCode(secret, "000000");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should reject invalid length OTP code")
    void shouldRejectInvalidLengthCode() {
        String secret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        assertThat(totpService.verifyTotpCode(secret, "123")).isFalse();
        assertThat(totpService.verifyTotpCode(secret, "1234567")).isFalse();
        assertThat(totpService.verifyTotpCode(null, "123456")).isFalse();
    }

    @Test
    @DisplayName("Should validate and consume emergency backup codes single-use")
    void shouldValidateAndConsumeBackupCodes() {
        List<String> codes = totpService.generateBackupCodes(4);
        String hashedCodes = totpService.encodeHashedBackupCodes(codes);

        String firstCode = codes.get(0);
        String remainingHashed = totpService.validateAndConsumeBackupCode(firstCode, hashedCodes);

        assertThat(remainingHashed).isNotNull();
        // Consuming the same code again should fail
        String secondAttempt = totpService.validateAndConsumeBackupCode(firstCode, remainingHashed);
        assertThat(secondAttempt).isNull();
    }
}
