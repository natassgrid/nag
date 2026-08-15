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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SecurityValidationPipeline}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityValidationPipeline")
class SecurityValidationPipelineTest {

    @Mock
    private FilenameSanitizer filenameSanitizer;

    @Mock
    private FileSizeValidator fileSizeValidator;

    @Mock
    private MimeValidator mimeValidator;

    @Mock
    private MagicNumberValidator magicNumberValidator;

    @InjectMocks
    private SecurityValidationPipeline pipeline;

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("runs all validators in order and returns result")
        void runsAllValidatorsAndReturnsResult() {
            // Given
            String filename = "test photo.png";
            String contentType = "image/png";
            long size = 1024L;
            InputStream content = new ByteArrayInputStream(new byte[10]);

            when(filenameSanitizer.sanitize(filename)).thenReturn("test_photo.png");
            when(filenameSanitizer.extractExtension("test_photo.png")).thenReturn("png");
            doNothing().when(fileSizeValidator).validate(size);
            when(mimeValidator.validate(contentType)).thenReturn(AssetType.IMAGE);
            when(magicNumberValidator.validateAndDetect(any(InputStream.class), eq(contentType), eq("test_photo.png")))
                    .thenReturn("image/png");

            // When
            SecurityValidationPipeline.ValidationResult result =
                    pipeline.validate(filename, contentType, size, content);

            // Then
            assertThat(result.getSanitizedFilename()).isEqualTo("test_photo.png");
            assertThat(result.getExtension()).isEqualTo("png");
            assertThat(result.getAssetType()).isEqualTo(AssetType.IMAGE);
            assertThat(result.getDetectedContentType()).isEqualTo("image/png");

            verify(filenameSanitizer).sanitize(filename);
            verify(fileSizeValidator).validate(size);
            verify(mimeValidator).validate(contentType);
            verify(magicNumberValidator).validateAndDetect(any(), eq(contentType), eq("test_photo.png"));
        }

        @Test
        @DisplayName("propagates exception from filename sanitizer")
        void propagatesFilenameSanitizerException() {
            when(filenameSanitizer.sanitize(any()))
                    .thenThrow(new AssetValidationException("Filename is required"));

            InputStream content = new ByteArrayInputStream(new byte[10]);

            assertThatThrownBy(() -> pipeline.validate(null, "image/png", 1024, content))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Filename is required");
        }

        @Test
        @DisplayName("propagates exception from file size validator")
        void propagatesFileSizeException() {
            when(filenameSanitizer.sanitize("big.png")).thenReturn("big.png");
            when(filenameSanitizer.extractExtension("big.png")).thenReturn("png");
            doThrow(new AssetValidationException("File size exceeds maximum"))
                    .when(fileSizeValidator).validate(999999999L);

            InputStream content = new ByteArrayInputStream(new byte[10]);

            assertThatThrownBy(() -> pipeline.validate("big.png", "image/png", 999999999L, content))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("exceeds maximum");
        }

        @Test
        @DisplayName("propagates exception from MIME validator")
        void propagatesMimeValidatorException() {
            when(filenameSanitizer.sanitize("malware.exe")).thenReturn("malware.exe");
            when(filenameSanitizer.extractExtension("malware.exe")).thenReturn("exe");
            doNothing().when(fileSizeValidator).validate(1024L);
            when(mimeValidator.validate("application/x-executable"))
                    .thenThrow(new AssetValidationException("Blocked MIME type"));

            InputStream content = new ByteArrayInputStream(new byte[10]);

            assertThatThrownBy(() -> pipeline.validate("malware.exe", "application/x-executable", 1024L, content))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("Blocked");
        }
    }

    @Nested
    @DisplayName("computeSha256")
    class ComputeSha256 {

        @Test
        @DisplayName("produces 64-character hex string")
        void produces64CharHex() {
            byte[] data = "hello world".getBytes();
            String hash = SecurityValidationPipeline.computeSha256(data);
            assertThat(hash).hasSize(64);
            assertThat(hash).matches("[0-9a-f]{64}");
        }

        @Test
        @DisplayName("is deterministic for same input")
        void isDeterministic() {
            byte[] data = "test data".getBytes();
            String hash1 = SecurityValidationPipeline.computeSha256(data);
            String hash2 = SecurityValidationPipeline.computeSha256(data);
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("produces different hashes for different inputs")
        void differentInputsDifferentHashes() {
            String hash1 = SecurityValidationPipeline.computeSha256("input1".getBytes());
            String hash2 = SecurityValidationPipeline.computeSha256("input2".getBytes());
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("matches known SHA-256 value")
        void matchesKnownValue() {
            // SHA-256 of empty string
            String hash = SecurityValidationPipeline.computeSha256(new byte[0]);
            assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        }
    }
}
