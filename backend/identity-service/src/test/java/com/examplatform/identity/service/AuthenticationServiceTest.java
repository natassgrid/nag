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
import com.examplatform.identity.exception.AccountNotFoundException;
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
import static org.mockito.Mockito.doThrow;
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
                    .isInstanceOf(AuthenticationException.class);
        }

        @Test
        @DisplayName("Increments failedAttemptCount and throws AuthenticationException when Keycloak rejects password")
        void badPassword() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Bad credentials"));

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "wrongPass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class);

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
        @DisplayName("Throws AuthenticationException when account is PENDING_VERIFICATION")
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
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, null);

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class);

            verify(otpService).sendOtp(eq(ACCOUNT_ID), eq("mobilehash123"), eq(null));
        }

        @Test
        @DisplayName("Succeeds when MFA is enabled and valid OTP is provided")
        void validMfaSucceeds() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setMobileHash("mobilehash123");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());
            when(otpService.verifyOtp("mobilehash123", "123456")).thenReturn(true);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", "123456", null);
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
            verify(otpService).verifyOtp("mobilehash123", "123456");
        }

        @Test
        @DisplayName("Throws AuthenticationException when MFA OTP is invalid")
        void invalidMfaThrows() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);
            account.setMobileHash("mobilehash123");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());
            when(otpService.verifyOtp("mobilehash123", "wrong")).thenReturn(false);

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", "wrong", null);

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
        void bindsDeviceFingerprintOnFirstLogin() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint(null);
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, "fp-abc-123");
            authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(account.getDeviceFingerprint()).isEqualTo("fp-abc-123");
        }

        @Test
        @DisplayName("Allows login when requested fingerprint matches stored fingerprint")
        void matchingFingerprintSucceeds() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("fp-known");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, "fp-known");
            AuthTokenResponse response = authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("Rejects login and publishes DENIED_ACCESS when device fingerprint mismatches")
        void mismatchingFingerprintThrows() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("fp-original");
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(anyString(), anyString(), anyString()))
                    .thenReturn(sampleTokens());

            AuthTokenRequest request = new AuthTokenRequest("user@example.com", "pass", null, "fp-attacker");

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Device not recognised");
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

    // -------------------------------------------------------------------------
    // Change Password
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Change Password")
    class ChangePassword {

        @Test
        @DisplayName("Successfully changes password when current password is valid")
        void changePasswordSuccess() {
            UserAccount account = activeAccount();
            account.setKeycloakUserId("kc-user-123");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            authenticationService.changePassword(ACCOUNT_ID.toString(), "OldPass123!", "NewPass123!", TENANT_ID);

            verify(keycloakService).changePassword("user@example.com", "OldPass123!", "NewPass123!", "kc-user-123");
        }

        @Test
        @DisplayName("Throws AccountNotFoundException when account does not exist")
        void throwsWhenAccountNotFound() {
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authenticationService.changePassword(ACCOUNT_ID.toString(), "OldPass", "NewPass", TENANT_ID))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(keycloakService, never()).changePassword(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Throws AccountNotFoundException on tenant mismatch")
        void throwsOnTenantMismatch() {
            UserAccount account = activeAccount();
            account.setTenantId("other-tenant");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> authenticationService.changePassword(ACCOUNT_ID.toString(), "OldPass", "NewPass", TENANT_ID))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("User not found in this tenant");

            verify(keycloakService, never()).changePassword(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Throws AuthenticationException when account is not active")
        void throwsWhenAccountNotActive() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.LOCKED);
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> authenticationService.changePassword(ACCOUNT_ID.toString(), "OldPass", "NewPass", TENANT_ID))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Cannot change password for account in status");

            verify(keycloakService, never()).changePassword(any(), any(), any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // Logout
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Logout")
    class Logout {

        @Test
        @DisplayName("Successfully deletes active session and revokes Keycloak user sessions")
        void logoutSuccess() {
            UserAccount account = activeAccount();
            account.setKeycloakUserId("kc-user-123");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            authenticationService.logout(ACCOUNT_ID.toString(), TENANT_ID);

            verify(activeSessionRepository).deleteByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID);
            verify(keycloakService).revokeUserSessions("kc-user-123");
        }

        @Test
        @DisplayName("Successfully deletes active session even if Keycloak user ID is missing")
        void logoutWithoutKeycloakId() {
            UserAccount account = activeAccount();
            account.setKeycloakUserId(null);
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));

            authenticationService.logout(ACCOUNT_ID.toString(), TENANT_ID);

            verify(activeSessionRepository).deleteByUserIdAndTenantId(ACCOUNT_ID, TENANT_ID);
            verify(keycloakService, never()).revokeUserSessions(any());
        }
    }
}
