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

package com.examplatform.translation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.Ciphertext;
import org.springframework.vault.support.Plaintext;

/**
 * Vault Transit-backed implementation of {@link VaultCryptoService}.
 * Encrypts/decrypts translation content; key material never leaves Vault.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VaultCryptoServiceImpl implements VaultCryptoService {

    private final VaultTemplate vaultTemplate;

    private VaultTransitOperations transit() {
        return vaultTemplate.opsForTransit();
    }

    @Override
    public String encrypt(String keyName, String plaintext) {
        try {
            return transit().encrypt(keyName, Plaintext.of(plaintext)).getCiphertext();
        } catch (Exception e) {
            log.error("Vault encrypt failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String decrypt(String keyName, String ciphertext) {
        try {
            return transit().decrypt(keyName, Ciphertext.of(ciphertext)).asString();
        } catch (Exception e) {
            log.error("Vault decrypt failed for key [{}]: {}", keyName, e.getMessage());
            throw new RuntimeException("Decryption failed: " + e.getMessage(), e);
        }
    }
}
