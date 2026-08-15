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

/**
 * Contract for HSM/Vault-backed cryptographic operations.
 * Used for encrypting translation content at rest via Vault Transit.
 */
public interface VaultCryptoService {

    /**
     * Encrypt the given plaintext using the named Vault Transit key.
     *
     * @param keyName   name of the Transit key to use
     * @param plaintext data to encrypt
     * @return Vault ciphertext blob (vault:v1:...)
     */
    String encrypt(String keyName, String plaintext);

    /**
     * Decrypt the given Vault ciphertext using the named Transit key.
     *
     * @param keyName    name of the Transit key to use
     * @param ciphertext Vault ciphertext blob
     * @return original plaintext
     */
    String decrypt(String keyName, String ciphertext);
}
