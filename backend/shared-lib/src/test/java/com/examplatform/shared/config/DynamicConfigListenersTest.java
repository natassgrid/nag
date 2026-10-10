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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("DynamicConfig Listeners Unit Tests")
class DynamicConfigListenersTest {

    private final DynamicConfigService dynamicConfigService = mock(DynamicConfigService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("DynamicConfigInvalidationListener updates cache when receiving SystemConfigChangeEvent")
    void testInvalidationListenerDirectEvent() {
        DynamicConfigInvalidationListener listener =
                new DynamicConfigInvalidationListener(dynamicConfigService, objectMapper);

        SystemConfigChangeEvent event = new SystemConfigChangeEvent("auth.session.ttl", "3600", "7200", "t1", null);
        listener.onConfigChangeEvent(event);

        verify(dynamicConfigService).updateLocalCache("t1", "auth.session.ttl", "7200");
    }

    @Test
    @DisplayName("DynamicConfigInvalidationListener updates cache when receiving Map")
    void testInvalidationListenerMap() {
        DynamicConfigInvalidationListener listener =
                new DynamicConfigInvalidationListener(dynamicConfigService, objectMapper);

        Map<String, String> map = Map.of(
                "paramName", "rate.limit",
                "newValue", "100",
                "tenantId", "t2"
        );
        listener.onConfigChangeEvent(map);

        verify(dynamicConfigService).updateLocalCache("t2", "rate.limit", "100");
    }

    @Test
    @DisplayName("DynamicConfigSpringEventListener updates cache on matching topic")
    void testSpringEventListenerMatchingTopic() {
        DynamicConfigSpringEventListener listener =
                new DynamicConfigSpringEventListener(dynamicConfigService, objectMapper);

        String json = "{\"paramName\":\"max.upload\",\"newValue\":\"50MB\",\"tenantId\":\"t3\"}";
        GenericDomainEvent domainEvent = new GenericDomainEvent(AbstractDynamicConfigListener.CONFIG_EVENTS_TOPIC, "k", json);

        listener.onConfigChangeEvent(domainEvent);

        verify(dynamicConfigService).updateLocalCache("t3", "max.upload", "50MB");
    }

    @Test
    @DisplayName("DynamicConfigSpringEventListener ignores non-matching topic")
    void testSpringEventListenerNonMatchingTopic() {
        DynamicConfigSpringEventListener listener =
                new DynamicConfigSpringEventListener(dynamicConfigService, objectMapper);

        GenericDomainEvent domainEvent = new GenericDomainEvent("other.topic", "k", "{}");

        listener.onConfigChangeEvent(domainEvent);

        verifyNoInteractions(dynamicConfigService);
    }
}
