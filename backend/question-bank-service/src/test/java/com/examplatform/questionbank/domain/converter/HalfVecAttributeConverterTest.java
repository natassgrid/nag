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

package com.examplatform.questionbank.domain.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link HalfVecAttributeConverter}.
 */
class HalfVecAttributeConverterTest {

    private HalfVecAttributeConverter converter;

    @BeforeEach
    void setUp() {
        converter = new HalfVecAttributeConverter();
    }

    @Test
    void convertToDatabaseColumn_withNullArray_returnsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
    }

    @Test
    void convertToEntityAttribute_withNullString_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToDatabaseColumn_producesHalfvecStringFormat() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        String result = converter.convertToDatabaseColumn(embedding);

        // pgvector halfvec format: [val1,val2,val3]
        assertThat(result).startsWith("[");
        assertThat(result).endsWith("]");
        assertThat(result).contains(",");
    }

    @Test
    void roundTrip_preservesValues() {
        float[] original = {0.5f, -0.25f, 1.0f, 0.0f};
        String dbValue = converter.convertToDatabaseColumn(original);
        float[] restored = converter.convertToEntityAttribute(dbValue);

        // halfvec uses FP16, so precision is reduced — use tolerance
        assertThat(restored).hasSize(original.length);
        for (int i = 0; i < original.length; i++) {
            assertThat(restored[i]).isCloseTo(original[i], org.assertj.core.data.Offset.offset(0.01f));
        }
    }

    @Test
    void roundTrip_384Dimensions() {
        float[] embedding = new float[384];
        for (int i = 0; i < 384; i++) {
            embedding[i] = (float) (Math.random() * 2.0 - 1.0);
        }

        String dbValue = converter.convertToDatabaseColumn(embedding);
        float[] restored = converter.convertToEntityAttribute(dbValue);

        assertThat(restored).hasSize(384);
        for (int i = 0; i < 384; i++) {
            // halfvec FP16 has limited precision
            assertThat(restored[i]).isCloseTo(embedding[i], org.assertj.core.data.Offset.offset(0.01f));
        }
    }

    @Test
    void convertToEntityAttribute_withInvalidString_throwsIllegalArgument() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not-a-vector"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void convertToDatabaseColumn_withEmptyArray_producesEmptyVector() {
        float[] empty = {};
        String result = converter.convertToDatabaseColumn(empty);
        assertThat(result).isEqualTo("[]");
    }
}
