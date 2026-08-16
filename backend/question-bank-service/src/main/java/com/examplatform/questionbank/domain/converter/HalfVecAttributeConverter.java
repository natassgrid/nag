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

import com.pgvector.PGhalfvec;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.SQLException;

/**
 * JPA {@link AttributeConverter} that converts between {@code float[]} (Java)
 * and the PostgreSQL pgvector {@code halfvec} string representation.
 *
 * <p>pgvector's halfvec type stores half-precision (FP16) floating point vectors.
 * The wire format is a bracket-enclosed, comma-separated list: {@code [0.1,0.2,0.3]}.
 * This converter uses pgvector-java's {@link PGhalfvec} class for correct
 * serialization and deserialization.</p>
 *
 * <p>Applied explicitly on entity fields via {@code @Convert(converter = HalfVecAttributeConverter.class)}.
 *
 * Validates: Requirements FR-1 (embedding storage as halfvec(384))
 */
@Converter
public class HalfVecAttributeConverter implements AttributeConverter<float[], String> {

    @Override
    public String convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        return new PGhalfvec(attribute).toString();
    }

    @Override
    public float[] convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return new PGhalfvec(dbData).toArray();
        } catch (SQLException | NumberFormatException e) {
            throw new IllegalArgumentException("Failed to parse halfvec value: " + dbData, e);
        }
    }
}
