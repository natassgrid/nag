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

package com.examplatform.identity.integration;

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.AuthTokenRequest;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.exception.MfaRequiredException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.service.AuthenticationService;
import com.examplatform.identity.service.GeoIpService;
import com.examplatform.shared.crypto.HashingService;
import com.examplatform.identity.service.KeycloakService;
import com.examplatform.identity.service.OtpService;
import com.examplatform.identity.service.RiskAssessmentService;
import com.examplatform.identity.support.AbstractIntegrationTest;
import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration tests for Risk Signal 3 (IP and Geo-change detection, Tor/VPN threat intelligence,
 * and Redis-backed last-login IP tracking).
 *
 * <p><strong>Validates: Requirements 2.6 (IP/geo risk signal — Issue #115)</strong></p>
 */
@DisplayName("Risk Signal 3: IP and Geo-Change Risk Assessment Integration Tests")
class IpGeoRiskIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RiskAssessmentService riskAssessmentService;

    @Autowired
    private GeoIpService geoIpService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private HashingService hashingService;

    @MockitoBean
    private KeycloakService keycloakService;

    @MockitoBean
    private OtpService otpService;

    @MockitoBean
    private DynamicConfigService dynamicConfigService;

    private static final String TENANT_ID = "default";
    private static final String USERNAME = "risk.candidate@example.com";
    private static final String PASSWORD = "ValidPassword123!";

    private UserAccount testAccount;

    @BeforeEach
    void setUpTestData() {
        // Clear Redis last-login IP keys
        if (redisTemplate != null && testcontainersAvailable) {
            try {
                var keys = redisTemplate.keys(RiskAssessmentService.LAST_LOGIN_IP_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception ignored) {}
        }

        // Mock DynamicConfigService defaults
        when(dynamicConfigService.getBoolean(eq("auth.mfa.enforced"), eq(TENANT_ID), any(Boolean.class))).thenReturn(false);
        when(dynamicConfigService.getBoolean(eq("auth.stepup.enforced"), eq(TENANT_ID), any(Boolean.class))).thenReturn(true);
        when(dynamicConfigService.getBoolean(eq("auth.risk.ip.enabled"), eq(TENANT_ID), eq(true))).thenReturn(true);
        when(dynamicConfigService.getBoolean(eq("auth.risk.ip.country-change.enabled"), eq(TENANT_ID), eq(true))).thenReturn(true);
        when(dynamicConfigService.getBoolean(eq("auth.risk.ip.vpn-tor.enabled"), eq(TENANT_ID), eq(true))).thenReturn(true);
        when(dynamicConfigService.getInt(eq("auth.risk.ip.ttl.days"), eq(TENANT_ID), eq(30))).thenReturn(30);
        when(dynamicConfigService.getInt(eq("auth.session.timeout.minutes"), eq(TENANT_ID), any(Integer.class))).thenReturn(30);
        when(dynamicConfigService.getString(eq("auth.mfa.candidate.policy"), eq(TENANT_ID), eq("OPTIONAL"))).thenReturn("OPTIONAL");
        when(dynamicConfigService.getString(eq("auth.mfa.admin.policy"), eq(TENANT_ID), eq("OPTIONAL"))).thenReturn("OPTIONAL");

        // Create or find test candidate in DB
        String emailHash = hashingService.sha256(USERNAME.toLowerCase());
        testAccount = userAccountRepository.findByUsernameIgnoreCaseAndTenantId(USERNAME, TENANT_ID)
                .orElseGet(() -> {
                    UserAccount acc = UserAccount.builder()
                            .username(USERNAME)
                            .fullName("Risk Test Candidate")
                            .email(USERNAME)
                            .emailHash(emailHash)
                            .mobileHash(hashingService.sha256("+919876543210"))
                            .accountStatus(AccountStatus.ACTIVE)
                            .emailVerified(true)
                            .mfaEnabled(false)
                            .deviceFingerprint("test-device-fp")
                            .build();
                    acc.setTenantId(TENANT_ID);
                    return userAccountRepository.save(acc);
                });
    }

    @Nested
    @DisplayName("Redis Last-Login IP Persistence")
    class RedisIpPersistence {

        @Test
        @DisplayName("+ve: Record last-login IP in Redis and verify retrieval")
        void recordAndRetrieveLastLoginIp() {
            String ip = "103.21.244.15";
            riskAssessmentService.recordSuccessfulLoginIp(testAccount.getId(), ip, TENANT_ID);

            String storedIp = riskAssessmentService.getLastLoginIp(testAccount.getId());
            assertThat(storedIp).isEqualTo(ip);
        }
    }

    @Nested
    @DisplayName("Country-Change and Threat Intelligence Detection")
    class CountryChangeDetection {

        @Test
        @DisplayName("+ve: Same country IP change does NOT trigger step-up MFA")
        void sameCountryDoesNotTriggerStepUp() {
            // First login from India IP
            riskAssessmentService.recordSuccessfulLoginIp(testAccount.getId(), "103.21.244.1", TENANT_ID);

            // Second login from another India IP (within normal hours)
            boolean stepUp = riskAssessmentService.isStepUpRequired(
                    testAccount, "test-device-fp", "49.207.55.10", LocalDateTime.of(2026, 10, 3, 14, 0));

            assertThat(stepUp).isFalse();
        }

        @Test
        @DisplayName("-ve: Different country IP change triggers step-up MFA")
        void differentCountryTriggersStepUp() {
            // Initial login from India
            riskAssessmentService.recordSuccessfulLoginIp(testAccount.getId(), "103.21.244.1", TENANT_ID);

            // Subsequent login from US IP
            boolean stepUp = riskAssessmentService.isStepUpRequired(
                    testAccount, "test-device-fp", "8.8.8.8", LocalDateTime.of(2026, 10, 3, 14, 0));

            assertThat(stepUp).isTrue();
        }

        @Test
        @DisplayName("-ve: Tor exit node triggers step-up MFA immediately")
        void torExitNodeTriggersStepUp() {
            String torIp = "185.220.101.5";

            boolean stepUp = riskAssessmentService.isStepUpRequired(
                    testAccount, "test-device-fp", torIp, LocalDateTime.of(2026, 10, 3, 14, 0));

            assertThat(stepUp).isTrue();
        }
    }

    @Nested
    @DisplayName("End-to-End Authentication Step-Up Enforcement on Geo-Change")
    class EndToEndAuthenticationGeoStepUp {

        @Test
        @DisplayName("Authentication succeeds on initial login and records IP")
        void initialLoginSucceedsAndRecordsIp() {
            when(keycloakService.getTokens(eq(USERNAME), eq(PASSWORD), eq(testAccount.getId().toString())))
                    .thenReturn(AuthTokenResponse.builder()
                            .accessToken("jwt.test.token")
                            .refreshToken("refresh.test.token")
                            .tokenType("Bearer")
                            .userId(testAccount.getId().toString())
                            .build());

            AuthTokenRequest request = new AuthTokenRequest(USERNAME, PASSWORD, null, "test-device-fp");
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, "103.21.244.1");

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("jwt.test.token");
            assertThat(riskAssessmentService.getLastLoginIp(testAccount.getId())).isEqualTo("103.21.244.1");
        }

        @Test
        @DisplayName("Authentication from different country triggers MfaRequiredException for step-up")
        void geoChangeTriggersMfaRequiredException() {
            // Pre-seed last login IP from India
            riskAssessmentService.recordSuccessfulLoginIp(testAccount.getId(), "103.21.244.1", TENANT_ID);

            when(keycloakService.getTokens(eq(USERNAME), eq(PASSWORD), eq(testAccount.getId().toString())))
                    .thenReturn(AuthTokenResponse.builder()
                            .accessToken("jwt.test.token")
                            .refreshToken("refresh.test.token")
                            .tokenType("Bearer")
                            .userId(testAccount.getId().toString())
                            .build());

            // Attempt login from US IP without OTP
            AuthTokenRequest request = new AuthTokenRequest(USERNAME, PASSWORD, null, "test-device-fp");

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, "8.8.8.8"))
                    .isInstanceOf(MfaRequiredException.class)
                    .hasMessageContaining("Step-up authentication required");
        }
    }
}
