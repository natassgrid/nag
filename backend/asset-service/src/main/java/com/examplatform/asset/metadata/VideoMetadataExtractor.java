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
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Extracts video metadata (duration, resolution, codec, frame rate) using Apache Tika.
 */
@Slf4j
@Component
public class VideoMetadataExtractor implements MetadataExtractor {

    @Override
    public AssetType supportedType() {
        return AssetType.VIDEO;
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

            // Duration
            builder.durationSeconds(parseDuration(metadata));

            // Resolution
            builder.width(parseInteger(metadata.get("tiff:ImageWidth")));
            builder.height(parseInteger(metadata.get("tiff:ImageLength")));

            // Codec
            String codec = metadata.get(XMPDM.VIDEO_COMPRESSOR);
            if (codec == null) {
                codec = metadata.get("xmpDM:videoCompressor");
            }
            builder.codec(codec);

            // Frame rate
            String frameRate = metadata.get(XMPDM.VIDEO_FRAME_RATE);
            if (frameRate == null) {
                frameRate = metadata.get("xmpDM:videoFrameRate");
            }
            builder.frameRate(parseDouble(frameRate));

            // Bitrate
            builder.bitrate(parseInteger(metadata.get("bitrate")));

        } catch (Exception e) {
            log.warn("Failed to extract video metadata: {}", e.getMessage());
        }

        return builder.build();
    }

    private Double parseDuration(Metadata metadata) {
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

    private Integer parseInteger(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String cleaned = value.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) return null;
            return (int) Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            String cleaned = value.replaceAll("[^0-9.]", "");
            if (cleaned.isEmpty()) return null;
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
