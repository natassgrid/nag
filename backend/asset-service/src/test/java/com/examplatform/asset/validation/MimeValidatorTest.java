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
        @ValueSource(strings = {"image/png", "image/jpeg", "image/webp"})
        @DisplayName("accepts image MIME types and returns IMAGE")
        void acceptsImageTypes(String mimeType) {
            assertThat(validator.validate(mimeType)).isEqualTo(AssetType.IMAGE);
        }

        @Test
        @DisplayName("accepts SVG MIME type and returns SVG")
        void acceptsSvgType() {
            assertThat(validator.validate("image/svg+xml")).isEqualTo(AssetType.SVG);
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
        @DisplayName("ignores MIME parameters like charset")
        void ignoresMimeParameters() {
            assertThat(validator.validate("image/png; charset=utf-8")).isEqualTo(AssetType.IMAGE);
            assertThat(validator.validate("image/svg+xml; charset=utf-8")).isEqualTo(AssetType.SVG);
        }

        @Test
        @DisplayName("is case-insensitive")
        void caseInsensitive() {
            assertThat(validator.validate("IMAGE/PNG")).isEqualTo(AssetType.IMAGE);
            assertThat(validator.validate("Image/Svg+Xml")).isEqualTo(AssetType.SVG);
        }
    }

    @Nested
    @DisplayName("validate - blocked types")
    class BlockedTypes {

        @ParameterizedTest
        @ValueSource(strings = {
                "application/x-executable",
                "application/x-msdos-program",
                "application/x-msdownload",
                "application/x-sh",
                "application/x-shellscript",
                "application/java-archive",
                "application/x-java-class",
                "application/javascript"
        })
        @DisplayName("rejects dangerous/executable MIME types")
        void rejectsBlockedTypes(String mimeType) {
            assertThatThrownBy(() -> validator.validate(mimeType))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Blocked MIME type");
        }
    }

    @Nested
    @DisplayName("validate - unsupported types")
    class UnsupportedTypes {

        @ParameterizedTest
        @ValueSource(strings = {
                "text/html",
                "application/pdf",
                "application/zip",
                "image/gif",
                "image/bmp",
                "video/quicktime",
                "application/octet-stream"
        })
        @DisplayName("rejects types not in allowed list")
        void rejectsUnsupportedTypes(String mimeType) {
            assertThatThrownBy(() -> validator.validate(mimeType))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Unsupported MIME type");
        }
    }

    @Nested
    @DisplayName("validate - null or blank")
    class NullOrBlank {

        @Test
        @DisplayName("rejects null content type")
        void rejectsNull() {
            assertThatThrownBy(() -> validator.validate(null))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Content type is required");
        }

        @Test
        @DisplayName("rejects blank content type")
        void rejectsBlank() {
            assertThatThrownBy(() -> validator.validate("   "))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Content type is required");
        }
    }

    @Nested
    @DisplayName("isSupported")
    class IsSupported {

        @Test
        @DisplayName("returns true for allowed types")
        void returnsTrueForAllowed() {
            assertThat(validator.isSupported("image/png")).isTrue();
            assertThat(validator.isSupported("image/svg+xml")).isTrue();
            assertThat(validator.isSupported("video/mp4")).isTrue();
        }

        @Test
        @DisplayName("returns false for unsupported types")
        void returnsFalseForUnsupported() {
            assertThat(validator.isSupported("text/html")).isFalse();
            assertThat(validator.isSupported(null)).isFalse();
        }
    }
}
