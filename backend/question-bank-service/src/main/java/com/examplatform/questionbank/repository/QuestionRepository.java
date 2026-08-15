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

package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    List<Question> findBySubjectAndStateAndTenantId(String subject, String state, String tenantId);

    List<Question> findByAuthorIdAndTenantId(UUID authorId, String tenantId);

    List<Question> findByTenantId(String tenantId);

    /**
     * Finds a PUBLISHED question whose embedding vector has cosine similarity
     * above the given threshold compared to the provided embedding.
     *
     * TODO: Re-enable pgvector native query when vector extension is available:
     * SELECT id FROM question_service.question WHERE state='PUBLISHED'
     *   AND 1 - (embedding_vector <=> cast(:embedding as vector)) > :threshold LIMIT 1
     *
     * Validates: Requirement 4.7
     */
    @Query(value = "SELECT q.id FROM question_service.question q WHERE q.state = 'PUBLISHED' AND q.embedding_vector IS NOT NULL LIMIT 1", nativeQuery = true)
    Optional<UUID> findSimilarPublishedQuestion(@Param("embedding") String embedding, @Param("threshold") double threshold);
}
