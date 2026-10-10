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

import com.examplatform.shared.messaging.GenericDomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;

/**
 * Spring in-memory EventListener that subscribes to 'system.config.events'.
 * Used for zero-broker monolith or embedded deployment modes to update L1 Near Cache.
 */
@Slf4j
public class DynamicConfigSpringEventListener extends AbstractDynamicConfigListener {

    public static final String CONFIG_EVENTS_TOPIC = AbstractDynamicConfigListener.CONFIG_EVENTS_TOPIC;

    public DynamicConfigSpringEventListener(DynamicConfigService dynamicConfigService, ObjectMapper objectMapper) {
        super(dynamicConfigService, objectMapper);
    }

    @EventListener
    public void onConfigChangeEvent(GenericDomainEvent genericEvent) {
        if (!CONFIG_EVENTS_TOPIC.equals(genericEvent.topic())) {
            return;
        }
        processConfigChangeEvent(genericEvent.payload(), "in-memory event");
    }
}
