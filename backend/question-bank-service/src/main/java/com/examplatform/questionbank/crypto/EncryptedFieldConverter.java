package com.examplatform.questionbank.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA {@link AttributeConverter} that transparently encrypts/decrypts
 * question content fields using Vault Transit (AES-256-GCM).
 *
 * <p>Because JPA converters are instantiated by Hibernate (not Spring),
 * this class obtains the {@link com.examplatform.questionbank.service.VaultCryptoService}
 * via the static {@link VaultCryptoServiceHolder}.
 *
 * <p>Apply on entity fields with:
 * {@code @Convert(converter = EncryptedFieldConverter.class)}
 *
 * Validates: Requirements 4.5, 16.1
 */
@Converter
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private static final String ENCRYPTION_KEY = "question-content-key";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        return VaultCryptoServiceHolder.getInstance().encrypt(ENCRYPTION_KEY, attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return VaultCryptoServiceHolder.getInstance().decrypt(ENCRYPTION_KEY, dbData);
    }
}
