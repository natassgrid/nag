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

package com.examplatform.candidate.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA {@link AttributeConverter} that transparently encrypts/decrypts
 * PII fields using Vault Transit (AES-256-GCM).
 *
 * <p>Because JPA converters are instantiated by Hibernate (not Spring),
 * this class obtains the {@link com.examplatform.candidate.service.VaultCryptoService}
 * via the static {@link VaultCryptoServiceHolder}.
 *
 * <p>Apply on entity fields with:
 * {@code @Convert(converter = EncryptedFieldConverter.class)}
 *
 * Validates: Requirements 1.6, 16.1, 25.1
 */
@Slf4j
@Converter
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private static final String ENCRYPTION_KEY = "candidate-pii-key";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return VaultCryptoServiceHolder.getInstance().encrypt(ENCRYPTION_KEY, attribute);
        } catch (Exception e) {
            log.warn("Vault encryption failed for attribute, storing unencrypted fallback: {}", e.getMessage());
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (!dbData.startsWith("vault:v")) {
            return dbData;
        }
        try {
            return VaultCryptoServiceHolder.getInstance().decrypt(ENCRYPTION_KEY, dbData);
        } catch (Exception e) {
            log.warn("Vault decryption failed for data, returning raw payload: {}", e.getMessage());
            return dbData;
        }
    }
}
