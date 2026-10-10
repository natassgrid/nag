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

package com.examplatform.shared.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("HashingService Unit Tests")
class HashingServiceTest {

    private final HashingService hashingService = new HashingService();

    @Nested
    @DisplayName("SHA-256")
    class Sha256 {

        @Test
        @DisplayName("known input produces expected hex")
        void knownInput() {
            assertThat(hashingService.sha256("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
            assertThat(HashingService.computeSha256("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        }

        @Test
        @DisplayName("null input returns null")
        void nullInput() {
            assertThat(hashingService.sha256(null)).isNull();
            assertThat(HashingService.computeSha256(null)).isNull();
        }

        @Test
        @DisplayName("different inputs produce different hashes")
        void differentInputs() {
            assertThat(hashingService.sha256("input1"))
                    .isNotEqualTo(hashingService.sha256("input2"));
        }

        @Test
        @DisplayName("same input always produces same hash")
        void deterministic() {
            String hash1 = hashingService.sha256("test-value");
            String hash2 = hashingService.sha256("test-value");
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("output is lowercase 64-character hex string")
        void outputFormat() {
            String hash = hashingService.sha256("any-input");
            assertThat(hash)
                    .hasSize(64)
                    .matches("[0-9a-f]+");
        }
    }

    @Nested
    @DisplayName("HMAC")
    class Hmac {

        @Test
        @DisplayName("same input and key produce same output")
        void deterministic() {
            String h1 = hashingService.hmac("data", "key");
            String h2 = hashingService.hmac("data", "key");
            assertThat(h1).isEqualTo(h2);
            assertThat(HashingService.computeHmac("data", "key")).isEqualTo(h1);
        }

        @Test
        @DisplayName("null input or key returns null")
        void nullHandling() {
            assertThat(hashingService.hmac(null, "key")).isNull();
            assertThat(hashingService.hmac("data", null)).isNull();
        }

        @Test
        @DisplayName("different keys produce different HMACs")
        void differentKeys() {
            assertThat(hashingService.hmac("data", "key1"))
                    .isNotEqualTo(hashingService.hmac("data", "key2"));
        }

        @Test
        @DisplayName("different inputs produce different HMACs for same key")
        void differentInputs() {
            assertThat(hashingService.hmac("data1", "key"))
                    .isNotEqualTo(hashingService.hmac("data2", "key"));
        }
    }
}
