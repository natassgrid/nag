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

import com.examplatform.questionbank.translation.domain.TranslatedQuestionPayload;
import com.examplatform.questionbank.translation.domain.TranslatedQuestionPayload.TranslatedOption;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TranslationPayloadService} in plain-text mode (encryption disabled).
 * Encryption=true mode requires a live Vault — covered by integration tests.
 */
class TranslationPayloadServiceTest {

    private TranslationPayloadService payloadService;

    @BeforeEach
    void setUp() {
        payloadService = new TranslationPayloadService(new ObjectMapper());
        // Default: encryption disabled
        ReflectionTestUtils.setField(payloadService, "encryptionEnabled", false);
    }

    @Test
    @DisplayName("serialize + deserialize round-trip preserves all fields (plain mode)")
    void shouldRoundTripPlainPayload() {
        TranslatedQuestionPayload original = new TranslatedQuestionPayload(
                "प्रकाश संश्लेषण क्या है?",
                List.of(
                        new TranslatedOption("A", "पौधों द्वारा भोजन बनाने की प्रक्रिया"),
                        new TranslatedOption("B", "श्वसन की प्रक्रिया")
                ),
                "प्रकाश संश्लेषण एक जैविक प्रक्रिया है।"
        );

        String stored = payloadService.serialize(original);
        assertThat(stored).isNotBlank();
        // In plain mode the stored value is valid JSON
        assertThat(stored).contains("प्रकाश संश्लेषण");

        TranslatedQuestionPayload restored = payloadService.deserialize(stored, false);
        assertThat(restored).isNotNull();
        assertThat(restored.content()).isEqualTo(original.content());
        assertThat(restored.explanation()).isEqualTo(original.explanation());
        assertThat(restored.options()).hasSize(2);
        assertThat(restored.options().get(0).id()).isEqualTo("A");
        assertThat(restored.options().get(1).text()).isEqualTo("श्वसन की प्रक्रिया");
    }

    @Test
    @DisplayName("serialize + deserialize round-trip with null options and null explanation")
    void shouldHandleNullOptionalFields() {
        TranslatedQuestionPayload original = new TranslatedQuestionPayload(
                "What is osmosis?", null, null
        );

        String stored = payloadService.serialize(original);
        TranslatedQuestionPayload restored = payloadService.deserialize(stored, false);

        assertThat(restored.content()).isEqualTo("What is osmosis?");
        assertThat(restored.options()).isNull();
        assertThat(restored.explanation()).isNull();
    }

    @Test
    @DisplayName("deserialize returns null when stored value is null")
    void shouldReturnNullForNullInput() {
        assertThat(payloadService.deserialize(null, false)).isNull();
    }

    @Test
    @DisplayName("isEncryptionEnabled returns false when flag is off")
    void shouldReportEncryptionDisabledByDefault() {
        assertThat(payloadService.isEncryptionEnabled()).isFalse();
    }

    @Test
    @DisplayName("When payloadEncrypted=true but value has no vault prefix, reads as plain JSON (backward-compat)")
    void shouldReadAsPlainWhenNoVaultPrefix() {
        TranslatedQuestionPayload original = new TranslatedQuestionPayload(
                "Tamil content", List.of(new TranslatedOption("A", "விடை A")), null
        );
        String stored = payloadService.serialize(original);  // plain JSON, no vault prefix

        // Simulate a row written before encryption was turned on
        TranslatedQuestionPayload restored = payloadService.deserialize(stored, true);
        assertThat(restored.content()).isEqualTo("Tamil content");
    }
}
