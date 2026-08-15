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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MagicNumberValidator}.
 */
@DisplayName("MagicNumberValidator")
class MagicNumberValidatorTest {

    private MagicNumberValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MagicNumberValidator();
    }

    @Nested
    @DisplayName("validateAndDetect")
    class ValidateAndDetect {

        @Test
        @DisplayName("accepts PNG file with correct magic bytes")
        void acceptsPngWithCorrectMagic() {
            // PNG magic bytes: 89 50 4E 47 0D 0A 1A 0A
            byte[] pngHeader = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
            };
            InputStream content = new ByteArrayInputStream(pngHeader);

            String detected = validator.validateAndDetect(content, "image/png", "test.png");
            assertThat(detected).contains("image/png");
        }

        @Test
        @DisplayName("accepts JPEG file with correct magic bytes")
        void acceptsJpegWithCorrectMagic() {
            // JPEG magic bytes: FF D8 FF
            byte[] jpegHeader = new byte[]{
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                    0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
            };
            InputStream content = new ByteArrayInputStream(jpegHeader);

            String detected = validator.validateAndDetect(content, "image/jpeg", "photo.jpg");
            assertThat(detected).contains("image/jpeg");
        }

        @Test
        @DisplayName("rejects file when declared type mismatches detected type")
        void rejectsOnMismatch() {
            // PNG magic bytes but declared as audio
            byte[] pngHeader = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                    0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
            };
            InputStream content = new ByteArrayInputStream(pngHeader);

            assertThatThrownBy(() -> validator.validateAndDetect(content, "audio/mpeg", "fake.mp3"))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("mismatch");
        }

        @Test
        @DisplayName("accepts SVG declared as image/svg+xml even if detected as XML")
        void acceptsSvgAsXml() {
            // SVG is XML
            String svgContent = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>";
            InputStream content = new ByteArrayInputStream(svgContent.getBytes());

            String detected = validator.validateAndDetect(content, "image/svg+xml", "image.svg");
            // Should not throw - SVG detected as application/xml is allowed
            assertThat(detected).isNotNull();
        }

        @Test
        @DisplayName("accepts same-primary-type content")
        void acceptsSamePrimaryType() {
            // WAV magic bytes: RIFF....WAVE
            byte[] wavHeader = new byte[]{
                    0x52, 0x49, 0x46, 0x46, // RIFF
                    0x24, 0x00, 0x00, 0x00, // size
                    0x57, 0x41, 0x56, 0x45, // WAVE
                    0x66, 0x6D, 0x74, 0x20  // fmt
            };
            InputStream content = new ByteArrayInputStream(wavHeader);

            // audio/wav vs audio/x-wav should be compatible
            String detected = validator.validateAndDetect(content, "audio/wav", "sound.wav");
            assertThat(detected).startsWith("audio/");
        }
    }
}
