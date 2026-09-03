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

import com.examplatform.admin.domain.SystemConfig;
import com.examplatform.admin.repository.SystemConfigRepository;
import com.examplatform.shared.config.DynamicConfigInvalidationListener;
import com.examplatform.shared.config.DynamicConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigChangeServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Mock
    private DynamicConfigService dynamicConfigService;

    private ConfigChangeService configChangeService;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID ACTOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        configChangeService = new ConfigChangeService(
                systemConfigRepository, kafkaTemplate, redisTemplateProvider, dynamicConfigService);
    }

    @Test
    @DisplayName("getConfigs initializes defaults when repository is empty")
    void getConfigs_initializesDefaultsWhenEmpty() {
        when(systemConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        List<SystemConfig> configs = configChangeService.getConfigs(TENANT_ID);

        assertThat(configs).isNotEmpty();
        assertThat(configs.size()).isEqualTo(ConfigChangeService.DEFAULT_CONFIGS.size());
        verify(systemConfigRepository, atLeastOnce()).save(any(SystemConfig.class));
    }

    @Test
    @DisplayName("updateConfig updates existing param and publishes audit event and broadcast event")
    void updateConfig_updatesExistingAndPublishesAudit() {
        SystemConfig existing = SystemConfig.builder()
                .paramName("auth.session.timeout.minutes")
                .paramValue("30")
                .updatedAtConfig(Instant.now())
                .build();
        existing.setTenantId(TENANT_ID);

        when(systemConfigRepository.findByParamNameAndTenantId("auth.session.timeout.minutes", TENANT_ID))
                .thenReturn(Optional.of(existing));
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        SystemConfig updated = configChangeService.updateConfig("auth.session.timeout.minutes", "45", ACTOR_ID, TENANT_ID);

        assertThat(updated.getParamValue()).isEqualTo("45");
        assertThat(updated.getUpdatedBy()).isEqualTo(ACTOR_ID);

        // Verify Kafka audit event
        ArgumentCaptor<Map<String, Object>> auditCaptor = ArgumentCaptor.forClass(Map.class);
        verify(kafkaTemplate).send(eq("exam.audit.events"), eq(TENANT_ID), auditCaptor.capture());

        Map<String, Object> audit = auditCaptor.getValue();
        assertThat(audit.get("paramName")).isEqualTo("auth.session.timeout.minutes");
        assertThat(audit.get("oldValue")).isEqualTo("30");
        assertThat(audit.get("newValue")).isEqualTo("45");

        // Verify Kafka broadcast invalidation event
        verify(kafkaTemplate).send(eq(DynamicConfigInvalidationListener.CONFIG_EVENTS_TOPIC), eq(TENANT_ID), any());

        // Verify Near Cache local update
        verify(dynamicConfigService).updateLocalCache(TENANT_ID, "auth.session.timeout.minutes", "45");
    }

    @Test
    @DisplayName("updateBulkConfigs updates multiple parameters")
    void updateBulkConfigs_updatesMultipleParameters() {
        when(systemConfigRepository.findByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());
        when(systemConfigRepository.save(any(SystemConfig.class))).thenAnswer(i -> i.getArgument(0));

        Map<String, String> updates = Map.of(
                "auth.mfa.enforced", "false",
                "delivery.tamper.detection.enabled", "true"
        );

        Map<String, String> result = configChangeService.updateBulkConfigs(updates, ACTOR_ID, TENANT_ID);
        assertThat(result).isNotNull();
    }
}
