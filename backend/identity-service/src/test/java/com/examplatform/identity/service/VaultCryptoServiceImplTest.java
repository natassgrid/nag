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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;
import org.springframework.vault.support.Signature;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VaultCryptoServiceImpl}.
 *
 * Validates: Requirements 16.3, 16.4, 16.5
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VaultCryptoServiceImpl")
class VaultCryptoServiceImplTest {

    @Mock
    private VaultTemplate vaultTemplate;

    @Mock
    private VaultTransitOperations transitOperations;

    @InjectMocks
    private VaultCryptoServiceImpl vaultCryptoService;

    private void setupTransit() {
        when(vaultTemplate.opsForTransit()).thenReturn(transitOperations);
    }

    @Nested
    @DisplayName("encrypt")
    class Encrypt {

        @Test
        @DisplayName("calls transit encrypt and returns ciphertext")
        void callsTransitEncryptAndReturnsCiphertext() {
            setupTransit();
            String expectedCiphertext = "vault:v1:abc123encrypted";
            Ciphertext ciphertext = mock(Ciphertext.class);
            when(ciphertext.getCiphertext()).thenReturn(expectedCiphertext);
            when(transitOperations.encrypt(eq("my-key"), any(Plaintext.class))).thenReturn(ciphertext);

            String result = vaultCryptoService.encrypt("my-key", "secret-data");

            assertThat(result).isEqualTo(expectedCiphertext);
            verify(transitOperations).encrypt(eq("my-key"), any(Plaintext.class));
        }

        @Test
        @DisplayName("throws RuntimeException on Vault failure")
        void throwsRuntimeExceptionOnVaultFailure() {
            setupTransit();
            when(transitOperations.encrypt(eq("my-key"), any(Plaintext.class)))
                    .thenThrow(new RuntimeException("Vault unreachable"));

            assertThatThrownBy(() -> vaultCryptoService.encrypt("my-key", "secret-data"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Encryption failed");
        }
    }

    @Nested
    @DisplayName("decrypt")
    class Decrypt {

        @Test
        @DisplayName("calls transit decrypt and returns plaintext")
        void callsTransitDecryptAndReturnsPlaintext() {
            setupTransit();
            Plaintext plaintext = Plaintext.of("secret-data");
            when(transitOperations.decrypt(eq("my-key"), any(Ciphertext.class))).thenReturn(plaintext);

            String result = vaultCryptoService.decrypt("my-key", "vault:v1:abc123encrypted");

            assertThat(result).isEqualTo("secret-data");
            verify(transitOperations).decrypt(eq("my-key"), any(Ciphertext.class));
        }
    }

    @Nested
    @DisplayName("sign")
    class Sign {

        @Test
        @DisplayName("calls transit sign and returns signature")
        void callsTransitSignAndReturnsSignature() {
            setupTransit();
            String expectedSignature = "vault:v1:signature-base64";
            Signature signature = Signature.of(expectedSignature);
            when(transitOperations.sign(eq("signing-key"), any(Plaintext.class))).thenReturn(signature);

            String result = vaultCryptoService.sign("signing-key", "data-to-sign");

            assertThat(result).isEqualTo(expectedSignature);
            verify(transitOperations).sign(eq("signing-key"), any(Plaintext.class));
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("returns true for valid signature")
        void returnsTrueForValidSignature() {
            setupTransit();
            when(transitOperations.verify(eq("signing-key"), any(Plaintext.class), any(Signature.class)))
                    .thenReturn(true);

            boolean result = vaultCryptoService.verify("signing-key", "payload", "vault:v1:sig");

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("returns false for invalid signature")
        void returnsFalseForInvalidSignature() {
            setupTransit();
            when(transitOperations.verify(eq("signing-key"), any(Plaintext.class), any(Signature.class)))
                    .thenReturn(false);

            boolean result = vaultCryptoService.verify("signing-key", "payload", "vault:v1:bad-sig");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("returns false on exception")
        void returnsFalseOnException() {
            setupTransit();
            when(transitOperations.verify(eq("signing-key"), any(Plaintext.class), any(Signature.class)))
                    .thenThrow(new RuntimeException("Vault error"));

            boolean result = vaultCryptoService.verify("signing-key", "payload", "vault:v1:sig");

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("rotateKey")
    class RotateKey {

        @Test
        @DisplayName("calls transit rotate")
        void callsTransitRotate() {
            setupTransit();

            vaultCryptoService.rotateKey("my-key");

            verify(transitOperations).rotate("my-key");
        }
    }

    @Nested
    @DisplayName("revokeKey")
    class RevokeKey {

        @Test
        @DisplayName("calls vaultTemplate write and delete for key revocation")
        void callsWriteAndDeleteForRevocation() {
            vaultCryptoService.revokeKey("compromised-key");

            verify(vaultTemplate).write(eq("transit/keys/compromised-key/config"),
                    eq(Map.of("deletion_allowed", true)));
            verify(vaultTemplate).delete("transit/keys/compromised-key");
        }
    }
}
