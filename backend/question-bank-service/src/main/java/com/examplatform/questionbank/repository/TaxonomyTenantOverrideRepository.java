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

package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.TaxonomyTenantOverride;
import com.examplatform.shared.taxonomy.TaxonomyNodeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaxonomyTenantOverrideRepository extends JpaRepository<TaxonomyTenantOverride, Long> {

    List<TaxonomyTenantOverride> findByTenantId(String tenantId);

    Optional<TaxonomyTenantOverride> findByTenantIdAndNodeTypeAndNodeId(String tenantId, TaxonomyNodeType nodeType, Long nodeId);

    List<TaxonomyTenantOverride> findByTenantIdAndNodeTypeAndNodeIdIn(String tenantId, TaxonomyNodeType nodeType, Collection<Long> nodeIds);

    List<TaxonomyTenantOverride> findByNodeTypeAndNodeId(TaxonomyNodeType nodeType, Long nodeId);

    void deleteByTenantIdAndNodeTypeAndNodeId(String tenantId, TaxonomyNodeType nodeType, Long nodeId);
}
