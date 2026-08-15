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
