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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Spring configuration class that registers the Near Cache components.
 */
@Configuration
public class DynamicConfigAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(DynamicConfigService.class)
    public DynamicConfigService dynamicConfigService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        return new NearCacheConfigService(redisTemplateProvider);
    }

    @Bean
    @ConditionalOnMissingBean(DynamicConfigInvalidationListener.class)
    public DynamicConfigInvalidationListener dynamicConfigInvalidationListener(
            DynamicConfigService dynamicConfigService,
            ObjectMapper objectMapper) {
        return new DynamicConfigInvalidationListener(dynamicConfigService, objectMapper);
    }
}
