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
import com.examplatform.identity.dto.RefreshTokenRequest;
import com.examplatform.identity.exception.AccountNotVerifiedException;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit tests for {@link AuthenticationService}.
 *
 * <p>Validates:
 * <ul>
 *   <li>Requirement 2.1: Credential Verification (username/password + account state)</li>
 *   <li>Requirement 2.2: MFA Enforcement (TOTP/SMS validation)</li>
 *   <li>Requirement 2.5: Device Binding (fingerprint validation and binding)</li>
 *   <li>Requirement 2.7: Single Concurrent Session (session creation and replacement)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService")
class AuthenticationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ActiveSessionRepository activeSessionRepository;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private HashingService hashingService;

    @Mock
    private OtpService otpService;

    @Mock
    private TotpService totpService;

    @Mock
    private RiskAssessmentService riskAssessmentService;

    @Mock
    private AccountLockoutService accountLockoutService;

    @Mock
    private DynamicConfigService dynamicConfigService;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @Mock
    private AppSecurityProperties appSecurityProperties;

    @InjectMocks
    private AuthenticationService authenticationService;

    private static final String TENANT_ID = "tenant-test";
    private static final String IP = "192.168.1.100";
    private static final String EMAIL_HASH = "emailhash123";
    private static final String MOBILE_HASH = "mobilehash123";
    private static final UUID ACCOUNT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(hashingService.sha256(anyString())).thenReturn(EMAIL_HASH);
        lenient().when(dynamicConfigService.getInt(anyString(), anyString(), anyInt())).thenReturn(30);
        lenient().when(dynamicConfigService.getBoolean(eq("auth.mfa.enforced"), anyString(), anyBoolean())).thenReturn(false);
        lenient().when(dynamicConfigService.getBoolean(eq("auth.stepup.enforced"), anyString(), anyBoolean())).thenReturn(false);
        lenient().when(appSecurityProperties.getSessionIdleTimeoutSeconds()).thenReturn(1800);
    }

    private UserAccount activeAccount() {
        UserAccount account = UserAccount.builder()
                .accountStatus(AccountStatus.ACTIVE)
                .username("user@example.com")
                .emailVerified(true)
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
                .expiresIn(900L)
                .tokenType("Bearer")
                .userId(ACCOUNT_ID.toString())
                .build();
    }

    // -------------------------------------------------------------------------
    // Requirement 2.1: Credential Verification
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Credential Verification")
    class CredentialVerification {

        @Test
        @DisplayName("Returns AuthTokenResponse when credentials and Keycloak auth succeed")
        void successfulAuth() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "validPass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", null, null);
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertAll(
                    () -> assertThat(response.getAccessToken()).isEqualTo("access.jwt.token"),
                    () -> assertThat(response.getTokenType()).isEqualTo("Bearer"),
                    () -> assertThat(response.getUserId()).isEqualTo(ACCOUNT_ID.toString()),
                    () -> verify(activeSessionRepository).save(any(ActiveSession.class))
            );
        }

        @Test
        @DisplayName("Throws AuthenticationException when user account is not found")
        void unknownUser() {
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(userAccountRepository.findByMobileHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());
            when(userAccountRepository.findByUsernameIgnoreCaseAndTenantId("unknown@example.com", TENANT_ID))
                    .thenReturn(Optional.empty());

            AuthTokenRequest request = new AuthTokenRequest("unknown@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("Increments failedAttemptCount and throws AuthenticationException when Keycloak rejects password")
        void badPassword() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "badPass", ACCOUNT_ID.toString()))
                    .thenThrow(new RuntimeException("Keycloak: invalid password"));

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "badPass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid credentials");

            assertThat(account.getFailedAttemptCount()).isEqualTo(1);
            verify(userAccountRepository).save(account);
            verify(accountLockoutService).checkAndLockIfNeeded(account, TENANT_ID);
        }

        @Test
        @DisplayName("Throws AuthenticationException when account is LOCKED")
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
        @DisplayName("Throws AuthenticationException when account is DEACTIVATED")
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
        @DisplayName("Throws AccountNotVerifiedException when account is PENDING_VERIFICATION")
        void pendingVerificationAccount() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AccountNotVerifiedException.class)
                    .satisfies(ex -> {
                        AccountNotVerifiedException anve = (AccountNotVerifiedException) ex;
                        assertThat(anve.getUserId()).isEqualTo(ACCOUNT_ID);
                        assertThat(anve.getEmail()).isEqualTo("user@example.com");
                    })
                    .hasMessageContaining("not yet verified");
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.2: MFA Enforcement
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("MFA Enforcement")
    class MfaEnforcement {

        @Test
        @DisplayName("Throws MfaRequiredException and triggers OTP send when MFA is enabled and no OTP provided")
        void mfaRequiredTriggersOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setMobileHash("mobilehash123");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "validPass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class)
                    .hasMessageContaining("2FA / MFA required");

            verify(otpService).sendOtp(eq(ACCOUNT_ID), eq("mobilehash123"), isNull());
        }

        @Test
        @DisplayName("Succeeds when MFA is enabled and valid OTP is provided")
        void mfaSuccessWithValidOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setMobileHash("mobilehash123");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "validPass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(otpService.verifyOtp("mobilehash123", "123456")).thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", "123456", null);
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response.getAccessToken()).isEqualTo("access.jwt.token");
        }

        @Test
        @DisplayName("Throws AuthenticationException when MFA OTP is invalid")
        void mfaFailsWithInvalidOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setMobileHash("mobilehash123");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "validPass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(otpService.verifyOtp("mobilehash123", "999999")).thenReturn(false);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", "999999", null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid MFA code");
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.5: Device Binding
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Device Binding")
    class DeviceBinding {

        @Test
        @DisplayName("Binds device fingerprint on first login when account has no stored fingerprint")
        void bindsFingerprintOnFirstLogin() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint(null);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", null, "fp-device-abc");
            authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(account.getDeviceFingerprint()).isEqualTo("fp-device-abc");
            verify(userAccountRepository).save(account);
        }

        @Test
        @DisplayName("Allows login when requested fingerprint matches stored fingerprint")
        void allowsMatchingFingerprint() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("fp-device-abc");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", null, "fp-device-abc");
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Rejects login and publishes DENIED_ACCESS when device fingerprint mismatches")
        void rejectsMismatchedFingerprint() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("fp-device-original");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", null, "fp-device-DIFFERENT");

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Device not recognised");

            verify(auditEventPublisher).publish(
                    eq(AuditEventType.DENIED_ACCESS),
                    eq(ACCOUNT_ID.toString()),
                    eq("identity:auth/token"),
                    eq(IP),
                    eq("fp-device-DIFFERENT"),
                    any()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.7: Single Concurrent Session
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
            when(activeSessionRepository.existsByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID))
                    .thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", null, null);
            authenticationService.authenticate(request, TENANT_ID, IP);

            verify(activeSessionRepository).deleteByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Creates new active session record on login")
        void createsNewSession() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID))
                    .thenReturn(false);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "validPass", null, null);
            authenticationService.authenticate(request, TENANT_ID, IP);

            verify(activeSessionRepository).save(any(ActiveSession.class));
            verify(auditEventPublisher).publish(
                    eq(AuditEventType.LOGIN),
                    eq(ACCOUNT_ID.toString()),
                    eq("identity:auth/token"),
                    eq(IP),
                    isNull(),
                    any()
            );
        }
    }

    // -------------------------------------------------------------------------
    // Refresh Token & Sliding Session
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Refresh Token Handling")
    class RefreshTokenHandling {

        @Test
        @DisplayName("Rotates tokens and extends active session on valid refresh request")
        void successfulTokenRefresh() {
            UserAccount account = activeAccount();
            when(keycloakService.refreshToken("valid.refresh.token")).thenReturn(sampleTokens());
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            ActiveSession session = ActiveSession.builder()
                    .userId(ACCOUNT_ID)
                    .sessionToken(UUID.randomUUID().toString())
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            session.setTenantId(TENANT_ID);
            when(activeSessionRepository.findByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(Optional.of(session));

            RefreshTokenRequest request = new RefreshTokenRequest("valid.refresh.token", null);
            AuthTokenResponse response = authenticationService.refreshToken(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access.jwt.token");
            verify(activeSessionRepository).save(session);
        }

        @Test
        @DisplayName("Rejects refresh token when user account is deactivated")
        void rejectsRefreshForDeactivatedUser() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.DEACTIVATED);
            when(keycloakService.refreshToken("valid.refresh.token")).thenReturn(sampleTokens());
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            RefreshTokenRequest request = new RefreshTokenRequest("valid.refresh.token", null);

            assertThatThrownBy(() -> authenticationService.refreshToken(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Account is not active");
        }

        @Test
        @DisplayName("Rejects refresh token when tenant mismatch is detected")
        void rejectsRefreshForTenantMismatch() {
            UserAccount account = activeAccount();
            account.setTenantId("different-tenant");
            when(keycloakService.refreshToken("valid.refresh.token")).thenReturn(sampleTokens());
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            RefreshTokenRequest request = new RefreshTokenRequest("valid.refresh.token", null);

            assertThatThrownBy(() -> authenticationService.refreshToken(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid tenant");
        }
    }
}
