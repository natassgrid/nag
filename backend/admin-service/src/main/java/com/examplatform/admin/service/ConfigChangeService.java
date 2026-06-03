package com.examplatform.admin.service;

import com.examplatform.admin.domain.SystemConfig;
import com.examplatform.admin.repository.SystemConfigRepository;
import com.examplatform.shared.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for managing system configuration changes with full audit trail.
 * Publishes audit events for every configuration change including old and new values.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigChangeService {

    private static final String AUDIT_TOPIC = "exam.audit.events";

    private final SystemConfigRepository systemConfigRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Updates a system configuration parameter and publishes an audit event
     * capturing the old value, new value, and the actor who made the change.
     *
     * @param paramName the configuration parameter name
     * @param newValue  the new value to set
     * @param actorId   the admin who initiated the change
     * @param tenantId  the tenant (examination authority) identifier
     * @return the updated SystemConfig entity
     */
    @Transactional
    public SystemConfig updateConfig(String paramName, String newValue, UUID actorId, String tenantId) {
        log.info("Updating config '{}' in tenant {} by actor {}", paramName, tenantId, actorId);

        // Load existing config to get old value
        SystemConfig config = systemConfigRepository.findByParamNameAndTenantId(paramName, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Configuration parameter '" + paramName + "' not found for tenant " + tenantId));

        String oldValue = config.getParamValue();

        // Update to new value
        config.setParamValue(newValue);
        config.setUpdatedBy(actorId);
        config.setUpdatedAtConfig(Instant.now());

        SystemConfig saved = systemConfigRepository.save(config);

        // Publish audit event
        publishConfigChangeAuditEvent(paramName, oldValue, newValue, actorId, tenantId);

        log.info("Config '{}' updated from '{}' to '{}' in tenant {}", paramName, oldValue, newValue, tenantId);
        return saved;
    }

    private void publishConfigChangeAuditEvent(String paramName, String oldValue, String newValue,
                                                UUID actorId, String tenantId) {
        Map<String, Object> auditEvent = Map.of(
                "eventType", AuditEventType.CONFIG_CHANGED.name(),
                "paramName", paramName,
                "oldValue", oldValue,
                "newValue", newValue,
                "actorId", actorId.toString(),
                "tenantId", tenantId,
                "timestamp", Instant.now().toString()
        );
        kafkaTemplate.send(AUDIT_TOPIC, tenantId, auditEvent);
    }
}
