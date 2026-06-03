package com.examplatform.identity.service;

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.identity.domain.WebAuthnCredential;
import com.examplatform.identity.domain.enums.AccountStatus;
import com.examplatform.identity.dto.AuthTokenResponse;
import com.examplatform.identity.dto.WebAuthnAssertionRequest;
import com.examplatform.identity.exception.AuthenticationException;
import com.examplatform.identity.repository.UserAccountRepository;
import com.examplatform.identity.repository.WebAuthnCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WebAuthnService}.
 *
 * <p><strong>Validates: Requirements 2.3</strong>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebAuthnService")
class WebAuthnServiceTest {

    @Mock
    WebAuthnCredentialRepository webAuthnCredentialRepository;
    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    KeycloakService keycloakService;
    @Mock
    AuditEventPublisher auditEventPublisher;

    @InjectMocks
    WebAuthnService webAuthnService;

    private static final String TENANT_ID = "default";
    private static final String IP = "192.168.1.10";
    private static final UUID ACCOUNT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID CREDENTIAL_UUID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final String CREDENTIAL_ID = "test-credential-id-abc";

    // Authenticator data: 37 bytes minimum; bytes 33-36 hold the sign count (big-endian)
    // Sign count = 5 → bytes [0,0,0,5] at positions 33-36
    private static final byte[] AUTH_DATA_BYTES = buildAuthData(5);
    private static final String AUTH_DATA_B64 = Base64.getUrlEncoder().withoutPadding().encodeToString(AUTH_DATA_BYTES);

