package com.examplatform.asset.metadata;

import com.examplatform.asset.domain.enums.AssetType;

import java.io.InputStream;

/**
 * Strategy interface for extracting media-specific metadata from an uploaded asset.
 *
 * <p>Implementations handle specific asset types (image, audio, video).
 * The {@link MetadataExtractionService} selects the appropriate extractor
 * based on the detected {@link AssetType}.
 */
public interface MetadataExtractor {

    /**
     * @return the asset type this extractor handles
     */
    AssetType supportedType();

    /**
     * Extract metadata from the binary content.
     *
     * @param content     the asset binary stream (positioned at start)
     * @param contentType the detected MIME type
     * @return extracted metadata (fields may be null if extraction fails gracefully)
     */
    MediaMetadata extract(InputStream content, String contentType);
}
