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

package com.examplatform.notification.consumer;

import com.examplatform.notification.service.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for outbound notification events on the
 * {@code exam.notifications.outbound} topic. Delegates processing to
 * {@link NotificationProcessingService} which handles notification creation
 * and delivery dispatch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationProcessingService notificationProcessingService;

    /**
     * Consumes notification events from Kafka and delegates to the processing service.
     * The processing service handles parsing, notification creation, and delivery dispatch.
     *
     * @param message the raw event payload (JSON)
     */
    @KafkaListener(topics = "exam.notifications.outbound", groupId = "notification-service")
    public void onNotificationEvent(String message) {
        log.info("Received notification event: {}", message);
        notificationProcessingService.processEvent(message);
    }
}
