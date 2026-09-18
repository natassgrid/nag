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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for scorecard PDF storage.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
@ConfigurationProperties(prefix = "scorecard.storage")
public class ScorecardStorageProperties {

    /** The active storage mode: "s3" or "local" (default: "s3"). */
    @Builder.Default
    private String mode = "s3";

    /** Top-level bucket name shortcut for S3. */
    private String bucket;

    /** Top-level path shortcut for local filesystem. */
    private String path;

    /** Local filesystem configuration. */
    @Builder.Default
    private LocalProperties local = new LocalProperties();

    /** S3 / MinIO configuration. */
    @Builder.Default
    private S3Properties s3 = new S3Properties();

    public String getEffectiveLocalPath() {
        if (path != null && !path.isBlank()) {
            return path;
        }
        return local != null && local.getPath() != null && !local.getPath().isBlank() ? local.getPath() : "./scorecards";
    }

    public String getEffectiveS3Bucket() {
        if (bucket != null && !bucket.isBlank()) {
            return bucket;
        }
        return s3 != null && s3.getBucket() != null && !s3.getBucket().isBlank() ? s3.getBucket() : "exam-platform-scorecards";
    }

    public boolean isLocalMode() {
        return "local".equalsIgnoreCase(mode) || "filesystem".equalsIgnoreCase(mode);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LocalProperties {
        /** Base directory for storing scorecards locally. */
        @Builder.Default
        private String path = "./scorecards";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class S3Properties {
        /** S3 bucket name. */
        @Builder.Default
        private String bucket = "exam-platform-scorecards";
        /** AWS region (default: ap-south-1). */
        @Builder.Default
        private String region = "ap-south-1";
        /** Optional custom endpoint URI (e.g. for MinIO or LocalStack). */
        private String endpoint;
        /** Optional access key for authentication. */
        private String accessKey;
        /** Optional secret key for authentication. */
        private String secretKey;
        /** Whether to use path-style access (recommended for MinIO/LocalStack). */
        @Builder.Default
        private boolean pathStyleAccess = true;
        /** Optional public base URL for resolving direct download links. */
        private String publicBaseUrl;
        /** Duration in minutes for which presigned download URLs are valid (default: 15). */
        @Builder.Default
        private int presignedUrlDurationMinutes = 15;
    }
}
