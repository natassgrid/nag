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

package com.examplatform.shared.config;

import com.examplatform.shared.tenant.TenantContext;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pure in-memory implementation of {@link DynamicConfigService}.
 * <p>
 * Does not depend on Redis or Spring Data Redis classes.
 * Suitable for standalone operation, services without Redis dependencies,
 * or as a fallback when Redis is absent from the classpath.
 */
@Slf4j
public class InMemoryDynamicConfigService implements DynamicConfigService {

    protected final ConcurrentHashMap<String, String> l1Cache = new ConcurrentHashMap<>();

    public InMemoryDynamicConfigService() {
        log.info("DynamicConfigService initialized in standalone in-memory mode");
    }

    @Override
    public boolean getBoolean(String paramName, boolean defaultValue) {
        return getBoolean(paramName, resolveTenant(), defaultValue);
    }

    @Override
    public boolean getBoolean(String paramName, String tenantId, boolean defaultValue) {
        String val = getRawValue(paramName, tenantId);
        if (val == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(val.trim());
    }

    @Override
    public int getInt(String paramName, int defaultValue) {
        return getInt(paramName, resolveTenant(), defaultValue);
    }

    @Override
    public int getInt(String paramName, String tenantId, int defaultValue) {
        String val = getRawValue(paramName, tenantId);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid integer config for '{}': '{}'. Falling back to default: {}", paramName, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public long getLong(String paramName, long defaultValue) {
        return getLong(paramName, resolveTenant(), defaultValue);
    }

    @Override
    public long getLong(String paramName, String tenantId, long defaultValue) {
        String val = getRawValue(paramName, tenantId);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid long config for '{}': '{}'. Falling back to default: {}", paramName, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public double getDouble(String paramName, double defaultValue) {
        return getDouble(paramName, resolveTenant(), defaultValue);
    }

    @Override
    public double getDouble(String paramName, String tenantId, double defaultValue) {
        String val = getRawValue(paramName, tenantId);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Invalid double config for '{}': '{}'. Falling back to default: {}", paramName, val, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public String getString(String paramName, String defaultValue) {
        return getString(paramName, resolveTenant(), defaultValue);
    }

    @Override
    public String getString(String paramName, String tenantId, String defaultValue) {
        String val = getRawValue(paramName, tenantId);
        return (val != null) ? val : defaultValue;
    }

    @Override
    public Map<String, String> getAllConfigs(String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        Map<String, String> result = new LinkedHashMap<>(DefaultPlatformConfigs.DEFAULTS);

        // Overlay L1 near cache
        String prefix = effectiveTenant + ":";
        l1Cache.forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                String paramName = k.substring(prefix.length());
                result.put(paramName, v);
            }
        });

        return result;
    }

    @Override
    public void updateLocalCache(String tenantId, String paramName, String newValue) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        String cacheKey = makeCacheKey(effectiveTenant, paramName);
        if (newValue == null) {
            l1Cache.remove(cacheKey);
            log.debug("Evicted Near Cache entry for key '{}'", cacheKey);
        } else {
            l1Cache.put(cacheKey, newValue);
            log.debug("Updated Near Cache key '{}' to '{}'", cacheKey, newValue);
        }
    }

    @Override
    public void clearLocalCache() {
        l1Cache.clear();
        log.info("Cleared all L1 Near Cache entries");
    }

    protected String getRawValue(String paramName, String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        String cacheKey = makeCacheKey(effectiveTenant, paramName);

        // 1. L1 In-Memory Near Cache Hit
        String l1Val = l1Cache.get(cacheKey);
        if (l1Val != null) {
            return l1Val;
        }

        // 2. L3 Platform Hardcoded Default
        String defaultVal = DefaultPlatformConfigs.getDefault(paramName, null);
        if (defaultVal != null) {
            l1Cache.put(cacheKey, defaultVal);
            return defaultVal;
        }

        return null;
    }

    protected String makeCacheKey(String tenantId, String paramName) {
        return tenantId + ":" + paramName;
    }

    protected String resolveTenant() {
        String contextTenant = TenantContext.get();
        return (contextTenant != null && !contextTenant.isBlank()) ? contextTenant : "default";
    }
}
