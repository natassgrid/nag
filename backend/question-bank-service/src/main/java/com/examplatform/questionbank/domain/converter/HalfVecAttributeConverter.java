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
 * JPA AttributeConverter that converts between float[] (Java) and PostgreSQL
 * pgvector halfvec type via PGhalfvec (a PGobject subclass).
 *
 * Uses PGhalfvec as the DB column type so the JDBC driver sends the correct
 * type OID, avoiding the "column is of type halfvec but expression is of type
 * character varying" error.
 */
@Converter
public class HalfVecAttributeConverter implements AttributeConverter<float[], PGhalfvec> {

    @Override
    public PGhalfvec convertToDatabaseColumn(float[] attribute) {
        if (attribute == null) {
            return null;
        }
        return new PGhalfvec(attribute);
    }

    @Override
    public float[] convertToEntityAttribute(PGhalfvec dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return dbData.toArray();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse halfvec value: " + dbData, e);
        }
    }
}