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

package com.examplatform.analytics.config;

import com.examplatform.shared.config.DynamicConfigService;
import com.examplatform.shared.config.InMemoryDynamicConfigService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsDynamicConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    com.examplatform.shared.config.DynamicConfigAutoConfiguration.class
            ));

    @Test
    @DisplayName("DynamicConfigService falls back to InMemoryDynamicConfigService when Redis is not on classpath")
    void dynamicConfigService_loadsInMemoryFallbackWithoutRedis() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(DynamicConfigService.class);
            DynamicConfigService service = context.getBean(DynamicConfigService.class);
            assertThat(service).isInstanceOf(InMemoryDynamicConfigService.class);
            assertThat(service.getBoolean("auth.mfa.enforced", true)).isFalse();
        });
    }
}
