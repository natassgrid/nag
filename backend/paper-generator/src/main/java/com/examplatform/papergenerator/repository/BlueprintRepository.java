/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

import com.examplatform.papergenerator.domain.Blueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BlueprintRepository extends JpaRepository<Blueprint, UUID> {

    @Query("SELECT b FROM Blueprint b WHERE b.tenantId = :tenantId OR b.tenantId IS NULL ORDER BY b.createdAt DESC")
    List<Blueprint> findVisibleBlueprints(@Param("tenantId") String tenantId);

    @Query("SELECT b FROM Blueprint b WHERE b.id = :id AND (b.tenantId = :tenantId OR b.tenantId IS NULL)")
    Optional<Blueprint> findVisibleById(@Param("id") UUID id, @Param("tenantId") String tenantId);

    boolean existsByNameAndTenantId(String name, String tenantId);

    boolean existsByNameAndTenantIdAndIdNot(String name, String tenantId, UUID id);

    Optional<Blueprint> findByCode(String code);
}
