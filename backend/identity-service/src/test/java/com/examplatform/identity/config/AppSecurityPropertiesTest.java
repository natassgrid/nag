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

package com.examplatform.identity.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("AppSecurityProperties — default values")
class AppSecurityPropertiesTest {

    @Test
    @DisplayName("all default values match the specification")
    void defaultValuesMatchSpec() {
        AppSecurityProperties props = new AppSecurityProperties();

        assertAll(
            () -> assertEquals(5,     props.getMaxFailedAttempts(),
                    "maxFailedAttempts must default to 5"),
            () -> assertEquals(600,   props.getLockoutWindowSeconds(),
                    "lockoutWindowSeconds must default to 600"),
            () -> assertEquals(10,    props.getRateLimitAuthPerIpPerMinute(),
                    "rateLimitAuthPerIpPerMinute must default to 10"),
            () -> assertEquals(900L,  props.getJwtAccessTokenLifetimeSeconds(),
                    "jwtAccessTokenLifetimeSeconds must default to 900"),
            () -> assertEquals(28800L, props.getJwtRefreshTokenLifetimeSeconds(),
                    "jwtRefreshTokenLifetimeSeconds must default to 28800"),
            () -> assertEquals(1800,  props.getSessionIdleTimeoutSeconds(),
                    "sessionIdleTimeoutSeconds must default to 1800")
        );
    }
}
