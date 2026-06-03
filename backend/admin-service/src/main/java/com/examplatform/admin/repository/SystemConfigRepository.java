package com.examplatform.admin.repository;

import com.examplatform.admin.domain.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for system configuration parameters.
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID> {

    /**
     * Retrieves a config parameter by name within a tenant.
     */
    Optional<SystemConfig> findByParamNameAndTenantId(String paramName, String tenantId);

    /**
     * Retrieves all config parameters for a tenant.
     */
    List<SystemConfig> findByTenantId(String tenantId);
}
