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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.shared.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DataConversionUtils Tests")
class DataConversionUtilsTest {

    @Test
    @DisplayName("parseUUID converts valid UUID string and instances")
    void parseUUID_valid() {
        UUID uuid = UUID.randomUUID();
        assertThat(DataConversionUtils.parseUUID(uuid)).isEqualTo(uuid);
        assertThat(DataConversionUtils.parseUUID(uuid.toString())).isEqualTo(uuid);
    }

    @Test
    @DisplayName("parseUUID returns null for null, empty or invalid strings")
    void parseUUID_invalid() {
        assertThat(DataConversionUtils.parseUUID(null)).isNull();
        assertThat(DataConversionUtils.parseUUID("")).isNull();
        assertThat(DataConversionUtils.parseUUID("   ")).isNull();
        assertThat(DataConversionUtils.parseUUID("invalid-uuid")).isNull();
    }

    @Test
    @DisplayName("toDouble converts numbers and numeric strings")
    void toDouble_valid() {
        assertThat(DataConversionUtils.toDouble(42)).isEqualTo(42.0);
        assertThat(DataConversionUtils.toDouble(12.34)).isEqualTo(12.34);
        assertThat(DataConversionUtils.toDouble("56.78")).isEqualTo(56.78);
    }

    @Test
    @DisplayName("toDouble returns 0.0 for null or non-numeric strings")
    void toDouble_invalid() {
        assertThat(DataConversionUtils.toDouble(null)).isEqualTo(0.0);
        assertThat(DataConversionUtils.toDouble("abc")).isEqualTo(0.0);
        assertThat(DataConversionUtils.toDouble("")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("parseInstant converts Instant objects and ISO strings")
    void parseInstant_valid() {
        Instant now = Instant.parse("2026-10-10T07:00:00Z");
        assertThat(DataConversionUtils.parseInstant(now)).isEqualTo(now);
        assertThat(DataConversionUtils.parseInstant("2026-10-10T07:00:00Z")).isEqualTo(now);
    }

    @Test
    @DisplayName("parseInstant returns fallback Instant for null or invalid inputs")
    void parseInstant_invalid() {
        assertThat(DataConversionUtils.parseInstant(null)).isNotNull();
        assertThat(DataConversionUtils.parseInstant("not-an-instant")).isNotNull();
    }
}
