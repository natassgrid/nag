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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for managing system configuration changes with full audit trail.
 * Publishes audit events for every configuration change including old and new values.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigChangeService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    public static final Map<String, String> DEFAULT_CONFIGS;

    static {
        Map<String, String> m = new LinkedHashMap<>();
        // Security & Authentication
        m.put("auth.mfa.enforced", "false");
        m.put("auth.session.timeout.minutes", "30");
        m.put("auth.max.login.attempts", "5");
        m.put("auth.password.expiry.days", "90");
        m.put("auth.password.min.length", "12");
        m.put("auth.lockout.duration.minutes", "15");

        // Exam Delivery & Proctoring
        m.put("delivery.tamper.detection.enabled", "true");
        m.put("delivery.kiosk.mode.enforced", "true");
        m.put("delivery.telemetry.heartbeat.seconds", "10");
        m.put("delivery.autosave.interval.seconds", "15");
        m.put("delivery.max.disconnect.grace.seconds", "180");
        m.put("delivery.retest.authorization.required", "true");

        // Assessment & Question Bank Governance
        m.put("question.dual.review.required", "true");
        m.put("question.ai.generation.enabled", "true");
        m.put("evaluation.auto.grade.instant", "true");
        m.put("evaluation.anonymize.candidate.sheets", "true");

        // Alerts & Notification Operations
        m.put("alert.failed.login.spikes.enabled", "true");
        m.put("alert.exam.window.start.enabled", "true");
        m.put("alert.email.recipients", "sec-ops@nag.gov.in, admin@nag.gov.in");
        m.put("alert.critical.error.webhook", "");

        // Platform Infrastructure & DPI Integration
        m.put("dpi.digilocker.verification.enabled", "true");
        m.put("dpi.face.verification.threshold", "85");
        m.put("platform.maintenance.mode", "false");
        m.put("platform.banner.message", "");

        DEFAULT_CONFIGS = Collections.unmodifiableMap(m);
    }

    private final SystemConfigRepository systemConfigRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
                initialized.add(systemConfigRepository.save(sc));
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
     * Updates a single system configuration parameter and publishes an audit event.
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

        if (!Objects.equals(oldValue, newValue)) {
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

            if (!Objects.equals(oldValue, newValue)) {
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
