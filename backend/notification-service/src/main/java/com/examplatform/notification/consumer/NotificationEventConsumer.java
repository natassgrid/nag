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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer that listens for outbound notification events on the
 * {@code exam.notifications.outbound} topic. Delegates processing to
 * {@link NotificationProcessingService} which handles notification creation
 * and delivery dispatch.
 * Supports both Kafka and RabbitMQ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationProcessingService notificationProcessingService;
    private final ObjectMapper objectMapper;

    /**
     * Consumes notification events from Kafka.
     *
     * @param message the raw event payload (JSON)
     */
    @KafkaListener(topics = "exam.notifications.outbound", groupId = "notification-service")
    public void onKafkaNotificationEvent(String message) {
        log.info("Received Kafka notification event: {}", message);
        notificationProcessingService.processEvent(message);
    }

    /**
     * Consumes notification events from RabbitMQ.
     *
     * @param message the event payload (String or Object)
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "notification.events.queue", durable = "true"),
                    exchange = @Exchange(value = "exam.events", type = ExchangeTypes.TOPIC),
                    key = "exam.notifications.outbound"
            )
    )
    public void onRabbitNotificationEvent(Object message) {
        log.info("Received RabbitMQ notification event: {}", message);
        try {
            String payload;
            if (message instanceof String s) {
                payload = s;
            } else {
                payload = objectMapper.writeValueAsString(message);
            }
            notificationProcessingService.processEvent(payload);
        } catch (Exception e) {
            log.error("Failed to process RabbitMQ notification event: {}", e.getMessage(), e);
        }
    }
}
