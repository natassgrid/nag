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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    private AuthTokenResponse stubTokens() {
        return AuthTokenResponse.builder()
                .accessToken("access-token-abc")
                .refreshToken("refresh-token-xyz")
                .expiresIn(900L)
                .build();
    }

    // -------------------------------------------------------------------------
    // Nested test classes
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Happy path — no MFA")
    class HappyPathNoMfa {

        @Test
        @DisplayName("valid credentials create session and return tokens")
        void returnsTokensAndCreatesSession() {
            UserAccount account = activeAccount();
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(false);

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .build();

            AuthTokenResponse result = authenticationService.authenticate(request, TENANT_ID, IP);

            assertAll(
                    () -> assertThat(result.getAccessToken()).isEqualTo("access-token-abc"),
                    () -> assertThat(result.getRefreshToken()).isEqualTo("refresh-token-xyz"),
                    () -> verify(activeSessionRepository).save(any(ActiveSession.class)),
                    () -> verify(userAccountRepository).save(account)
            );
        }
    }

    @Nested
    @DisplayName("Happy path — with MFA")
    class HappyPathWithMfa {

        @Test
        @DisplayName("valid credentials and valid OTP return tokens")
        void returnsTokensWhenOtpValid() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());
            when(otpService.verifyOtp(EMAIL_HASH, "123456")).thenReturn(true);
            when(activeSessionRepository.existsByUserIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(false);

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .otpCode("123456")
                    .build();

            AuthTokenResponse result = authenticationService.authenticate(request, TENANT_ID, IP);

            assertAll(
                    () -> assertThat(result.getAccessToken()).isNotBlank(),
                    () -> verify(otpService).verifyOtp(EMAIL_HASH, "123456"),
                    () -> verify(activeSessionRepository).save(any(ActiveSession.class))
            );
        }
    }

    @Nested
    @DisplayName("MFA required but not provided")
    class MfaRequiredButMissing {

        @Test
        @DisplayName("throws MfaRequiredException when OTP is null")
        void throwsWhenOtpNull() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .otpCode(null)
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class)
                    .hasMessageContaining("MFA required");
        }

        @Test
        @DisplayName("throws MfaRequiredException when OTP is blank")
        void throwsWhenOtpBlank() {
            UserAccount account = activeAccount();
            account.setMfaEnabled(true);

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .otpCode("   ")
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(MfaRequiredException.class);
        }
    }

    @Nested
    @DisplayName("Invalid credentials")
    class InvalidCredentials {

        @Test
        @DisplayName("throws AuthenticationException when Keycloak rejects credentials")
        void keycloakThrows() {
            UserAccount account = activeAccount();

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any()))
                    .thenThrow(new RuntimeException("401 Unauthorized"));

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("wrongpass")
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid credentials");

            // failed attempt count should have been incremented and saved
            assertAll(
                    () -> assertThat(account.getFailedAttemptCount()).isEqualTo(1),
                    () -> verify(userAccountRepository).save(account)
            );
        }

        @Test
        @DisplayName("throws AuthenticationException when account is not found")
        void accountNotFound() {
            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.empty());

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("ghost@example.com")
                    .password("any")
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessage("Invalid credentials");
        }
    }

    @Nested
    @DisplayName("Account status checks")
    class AccountStatusChecks {

        @Test
        @DisplayName("throws AuthenticationException for LOCKED account")
        void lockedAccount() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.LOCKED);

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("pass")
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("locked");
        }

        @Test
        @DisplayName("throws AuthenticationException for PENDING_VERIFICATION account")
        void pendingAccount() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.PENDING_VERIFICATION);

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("pass")
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("not yet verified");
        }

        @Test
        @DisplayName("throws AuthenticationException for DEACTIVATED account")
        void deactivatedAccount() {
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.DEACTIVATED);

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("pass")
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("deactivated");
        }
    }

    @Nested
    @DisplayName("Device fingerprint enforcement")
    class DeviceFingerprint {

        @Test
        @DisplayName("throws AuthenticationException when device fingerprint does not match stored one")
        void fingerprintMismatch() {
            UserAccount account = activeAccount();
            account.setDeviceFingerprint("stored-fp-abc");

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .deviceFingerprint("different-fp-xyz")
                    .build();

            assertThatThrownBy(() -> authenticationService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Device not recognised");
        }

        @Test
        @DisplayName("binds device fingerprint when account has none and request provides one")
        void bindsFingerprintOnFirstLogin() {
            UserAccount account = activeAccount();
            // no stored fingerprint
            account.setDeviceFingerprint(null);

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(false);

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .deviceFingerprint("new-device-fp")
                    .build();

            authenticationService.authenticate(request, TENANT_ID, IP);

            assertThat(account.getDeviceFingerprint()).isEqualTo("new-device-fp");
        }
    }

    @Nested
    @DisplayName("Single concurrent session enforcement")
    class SingleConcurrentSession {

        @Test
        @DisplayName("invalidates existing session when new login arrives")
        void existingSessionIsInvalidated() {
            UserAccount account = activeAccount();

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(true);  // session already exists

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .build();

            authenticationService.authenticate(request, TENANT_ID, IP);

            assertAll(
                    // old session must be deleted
                    () -> verify(activeSessionRepository).deleteByUserIdAndTenantId(any(), eq(TENANT_ID)),
                    // new session must be created
                    () -> verify(activeSessionRepository).save(any(ActiveSession.class))
            );
        }

        @Test
        @DisplayName("no delete when no prior session exists")
        void noDeleteWhenNoSession() {
            UserAccount account = activeAccount();

            when(userAccountRepository.findByEmailHashAndTenantId(EMAIL_HASH, TENANT_ID))
                    .thenReturn(Optional.of(account));
            when(keycloakService.getTokens(any(), any())).thenReturn(stubTokens());
            when(activeSessionRepository.existsByUserIdAndTenantId(any(), eq(TENANT_ID)))
                    .thenReturn(false);

            AuthTokenRequest request = AuthTokenRequest.builder()
                    .username("user@example.com")
                    .password("Password1!")
                    .build();

            authenticationService.authenticate(request, TENANT_ID, IP);

            verify(activeSessionRepository, never()).deleteByUserIdAndTenantId(any(), any());
            verify(activeSessionRepository).save(any(ActiveSession.class));
        }
    }
}
