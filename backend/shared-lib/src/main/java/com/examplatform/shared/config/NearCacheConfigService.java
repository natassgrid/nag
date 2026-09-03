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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * High-performance Near Cache implementation for system-wide dynamic configurations.
 * <p>
 * Reading hierarchy:
 * 1. L1 In-Memory Near Cache (ConcurrentHashMap): ~10-50 nanoseconds, zero network hops.
 * 2. L2 Shared Redis Cache (nag:config:{tenantId}): ~0.5-1.5 milliseconds, populates L1 on hit.
 * 3. L3 Platform Hardcoded Defaults (DefaultPlatformConfigs): failsafe fallback.
 */
@Slf4j
public class NearCacheConfigService extends InMemoryDynamicConfigService {

    private static final String REDIS_CONFIG_KEY_PREFIX = "nag:config:";

    private final StringRedisTemplate redisTemplate;

    public NearCacheConfigService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        super();
        this.redisTemplate = redisTemplateProvider != null ? redisTemplateProvider.getIfAvailable() : null;
        if (this.redisTemplate != null) {
            log.info("NearCacheConfigService initialized with Redis L2 shared cache");
        } else {
            log.info("NearCacheConfigService initialized in standalone in-memory mode (Redis unavailable)");
        }
    }

    @Override
    public Map<String, String> getAllConfigs(String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        Map<String, String> result = new LinkedHashMap<>(DefaultPlatformConfigs.DEFAULTS);

        // Overlay L2 from Redis if present
        if (redisTemplate != null) {
            try {
                String redisKey = REDIS_CONFIG_KEY_PREFIX + effectiveTenant;
                Map<Object, Object> redisEntries = redisTemplate.opsForHash().entries(redisKey);
                if (redisEntries != null && !redisEntries.isEmpty()) {
                    redisEntries.forEach((k, v) -> {
                        if (k != null && v != null) {
                            result.put(k.toString(), v.toString());
                        }
                    });
                }
            } catch (Exception e) {
                log.warn("Failed to fetch all configs from Redis for tenant {}: {}", effectiveTenant, e.getMessage());
            }
        }

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
    protected String getRawValue(String paramName, String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        String cacheKey = makeCacheKey(effectiveTenant, paramName);

        // 1. L1 In-Memory Near Cache Hit
        String l1Val = l1Cache.get(cacheKey);
        if (l1Val != null) {
            return l1Val;
        }

        // 2. L2 Shared Redis Cache Hit
        if (redisTemplate != null) {
            try {
                String redisKey = REDIS_CONFIG_KEY_PREFIX + effectiveTenant;
                Object redisVal = redisTemplate.opsForHash().get(redisKey, paramName);
                if (redisVal != null) {
                    String strVal = redisVal.toString();
                    l1Cache.put(cacheKey, strVal);
                    return strVal;
                }
            } catch (Exception e) {
                log.warn("Failed to read config '{}' from Redis for tenant {}: {}", paramName, effectiveTenant, e.getMessage());
            }
        }

        // 3. L3 Platform Hardcoded Default
        String defaultVal = DefaultPlatformConfigs.getDefault(paramName, null);
        if (defaultVal != null) {
            l1Cache.put(cacheKey, defaultVal);
            return defaultVal;
        }

        return null;
    }
}
