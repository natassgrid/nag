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

import com.examplatform.identity.config.AppSecurityProperties;
import com.examplatform.identity.domain.ActiveSession;
import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.AuthTokenRequest;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.exception.MfaRequiredException;
import com.examplatform.identity.repository.ActiveSessionRepository;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthenticationService}.
 *
 * <p><strong>Validates: Requirements 2.1, 2.2, 2.5, 2.7</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService")
class AuthenticationServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    ActiveSessionRepository activeSessionRepository;
    @Mock
    HashingService hashingService;
    @Mock
    KeycloakService keycloakService;
    @Mock
    OtpService otpService;
    @Mock
    AuditEventPublisher auditEventPublisher;
    @Mock
    AppSecurityProperties appSecurityProperties;
    @Mock
    AccountLockoutService accountLockoutService;
    @Mock
    RiskAssessmentService riskAssessmentService;
    @Mock
    DynamicConfigService dynamicConfigService;

    @InjectMocks
    AuthenticationService authenticationService;

    private static final String TENANT_ID = "default";
    private static final String IP = "127.0.0.1";
    private static final String EMAIL_HASH = "emailhash123";
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUpCommon() {
        // Use lenient to avoid UnnecessaryStubbingException for tests that throw early
        lenient().when(hashingService.sha256(anyString())).thenReturn(EMAIL_HASH);
        lenient().when(appSecurityProperties.getSessionIdleTimeoutSeconds()).thenReturn(1800);
        lenient().when(dynamicConfigService.getBoolean(eq("auth.mfa.enforced"), anyString(), anyBoolean()))
                .thenAnswer(inv -> inv.getArgument(2));
        lenient().when(dynamicConfigService.getInt(eq("auth.session.timeout.minutes"), anyString(), anyInt()))
                .thenAnswer(inv -> inv.getArgument(2));
    }

    // -------------------------------------------------------------------------
    // Helper factory
    // -------------------------------------------------------------------------

    private UserAccount activeAccount() {
        UserAccount account = UserAccount.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .username("user@example.com")
                .mfaEnabled(false)
                .failedAttemptCount(0)
                .build();
        account.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(account, "id", ACCOUNT_ID);
        return account;
    }

    private AuthTokenResponse sampleTokens() {
        return AuthTokenResponse.builder()
                .accessToken("access.jwt.token")
                .refreshToken("refresh.jwt.token")
                .tokenType("Bearer")
                .expiresIn(900L)
                .build();
    }

    // -------------------------------------------------------------------------
    // Requirement 2.1: Password authentication + JWT issuance
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Password Authentication")
    class PasswordAuth {

        @Test
        @DisplayName("Returns tokens on valid credentials")
        void successfulAuth() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "validPass123!", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest(
                    "user@example.com", "validPass123!", null, null);

            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access.jwt.token");
            assertThat(response.getExpiresIn()).isEqualTo(900L);
        }

        @Test
        @DisplayName("Throws AuthenticationException on unknown username")
        void unknownUser() {
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(userAccountRepository.findByMobileHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(userAccountRepository.findByUsernameIgnoreCaseAndTenantId(anyString(), eq(TENANT_ID)))
                    .thenReturn(Optional.empty());

            AuthTokenRequest request = new AuthTokenRequest(
                    "nonexistent@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("Throws AuthenticationException and increments counter on bad password")
        void badPassword() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "wrongPass", ACCOUNT_ID.toString()))
                    .thenThrow(new RuntimeException("401 Unauthorized"));

            AuthTokenRequest request = new AuthTokenRequest(
                    "user@example.com", "wrongPass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid credentials");

            assertThat(account.getFailedAttemptCount()).isEqualTo(1);
            assertThat(account.getLastFailedAt()).isNotNull();
            verify(accountLockoutService).checkAndLockIfNeeded(account, TENANT_ID);
        }

        @Test
        @DisplayName("Resets failed attempt count on successful login")
        void resetsFailedAttemptsOnSuccess() {
            UserAccount account = activeAccount();
            account.setFailedAttemptCount(3);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);
            authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(account.getFailedAttemptCount()).isZero();
            assertThat(account.getLastFailedAt()).isNull();
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.2: MFA Enforcement
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("MFA Verification")
    class MfaVerification {

        @Test
        @DisplayName("Throws MfaRequiredException when account requires MFA but no OTP provided")
        void mfaRequiredNoOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class);
        }

        @Test
        @DisplayName("Throws AuthenticationException when MFA OTP is invalid")
        void invalidMfaOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(otpService.verifyOtp(anyString(), eq("999999"))).thenReturn(false);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", "999999", null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid MFA code.");
        }

        @Test
        @DisplayName("Succeeds when valid MFA OTP provided for MFA-enabled account")
        void validMfaOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(otpService.verifyOtp(anyString(), eq("123456"))).thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", "123456", null);

            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Enforces MFA when auth.mfa.enforced is globally enabled")
        void globallyEnforcedMfa() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(false); // account has not enrolled MFA, but globally required
            when(dynamicConfigService.getBoolean(eq("auth.mfa.enforced"), eq(TENANT_ID), anyBoolean()))
                    .thenReturn(true);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.4: Account Status checks
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Account Status Checks")
    class AccountStatusChecks {

        @Test
        @DisplayName("Rejects login for LOCKED account")
        void lockedAccount() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.LOCKED);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("locked");
        }

        @Test
        @DisplayName("Rejects login for DEACTIVATED account")
        void deactivatedAccount() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.DEACTIVATED);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("deactivated");
        }

        @Test
        @DisplayName("Rejects login for PENDING_VERIFICATION account")
        void pendingVerificationAccount() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("not yet verified");
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.5: Device Fingerprint Binding
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Device Fingerprint Binding")
    class DeviceBinding {

        @Test
        @DisplayName("Binds device fingerprint on first login when provided")
        void bindsFpOnFirstLogin() {
            UserAccount account = activeAccount(); // deviceFingerprint is null
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest(
                    "user@example.com", "pass", null, "device-fp-xyz-123");

            authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(account.getDeviceFingerprint()).isEqualTo("device-fp-xyz-123");
            verify(userAccountRepository).save(account);
        }

        @Test
        @DisplayName("Rejects login and publishes audit event on device fingerprint mismatch")
        void rejectsMismatchedDevice() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("trusted-device-001");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest(
                    "user@example.com", "pass", null, "unknown-device-999");

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Device not recognised.");

            verify(auditEventPublisher).publish(
                    eq(AuditEventType.DENIED_ACCESS),
                    eq(ACCOUNT_ID.toString()),
                    eq("identity:auth/token"),
                    eq(IP),
                    eq("unknown-device-999"),
                    any()
            );
        }

        @Test
        @DisplayName("Allows login with matching device fingerprint")
        void allowsMatchingDevice() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("trusted-device-001");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest(
                    "user@example.com", "pass", null, "trusted-device-001");

            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);
            assertThat(response).isNotNull();
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.7: Single Concurrent Session (New login wins)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Single Concurrent Session")
    class SingleConcurrentSession {

        @Test
        @DisplayName("Invalidates existing active session when new login occurs")
        void invalidatesExistingSession() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);
            authenticationService.authenticate(request, TENANT_ID, IP);

            verify(activeSessionRepository).deleteByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID);
            verify(activeSessionRepository).save(any(ActiveSession.class));
        }

        @Test
        @DisplayName("Creates new active session record on login")
        void createsNewSession() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(false);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);
            authenticationService.authenticate(request, TENANT_ID, IP);

            ArgumentCaptor<ActiveSession> captor = ArgumentCaptor.forClass(ActiveSession.class);
            verify(activeSessionRepository).save(captor.capture());

            ActiveSession session = captor.getValue();
            assertAll(
                    () -> assertThat(session.getUserId()).isEqualTo(ACCOUNT_ID),
                    () -> assertThat(session.getTenantId()).isEqualTo(TENANT_ID),
                    () -> assertThat(session.getSessionToken()).isNotBlank(),
                    () -> assertThat(session.getExpiresAt()).isNotNull()
            );
        }
    }
}
