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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

package com.examplatform.notification.service;

import com.examplatform.notification.domain.Notification;
import com.examplatform.notification.domain.Notification.NotificationStatus;
import com.examplatform.notification.repository.NotificationRepository;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared helper methods for notification delivery services (SMS, Push, WhatsApp).
 * Consolidates duplicated entity lookup and delivery status update logic across channels.
 */
@Slf4j
@UtilityClass
public class NotificationDeliveryHelper {

    /**
     * Resolves the current persisted notification record from repository, or returns fallback.
     */
    public static Notification findCurrent(NotificationRepository repository, UUID notificationId, Notification fallback) {
        if (notificationId != null && repository != null) {
            return repository.findById(notificationId).orElse(fallback);
        }
        return fallback;
    }

    /**
     * Overload for 2-parameter invocation where repository lookup is not performed or fallback is returned.
     */
    public static Notification findCurrent(UUID notificationId, Notification fallback) {
        return fallback;
    }

    /**
     * Records a successful delivery and persists status update.
     */
    public static Notification recordSuccess(
            NotificationRepository repository,
            UUID notificationId,
            Notification fallback,
            int attempt,
            String externalId
    ) {
        Notification current = findCurrent(repository, notificationId, fallback);
        current.setStatus(NotificationStatus.SENT);
        current.setSentAt(Instant.now());
        current.setRetryCount(attempt);
        current.setExternalMessageId(externalId);
        return repository.save(current);
    }

    /**
     * Records a permanent undelivered failure after exhaustion of retries.
     */
    public static Notification recordFailure(
            NotificationRepository repository,
            UUID notificationId,
            Notification fallback,
            int maxRetries
    ) {
        Notification current = findCurrent(repository, notificationId, fallback);
        current.setStatus(NotificationStatus.UNDELIVERED);
        current.setRetryCount(maxRetries);
        return repository.save(current);
    }
}
