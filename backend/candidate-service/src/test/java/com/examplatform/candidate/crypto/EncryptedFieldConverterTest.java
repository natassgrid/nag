package com.examplatform.candidate.crypto;

import com.examplatform.candidate.service.VaultCryptoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link EncryptedFieldConverter}.
 * Verifies encryption/decryption delegation and null handling.
 */
class EncryptedFieldConverterTest {

    private VaultCryptoService mockCryptoService;
    private EncryptedFieldConverter converter;

    @BeforeEach
    void setUp() {
        mockCryptoService = mock(VaultCryptoService.class);
        // Inject mock via the static holder
        new VaultCryptoServiceHolder(mockCryptoService);
        converter = new EncryptedFieldConverter();
    }

    @AfterEach
    void tearDown() {
        // Reset static holder to avoid cross-test pollution
        new VaultCryptoServiceHolder(null);
    }

    @Test
    @DisplayName("convertToDatabaseColumn should delegate to VaultCryptoService.encrypt")
    void convertToDatabaseColumn_callsEncrypt() {
        String plaintext = "John Doe";
        String expectedCiphertext = "vault:v1:encrypted_blob";
        when(mockCryptoService.encrypt(eq("candidate-pii-key"), eq(plaintext)))
                .thenReturn(expectedCiphertext);

        String result = converter.convertToDatabaseColumn(plaintext);

        assertThat(result).isEqualTo(expectedCiphertext);
        verify(mockCryptoService).encrypt("candidate-pii-key", plaintext);
    }

    @Test
    @DisplayName("convertToEntityAttribute should delegate to VaultCryptoService.decrypt")
    void convertToEntityAttribute_callsDecrypt() {
        String ciphertext = "vault:v1:encrypted_blob";
        String expectedPlaintext = "John Doe";
        when(mockCryptoService.decrypt(eq("candidate-pii-key"), eq(ciphertext)))
                .thenReturn(expectedPlaintext);

        String result = converter.convertToEntityAttribute(ciphertext);

        assertThat(result).isEqualTo(expectedPlaintext);
        verify(mockCryptoService).decrypt("candidate-pii-key", ciphertext);
    }

    @Test
    @DisplayName("convertToDatabaseColumn should return null for null input without calling Vault")
    void convertToDatabaseColumn_nullInput_returnsNullWithoutCallingVault() {
        String result = converter.convertToDatabaseColumn(null);

        assertThat(result).isNull();
        verifyNoInteractions(mockCryptoService);
    }

    @Test
    @DisplayName("convertToEntityAttribute should return null for null input without calling Vault")
    void convertToEntityAttribute_nullInput_returnsNullWithoutCallingVault() {
        String result = converter.convertToEntityAttribute(null);

        assertThat(result).isNull();
        verifyNoInteractions(mockCryptoService);
    }
}
