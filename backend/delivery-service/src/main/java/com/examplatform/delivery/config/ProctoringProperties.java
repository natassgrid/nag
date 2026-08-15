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

package com.examplatform.delivery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the proctoring subsystem.
 *
 * Validates: Requirements 11.1, 11.2
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.proctoring")
public class ProctoringProperties {

    /**
     * Minimum interval between webcam captures in seconds.
     * Must be at least 30 seconds to limit bandwidth and storage.
     */
    private int captureIntervalSeconds = 30;

    /**
     * Number of days to retain proctoring snapshots before deletion.
     */
    private int retentionDays = 90;

    /**
     * Maximum number of full-screen exits before the session is flagged.
     */
    private int maxFullScreenExits = 3;
}
