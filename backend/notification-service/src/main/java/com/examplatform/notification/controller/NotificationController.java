/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.notification.controller;

import com.examplatform.notification.domain.DeviceToken;
import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.NotificationPreference;
import com.examplatform.notification.dto.DeviceTokenRequest;
import com.examplatform.notification.dto.NotificationPreferenceRequest;
import com.examplatform.notification.dto.NotificationSendRequest;
import com.examplatform.notification.service.DeviceTokenService;
import com.examplatform.notification.service.NotificationPreferenceService;
import com.examplatform.notification.service.NotificationProcessingService;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for multi-channel notification dispatch, device token registration,
 * and user communication preferences.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationProcessingService notificationProcessingService;
    private final NotificationPreferenceService preferenceService;
    private final DeviceTokenService deviceTokenService;

    /**
     * Dispatches a notification across multi-channel endpoints (Email, SMS, WhatsApp, Push, In-App).
     */
    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> sendNotification(
            @Valid @RequestBody NotificationSendRequest request) {
        String tenantId = TenantContext.getTenantIdOrDefault();
        if (request.getTenantId() == null) {
            request.setTenantId(tenantId);
        }
        if (request.getUserId() == null) {
            request.setUserId(UserContext.getUserId());
        }

        Notification notification = notificationProcessingService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(notification);
    }

    /**
     * Registers a Web PWA or mobile push notification device token.
     */
    @PostMapping("/push/register-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DeviceToken> registerDeviceToken(
            @Valid @RequestBody DeviceTokenRequest request) {
        UUID userId = request.getUserId() != null ? request.getUserId() : UserContext.getUserId();
        String tenantId = TenantContext.getTenantIdOrDefault();

        DeviceToken registered = deviceTokenService.registerToken(
                userId != null ? userId : UUID.randomUUID(),
                tenantId,
                request.getToken(),
                request.getDeviceType());
        return ResponseEntity.status(HttpStatus.CREATED).body(registered);
    }

    /**
     * Retrieves communication channel preferences for a candidate.
     */
    @GetMapping("/preferences/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreference> getPreferences(
            @PathVariable UUID userId) {
        String tenantId = TenantContext.getTenantIdOrDefault();
        NotificationPreference preference = preferenceService.getPreferences(userId, tenantId);
        return ResponseEntity.ok(preference);
    }

    /**
     * Updates communication channel preferences for a candidate.
     */
    @PutMapping("/preferences/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<NotificationPreference> updatePreferences(
            @PathVariable UUID userId,
            @RequestBody NotificationPreferenceRequest request) {
        String tenantId = TenantContext.getTenantIdOrDefault();
        NotificationPreference updated = preferenceService.updatePreferences(userId, tenantId, request);
        return ResponseEntity.ok(updated);
    }
}
