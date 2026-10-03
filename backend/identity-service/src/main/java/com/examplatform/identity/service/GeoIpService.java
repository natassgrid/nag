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

/**
 * Service for IP geolocation resolution, country matching, and threat intelligence (Tor/VPN detection).
 *
 * <p><strong>Validates: Requirements 2.6 (Risk Signal 3 — IP/geo change)</strong></p>
 */
public interface GeoIpService {

    /**
     * Resolves the 2-letter ISO country code (e.g., "IN", "US", "GB") for an IP address.
     * Returns {@code "LOCAL"} for private / loopback IP addresses, or {@code "UNKNOWN"} if resolution fails.
     *
     * @param ipAddress the IPv4 or IPv6 address string
     * @return 2-letter ISO country code, "LOCAL", or "UNKNOWN"
     */
    String resolveCountry(String ipAddress);

    /**
     * Determines whether the given IP address is a known Tor exit node, VPN, or high-risk proxy.
     *
     * @param ipAddress the IPv4 or IPv6 address string
     * @return {@code true} if the IP is identified as high risk
     */
    boolean isHighRiskIp(String ipAddress);
}