    private static final String CLIENT_DATA_JSON_B64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"type\":\"webauthn.get\",\"challenge\":\"abc\"}".getBytes());

    private static final String SIGNATURE_B64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});

    private static final byte[] PUBLIC_KEY_COSE = new byte[]{10, 20, 30, 40};

    /**
     * Build a minimal authenticator data byte array with the given sign count at bytes 33-36.
     */
    private static byte[] buildAuthData(long signCount) {
        byte[] data = new byte[37];
        data[33] = (byte) ((signCount >> 24) & 0xFF);
        data[34] = (byte) ((signCount >> 16) & 0xFF);
        data[35] = (byte) ((signCount >> 8) & 0xFF);
        data[36] = (byte) (signCount & 0xFF);
        return data;
    }

    private WebAuthnCredential credential(long signCount) {
        WebAuthnCredential cred = WebAuthnCredential.builder()
                .userId(ACCOUNT_ID)
                .credentialId(CREDENTIAL_ID)
                .publicKeyCose(PUBLIC_KEY_COSE)
                .signCount(signCount)
                .build();
        cred.setTenantId(TENANT_ID);
        ReflectionTestUtils.setField(cred, "id", CREDENTIAL_UUID);
        return cred;
    }

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
                .accessToken("webauthn-access-token")
                .refreshToken("webauthn-refresh-token")
                .expiresIn(900L)
                .build();
    }

    private WebAuthnAssertionRequest validRequest() {
        return WebAuthnAssertionRequest.builder()
                .credentialId(CREDENTIAL_ID)
                .authenticatorData(AUTH_DATA_B64)
                .clientDataJSON(CLIENT_DATA_JSON_B64)
                .signature(SIGNATURE_B64)
                .build();
    }

    // -------------------------------------------------------------------------
    // Test classes
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("Happy path — valid assertion")
    class HappyPath {

        @Test
        @DisplayName("valid assertion returns tokens and updates signCount")
        void validAssertionReturnsTokensAndUpdatesSignCount() {
            WebAuthnCredential cred = credential(3L); // stored sign count = 3, incoming = 5
            when(webAuthnCredentialRepository.findByCredentialIdAndTenantId(CREDENTIAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(cred));
            when(userAccountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(activeAccount()));
            when(keycloakService.getTokens(eq("user@example.com"), eq("")))
                    .thenReturn(stubTokens());

            AuthTokenResponse result = webAuthnService.authenticate(validRequest(), TENANT_ID, IP);

            assertAll(
                    () -> assertThat(result.getAccessToken()).isEqualTo("webauthn-access-token"),
                    () -> assertThat(result.getRefreshToken()).isEqualTo("webauthn-refresh-token"),
                    () -> assertThat(cred.getSignCount()).isEqualTo(5L),
                    () -> verify(webAuthnCredentialRepository).save(cred),
                    () -> verify(auditEventPublisher).publish(any(), anyString(), eq("identity:auth/webauthn"),
                            eq(IP), any(), any())
            );
        }
    }

    @Nested
    @DisplayName("Unknown credential")
    class UnknownCredential {

        @Test
        @DisplayName("throws AuthenticationException when credential not found")
        void throwsWhenCredentialNotFound() {
            when(webAuthnCredentialRepository.findByCredentialIdAndTenantId(CREDENTIAL_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> webAuthnService.authenticate(validRequest(), TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Unknown WebAuthn credential");
        }
    }

    @Nested
    @DisplayName("Account not active")
    class AccountNotActive {

        @Test
        @DisplayName("throws AuthenticationException when account is not active")
        void throwsWhenAccountNotActive() {
            WebAuthnCredential cred = credential(0L);
            UserAccount account = activeAccount();
            account.setAccountStatus(AccountStatus.LOCKED);

            when(webAuthnCredentialRepository.findByCredentialIdAndTenantId(CREDENTIAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(cred));
            when(userAccountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(account));

            assertThatThrownBy(() -> webAuthnService.authenticate(validRequest(), TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("Account is not active");
        }
    }

    @Nested
    @DisplayName("Invalid signature")
    class InvalidSignature {

        @Test
        @DisplayName("throws AuthenticationException when authenticator data is empty")
        void throwsWhenAuthenticatorDataEmpty() {
            WebAuthnCredential cred = credential(0L);
            when(webAuthnCredentialRepository.findByCredentialIdAndTenantId(CREDENTIAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(cred));
            when(userAccountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(activeAccount()));

            // Build request with empty authenticator data (Base64URL of empty bytes)
            WebAuthnAssertionRequest request = WebAuthnAssertionRequest.builder()
                    .credentialId(CREDENTIAL_ID)
                    .authenticatorData(Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[0]))
                    .clientDataJSON(CLIENT_DATA_JSON_B64)
                    .signature(SIGNATURE_B64)
                    .build();

            assertThatThrownBy(() -> webAuthnService.authenticate(request, TENANT_ID, IP))
                    .isInstanceOf(AuthenticationException.class)
                    .hasMessageContaining("signature verification failed");
        }
    }

    @Nested
    @DisplayName("Sign count not increasing")
    class SignCountNotIncreasing {

        @Test
        @DisplayName("still succeeds when signCount does not increase (logs warning)")
        void succeedsWithWarningWhenSignCountNotIncreasing() {
            // Stored sign count = 10, incoming sign count = 5 (lower)
            WebAuthnCredential cred = credential(10L);
            when(webAuthnCredentialRepository.findByCredentialIdAndTenantId(CREDENTIAL_ID, TENANT_ID))
                    .thenReturn(Optional.of(cred));
            when(userAccountRepository.findById(ACCOUNT_ID))
                    .thenReturn(Optional.of(activeAccount()));
            when(keycloakService.getTokens(eq("user@example.com"), eq("")))
                    .thenReturn(stubTokens());

            AuthTokenResponse result = webAuthnService.authenticate(validRequest(), TENANT_ID, IP);

            assertAll(
                    () -> assertThat(result.getAccessToken()).isEqualTo("webauthn-access-token"),
                    // Sign count is still updated to the new (lower) value
                    () -> assertThat(cred.getSignCount()).isEqualTo(5L),
                    () -> verify(webAuthnCredentialRepository).save(cred)
            );
        }
    }
}
