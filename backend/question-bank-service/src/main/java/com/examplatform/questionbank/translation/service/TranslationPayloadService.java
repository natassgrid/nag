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

package com.examplatform.questionbank.translation.service;

import com.examplatform.questionbank.crypto.VaultCryptoServiceHolder;
import com.examplatform.questionbank.translation.domain.TranslatedQuestionPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Handles serialization and optional encryption of {@link TranslatedQuestionPayload}.
 *
 * <h3>Encryption toggle</h3>
 * <p>Controlled by {@code app.translation.encryption.enabled} (default {@code false}).
 * When disabled the payload is stored and returned as plain JSON.
 * When enabled the JSON string is encrypted via Vault Transit using the key
 * {@code translation-content-key} — the same Vault backend used by
 * {@link com.examplatform.questionbank.crypto.EncryptedFieldConverter}.
 *
 * <p>The per-row {@code payload_encrypted} column is the authoritative flag
 * that readers use to decide how to decode a value, so rows written under
 * different settings coexist safely.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationPayloadService {

    private static final String VAULT_KEY = "translation-content-key";
    private static final String VAULT_PREFIX = "vault:";

    private final ObjectMapper objectMapper;

    @Value("${app.translation.encryption.enabled:false}")
    private boolean encryptionEnabled;

    /**
     * Serialize (and optionally encrypt) a payload for storage.
     *
     * @param payload the structured translation payload
     * @return the string to write into the {@code translated_payload} column
     */
    public String serialize(TranslatedQuestionPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (encryptionEnabled) {
                return VaultCryptoServiceHolder.getInstance().encrypt(VAULT_KEY, json);
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize TranslatedQuestionPayload", e);
        }
    }

    /**
     * Deserialize (and optionally decrypt) a raw stored string.
     *
     * @param stored           raw value from the {@code translated_payload} column
     * @param payloadEncrypted the per-row {@code payload_encrypted} flag
     * @return the deserialized payload, or {@code null} when {@code stored} is null
     */
    public TranslatedQuestionPayload deserialize(String stored, boolean payloadEncrypted) {
        if (stored == null) {
            return null;
        }
        try {
            String json;
            if (payloadEncrypted && stored.startsWith(VAULT_PREFIX)) {
                json = VaultCryptoServiceHolder.getInstance().decrypt(VAULT_KEY, stored);
            } else {
                // Either not encrypted, or written before the flag was enabled — read as-is.
                json = stored;
            }
            return objectMapper.readValue(json, TranslatedQuestionPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize TranslatedQuestionPayload", e);
        }
    }

    /**
     * Returns {@code true} when encryption is currently enabled.
     * Callers use this to set the {@code payload_encrypted} flag on new rows.
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
}
