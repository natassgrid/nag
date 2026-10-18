/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

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

    /**
     * -- GETTER --
     *  Returns
     *  when encryption is currently enabled.
     *  Callers use this to set the
     *  flag on new rows.
     */
    @Getter
    @Value("${app.translation.encryption.enabled:false}")
    private boolean encryptionEnabled;

    /**
     * Serialize (and optionally encrypt) a payload for storage.
     * Automatically strips stray underscore artifacts around formulas and non-translatable tokens.
     *
     * @param payload the structured translation payload
     * @return the string to write into the {@code translated_payload} column
     */
    public String serialize(TranslatedQuestionPayload payload) {
        if (payload == null) {
            return null;
        }
        try {
            TranslatedQuestionPayload sanitized = sanitizePayload(payload);
            String json = objectMapper.writeValueAsString(sanitized);
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
     * Cleans any stray underscore artifacts around formulas and numbers left by translation models.
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
            TranslatedQuestionPayload payload = objectMapper.readValue(json, TranslatedQuestionPayload.class);
            return sanitizePayload(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize TranslatedQuestionPayload", e);
        }
    }

    private TranslatedQuestionPayload sanitizePayload(TranslatedQuestionPayload payload) {
        if (payload == null) {
            return null;
        }
        List<TranslatedQuestionPayload.TranslatedOption> cleanedOptions = null;
        if (payload.options() != null) {
            cleanedOptions = payload.options().stream()
                    .map(opt -> new TranslatedQuestionPayload.TranslatedOption(
                            opt.id(),
                            LatexPreservationUtil.cleanStrayUnderscores(opt.text()),
                            opt.imageUrl(),
                            opt.imageAltText()))
                    .toList();
        }
        return new TranslatedQuestionPayload(
                LatexPreservationUtil.cleanStrayUnderscores(payload.content()),
                cleanedOptions,
                LatexPreservationUtil.cleanStrayUnderscores(payload.explanation())
        );
    }

}
