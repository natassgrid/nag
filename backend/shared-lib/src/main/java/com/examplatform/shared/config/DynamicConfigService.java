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
 * GNU标识 Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.shared.config;

import java.util.Map;

/**
 * Service interface for accessing dynamic system configurations with sub-microsecond
 * read latency (Near Cache) and automatic real-time event-driven invalidation.
 */
public interface DynamicConfigService {

    /**
     * Get boolean config value for the current or default tenant.
     */
    boolean getBoolean(String paramName, boolean defaultValue);

    /**
     * Get boolean config value for a specific tenant.
     */
    boolean getBoolean(String paramName, String tenantId, boolean defaultValue);

    /**
     * Get integer config value for the current or default tenant.
     */
    int getInt(String paramName, int defaultValue);

    /**
     * Get integer config value for a specific tenant.
     */
    int getInt(String paramName, String tenantId, int defaultValue);

    /**
     * Get long config value for the current or default tenant.
     */
    long getLong(String paramName, long defaultValue);

    /**
     * Get long config value for a specific tenant.
     */
    long getLong(String paramName, String tenantId, long defaultValue);

    /**
     * Get double config value for the current or default tenant.
     */
    double getDouble(String paramName, double defaultValue);

    /**
     * Get double config value for a specific tenant.
     */
    double getDouble(String paramName, String tenantId, double defaultValue);

    /**
     * Get raw String config value for the current or default tenant.
     */
    String getString(String paramName, String defaultValue);

    /**
     * Get raw String config value for a specific tenant.
     */
    String getString(String paramName, String tenantId, String defaultValue);

    /**
     * Get all active configuration key-value pairs for a tenant.
     */
    Map<String, String> getAllConfigs(String tenantId);

    /**
     * Manually updates or invalidates the local L1 near cache entry.
     */
    void updateLocalCache(String tenantId, String paramName, String newValue);

    /**
     * Clears local in-memory cache entries.
     */
    void clearLocalCache();
}
