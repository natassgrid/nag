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

import com.examplatform.shared.jackson.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring configuration class that registers the Dynamic Config components.
 * Supports both Redis-backed Near Cache (when Spring Data Redis is present)
 * and pure in-memory fallback (when Spring Data Redis is not on the classpath).
 */
@AutoConfiguration
@AutoConfigureAfter(JacksonConfig.class)
public class DynamicConfigAutoConfiguration {

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisNearCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(DynamicConfigService.class)
        public DynamicConfigService dynamicConfigService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
            return new NearCacheConfigService(redisTemplateProvider);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingClass("org.springframework.data.redis.core.StringRedisTemplate")
    static class StandaloneNearCacheConfiguration {

        @Bean
        @ConditionalOnMissingBean(DynamicConfigService.class)
        public DynamicConfigService dynamicConfigService() {
            return new InMemoryDynamicConfigService();
        }
    }

    @Bean
    @ConditionalOnClass(name = "org.springframework.kafka.annotation.KafkaListener")
    @ConditionalOnMissingBean(DynamicConfigInvalidationListener.class)
    public DynamicConfigInvalidationListener dynamicConfigInvalidationListener(
            DynamicConfigService dynamicConfigService,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        ObjectMapper mapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new DynamicConfigInvalidationListener(dynamicConfigService, mapper);
    }
}
