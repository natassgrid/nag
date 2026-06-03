package com.examplatform.identity.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HashingService}.
 *
 * Validates: Requirements 1.1, 1.5
 */
@DisplayName("HashingService")
class HashingServiceTest {

    private final HashingService hashingService = new HashingService();

    @Nested
    @DisplayName("SHA-256")
    class Sha256 {

        @Test
        @DisplayName("known input produces expected hex")
        void knownInput() {
            // SHA-256("abc") = ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469348423f656d6cacc0
            assertThat(hashingService.sha256("abc"))
                    .isEqualTo("ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469348423f656d6cacc0");
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

        @Test
        @DisplayName("output is lowercase 64-character hex string")
        void outputFormat() {
            String hmac = hashingService.hmac("any-input", "any-key");
            assertThat(hmac)
                    .hasSize(64)
                    .matches("[0-9a-f]+");
        }
    }
}
