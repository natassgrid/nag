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

package com.examplatform.examination.repository;

import com.examplatform.examination.domain.ExaminationCentre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExaminationCentreRepository extends JpaRepository<ExaminationCentre, UUID> {

    List<ExaminationCentre> findByTenantIdAndActiveTrue(String tenantId);

    Page<ExaminationCentre> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);

    Page<ExaminationCentre> findByTenantIdAndCentreNameContainingIgnoreCaseAndActiveTrue(
            String tenantId, String centreName, Pageable pageable);

    List<ExaminationCentre> findByTenantIdAndStateIgnoreCaseAndActiveTrue(String tenantId, String state);

    List<ExaminationCentre> findByTenantIdAndCityIgnoreCaseAndActiveTrue(String tenantId, String city);
}
