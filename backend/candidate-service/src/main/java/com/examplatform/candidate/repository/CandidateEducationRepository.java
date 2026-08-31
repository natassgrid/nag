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

package com.examplatform.candidate.repository;

import com.examplatform.candidate.domain.CandidateEducation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateEducationRepository extends JpaRepository<CandidateEducation, UUID> {

    List<CandidateEducation> findByUserIdAndTenantIdOrderByPassingYearAsc(UUID userId, String tenantId);

    Optional<CandidateEducation> findByIdAndUserIdAndTenantId(UUID id, UUID userId, String tenantId);

    void deleteByUserIdAndTenantId(UUID userId, String tenantId);

    void deleteByIdAndUserIdAndTenantId(UUID id, UUID userId, String tenantId);
}
