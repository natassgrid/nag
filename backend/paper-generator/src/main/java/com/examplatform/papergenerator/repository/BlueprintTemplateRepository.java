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

package com.examplatform.papergenerator.repository;

import com.examplatform.papergenerator.domain.BlueprintTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlueprintTemplateRepository extends JpaRepository<BlueprintTemplate, UUID> {

    /** All templates for a tenant, newest first. */
    List<BlueprintTemplate> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    /** Templates pinned strictly to a specific exam. */
    List<BlueprintTemplate> findByExamIdAndTenantIdOrderByCreatedAtDesc(UUID examId, String tenantId);

    /**
     * Templates mapped to a specific exam PLUS universal templates (examId is null),
     * with mapped rules listed first followed by universal rules.
     */
    @Query("SELECT b FROM BlueprintTemplate b WHERE b.tenantId = :tenantId AND (b.examId = :examId OR b.examId IS NULL) ORDER BY CASE WHEN b.examId = :examId THEN 0 ELSE 1 END, b.createdAt DESC")
    List<BlueprintTemplate> findByExamIdOrUniversal(@Param("examId") UUID examId, @Param("tenantId") String tenantId);

    /** Lookup by name within a tenant (name is unique per tenant). */
    Optional<BlueprintTemplate> findByNameAndTenantId(String name, String tenantId);

    /** Check existence before saving to give a friendly duplicate-name error. */
    boolean existsByNameAndTenantId(String name, String tenantId);

    /** Used when renaming: check name clash excluding the current record. */
    boolean existsByNameAndTenantIdAndIdNot(String name, String tenantId, UUID id);
}
