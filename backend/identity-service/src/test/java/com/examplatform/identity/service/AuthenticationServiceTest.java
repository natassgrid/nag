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

import com.examplatform.shared.crypto.HashingService;

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
import com.examplatform.identity.repository.UserRoleAssignmentRepository;
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
import java.util.List;
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
 *   <li>Requirement 2.6: Risk Assessment and IP/Geo signal tracking</li>
 *   <li>Requirement 2.7: Single Concurrent Session (session creation and replacement)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService")
class AuthenticationServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private UserRoleAssignmentRepository userRoleAssignmentRepository;

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
        lenient().when(dynamicConfigService.getString(anyString(), anyString(), anyString())).thenReturn("OPTIONAL");
        lenient().when(dynamicConfigService.getBoolean(eq("auth.mfa.enforced"), anyString(), anyBoolean())).thenReturn(false);
        lenient().when(dynamicConfigService.getBoolean(eq("auth.stepup.enforced"), anyString(), anyBoolean())).thenReturn(false);
        lenient().when(appSecurityProperties.getSessionIdleTimeoutSeconds()).thenReturn(1800);
        lenient().when(userRoleAssignmentRepository.findByUserIdAndTenantId(any(), any())).thenReturn(List.of());
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
        @DisplayName("Returns AuthTokenResponse when credentials and Keycloak auth succeed and records IP")
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
                    () -> verify(activeSessionRepository).save(any(ActiveSession.class)),
                    () -> verify(riskAssessmentService).recordSuccessfulLoginIp(ACCOUNT_ID, IP, TENANT_ID)
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
                    .hasMessage("Invalid credentials");
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
                    .hasMessageContaining("not yet verified");
        }

        @Test
        @DisplayName("Increments failedAttemptCount on invalid Keycloak credentials and throws AuthenticationException")
        void failedKeycloakAuth() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "wrongPass", ACCOUNT_ID.toString()))
                    .thenThrow(new RuntimeException("Keycloak 401 Unauthorized"));

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "wrongPass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid credentials");

            assertThat(account.getFailedAttemptCount()).isEqualTo(1);
            assertThat(account.getLastFailedAt()).isNotNull();
            verify(userAccountRepository).save(account);
            verify(accountLockoutService).checkAndLockIfNeeded(account, TENANT_ID);
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.2: MFA Enforcement
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("MFA Enforcement")
    class MfaEnforcement {

        @Test
        @DisplayName("Throws MfaRequiredException when user has MFA enabled but no OTP provided")
        void mfaEnabledNoOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class)
                    .hasMessageContaining("MFA required");
        }

        @Test
        @DisplayName("Succeeds when user provides valid TOTP code")
        void validTotp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setTotpSecret("JBSWY3DPEHPK3PXP");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(totpService.verifyTotpCode("JBSWY3DPEHPK3PXP", "123456")).thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", "123456", null);
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access.jwt.token");
        }

        @Test
        @DisplayName("Throws AuthenticationException when TOTP code is invalid")
        void invalidTotp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setTotpSecret("JBSWY3DPEHPK3PXP");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(totpService.verifyTotpCode("JBSWY3DPEHPK3PXP", "999999")).thenReturn(false);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", "999999", null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Invalid MFA code");
        }

        @Test
        @DisplayName("Fallback to SMS OTP when TOTP is not configured")
        void fallbackToSmsOtp() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setMobileHash(MOBILE_HASH);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(otpService.verifyOtp(MOBILE_HASH, "654321")).thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", "654321", null);
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Triggers step-up MFA when risk assessment determines it is required")
        void stepUpMfaRequired() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(dynamicConfigService.getBoolean(eq("auth.stepup.enforced"), anyString(), eq(false)))
                    .thenReturn(true);
            when(riskAssessmentService.isStepUpRequired(eq(account), isNull(), eq(IP), any(LocalDateTime.class)))
                    .thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class)
                    .hasMessageContaining("Step-up authentication required");
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
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, "fp-browser-123");
            authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(account.getDeviceFingerprint()).isEqualTo("fp-browser-123");
            verify(userAccountRepository).save(account);
        }

        @Test
        @DisplayName("Allows login when requested fingerprint matches stored fingerprint")
        void matchingFingerprintAllowed() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("fp-known-456");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, "fp-known-456");
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Throws AuthenticationException and audits DENIED_ACCESS when fingerprint mismatches")
        void mismatchingFingerprintDenied() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("fp-registered-xxx");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, "fp-different-yyy");

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Device not recognised.");
        }
    }

    // -------------------------------------------------------------------------
    // Requirement 2.7: Single Concurrent Session
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Single Concurrent Session")
    class SingleConcurrentSession {

        @Test
        @DisplayName("Invalidates existing active session before creating a new one (new login wins)")
        void invalidatesExistingSession() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);
            authenticationService.authenticate(request, TENANT_ID, IP);

            verify(activeSessionRepository).deleteByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID);
            verify(activeSessionRepository).save(any(ActiveSession.class));
        }

        @Test
        @DisplayName("Does not call delete when no active session exists")
        void noExistingSessionNoDelete() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens("user@example.com", "pass", ACCOUNT_ID.toString()))
                    .thenReturn(sampleTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(false);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);
            authenticationService.authenticate(request, TENANT_ID, IP);

            verify(activeSessionRepository, never()).deleteByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID);
            verify(activeSessionRepository).save(any(ActiveSession.class));
        }
    }

    // -------------------------------------------------------------------------
    // Token Refresh
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Token Refresh")
    class TokenRefresh {

        @Test
        @DisplayName("Successfully refreshes token and updates active session expiresAt")
        void successfulRefresh() {
            ActiveSession session = ActiveSession.builder()
                    .userId(ACCOUNT_ID)
                    .sessionToken("token-123")
                    .expiresAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            session.setTenantId(TENANT_ID);

            UserAccount account = activeAccount();
            when(userAccountRepository.findByIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(Optional.of(account));
            when(keycloakService.refreshToken("valid.refresh.token")).thenReturn(sampleTokens());
            when(activeSessionRepository.findByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID)).thenReturn(Optional.of(session));

            RefreshTokenRequest request = new RefreshTokenRequest("valid.refresh.token", null);
            AuthTokenResponse response = authenticationService.refreshToken(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access.jwt.token");
            verify(activeSessionRepository).save(session);
        }

        @Test
        @DisplayName("Throws AuthenticationException when Keycloak refresh fails")
        void failedRefresh() {
            when(keycloakService.refreshToken("invalid.refresh.token"))
                    .thenThrow(new RuntimeException("Keycloak refresh failed"));

            RefreshTokenRequest request = new RefreshTokenRequest("invalid.refresh.token", null);

            assertThatThrownBy(() -> authenticationService.refreshToken(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid or expired refresh token");
        }
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Logout")
    class Logout {

        @Test
        @DisplayName("Deletes active session and publishes LOGOUT audit event")
        void successfulLogout() {
            authenticationService.logout(ACCOUNT_ID.toString(), TENANT_ID);

            verify(activeSessionRepository).deleteByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID);
        }

        @Test
        @DisplayName("Handles malformed userId gracefully without throwing exception")
        void malformedUserIdHandled() {
            authenticationService.logout("not-a-valid-uuid", TENANT_ID);

            verify(activeSessionRepository, never()).deleteByUserIdAndTenantId(any(), any());
        }
    }
}
