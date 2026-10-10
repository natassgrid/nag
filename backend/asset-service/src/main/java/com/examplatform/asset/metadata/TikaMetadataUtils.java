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

package com.examplatform.asset.metadata;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;

/**
 * Utility methods for parsing audio/video metadata extracted by Apache Tika.
 */
public final class TikaMetadataUtils {

    private TikaMetadataUtils() {
    }

    public static Double parseDuration(Metadata metadata) {
        String duration = metadata.get(XMPDM.DURATION);
        if (duration == null) {
            duration = metadata.get("xmpDM:duration");
        }
        if (duration != null) {
            try {
                double val = Double.parseDouble(duration.replaceAll("[^0-9.]", ""));
                return val > 100000 ? val / 1000.0 : val;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return null;
    }

    public static Integer parseInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String cleaned = value.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            return (int) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            String cleaned = value.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
