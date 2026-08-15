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

package com.examplatform.questionbank.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA {@link AttributeConverter} that encrypts/decrypts question content fields
 * via Vault Transit (AES-256-GCM) when the feature flag is enabled.
 *
 * <p>When {@code app.encryption.enabled=false} (default), this converter is a
 * transparent no-op — data is stored and read as plain text. Set the flag to
 * {@code true} (via env var {@code QUESTION_ENCRYPTION_ENABLED=true}) to enable
 * Vault-backed encryption, e.g. in staging/production.
 *
 * <p>Mixed-mode safety: if encryption is enabled but a value does not start with
 * the Vault ciphertext prefix {@code vault:}, it is returned as-is so that rows
 * written before encryption was turned on are still readable.
 *
 * Validates: Requirements 4.5, 16.1
 */
@Converter
public class EncryptedFieldConverter implements AttributeConverter<String, String> {

    private static final String VAULT_PREFIX = "vault:";
    private static final String ENCRYPTION_KEY = "question-content-key";

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (!VaultCryptoServiceHolder.isEncryptionEnabled()) {
            return attribute;
        }
        return VaultCryptoServiceHolder.getInstance().encrypt(ENCRYPTION_KEY, attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (!VaultCryptoServiceHolder.isEncryptionEnabled()) {
            return dbData;
        }
        // If the value is not a Vault ciphertext (e.g. written before flag was enabled),
        // return it as-is rather than failing decryption.
        if (!dbData.startsWith(VAULT_PREFIX)) {
            return dbData;
        }
        return VaultCryptoServiceHolder.getInstance().decrypt(ENCRYPTION_KEY, dbData);
    }
}
