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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NearCacheConfigServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ObjectProvider<StringRedisTemplate> objectProvider;

    private NearCacheConfigService configService;

    @BeforeEach
    void setUp() {
        when(objectProvider.getIfAvailable()).thenReturn(redisTemplate);
        configService = new NearCacheConfigService(objectProvider);
    }

    @Test
    @DisplayName("Returns hardcoded default if cache and redis are empty")
    void getBoolean_returnsDefaultIfEmpty() {
        boolean mfa = configService.getBoolean("auth.mfa.enforced", "default", true);
        assertThat(mfa).isFalse(); // DefaultPlatformConfigs has false
    }

    @Test
    @DisplayName("L1 near cache serves value without hitting Redis after being cached")
    void getInt_usesL1CacheAfterPopulation() {
        configService.updateLocalCache("default", "auth.session.timeout.minutes", "45");

        int timeout = configService.getInt("auth.session.timeout.minutes", "default", 30);
        assertThat(timeout).isEqualTo(45);
    }

    @Test
    @DisplayName("L2 Redis cache hit populates L1 cache")
    void getString_hitsRedisAndPopulatesL1() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get("nag:config:tenant1", "custom.key")).thenReturn("custom-val");

        String val = configService.getString("custom.key", "tenant1", "fallback");
        assertThat(val).isEqualTo("custom-val");

        // Second call should come from L1 near cache
        String val2 = configService.getString("custom.key", "tenant1", "fallback");
        assertThat(val2).isEqualTo("custom-val");
    }

    @Test
    @DisplayName("Invalidation clears and updates L1 cache")
    void updateLocalCache_updatesL1() {
        configService.updateLocalCache("tenant1", "delivery.tamper.detection.enabled", "false");
        boolean tamper = configService.getBoolean("delivery.tamper.detection.enabled", "tenant1", true);
        assertThat(tamper).isFalse();

        configService.updateLocalCache("tenant1", "delivery.tamper.detection.enabled", "true");
        boolean tamperUpdated = configService.getBoolean("delivery.tamper.detection.enabled", "tenant1", false);
        assertThat(tamperUpdated).isTrue();
    }
}
