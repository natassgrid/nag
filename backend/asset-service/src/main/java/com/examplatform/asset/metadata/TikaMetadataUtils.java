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

    public static Metadata parseMetadata(java.io.InputStream content, String contentType) throws Exception {
        Metadata metadata = new Metadata();
        if (contentType != null) {
            metadata.set(org.springframework.http.HttpHeaders.CONTENT_TYPE, contentType);
        }
        org.apache.tika.parser.AutoDetectParser parser = new org.apache.tika.parser.AutoDetectParser();
        org.apache.tika.sax.BodyContentHandler handler = new org.apache.tika.sax.BodyContentHandler(-1);
        try (org.apache.tika.io.TikaInputStream tis = org.apache.tika.io.TikaInputStream.get(content)) {
            parser.parse(tis, handler, metadata, new org.apache.tika.parser.ParseContext());
        }
        return metadata;
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
