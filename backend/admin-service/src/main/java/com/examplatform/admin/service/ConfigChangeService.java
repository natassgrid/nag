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
import com.examplatform.admin.dto.SystemConfigResponse;
import com.examplatform.admin.repository.SystemConfigRepository;
import com.examplatform.shared.audit.AuditEventType;
import com.examplatform.shared.config.DefaultPlatformConfigs;
import com.examplatform.shared.config.DynamicConfigInvalidationListener;
import com.examplatform.shared.config.DynamicConfigService;
import com.examplatform.shared.config.SystemConfigChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for managing system configuration changes with full audit trail
 * and multi-tier Near Cache synchronization (DB + Redis L2 + Kafka invalidation + L1 Near Cache).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigChangeService {

    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String REDIS_CONFIG_KEY_PREFIX = "nag:config:";

    public static final Map<String, String> DEFAULT_CONFIGS = DefaultPlatformConfigs.DEFAULTS;

    private final SystemConfigRepository systemConfigRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final DynamicConfigService dynamicConfigService;

    /**
     * Retrieves all system configuration parameters for a tenant.
     * If tenant configs are not yet initialized in DB, seeds them from default values.
     */
    @Transactional
    public List<SystemConfig> getConfigs(String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        List<SystemConfig> existing = systemConfigRepository.findByTenantId(effectiveTenant);
        if (existing.isEmpty()) {
            log.info("No configs found for tenant {}. Initializing standard defaults.", effectiveTenant);
            List<SystemConfig> initialized = new ArrayList<>();
            DEFAULT_CONFIGS.forEach((param, val) -> {
                SystemConfig sc = SystemConfig.builder()
                        .paramName(param)
                        .paramValue(val)
                        .updatedAtConfig(Instant.now())
                        .build();
                sc.setTenantId(effectiveTenant);
                SystemConfig saved = systemConfigRepository.save(sc);
                initialized.add(saved);
                syncToRedis(effectiveTenant, param, val);
                dynamicConfigService.updateLocalCache(effectiveTenant, param, val);
            });
            return initialized;
        }
        return existing;
    }

    /**
     * Retrieves all configurations as a key-value Map.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getConfigMap(String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        List<SystemConfig> configs = systemConfigRepository.findByTenantId(effectiveTenant);
        if (configs.isEmpty()) {
            return new LinkedHashMap<>(DEFAULT_CONFIGS);
        }
        Map<String, String> map = new LinkedHashMap<>();
        configs.forEach(c -> map.put(c.getParamName(), c.getParamValue()));
        return map;
    }

    /**
     * Updates a single system configuration parameter, syncs to Redis, and broadcasts invalidation.
     */
    @Transactional
    public SystemConfig updateConfig(String paramName, String newValue, UUID actorId, String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        log.info("Updating config '{}' in tenant {} by actor {}", paramName, effectiveTenant, actorId);

        Optional<SystemConfig> opt = systemConfigRepository.findByParamNameAndTenantId(paramName, effectiveTenant);
        SystemConfig config;
        String oldValue;

        if (opt.isPresent()) {
            config = opt.get();
            oldValue = config.getParamValue();
        } else {
            config = SystemConfig.builder()
                    .paramName(paramName)
                    .build();
            config.setTenantId(effectiveTenant);
            oldValue = DEFAULT_CONFIGS.getOrDefault(paramName, "");
        }

        config.setParamValue(newValue);
        config.setUpdatedBy(actorId);
        config.setUpdatedAtConfig(Instant.now());

        SystemConfig saved = systemConfigRepository.save(config);

        // 1. Sync to Redis L2 cache
        syncToRedis(effectiveTenant, paramName, newValue);

        // 2. Update local L1 Near Cache
        dynamicConfigService.updateLocalCache(effectiveTenant, paramName, newValue);

        // 3. Broadcast Kafka invalidation event & Audit event if changed
        if (!Objects.equals(oldValue, newValue)) {
            broadcastConfigChangeEvent(paramName, oldValue, newValue, effectiveTenant);
            publishConfigChangeAuditEvent(paramName, oldValue, newValue, actorId, effectiveTenant);
        }

        log.info("Config '{}' updated from '{}' to '{}' in tenant {}", paramName, oldValue, newValue, effectiveTenant);
        return saved;
    }

    /**
     * Bulk updates multiple system configuration parameters in a single transaction.
     */
    @Transactional
    public Map<String, String> updateBulkConfigs(Map<String, String> updates, UUID actorId, String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        log.info("Bulk updating {} configs in tenant {} by actor {}", updates.size(), effectiveTenant, actorId);

        Map<String, SystemConfig> existingMap = systemConfigRepository.findByTenantId(effectiveTenant)
                .stream()
                .collect(Collectors.toMap(SystemConfig::getParamName, c -> c));

        updates.forEach((paramName, newValue) -> {
            SystemConfig config = existingMap.get(paramName);
            String oldValue;

            if (config != null) {
                oldValue = config.getParamValue();
                config.setParamValue(newValue);
                config.setUpdatedBy(actorId);
                config.setUpdatedAtConfig(Instant.now());
                systemConfigRepository.save(config);
            } else {
                oldValue = DEFAULT_CONFIGS.getOrDefault(paramName, "");
                config = SystemConfig.builder()
                        .paramName(paramName)
                        .paramValue(newValue)
                        .updatedBy(actorId)
                        .updatedAtConfig(Instant.now())
                        .build();
                config.setTenantId(effectiveTenant);
                systemConfigRepository.save(config);
            }

            // Sync to Redis and Near Cache
            syncToRedis(effectiveTenant, paramName, newValue);
            dynamicConfigService.updateLocalCache(effectiveTenant, paramName, newValue);

            if (!Objects.equals(oldValue, newValue)) {
                broadcastConfigChangeEvent(paramName, oldValue, newValue, effectiveTenant);
                publishConfigChangeAuditEvent(paramName, oldValue, newValue, actorId, effectiveTenant);
            }
        });

        return getConfigMap(effectiveTenant);
    }

    /**
     * Resets all configurations to platform defaults for the tenant.
     */
    @Transactional
    public Map<String, String> resetToDefaults(UUID actorId, String tenantId) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        log.info("Resetting system configs to defaults for tenant {} by actor {}", effectiveTenant, actorId);
        return updateBulkConfigs(DEFAULT_CONFIGS, actorId, effectiveTenant);
    }

    public SystemConfigResponse toResponse(SystemConfig entity) {
        return new SystemConfigResponse(
                entity.getId(),
                entity.getParamName(),
                entity.getParamValue(),
                entity.getTenantId(),
                entity.getUpdatedBy(),
                entity.getUpdatedAtConfig(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void syncToRedis(String tenantId, String paramName, String value) {
        try {
            StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
            if (redis != null) {
                String redisKey = REDIS_CONFIG_KEY_PREFIX + tenantId;
                redis.opsForHash().put(redisKey, paramName, value);
            }
        } catch (Exception e) {
            log.warn("Failed to sync config '{}' to Redis for tenant {}: {}", paramName, tenantId, e.getMessage());
        }
    }

    private void broadcastConfigChangeEvent(String paramName, String oldValue, String newValue, String tenantId) {
        try {
            SystemConfigChangeEvent event = new SystemConfigChangeEvent(
                    paramName, oldValue, newValue, tenantId, Instant.now());
            kafkaTemplate.send(DynamicConfigInvalidationListener.CONFIG_EVENTS_TOPIC, tenantId, event);
        } catch (Exception ex) {
            log.warn("Failed to broadcast config invalidation event for '{}': {}", paramName, ex.getMessage());
        }
    }

    private void publishConfigChangeAuditEvent(String paramName, String oldValue, String newValue,
                                                UUID actorId, String tenantId) {
        try {
            Map<String, Object> auditEvent = Map.of(
                    "eventType", AuditEventType.CONFIG_CHANGED.name(),
                    "paramName", paramName,
                    "oldValue", oldValue != null ? oldValue : "",
                    "newValue", newValue != null ? newValue : "",
                    "actorId", actorId != null ? actorId.toString() : "SYSTEM",
                    "tenantId", tenantId,
                    "timestamp", Instant.now().toString()
            );
            kafkaTemplate.send(AUDIT_TOPIC, tenantId, auditEvent);
        } catch (Exception ex) {
            log.warn("Failed to publish audit event for config change '{}': {}", paramName, ex.getMessage());
        }
    }
}
