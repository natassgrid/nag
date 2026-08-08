package com.examplatform.asset.metadata;

import com.examplatform.asset.domain.enums.AssetType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates metadata extraction by delegating to the appropriate
 * {@link MetadataExtractor} based on asset type.
 */
@Slf4j
@Service
public class MetadataExtractionService {

    private final Map<AssetType, MetadataExtractor> extractors;

    public MetadataExtractionService(List<MetadataExtractor> extractorList) {
        this.extractors = extractorList.stream()
                .collect(Collectors.toMap(MetadataExtractor::supportedType, Function.identity()));
        log.info("Metadata extractors registered for types: {}", extractors.keySet());
    }

    /**
     * Extract metadata from the given asset content.
     *
     * @param assetType   the classified asset type
     * @param content     the binary content stream
     * @param contentType the MIME type
     * @return extracted metadata, or empty metadata if no extractor is available
     */
    public MediaMetadata extract(AssetType assetType, InputStream content, String contentType) {
        return Optional.ofNullable(extractors.get(assetType))
                .map(extractor -> {
                    log.debug("Extracting metadata for type={} contentType={}", assetType, contentType);
                    return extractor.extract(content, contentType);
                })
                .orElseGet(() -> {
                    log.debug("No metadata extractor for type={}, returning empty metadata", assetType);
                    return MediaMetadata.builder().build();
                });
    }
}
