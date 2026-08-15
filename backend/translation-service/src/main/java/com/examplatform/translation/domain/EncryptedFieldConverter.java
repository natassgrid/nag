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
