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

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring configuration for selecting the active {@link ScorecardStorageProvider}.
 */
@Slf4j
@Configuration
public class ScorecardStorageConfig {

    @Bean
    @Primary
    public ScorecardStorageProvider scorecardStorageProvider(
            ScorecardStorageProperties properties,
            S3ScorecardStorageProvider s3Provider,
            LocalFileScorecardStorageProvider localProvider) {

        if (properties.isLocalMode()) {
            log.info("Configured active ScorecardStorageProvider: LOCAL (Filesystem path: {})",
                    properties.getEffectiveLocalPath());
            return localProvider;
        }

        log.info("Configured active ScorecardStorageProvider: S3/MinIO (Bucket: {}, Endpoint: {})",
                properties.getEffectiveS3Bucket(), properties.getS3().getEndpoint());
        return s3Provider;
    }
}
