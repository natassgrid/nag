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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Broadcast Kafka Listener that subscribes to 'system.config.events'.
 * Every microservice instance maintains its own broadcast consumer group
 * to receive real-time invalidation notifications and update its L1 Near Cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicConfigInvalidationListener {

    public static final String CONFIG_EVENTS_TOPIC = "system.config.events";

    private final DynamicConfigService dynamicConfigService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = CONFIG_EVENTS_TOPIC,
            groupId = "#{T(java.util.UUID).randomUUID().toString()}",
            properties = {"auto.offset.reset=latest"}
    )
    public void onConfigChangeEvent(Object message) {
        try {
            SystemConfigChangeEvent event = parseEvent(message);
            if (event != null && event.paramName() != null) {
                dynamicConfigService.updateLocalCache(event.tenantId(), event.paramName(), event.newValue());
                log.info("L1 Near Cache updated via Kafka invalidation for param '{}' (tenant: {}) to '{}'",
                        event.paramName(), event.tenantId(), event.newValue());
            }
        } catch (Exception e) {
            log.warn("Failed to process system configuration invalidation event: {}", e.getMessage());
        }
    }

    private SystemConfigChangeEvent parseEvent(Object message) {
        if (message instanceof SystemConfigChangeEvent e) {
            return e;
        }
        if (message instanceof Map<?, ?> map) {
            String paramName = (String) map.get("paramName");
            String oldValue = (String) map.get("oldValue");
            String newValue = (String) map.get("newValue");
            String tenantId = (String) map.get("tenantId");
            return new SystemConfigChangeEvent(paramName, oldValue, newValue, tenantId, null);
        }
        if (message instanceof String jsonStr) {
            try {
                return objectMapper.readValue(jsonStr, SystemConfigChangeEvent.class);
            } catch (Exception e) {
                log.debug("Could not parse json as SystemConfigChangeEvent: {}", e.getMessage());
            }
        }
        return null;
    }
}
