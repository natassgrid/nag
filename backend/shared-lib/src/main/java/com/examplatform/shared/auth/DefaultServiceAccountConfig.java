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

package com.examplatform.shared.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Default fallback configuration for ServiceAccountTokenClient.
 * Provides a mock token client for development and local testing if no custom client is provided.
 */
@Configuration
public class DefaultServiceAccountConfig {

    @Bean
    @ConditionalOnMissingBean(ServiceAccountTokenClient.class)
    public ServiceAccountTokenClient defaultServiceAccountTokenClient() {
        return serviceId -> TokenResponse.builder()
                .accessToken("dev-service-token")
                .expiresIn(3600)
                .tokenType("Bearer")
                .build();
    }
}
