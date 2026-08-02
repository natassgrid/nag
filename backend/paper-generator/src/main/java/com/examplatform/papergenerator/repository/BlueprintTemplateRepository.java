package com.examplatform.papergenerator.repository;

import com.examplatform.papergenerator.domain.BlueprintTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlueprintTemplateRepository extends JpaRepository<BlueprintTemplate, UUID> {

    /** All templates for a tenant, newest first. */
    List<BlueprintTemplate> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    /** Templates pinned to a specific exam, for quick suggestions. */
    List<BlueprintTemplate> findByExamIdAndTenantIdOrderByCreatedAtDesc(UUID examId, String tenantId);

    /** Lookup by name within a tenant (name is unique per tenant). */
    Optional<BlueprintTemplate> findByNameAndTenantId(String name, String tenantId);

    /** Check existence before saving to give a friendly duplicate-name error. */
    boolean existsByNameAndTenantId(String name, String tenantId);

    /** Used when renaming: check name clash excluding the current record. */
    boolean existsByNameAndTenantIdAndIdNot(String name, String tenantId, UUID id);
}
