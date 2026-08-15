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

package com.examplatform.result.repository;

import com.examplatform.result.domain.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for results.
 */
@Repository
public interface ResultRepository extends JpaRepository<Result, UUID> {

    /**
     * Retrieves all results for a given candidate within a tenant.
     */
    List<Result> findByCandidateIdAndTenantId(UUID candidateId, String tenantId);

    /**
     * Retrieves a specific result for a candidate in an exam within a tenant.
     */
    Optional<Result> findByCandidateIdAndExamIdAndTenantId(UUID candidateId, UUID examId, String tenantId);

    /**
     * Retrieves all results for an exam within a tenant, ordered by rank.
     */
    List<Result> findByExamIdAndTenantIdOrderByOverallRankAsc(UUID examId, String tenantId);
}
