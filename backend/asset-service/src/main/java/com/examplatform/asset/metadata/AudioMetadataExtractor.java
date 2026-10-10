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
 * GNU标志 Affero General Public License for more details.
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
 * Extracts audio metadata (duration, codec, sample rate, channels, bitrate) using Apache Tika.
 */
@Slf4j
@Component
public class AudioMetadataExtractor implements MetadataExtractor {

    @Override
    public AssetType supportedType() {
        return AssetType.AUDIO;
    }

    @Override
    public MediaMetadata extract(InputStream content, String contentType) {
        MediaMetadata.MediaMetadataBuilder builder = MediaMetadata.builder();

        try {
            Metadata metadata = new Metadata();
            metadata.set(HttpHeaders.CONTENT_TYPE, contentType);

            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            try (TikaInputStream tis = TikaInputStream.get(content)) {
                parser.parse(tis, handler, metadata, new ParseContext());
            }

            // Duration (Tika provides in seconds or milliseconds depending on format)
            builder.durationSeconds(TikaMetadataUtils.parseDuration(metadata));

            // Codec
            String audioCompressor = metadata.get(XMPDM.AUDIO_COMPRESSOR);
            if (audioCompressor == null) {
                audioCompressor = metadata.get("xmpDM:audioCompressor");
            }
            builder.codec(audioCompressor);

            // Sample Rate
            String sampleRate = metadata.get(XMPDM.AUDIO_SAMPLE_RATE);
            if (sampleRate == null) {
                sampleRate = metadata.get("xmpDM:audioSampleRate");
            }
            builder.sampleRate(TikaMetadataUtils.parseInteger(sampleRate));

            // Channels
            String channels = metadata.get(XMPDM.AUDIO_CHANNEL_TYPE);
            if (channels == null) {
                channels = metadata.get("channels");
            }
            builder.channels(mapChannels(channels));

            // Bitrate
            String bitrateStr = metadata.get("xmpDM:audioSampleRate");
            // Try alternate metadata keys for bitrate
            if (metadata.get("bitrate") != null) {
                builder.bitrate(TikaMetadataUtils.parseInteger(metadata.get("bitrate")));
            }

        } catch (Exception e) {
            log.warn("Failed to extract audio metadata: {}", e.getMessage());
        }

        return builder.build();
    }

    private Integer mapChannels(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase()) {
            case "mono" -> 1;
            case "stereo" -> 2;
            case "5.1" -> 6;
            case "7.1" -> 8;
            default -> TikaMetadataUtils.parseInteger(value);
        };
    }
}
