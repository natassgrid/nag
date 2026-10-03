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

package com.examplatform.identity.service;

import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultGeoIpService")
class DefaultGeoIpServiceTest {

    @Mock
    private DynamicConfigService dynamicConfigService;

    private DefaultGeoIpService geoIpService;

    @BeforeEach
    void setUp() {
        geoIpService = new DefaultGeoIpService(dynamicConfigService, "");
        geoIpService.init();
    }

    @Nested
    @DisplayName("resolveCountry")
    class ResolveCountry {

        @Test
        @DisplayName("null or blank IP returns UNKNOWN")
        void nullOrBlankReturnsUnknown() {
            assertThat(geoIpService.resolveCountry(null)).isEqualTo(DefaultGeoIpService.UNKNOWN_ZONE);
            assertThat(geoIpService.resolveCountry("  ")).isEqualTo(DefaultGeoIpService.UNKNOWN_ZONE);
        }

        @Test
        @DisplayName("loopback and private IPv4/IPv6 resolve to LOCAL")
        void privateIpsResolveToLocal() {
            assertThat(geoIpService.resolveCountry("127.0.0.1")).isEqualTo(DefaultGeoIpService.LOCAL_ZONE);
            assertThat(geoIpService.resolveCountry("::1")).isEqualTo(DefaultGeoIpService.LOCAL_ZONE);
            assertThat(geoIpService.resolveCountry("192.168.1.1")).isEqualTo(DefaultGeoIpService.LOCAL_ZONE);
            assertThat(geoIpService.resolveCountry("10.0.0.5")).isEqualTo(DefaultGeoIpService.LOCAL_ZONE);
        }

        @Test
        @DisplayName("standard public IP heuristics resolve correctly")
        void heuristicResolution() {
            assertThat(geoIpService.resolveCountry("103.21.244.1")).isEqualTo("IN");
            assertThat(geoIpService.resolveCountry("8.8.8.8")).isEqualTo("US");
            assertThat(geoIpService.resolveCountry("81.2.69.142")).isEqualTo("GB");
        }

        @Test
        @DisplayName("explicit IP country registration overrides heuristic")
        void explicitOverrideWorks() {
            geoIpService.registerIpCountry("203.0.113.195", "SG");
            assertThat(geoIpService.resolveCountry("203.0.113.195")).isEqualTo("SG");
        }
    }

    @Nested
    @DisplayName("isHighRiskIp")
    class IsHighRiskIp {

        @Test
        @DisplayName("known seed Tor exit nodes are flagged as high risk")
        void defaultTorExitNodesDetected() {
            assertThat(geoIpService.isHighRiskIp("185.220.101.5")).isTrue();
            assertThat(geoIpService.isHighRiskIp("198.51.100.25")).isTrue();
        }

        @Test
        @DisplayName("standard legitimate IP is not flagged as high risk")
        void standardIpNotFlagged() {
            assertThat(geoIpService.isHighRiskIp("103.21.244.1")).isFalse();
            assertThat(geoIpService.isHighRiskIp("127.0.0.1")).isFalse();
        }

        @Test
        @DisplayName("IP listed in dynamic threat intelligence feed is flagged")
        void dynamicThreatListDetected() {
            when(dynamicConfigService.getString(eq("auth.risk.ip.threat-list"), eq("default"), eq("")))
                    .thenReturn("203.0.113.50,198.51.100.99");

            assertThat(geoIpService.isHighRiskIp("203.0.113.50")).isTrue();
            assertThat(geoIpService.isHighRiskIp("203.0.113.51")).isFalse();
        }
    }
}
