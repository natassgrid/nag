/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.questionbank.domain.converter;

import com.pgvector.PGhalfvec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
    void convertToEntityAttribute_withNull_returnsNull() {
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    void convertToDatabaseColumn_producesValidPGhalfvec() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        PGhalfvec result = converter.convertToDatabaseColumn(embedding);
        assertThat(result).isNotNull();
        assertThat(result.toString()).startsWith("[").endsWith("]");
    }

    @Test
    void roundTrip_preservesValues() {
        float[] original = {0.5f, -0.25f, 1.0f, 0.0f};
        PGhalfvec dbValue = converter.convertToDatabaseColumn(original);
        float[] restored = converter.convertToEntityAttribute(dbValue);
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
        PGhalfvec dbValue = converter.convertToDatabaseColumn(embedding);
        float[] restored = converter.convertToEntityAttribute(dbValue);
        assertThat(restored).hasSize(384);
        for (int i = 0; i < 384; i++) {
            assertThat(restored[i]).isCloseTo(embedding[i], org.assertj.core.data.Offset.offset(0.01f));
        }
    }

    @Test
    void convertToDatabaseColumn_withEmptyArray_producesEmptyVector() {
        float[] empty = {};
        PGhalfvec result = converter.convertToDatabaseColumn(empty);
        assertThat(result).isNotNull();
        assertThat(result.toString()).isEqualTo("[]");
    }
}