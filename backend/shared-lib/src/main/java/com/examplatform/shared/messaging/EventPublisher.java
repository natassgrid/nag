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

/**
 * Common abstraction for event publishing across the platform.
 * Allows transparent switching between Kafka (full microservices mode),
 * RabbitMQ (lightweight broker / macro-services mode), and in-process Spring events.
 */
public interface EventPublisher {

    /**
     * Publish an event to the specified topic/routing key with a partition key and payload.
     *
     * @param topic   the logical destination (Kafka topic, RabbitMQ exchange/routing key)
     * @param key     the partition/routing key (e.g. tenantId, sessionId, or null)
     * @param payload the message payload object
     */
    void publish(String topic, String key, Object payload);

    /**
     * Publish an event to the specified topic/routing key without an explicit key.
     *
     * @param topic   the logical destination
     * @param payload the message payload object
     */
    default void publish(String topic, Object payload) {
        publish(topic, null, payload);
    }
}
