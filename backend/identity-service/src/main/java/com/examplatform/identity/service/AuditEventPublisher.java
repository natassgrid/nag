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

package com.examplatform.identity.service;

import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static com.examplatform.shared.constants.MessagingConstants.AUDIT_EVENTS_TOPIC;

/**
 * Publishes structured audit events to the {@code exam.audit.events} destination.
 * Failures are logged but never propagate — audit writes must not block or
 * fail the originating business operation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final EventPublisher eventPublisher;

    /**
     * Build and publish an audit event asynchronously.
     *
     * @param type              the type of audit event
     * @param actorId           identifier of the user performing the action
     * @param resource          URI or name of the affected resource
     * @param ip                originating IP address
     * @param deviceFingerprint device fingerprint from the request, may be {@code null}
     * @param extra             additional context properties to include in the event
     */
    public void publish(
            AuditEventType type,
            String actorId,
            String resource,
            String ip,
            String deviceFingerprint,
            Map<String, Object> extra) {

        try {
            Map<String, Object> event = new HashMap<>();
            event.put(SharedConstants.EVENT_TYPE, type.name());
            event.put("actorId", actorId);
            event.put("resource", resource);
            event.put("ip", ip);
            event.put("deviceFingerprint", deviceFingerprint);
            event.put(SharedConstants.OCCURRED_AT, Instant.now().toString());
            if (extra != null) {
                event.putAll(extra);
            }

            eventPublisher.publish(AUDIT_EVENTS_TOPIC, actorId, event);
            log.debug("Audit event published [type={}, actor={}]", type, actorId);
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [type={}]: {}", type, e.getMessage(), e);
        }
    }

    /**
     * Build and publish an audit event asynchronously on a background thread.
     *
     * @param type              the type of audit event
     * @param actorId           identifier of the user performing the action
     * @param resource          URI or name of the affected resource
     * @param ip                originating IP address
     * @param deviceFingerprint device fingerprint from the request, may be {@code null}
     * @param extra             additional context properties to include in the event
     */
    @Async
    public void publishAsync(
            AuditEventType type,
            String actorId,
            String resource,
            String ip,
            String deviceFingerprint,
            Map<String, Object> extra) {
        publish(type, actorId, resource, ip, deviceFingerprint, extra);
    }
}
