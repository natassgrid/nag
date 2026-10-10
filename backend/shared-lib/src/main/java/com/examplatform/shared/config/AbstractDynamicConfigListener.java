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

import java.util.Map;

/**
 * Base abstract listener for dynamic configuration change and invalidation events.
 * Encapsulates payload deserialization and L1 Near Cache update logic.
 *
 * @author Natassia Grid Development Team
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractDynamicConfigListener {

    public static final String CONFIG_EVENTS_TOPIC = "system.config.events";

    protected final DynamicConfigService dynamicConfigService;
    protected final ObjectMapper objectMapper;

    /**
     * Common processing logic for system configuration change messages.
     *
     * @param message the raw event message (SystemConfigChangeEvent, Map, or JSON string)
     * @param sourceDescription descriptive label for diagnostic log messages (e.g. "Kafka invalidation")
     */
    protected void processConfigChangeEvent(Object message, String sourceDescription) {
        try {
            SystemConfigChangeEvent event = parseEvent(message);
            if (event != null && event.paramName() != null) {
                dynamicConfigService.updateLocalCache(event.tenantId(), event.paramName(), event.newValue());
                log.info("L1 Near Cache updated via {} for param '{}' (tenant: {}) to '{}'",
                        sourceDescription, event.paramName(), event.tenantId(), event.newValue());
            }
        } catch (Exception e) {
            log.warn("Failed to process system configuration invalidation event: {}", e.getMessage());
        }
    }

    /**
     * Parses an arbitrary message into a {@link SystemConfigChangeEvent}.
     *
     * @param message the source message
     * @return the parsed change event, or null if unparseable
     */
    protected SystemConfigChangeEvent parseEvent(Object message) {
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
        if (message instanceof String jsonStr && objectMapper != null) {
            try {
                return objectMapper.readValue(jsonStr, SystemConfigChangeEvent.class);
            } catch (Exception e) {
                log.debug("Could not parse json as SystemConfigChangeEvent: {}", e.getMessage());
            }
        }
        return null;
    }
}
