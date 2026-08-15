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

import com.examplatform.asset.storage.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileSizeValidator}.
 */
@DisplayName("FileSizeValidator")
class FileSizeValidatorTest {

    private FileSizeValidator validator;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        properties = new StorageProperties();
        properties.setMaxFileSize(100 * 1024 * 1024L); // 100 MB
        validator = new FileSizeValidator(properties);
    }

    @Nested
    @DisplayName("validate")
    class Validate {

        @Test
        @DisplayName("accepts file within size limit")
        void acceptsWithinLimit() {
            assertThatCode(() -> validator.validate(1024 * 1024L)) // 1 MB
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts file at exact size limit")
        void acceptsAtExactLimit() {
            assertThatCode(() -> validator.validate(100 * 1024 * 1024L)) // exactly 100 MB
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("rejects file exceeding size limit")
        void rejectsExceedingLimit() {
            assertThatThrownBy(() -> validator.validate(100 * 1024 * 1024L + 1))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("exceeds maximum");
        }

        @Test
        @DisplayName("rejects empty file (size = 0)")
        void rejectsEmptyFile() {
            assertThatThrownBy(() -> validator.validate(0))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("rejects negative file size")
        void rejectsNegativeSize() {
            assertThatThrownBy(() -> validator.validate(-1))
                    .isInstanceOf(AssetValidationException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("accepts minimum valid file (1 byte)")
        void acceptsMinimumValid() {
            assertThatCode(() -> validator.validate(1))
                    .doesNotThrowAnyException();
        }
    }
}
