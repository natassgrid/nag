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
            metadata.set(Metadata.CONTENT_TYPE, contentType);

            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1);
            parser.parse(content, handler, metadata, new ParseContext());

            // Duration (Tika provides in seconds or milliseconds depending on format)
            builder.durationSeconds(parseDuration(metadata));

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
            builder.sampleRate(parseInteger(sampleRate));

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
                builder.bitrate(parseInteger(metadata.get("bitrate")));
            }

        } catch (Exception e) {
            log.warn("Failed to extract audio metadata: {}", e.getMessage());
        }

        return builder.build();
    }

    private Double parseDuration(Metadata metadata) {
        // Try xmpDM:duration first
        String duration = metadata.get(XMPDM.DURATION);
        if (duration == null) {
            duration = metadata.get("xmpDM:duration");
        }
        if (duration != null) {
            try {
                double val = Double.parseDouble(duration.replaceAll("[^0-9.]", ""));
                // Tika sometimes reports duration in ms
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

    private Integer mapChannels(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase()) {
            case "mono" -> 1;
            case "stereo" -> 2;
            case "5.1" -> 6;
            case "7.1" -> 8;
            default -> parseInteger(value);
        };
    }
}
