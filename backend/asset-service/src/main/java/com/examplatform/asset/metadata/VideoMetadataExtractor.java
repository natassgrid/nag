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
