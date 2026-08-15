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

package com.examplatform.notification.controller;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.service.InAppNotificationService;
import com.examplatform.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * REST controller for in-app notifications.
 * Provides paginated listing, SSE streaming, and mark-as-read endpoints.
 *
 * Validates: Requirements 14.3
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class InAppNotificationController {

    private final InAppNotificationService inAppNotificationService;

    /**
     * Active SSE emitters keyed by userId for real-time push.
     */
    private final Map<UUID, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    /**
     * Returns paginated notifications for the authenticated user,
     * filtered by role + userId.
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Notification>> getNotifications(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        Page<Notification> notifications = inAppNotificationService.getNotifications(
                userId, tenantId, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * SSE endpoint for real-time push notifications.
     * The client connects and receives notifications as they arrive.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter streamNotifications(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        log.info("SSE connection established for userId={}", userId);

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // Remove on completion/timeout/error
        emitter.onCompletion(() -> {
            activeEmitters.remove(userId);
            log.debug("SSE connection completed for userId={}", userId);
        });
        emitter.onTimeout(() -> {
            activeEmitters.remove(userId);
            log.debug("SSE connection timed out for userId={}", userId);
        });
        emitter.onError(ex -> {
            activeEmitters.remove(userId);
            log.debug("SSE connection error for userId={}: {}", userId, ex.getMessage());
        });

        activeEmitters.put(userId, emitter);

        // Send initial keep-alive event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connection established"));
        } catch (IOException e) {
            log.warn("Failed to send initial SSE event for userId={}", userId);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * Marks a notification as read.
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Notification> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        String tenantId = TenantContext.get() != null ? TenantContext.get() : "default";

        Notification notification = inAppNotificationService.markAsRead(id, userId, tenantId);
        return ResponseEntity.ok(notification);
    }

    /**
     * Sends a notification event to a connected SSE client.
     * Called internally when a new notification is created.
     *
     * @param userId       the target user's ID
     * @param notification the notification to push
     */
    public void pushNotification(UUID userId, Notification notification) {
        SseEmitter emitter = activeEmitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException e) {
                log.warn("Failed to push notification to userId={}, removing emitter", userId);
                activeEmitters.remove(userId);
                emitter.completeWithError(e);
            }
        }
    }
}
