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

import com.examplatform.identity.domain.UserAccount;
import com.examplatform.shared.config.DynamicConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Assesses risk signals during authentication and determines whether
 * step-up authentication (MFA/OTP) should be required even if the
 * account does not have {@code mfaEnabled=true}.
 *
 * <p>Risk signals evaluated:
 * <ul>
 *   <li>New device — device fingerprint differs from stored value</li>
 *   <li>Unusual login time — login outside 06:00–23:00 local time</li>
 *   <li>IP/Geo change — IP change across countries or known Tor/VPN detection</li>
 * </ul>
 *
 * <p><strong>Validates: Requirements 2.6 (Risk Signal 3 — IP/geo change)</strong></p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskAssessmentService {

    public static final String LAST_LOGIN_IP_PREFIX = "last-login-ip:";
    public static final Duration DEFAULT_IP_TTL = Duration.ofDays(30);

    private final RedisTemplate<String, Object> redisTemplate;
    private final GeoIpService geoIpService;
    private final DynamicConfigService dynamicConfigService;

    /**
     * Records the source IP address of a successfully authenticated user in Redis.
     *
     * @param userId   the user account identifier
     * @param ipAddress the originating IP address
     * @param tenantId  the tenant identifier
     */
    public void recordSuccessfulLoginIp(UUID userId, String ipAddress, String tenantId) {
        if (userId == null || ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        String key = LAST_LOGIN_IP_PREFIX + userId;
        int ttlDays = 30;
        if (dynamicConfigService != null) {
            ttlDays = dynamicConfigService.getInt("auth.risk.ip.ttl.days", tenantId, 30);
        }
        Duration ttl = Duration.ofDays(Math.max(1, ttlDays));
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, ipAddress.trim(), ttl);
                log.debug("Recorded last successful login IP [{}] for user [{}] with TTL {} days", ipAddress, userId, ttlDays);
            }
        } catch (Exception ex) {
            log.warn("Failed to store last login IP in Redis for user [{}]: {}", userId, ex.getMessage());
        }
    }

    /**
     * Retrieves the last successful login IP address recorded for a user.
     *
     * @param userId the user identifier
     * @return the IP address string, or {@code null} if none exists
     */
    public String getLastLoginIp(UUID userId) {
        if (userId == null || redisTemplate == null) {
            return null;
        }
        String key = LAST_LOGIN_IP_PREFIX + userId;
        try {
            Object val = redisTemplate.opsForValue().get(key);
            return val != null ? val.toString() : null;
        } catch (Exception ex) {
            log.warn("Failed to read last login IP from Redis for user [{}]: {}", userId, ex.getMessage());
            return null;
        }
    }

    /**
     * Determines if step-up authentication should be required based on
     * contextual risk signals.
     *
     * @param account           the user account being authenticated
     * @param deviceFingerprint the device fingerprint from the current request
     * @param ipAddress         the originating IP address
     * @param loginTime         the timestamp of the login attempt
     * @return {@code true} if step-up authentication is required
     */
    public boolean isStepUpRequired(UserAccount account, String deviceFingerprint,
                                    String ipAddress, LocalDateTime loginTime) {
        String tenantId = account != null && account.getTenantId() != null ? account.getTenantId() : "default";

        // Risk signal 1: new device
        if (account != null && account.getDeviceFingerprint() != null
                && deviceFingerprint != null
                && !account.getDeviceFingerprint().equals(deviceFingerprint)) {
            log.info("Risk signal: new device detected for user {}", account.getId());
            return true;
        }

        // Risk signal 2: unusual time (before 6 AM or after 11 PM)
        int hour = loginTime.getHour();
        if (hour < 6 || hour >= 23) {
            log.info("Risk signal: unusual login time ({}) for user {}", hour, account != null ? account.getId() : "unknown");
            return true;
        }

        // Risk signal 3: IP/Geo change & Threat Intelligence
        boolean ipRiskEnabled = dynamicConfigService == null
                || dynamicConfigService.getBoolean("auth.risk.ip.enabled", tenantId, true);

        if (ipRiskEnabled && ipAddress != null && !ipAddress.isBlank() && account != null) {
            String cleanIp = ipAddress.trim();

            // 3a. Tor exit node / Known VPN / High-risk IP detection
            boolean vpnTorEnabled = dynamicConfigService == null
                    || dynamicConfigService.getBoolean("auth.risk.ip.vpn-tor.enabled", tenantId, true);
            if (vpnTorEnabled && geoIpService != null && geoIpService.isHighRiskIp(cleanIp)) {
                log.warn("Risk signal: high-risk / Tor / VPN IP detected [{}] for user [{}]", cleanIp, account.getId());
                return true;
            }

            // 3b. IP change & Country change detection
            boolean countryChangeEnabled = dynamicConfigService == null
                    || dynamicConfigService.getBoolean("auth.risk.ip.country-change.enabled", tenantId, true);

            if (countryChangeEnabled && geoIpService != null) {
                String lastLoginIp = getLastLoginIp(account.getId());
                if (lastLoginIp != null && !lastLoginIp.isBlank() && !lastLoginIp.equalsIgnoreCase(cleanIp)) {
                    String lastCountry = geoIpService.resolveCountry(lastLoginIp);
                    String currentCountry = geoIpService.resolveCountry(cleanIp);

                    if (!DefaultGeoIpService.UNKNOWN_ZONE.equalsIgnoreCase(lastCountry)
                            && !DefaultGeoIpService.UNKNOWN_ZONE.equalsIgnoreCase(currentCountry)
                            && !lastCountry.equalsIgnoreCase(currentCountry)) {
                        log.info("Risk signal: IP geo-change detected for user [{}] from country [{}] ({}) to [{}] ({})",
                                account.getId(), lastCountry, lastLoginIp, currentCountry, cleanIp);
                        return true;
                    } else {
                        log.debug("IP changed for user [{}] within same country/zone [{}] ({} -> {})",
                                account.getId(), currentCountry, lastLoginIp, cleanIp);
                    }
                }
            }
        }

        return false;
    }
}
