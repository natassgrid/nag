/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.shared.storage.s3;

/**
 * Standard contract for AWS S3 and S3-compatible connection configuration parameters.
 *
 * @author Natassia Grid Development Team
 * @since 1.0.0
 */
public interface S3Config {

    /**
     * AWS region (e.g. "ap-south-1").
     */
    String getRegion();

    /**
     * Optional custom endpoint URI (e.g. for MinIO or LocalStack).
     */
    String getEndpoint();

    /**
     * Optional access key for authentication.
     */
    String getAccessKey();

    /**
     * Optional secret key for authentication.
     */
    String getSecretKey();

    /**
     * Whether to use path-style access (recommended for MinIO/LocalStack).
     */
    boolean isPathStyleAccess();
}
