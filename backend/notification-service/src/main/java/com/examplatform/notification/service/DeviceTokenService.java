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

package com.examplatform.notification.service;

import com.examplatform.notification.domain.DeviceToken;
import com.examplatform.notification.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service to register and manage device push tokens for Web PWA and mobile devices.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    /**
     * Registers or reactivates a device token for push notification delivery.
     */
    public DeviceToken registerToken(UUID userId, String tenantId, String token, String deviceType) {
        DeviceToken deviceToken = deviceTokenRepository.findByUserIdAndToken(userId, token)
                .orElseGet(() -> {
                    DeviceToken dt = DeviceToken.builder()
                            .userId(userId)
                            .token(token)
                            .deviceType(deviceType != null ? deviceType : "WEB_PWA")
                            .active(true)
                            .build();
                    dt.setTenantId(tenantId);
                    return dt;
                });

        deviceToken.setActive(true);
        if (deviceType != null) {
            deviceToken.setDeviceType(deviceType);
        }

        DeviceToken saved = deviceTokenRepository.save(deviceToken);
        log.info("Registered device push token for userId={}, type={}", userId, saved.getDeviceType());
        return saved;
    }

    /**
     * Retrieves active device tokens for a user.
     */
    @Transactional(readOnly = true)
    public List<DeviceToken> getActiveTokens(UUID userId) {
        return deviceTokenRepository.findByUserIdAndActiveTrue(userId);
    }
}
