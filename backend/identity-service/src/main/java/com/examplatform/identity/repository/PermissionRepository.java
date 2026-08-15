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

package com.examplatform.identity.repository;

import com.examplatform.identity.domain.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    @Query("SELECT p FROM Permission p WHERE p.tenantId = :tenantId " +
           "AND (:search IS NULL OR :search = '' " +
           "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(p.module) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Permission> findByTenantIdAndSearch(
            @Param("tenantId") String tenantId,
            @Param("search") String search,
            Pageable pageable);

    List<Permission> findByTenantId(String tenantId);

    Optional<Permission> findByIdAndTenantId(UUID id, String tenantId);

    boolean existsByCodeAndTenantId(String code, String tenantId);

    List<Permission> findByIdInAndTenantId(Set<UUID> ids, String tenantId);
}
