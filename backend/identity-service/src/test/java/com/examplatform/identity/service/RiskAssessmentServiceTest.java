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
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RiskAssessmentService}.
 *
 * <p><strong>Validates: Requirements 2.6 (Risk Signals 1, 2, 3)</strong></p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskAssessmentService")
class RiskAssessmentServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private GeoIpService geoIpService;

    @Mock
    private DynamicConfigService dynamicConfigService;

    private RiskAssessmentService riskAssessmentService;

    private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String IP_ADDRESS = "192.168.1.10";

    @BeforeEach
    void setUp() {
        riskAssessmentService = new RiskAssessmentService(redisTemplate, geoIpService, dynamicConfigService);
    }

    private UserAccount buildAccount(String storedFingerprint) {
        UserAccount account = UserAccount.builder()
                .username("user@example.com")
                .emailHash("hash")
                .mobileHash("mobile")
                .accountStatus(AccountStatus.ACTIVE)
                .deviceFingerprint(storedFingerprint)
                .build();
        account.setTenantId("default");
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    @Nested
    @DisplayName("Device fingerprint risk signal")
    class DeviceFingerprint {

        @Test
        @DisplayName("step-up required when device fingerprint differs from stored")
        void stepUpRequiredWhenFingerprintDiffers() {
            UserAccount account = buildAccount("stored-fp-abc");

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, "new-fp-xyz", IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("step-up NOT required when fingerprints match")
        void stepUpNotRequiredWhenFingerprintsMatch() {
            UserAccount account = buildAccount("same-fingerprint");

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, "same-fingerprint", IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("step-up NOT required when account has no stored fingerprint")
        void stepUpNotRequiredWhenNoStoredFingerprint() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, "any-fingerprint", IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Unusual time risk signal")
    class UnusualTime {

        @Test
        @DisplayName("step-up required at 3:00 AM (unusual hour)")
        void stepUpRequiredAtUnusualHour() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 3, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("step-up NOT required at 10:00 AM (normal hour)")
        void stepUpNotRequiredAtNormalHour() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 10, 0));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("step-up required at 23:30 (unusual hour)")
        void stepUpRequiredAtLateNight() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 23, 30));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("step-up NOT required at 06:00 (boundary - normal)")
        void stepUpNotRequiredAtEarlyBoundary() {
            UserAccount account = buildAccount(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, IP_ADDRESS, LocalDateTime.of(2024, 1, 15, 6, 0));

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("Risk Signal 3: IP/Geo change and Threat Intelligence")
    class IpGeoRiskSignal {

        @Test
        @DisplayName("Tor / VPN / High-risk IP triggers step-up MFA")
        void torExitNodeTriggersStepUp() {
            UserAccount account = buildAccount(null);
            String torIp = "185.220.101.5";

            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.vpn-tor.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(geoIpService.isHighRiskIp(torIp)).thenReturn(true);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, torIp, LocalDateTime.of(2024, 1, 15, 14, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("First login (no prior IP in Redis) does NOT trigger step-up for IP change")
        void firstLoginNoStoredIpDoesNotTrigger() {
            UserAccount account = buildAccount(null);
            String currentIp = "103.21.244.1";

            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.vpn-tor.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(geoIpService.isHighRiskIp(currentIp)).thenReturn(false);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.country-change.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("last-login-ip:" + ACCOUNT_ID)).thenReturn(null);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, currentIp, LocalDateTime.of(2024, 1, 15, 14, 0));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Same IP as previous login does NOT trigger step-up")
        void sameIpDoesNotTriggerStepUp() {
            UserAccount account = buildAccount(null);
            String currentIp = "103.21.244.1";

            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.vpn-tor.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(geoIpService.isHighRiskIp(currentIp)).thenReturn(false);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.country-change.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("last-login-ip:" + ACCOUNT_ID)).thenReturn("103.21.244.1");

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, currentIp, LocalDateTime.of(2024, 1, 15, 14, 0));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Different IP in SAME country does NOT trigger step-up")
        void sameCountryDifferentIpDoesNotTrigger() {
            UserAccount account = buildAccount(null);
            String lastIp = "103.21.244.1";
            String currentIp = "49.207.55.10";

            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.vpn-tor.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(geoIpService.isHighRiskIp(currentIp)).thenReturn(false);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.country-change.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("last-login-ip:" + ACCOUNT_ID)).thenReturn(lastIp);

            when(geoIpService.resolveCountry(lastIp)).thenReturn("IN");
            when(geoIpService.resolveCountry(currentIp)).thenReturn("IN");

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, currentIp, LocalDateTime.of(2024, 1, 15, 14, 0));

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Different country IP triggers step-up MFA")
        void differentCountryTriggersStepUp() {
            UserAccount account = buildAccount(null);
            String lastIp = "103.21.244.1"; // India
            String currentIp = "8.8.8.8";     // United States

            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.vpn-tor.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(geoIpService.isHighRiskIp(currentIp)).thenReturn(false);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.country-change.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("last-login-ip:" + ACCOUNT_ID)).thenReturn(lastIp);

            when(geoIpService.resolveCountry(lastIp)).thenReturn("IN");
            when(geoIpService.resolveCountry(currentIp)).thenReturn("US");

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, currentIp, LocalDateTime.of(2024, 1, 15, 14, 0));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Country change disabled in dynamic config does not trigger step-up")
        void countryChangeDisabledDoesNotTrigger() {
            UserAccount account = buildAccount(null);
            String lastIp = "103.21.244.1";
            String currentIp = "8.8.8.8";

            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.vpn-tor.enabled"), eq("default"), eq(true))).thenReturn(true);
            when(geoIpService.isHighRiskIp(currentIp)).thenReturn(false);
            when(dynamicConfigService.getBoolean(eq("auth.risk.ip.country-change.enabled"), eq("default"), eq(true))).thenReturn(false);

            boolean result = riskAssessmentService.isStepUpRequired(
                    account, null, currentIp, LocalDateTime.of(2024, 1, 15, 14, 0));

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("recordSuccessfulLoginIp")
    class RecordSuccessfulLoginIp {

        @Test
        @DisplayName("stores IP in Redis with 30-day default TTL")
        void recordsIpWithTtl() {
            when(dynamicConfigService.getInt(eq("auth.risk.ip.ttl.days"), eq("default"), eq(30))).thenReturn(30);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            riskAssessmentService.recordSuccessfulLoginIp(ACCOUNT_ID, "103.21.244.1", "default");

            verify(valueOperations).set(eq("last-login-ip:" + ACCOUNT_ID), eq("103.21.244.1"), eq(Duration.ofDays(30)));
        }

        @Test
        @DisplayName("null userId or blank IP does not write to Redis")
        void ignoresNullOrBlank() {
            riskAssessmentService.recordSuccessfulLoginIp(null, "103.21.244.1", "default");
            riskAssessmentService.recordSuccessfulLoginIp(ACCOUNT_ID, "  ", "default");

            verifyNoInteractions(redisTemplate);
        }
    }
}
