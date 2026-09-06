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

import com.examplatform.papergenerator.domain.Paper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaperRepository extends JpaRepository<Paper, UUID> {

    List<Paper> findByExamIdAndTenantId(UUID examId, String tenantId);

    List<Paper> findByExamIdAndShiftIdAndTenantId(UUID examId, String shiftId, String tenantId);

    List<Paper> findByStatusAndTenantId(String status, String tenantId);

    Optional<Paper> findByIdAndTenantId(UUID id, String tenantId);

    @Query("""
        SELECT p FROM Paper p
        WHERE p.tenantId = :tenantId
          AND (:examId IS NULL OR p.examId = :examId)
          AND (:status IS NULL OR p.status = :status)
        ORDER BY p.createdAt DESC
    """)
    Page<Paper> findPapers(
            @Param("tenantId") String tenantId,
            @Param("examId") UUID examId,
            @Param("status") String status,
            Pageable pageable);
}
