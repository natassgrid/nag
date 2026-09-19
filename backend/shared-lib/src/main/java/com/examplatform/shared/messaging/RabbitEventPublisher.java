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

package com.examplatform.shared.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * RabbitMQ implementation of {@link EventPublisher}.
 * Used in macro-services / monolith / lightweight broker deployment modes.
 * Publishes events to the shared platform topic exchange 'exam.events' using the topic name as routing key.
 */
@Slf4j
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    public static final String EXCHANGE_NAME = "exam.events";
    public static final String DEAD_LETTER_EXCHANGE = "exam.events.dlx";
    public static final String DEAD_LETTER_QUEUE = "exam.events.dlq";

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(String topic, String key, Object payload) {
        log.info("Publishing event via RabbitMQ to exchange '{}' with routingKey '{}' (key='{}')",
                EXCHANGE_NAME, topic, key);
        rabbitTemplate.convertAndSend(EXCHANGE_NAME, topic, payload);
    }
}
