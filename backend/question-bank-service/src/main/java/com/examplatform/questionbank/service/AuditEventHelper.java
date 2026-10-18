/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import com.examplatform.shared.constants.SharedConstants;
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

    public record AuditEventParams(
            String topic,
            String eventType,
            String entityKey,
            UUID entityId,
            UUID actorId,
            String tenantId,
            Map<String, Object> extra
    ) {
        public AuditEventParams(String topic, String eventType, String entityKey, UUID entityId, UUID actorId, String tenantId) {
            this(topic, eventType, entityKey, entityId, actorId, tenantId, null);
        }
    }

    public static void publishAuditEvent(EventPublisher eventPublisher, Logger log, AuditEventParams params) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put(SharedConstants.EVENT_TYPE, params.eventType());
            event.put(params.entityKey(), params.entityId().toString());
            event.put("actorId", params.actorId().toString());
            event.put(SharedConstants.TENANT_ID, params.tenantId());
            event.put(SharedConstants.OCCURRED_AT, Instant.now().toString());
            if (params.extra() != null) {
                event.putAll(params.extra());
            }
            eventPublisher.publish(params.topic(), params.entityId().toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", params.eventType(), e.getMessage());
        }
    }
}
