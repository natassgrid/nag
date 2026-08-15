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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserDeactivationService}.
 * Validates that deactivation invalidates sessions and publishes audit events.
 */
@ExtendWith(MockitoExtension.class)
class UserDeactivationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private KeycloakAdminClient keycloakAdminClient;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private UserDeactivationService userDeactivationService;

    @Captor
    private ArgumentCaptor<Map<String, Object>> auditEventCaptor;

    private UUID userId;
    private UUID actorId;
    private String tenantId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        tenantId = "tenant-001";
    }

    @Test
    void deactivateUser_invalidatesSessionsAndPublishesAuditEvent() {
        // Given active sessions exist in Redis
        String pattern = String.format("session:*:user:%s", userId);
        Set<String> sessionKeys = Set.of(
                "session:abc:user:" + userId,
                "session:def:user:" + userId
        );
        when(redisTemplate.keys(pattern)).thenReturn(sessionKeys);
        when(redisTemplate.delete(sessionKeys)).thenReturn(2L);

        // When
        userDeactivationService.deactivateUser(userId, actorId, tenantId);

        // Then: sessions are invalidated
        verify(redisTemplate).keys(pattern);
        verify(redisTemplate).delete(sessionKeys);

        // Then: Keycloak is called to disable the user
        verify(keycloakAdminClient).disableUser(userId, tenantId);

        // Then: audit event is published
        verify(kafkaTemplate).send(eq("exam.audit.events"), eq(tenantId), auditEventCaptor.capture());
        Map<String, Object> auditEvent = auditEventCaptor.getValue();
        assertThat(auditEvent.get("eventType")).isEqualTo("ROLE_CHANGE");
        assertThat(auditEvent.get("userId")).isEqualTo(userId.toString());
        assertThat(auditEvent.get("actorId")).isEqualTo(actorId.toString());
        assertThat(auditEvent.get("tenantId")).isEqualTo(tenantId);
        assertThat(auditEvent.get("action")).isEqualTo("USER_DEACTIVATED");
        assertThat(auditEvent.get("timestamp")).isNotNull();
    }

    @Test
    void deactivateUser_withNoActiveSessions_stillDisablesAndPublishes() {
        // Given no active sessions
        String pattern = String.format("session:*:user:%s", userId);
        when(redisTemplate.keys(pattern)).thenReturn(Set.of());

        // When
        userDeactivationService.deactivateUser(userId, actorId, tenantId);

        // Then: Keycloak is still called
        verify(keycloakAdminClient).disableUser(userId, tenantId);

        // Then: audit event is still published
        verify(kafkaTemplate).send(eq("exam.audit.events"), eq(tenantId), any());
    }
}
