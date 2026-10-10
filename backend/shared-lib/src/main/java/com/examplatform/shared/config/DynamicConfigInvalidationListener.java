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

package com.examplatform.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Broadcast Kafka Listener that subscribes to 'system.config.events'.
 * Every microservice instance maintains its own broadcast consumer group
 * to receive real-time invalidation notifications and update its L1 Near Cache.
 */
@Slf4j
public class DynamicConfigInvalidationListener extends AbstractDynamicConfigListener {

    public static final String CONFIG_EVENTS_TOPIC = AbstractDynamicConfigListener.CONFIG_EVENTS_TOPIC;

    public DynamicConfigInvalidationListener(DynamicConfigService dynamicConfigService, ObjectMapper objectMapper) {
        super(dynamicConfigService, objectMapper);
    }

    @KafkaListener(
            topics = CONFIG_EVENTS_TOPIC,
            groupId = "#{T(java.util.UUID).randomUUID().toString()}",
            properties = {"auto.offset.reset=latest"}
    )
    public void onConfigChangeEvent(Object message) {
        processConfigChangeEvent(message, "Kafka invalidation");
    }
}
