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
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.HttpHeaders;
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
            Metadata metadata = TikaMetadataUtils.parseMetadata(content, contentType);
            // Duration
            builder.durationSeconds(TikaMetadataUtils.parseDuration(metadata));

            // Resolution
            builder.width(TikaMetadataUtils.parseInteger(metadata.get("tiff:ImageWidth")));
            builder.height(TikaMetadataUtils.parseInteger(metadata.get("tiff:ImageLength")));

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
            builder.frameRate(TikaMetadataUtils.parseDouble(frameRate));

            // Bitrate
            builder.bitrate(TikaMetadataUtils.parseInteger(metadata.get("bitrate")));

        } catch (Exception e) {
            log.warn("Failed to extract video metadata: {}", e.getMessage());
        }

        return builder.build();
    }
}
