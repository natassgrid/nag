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

package com.examplatform.asset.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for asset storage.
 *
 * <pre>
 * asset:
 *   storage:
 *     provider: filesystem
 *     filesystem:
 *       base-path: ./asset-storage
 *     max-file-size: 104857600  # 100 MB
 * </pre>
 */
@Data
@Component
@ConfigurationProperties(prefix = "asset.storage")
public class StorageProperties {

    /** The active storage provider name (e.g. "filesystem", "s3"). */
    private String provider = "filesystem";

    /** Maximum allowed file size in bytes (default 100 MB). */
    private long maxFileSize = 104_857_600L;

    /** Filesystem-specific configuration. */
    private FilesystemProperties filesystem = new FilesystemProperties();

    @Data
    public static class FilesystemProperties {
        /** Base directory for storing files. */
        private String basePath = "./asset-storage";
    }
}
