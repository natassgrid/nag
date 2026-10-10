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

package com.examplatform.questionbank.service;

import com.examplatform.shared.messaging.EventPublisher;
import org.slf4j.Logger;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shared helper for publishing audit events within question-bank-service.
 */
public final class AuditEventHelper {

    private AuditEventHelper() {}

    public static void publishAuditEvent(EventPublisher eventPublisher, Logger log, String topic,
                                         String eventType, String entityKey, UUID entityId,
                                         UUID actorId, String tenantId, Map<String, Object> extra) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put(entityKey, entityId.toString());
            event.put("actorId", actorId.toString());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());
            if (extra != null) {
                event.putAll(extra);
            }
            eventPublisher.publish(topic, entityId.toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", eventType, e.getMessage());
        }
    }
}
