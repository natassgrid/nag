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

import com.examplatform.examination.domain.ExaminationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExaminationScheduleRepository extends JpaRepository<ExaminationSchedule, UUID> {

    /** All schedules for an examination, ordered newest first. */
    List<ExaminationSchedule> findByExaminationIdAndTenantIdOrderByScheduleVersionDesc(
            UUID examinationId, String tenantId);

    /** Paginated schedules for an examination, ordered newest first. */
    org.springframework.data.domain.Page<ExaminationSchedule> findByExaminationIdAndTenantId(
            UUID examinationId, String tenantId, org.springframework.data.domain.Pageable pageable);

    /** Find the latest (highest version) published schedule for an examination. */
    Optional<ExaminationSchedule> findFirstByExaminationIdAndStatusAndTenantIdOrderByScheduleVersionDesc(
            UUID examinationId, String status, String tenantId);

    /**
     * Check whether any other schedule in the same tenant has an exam_date
     * that conflicts with a proposed reserve date (Req 7b.10).
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM ExaminationSchedule s
            WHERE s.tenantId = :tenantId
              AND s.examDate = :reserveDate
              AND s.id <> :excludeId
              AND s.status <> 'CANCELLED'
            """)
    boolean existsConflictingExamDate(
            @Param("tenantId") String tenantId,
            @Param("reserveDate") LocalDate reserveDate,
            @Param("excludeId") UUID excludeId);

    /**
     * Check whether any other schedule in the same tenant has a reserve_date
     * equal to a proposed exam_date (Req 7b.10).
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM ExaminationSchedule s
            WHERE s.tenantId = :tenantId
              AND s.reserveDate = :examDate
              AND s.id <> :excludeId
              AND s.status <> 'CANCELLED'
            """)
    boolean existsConflictingReserveDate(
            @Param("tenantId") String tenantId,
            @Param("examDate") LocalDate examDate,
            @Param("excludeId") UUID excludeId);
}
