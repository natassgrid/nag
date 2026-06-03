package com.examplatform.admin.domain;

import com.examplatform.shared.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a platform-wide or tenant-specific system configuration parameter.
 * Managed exclusively by SUPER_ADMIN and SECURITY_ADMIN roles.
 */
@Entity
@Table(name = "system_config", schema = "admin_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfig extends BaseEntity {

    @Column(name = "param_name", nullable = false, length = 255)
    private String paramName;

    @Column(name = "param_value", nullable = false, columnDefinition = "TEXT")
    private String paramValue;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at_config", nullable = false)
    private Instant updatedAtConfig;
}
