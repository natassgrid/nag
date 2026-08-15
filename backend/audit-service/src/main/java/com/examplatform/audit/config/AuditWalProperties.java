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

package com.examplatform.audit.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Audit WAL (Write-Ahead Log) buffer.
 * Controls the local file-based buffer used when Kafka is unavailable.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "audit.wal")
public class AuditWalProperties {

    /**
     * Path where WAL files are stored.
     */
    private String walPath = "./audit-wal/";

    /**
     * Whether the WAL buffer is enabled.
     */
    private boolean enabled = true;

    /**
     * Maximum size of a single WAL file in bytes (default: 10MB).
     */
    private long maxFileSize = 10 * 1024 * 1024L;
}
