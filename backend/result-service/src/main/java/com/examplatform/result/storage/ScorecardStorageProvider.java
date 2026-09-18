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

package com.examplatform.result.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.Optional;

/**
 * Service Provider Interface for pluggable scorecard PDF storage backends.
 *
 * <p>Implementations must be thread-safe. Supports both cloud object storage (S3/MinIO)
 * and local filesystem storage for development/test environments.
 */
public interface ScorecardStorageProvider {

    /**
     * Unique identifier for this storage provider (e.g. "s3", "local", "filesystem").
     */
    String name();

    /**
     * Upload scorecard PDF content to the storage backend.
     *
     * @param path        the logical path/key where the scorecard should be stored
     * @param content     the binary content stream
     * @param contentType the MIME type of the content (e.g. "application/pdf")
     * @param size        the size in bytes of the content
     * @return the resolved storage key or location
     */
    String upload(String path, InputStream content, String contentType, long size);

    /**
     * Download binary content from the storage backend.
     *
     * @param storageLocation the storage key/location
     * @return an InputStream of the binary content, or empty if not found
     */
    Optional<InputStream> download(String storageLocation);

    /**
     * Delete scorecard from the storage backend.
     *
     * @param storageLocation the storage location
     * @return true if deleted, false if not found
     */
    boolean delete(String storageLocation);

    /**
     * Check if a scorecard exists at the given storage location.
     *
     * @param storageLocation the storage location to check
     * @return true if the file exists
     */
    boolean exists(String storageLocation);

    /**
     * Generate a time-limited presigned URL for downloading the scorecard.
     *
     * @param storageLocation the storage key/location
     * @param duration        duration for which the presigned URL remains valid
     * @return presigned download URL
     */
    String generatePresignedUrl(String storageLocation, Duration duration);

    /**
     * Check the health of this storage provider.
     *
     * @return true if operational
     */
    boolean health();
}
