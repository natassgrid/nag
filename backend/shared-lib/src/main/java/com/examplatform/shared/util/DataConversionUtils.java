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

package com.examplatform.shared.util;

import java.time.Instant;
import java.util.UUID;

/**
 * Null-safe data conversion utilities for common types across consumers and services.
 */
public final class DataConversionUtils {

    private DataConversionUtils() {
    }

    /**
     * Safely parses an object into a {@link UUID}.
     * Returns {@code null} if the input is null, blank, or invalid.
     *
     * @param obj the input object
     * @return parsed UUID, or {@code null}
     */
    public static UUID parseUUID(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof UUID uuid) {
            return uuid;
        }
        String str = obj.toString().trim();
        if (str.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Safely converts an object into a double.
     * Returns {@code 0.0} if the input is null or unparseable.
     *
     * @param value the input value
     * @return parsed double, or 0.0
     */
    public static double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Safely converts an object into a long.
     * Returns {@code 0L} if the input is null or unparseable.
     *
     * @param value the input value
     * @return parsed long, or 0L
     */
    public static long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Safely parses an object into an {@link Instant}.
     * Returns {@link Instant#now()} if the input is null or unparseable.
     *
     * @param obj the input object
     * @return parsed Instant, or Instant.now()
     */
    public static Instant parseInstant(Object obj) {
        if (obj == null) {
            return Instant.now();
        }
        if (obj instanceof Instant inst) {
            return inst;
        }
        try {
            return Instant.parse(obj.toString().trim());
        } catch (Exception e) {
            return Instant.now();
        }
    }
}
