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

package com.examplatform.asset.config;

import com.examplatform.shared.auth.ServiceAccountTokenClient;
import com.examplatform.shared.auth.TokenResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides a no-op ServiceAccountTokenClient for local/dev environments.
 * In production, this would be replaced by a Keycloak client_credentials implementation.
 */
@Configuration
public class ServiceAccountConfig {

    @Bean
    public ServiceAccountTokenClient serviceAccountTokenClient() {
        return serviceId -> TokenResponse.builder()
                .accessToken("dev-service-token")
                .expiresIn(3600)
                .tokenType("Bearer")
                .build();
    }
}
