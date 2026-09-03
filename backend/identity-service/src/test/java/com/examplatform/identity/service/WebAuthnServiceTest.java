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
 * You should have received a copy of the GNU标识 Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebAuthnService")
class WebAuthnServiceTest {

    @Mock
    private WebAuthnCredentialRepository webAuthnCredentialRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private KeycloakService keycloakService;

    @Mock
    private AuditEventPublisher auditEventPublisher;

    @InjectMocks
    private WebAuthnService webAuthnService;

    private static final String TENANT_ID = "test-tenant";
    private static final String IP = "127.0.0.1";
    private static final UUID ACCOUNT_ID = UUID.fromString("018f0000-0000-7000-8000-000000000001");
    private static final String CREDENTIAL_ID = "test-cred-id-base64url";

    // Valid Base64URL-encoded test data
    // 37 bytes: 32 bytes rpIdHash + 1 byte flags + 4 bytes sign count (value = 5)
    private static final byte[] AUTH_DATA_BYTES = new byte[37];
    static {
        // Set sign count to 5 (bytes 33..36 in big-endian)
        AUTH_DATA_BYTES[33] = 0;
        AUTH_DATA_BYTES[34] = 0;
        AUTH_DATA_BYTES[35] = 0;
        AUTH_DATA_BYTES[36] = 5;
    }
    private static final String AUTH_DATA_B64 = Base64.getUrlEncoder().withoutPadding().encodeToString(AUTH_DATA_BYTES);
    private static final String CLIENT_DATA_JSON_B64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("{\"type\":\"webauthn.get\",\"challenge\":\"test\"}".getBytes());
    private static final String SIGNATURE_B64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("dummy-signature-bytes".getBytes());
    private static final byte[] DUMMY_PUBLIC_KEY = "dummy-cose-key".getBytes();

    @BeforeEach
    void setUp() {
        // Default lenient stubs if needed
    }

    private UserAccount activeAccount() {
        UserAccount acc = UserAccount.builder()
                .username("user@example.com")
                .accountStatus(AccountStatus.ACTIVE)
                .build();
        acc.setTenantId(TENANT_ID);
        try {
            var f = acc.getClass().getSuperclass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(acc, ACCOUNT_ID);
        } catch (Exception e) {
            // ignore
        }
        return acc;
    }

    private WebAuthnCredential credential(long signCount) {
        return WebAuthnCredential.builder()
                .credentialId(CREDENTIAL_ID)
                .userId(ACCOUNT_ID)
                .publicKeyCose(DUMMY_PUBLIC_KEY)
                .signCount(signCount)
                .aaguid("00000000-0000-0000-0000-000000000000")
                .build();
    }

    private AuthTokenResponse stubTokens() {
        return AuthTokenResponse.builder()
                .accessToken("webauthn-access-token")
                .refreshToken("webauthn-refresh-token")
                .tokenType("Bearer")
                .expiresIn(900)
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
            when(keycloakService.getTokens(eq("user@example.com"), eq(""), eq(ACCOUNT_ID.toString())))
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
            when(keycloakService.getTokens(eq("user@example.com"), eq(""), eq(ACCOUNT_ID.toString())))
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
