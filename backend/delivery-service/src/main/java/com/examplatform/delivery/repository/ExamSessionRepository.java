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

package com.examplatform.delivery.repository;

import com.examplatform.delivery.domain.ExamSession;
import com.examplatform.delivery.domain.ExamSession.ExamSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * JPA repository for {@link ExamSession} entities.
 * Provides tenant-scoped queries for session management and HPA metrics.
 */
@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {

    /**
     * Find all sessions for a specific candidate within a tenant.
     */
    List<ExamSession> findByCandidateIdAndTenantId(UUID candidateId, String tenantId);

    /**
     * Find all sessions with the given status within a tenant.
     */
    List<ExamSession> findByStatusAndTenantId(ExamSessionStatus status, String tenantId);

    /**
     * Find all sessions with the given status across all tenants.
     * Used by SessionTimerService for expiration checks.
     */
    List<ExamSession> findByStatus(ExamSessionStatus status);

    /**
     * Find a session by its unique session ID.
     */
    java.util.Optional<ExamSession> findBySessionId(UUID sessionId);

    /**
     * Count sessions with a given status within a tenant.
     * Used for HPA metrics (active_exam_sessions gauge).
     */
    long countByStatusAndTenantId(ExamSessionStatus status, String tenantId);

    /**
     * Count all sessions with the given status across all tenants.
     * Used for the HPA custom metric gauge.
     */
    long countByStatus(ExamSessionStatus status);
}
