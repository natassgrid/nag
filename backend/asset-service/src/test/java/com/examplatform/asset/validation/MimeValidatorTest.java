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

package com.examplatform.asset.validation;

import com.examplatform.asset.domain.enums.AssetType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MimeValidator}.
 */
@DisplayName("MimeValidator")
class MimeValidatorTest {

    private MimeValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MimeValidator();
    }

    @Nested
    @DisplayName("validate - allowed types")
    class AllowedTypes {

        @ParameterizedTest
        @ValueSource(strings = {"image/png", "image/jpeg", "image/webp", "image/svg+xml"})
        @DisplayName("accepts image MIME types and returns IMAGE")
        void acceptsImageTypes(String mimeType) {
            assertThat(validator.validate(mimeType)).isEqualTo(AssetType.IMAGE);
        }

        @ParameterizedTest
        @ValueSource(strings = {"audio/mpeg", "audio/mp3", "audio/aac", "audio/wav", "audio/x-wav"})
        @DisplayName("accepts audio MIME types and returns AUDIO")
        void acceptsAudioTypes(String mimeType) {
            assertThat(validator.validate(mimeType)).isEqualTo(AssetType.AUDIO);
        }

        @Test
        @DisplayName("accepts video/mp4 and returns VIDEO")
        void acceptsVideoMp4() {
            assertThat(validator.validate("video/mp4")).isEqualTo(AssetType.VIDEO);
        }

        @Test
        @DisplayName("handles content type with charset parameter")
        void handlesContentTypeWithCharset() {
            assertThat(validator.validate("image/png; charset=utf-8")).isEqualTo(AssetType.IMAGE);
        }

        @Test
        @DisplayName("is case insensitive")
        void isCaseInsensitive() {
            assertThat(validator.validate("Image/PNG")).isEqualTo(AssetType.IMAGE);
        }
    }

    @Nested
    @DisplayName("validate - blocked types")
    class BlockedTypes {

        @ParameterizedTest
        @ValueSource(strings = {
                "application/x-executable",
                "application/x-msdownload",
                "application/x-sh",
                "application/java-archive",
                "application/javascript"
        })
        @DisplayName("rejects blocked executable MIME types")
        void rejectsBlockedTypes(String mimeType) {
            assertThatThrownBy(() -> validator.validate(mimeType))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Blocked");
        }
    }

    @Nested
    @DisplayName("validate - unsupported types")
    class UnsupportedTypes {

        @ParameterizedTest
        @ValueSource(strings = {"application/pdf", "text/plain", "application/zip"})
        @DisplayName("rejects unsupported but non-blocked types")
        void rejectsUnsupportedTypes(String mimeType) {
            assertThatThrownBy(() -> validator.validate(mimeType))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Unsupported");
        }

        @Test
        @DisplayName("rejects null content type")
        void rejectsNull() {
            assertThatThrownBy(() -> validator.validate(null))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("required");
        }

        @Test
        @DisplayName("rejects blank content type")
        void rejectsBlank() {
            assertThatThrownBy(() -> validator.validate("  "))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("required");
        }
    }

    @Nested
    @DisplayName("isSupported")
    class IsSupported {

        @Test
        @DisplayName("returns true for supported type")
        void returnsTrueForSupported() {
            assertThat(validator.isSupported("image/png")).isTrue();
        }

        @Test
        @DisplayName("returns false for unsupported type")
        void returnsFalseForUnsupported() {
            assertThat(validator.isSupported("application/pdf")).isFalse();
        }

        @Test
        @DisplayName("returns false for null")
        void returnsFalseForNull() {
            assertThat(validator.isSupported(null)).isFalse();
        }
    }
}
