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
