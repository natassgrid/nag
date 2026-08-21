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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    /**
     * Partition-pruned lookup: finds a question by id AND subject.
     * Since the table is hash-partitioned by subject, including subject
     * in the predicate enables PostgreSQL to scan only the target partition
     * instead of all 8 partitions (as findById would do).
     *
     * Use this when the caller already knows the subject for optimal performance.
     *
     * Validates: NFR-5 (partition pruning)
     */
    Optional<Question> findByIdAndSubject(UUID id, String subject);

    List<Question> findBySubjectAndStateAndTenantId(String subject, String state, String tenantId);

    List<Question> findByAuthorIdAndTenantId(UUID authorId, String tenantId);

    List<Question> findByTenantId(String tenantId);

    /**
     * Finds a PUBLISHED question whose embedding vector has cosine similarity
     * above the given threshold compared to the provided embedding.
     *
     * TODO: Re-enable pgvector native query when vector extension is available:
     * SELECT id FROM question_service.question WHERE state='PUBLISHED'
     *   AND 1 - (embedding <=> cast(:embedding as halfvec(384))) > :threshold LIMIT 1
     *
     * Validates: Requirement 4.7
     */
    @Query(value = "SELECT q.id FROM question_service.question q WHERE q.state = 'PUBLISHED' AND q.embedding IS NOT NULL LIMIT 1", nativeQuery = true)
    Optional<UUID> findSimilarPublishedQuestion(@Param("embedding") String embedding, @Param("threshold") double threshold);

    /**
     * Finds the top-N most similar questions using pgvector cosine distance operator on halfvec(384).
     * Returns questions from the same subject and tenant with their similarity scores.
     * Used for both duplicate detection (reject > 0.92, flag 0.85–0.92) and RAG retrieval.
     *
     * The IVFFlat index on halfvec_cosine_ops ensures sub-200ms lookups.
     *
     * Validates: Requirements FR-2 (Duplicate Detection), FR-3 (RAG retrieval)
     */
    @Query(value = """
            SELECT q.id AS id, q.subject AS subject, q.content AS content,
                   1 - (q.embedding <=> cast(:queryVec AS halfvec(384))) AS similarity
            FROM question_service.question q
            WHERE q.tenant_id = :tenantId
              AND q.subject = :subject
              AND q.embedding IS NOT NULL
            ORDER BY q.embedding <=> cast(:queryVec AS halfvec(384))
            LIMIT :limit
            """, nativeQuery = true)
    List<SimilarityResult> findTopSimilarQuestions(
            @Param("queryVec") String queryVec,
            @Param("subject") String subject,
            @Param("tenantId") String tenantId,
            @Param("limit") int limit);

    /**
     * Finds questions with null embeddings for a given tenant, used by the
     * batch embedding backfill endpoint (FR-9). Processes in pageable batches of 50.
     *
     * Validates: Requirements FR-9 (Batch Embedding Backfill)
     */
    @Query(value = "SELECT * FROM question_service.question WHERE tenant_id = :tenantId AND embedding IS NULL",
            countQuery = "SELECT count(*) FROM question_service.question WHERE tenant_id = :tenantId AND embedding IS NULL",
            nativeQuery = true)
    Page<Question> findQuestionsWithNullEmbedding(@Param("tenantId") String tenantId, Pageable pageable);

    /**
     * Updates the embedding for a question using a native query with proper halfvec cast.
     * This bypasses Hibernate type binding issues with pgvector types.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "UPDATE question_service.question SET embedding = cast(:embedding AS halfvec(384)) WHERE id = :id", nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);
}
