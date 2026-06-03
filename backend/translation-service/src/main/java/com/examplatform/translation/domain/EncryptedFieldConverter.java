package com.examplatform.translation.domain;

import com.examplatform.translation.service.VaultCryptoService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts
 * field values using the Vault Transit backend.
 * On write: plaintext → Vault ciphertext (stored in DB).
 * On read: Vault ciphertext → plaintext (returned to application).
 */
@Component
@Converter
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private static final String TRANSIT_KEY = "translation-content-key";

    private static VaultCryptoService vaultCryptoService;

    /**
     * Injected via Spring component scanning. Static reference allows
     * JPA converter (instantiated by Hibernate) to access the Spring bean.
     */
    public EncryptedFieldConverter(VaultCryptoService vaultCryptoService) {
        EncryptedFieldConverter.vaultCryptoService = vaultCryptoService;
    }

    /**
     * Default no-arg constructor required by JPA.
     */
    public EncryptedFieldConverter() {
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return attribute;
        }
        return vaultCryptoService.encrypt(TRANSIT_KEY, attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return dbData;
        }
        // Only decrypt if it looks like a Vault ciphertext
        if (dbData.startsWith("vault:")) {
            return vaultCryptoService.decrypt(TRANSIT_KEY, dbData);
        }
        return dbData;
    }
}
