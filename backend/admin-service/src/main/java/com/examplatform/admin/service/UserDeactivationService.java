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

package com.examplatform.admin.service;

import com.examplatform.admin.client.KeycloakAdminClient;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service responsible for deactivating user accounts.
 * Handles session invalidation via Redis, Keycloak account disabling,
 * and audit event publication for compliance and traceability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDeactivationService {

    private static final String SESSION_KEY_PATTERN = "session:*:user:%s";
    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final StringRedisTemplate redisTemplate;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Deactivates a user account by invalidating all active sessions,
     * disabling the account in Keycloak, and publishing an audit event.
     *
     * @param userId   the user to deactivate
     * @param actorId  the admin who initiated the deactivation
     * @param tenantId the tenant (examination authority) identifier
     */
    public void deactivateUser(UUID userId, UUID actorId, String tenantId) {
        log.info("Deactivating user {} in tenant {} by actor {}", userId, actorId, tenantId);

        // 1. Invalidate all active sessions via Redis
        invalidateUserSessions(userId, tenantId);

        // 2. Disable user in Keycloak
        keycloakAdminClient.disableUser(userId, tenantId);
        log.info("User {} disabled in Keycloak for tenant {}", userId, tenantId);

        // 3. Publish audit event
        publishAuditEvent(userId, actorId, tenantId);
        log.info("Deactivation complete for user {} in tenant {}", userId, tenantId);
    }

    private void invalidateUserSessions(UUID userId, String tenantId) {
        String pattern = String.format(SESSION_KEY_PATTERN, userId);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            Long deletedCount = redisTemplate.delete(keys);
            log.info("Invalidated {} sessions for user {} in tenant {}", deletedCount, userId, tenantId);
        } else {
            log.info("No active sessions found for user {} in tenant {}", userId, tenantId);
        }
    }

    private void publishAuditEvent(UUID userId, UUID actorId, String tenantId) {
        Map<String, Object> auditEvent = Map.of(
                "eventType", AuditEventType.ROLE_CHANGE.name(),
                "userId", userId.toString(),
                "actorId", actorId.toString(),
                "tenantId", tenantId,
                "action", "USER_DEACTIVATED",
                "timestamp", Instant.now().toString()
        );
        kafkaTemplate.send(AUDIT_TOPIC, tenantId, auditEvent);
    }
}
