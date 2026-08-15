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

package com.examplatform.response.repository;

import com.examplatform.response.domain.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for candidate responses.
 * Query methods leverage the composite index on (session_id, question_id, revision_sequence DESC)
 * and the partial index on candidate_id WHERE is_final=TRUE.
 */
@Repository
public interface ResponseRepository extends JpaRepository<Response, UUID> {

    /**
     * Retrieves all response revisions for a specific question within a session,
     * ordered by revision_sequence descending (latest first).
     * Uses composite index: (session_id, question_id, revision_sequence DESC).
     */
    List<Response> findBySessionIdAndQuestionIdOrderByRevisionSequenceDesc(UUID sessionId, UUID questionId);

    /**
     * Retrieves all responses for a given session within a tenant.
     */
    List<Response> findBySessionIdAndTenantId(UUID sessionId, String tenantId);

    /**
     * Retrieves all final responses for a candidate within a tenant.
     * Uses partial index: candidate_id WHERE is_final=TRUE.
     */
    List<Response> findByCandidateIdAndIsFinalTrueAndTenantId(UUID candidateId, String tenantId);
}
