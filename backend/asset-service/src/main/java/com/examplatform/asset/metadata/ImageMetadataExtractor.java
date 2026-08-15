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

import com.examplatform.asset.domain.enums.AssetType;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Extracts image metadata (width, height, DPI, orientation) using Apache Tika.
 */
@Slf4j
@Component
public class ImageMetadataExtractor implements MetadataExtractor {

    @Override
    public AssetType supportedType() {
        return AssetType.IMAGE;
    }

    @Override
    public MediaMetadata extract(InputStream content, String contentType) {
        MediaMetadata.MediaMetadataBuilder builder = MediaMetadata.builder();

        try {
            Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_TYPE, contentType);

            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(content, handler, metadata, new ParseContext());

            builder.width(parseInteger(metadata.get(TIFF.IMAGE_WIDTH)));
            builder.height(parseInteger(metadata.get(TIFF.IMAGE_LENGTH)));

            // DPI from X/Y resolution
            Integer xRes = parseInteger(metadata.get("X Resolution"));
            if (xRes == null) {
                xRes = parseInteger(metadata.get(TIFF.RESOLUTION_HORIZONTAL.getName()));
            }
            builder.dpi(xRes);

            // Orientation
            String orientationVal = metadata.get(TIFF.ORIENTATION.getName());
            builder.orientation(mapOrientation(orientationVal));

        } catch (Exception e) {
            log.warn("Failed to extract image metadata: {}", e.getMessage());
        }

        return builder.build();
    }

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            // Handle values like "72.0" or "72 dpi"
            String cleaned = value.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) return null;
            return (int) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String mapOrientation(String value) {
        if (value == null) return null;
        return switch (value) {
            case "1" -> "Normal";
            case "2" -> "Flipped Horizontal";
            case "3" -> "Rotated 180";
            case "4" -> "Flipped Vertical";
            case "5" -> "Transposed";
            case "6" -> "Rotated 90 CW";
            case "7" -> "Transverse";
            case "8" -> "Rotated 90 CCW";
            default -> value;
        };
    }
}
