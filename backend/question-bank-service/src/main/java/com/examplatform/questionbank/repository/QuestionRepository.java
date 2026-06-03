package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findBySubjectAndStateAndTenantId(String subject, String state, String tenantId);

    List<Question> findByAuthorIdAndTenantId(UUID authorId, String tenantId);

    /**
     * Finds a PUBLISHED question whose embedding vector has cosine similarity
     * above the given threshold compared to the provided embedding.
     * Uses pgvector's cosine distance operator (<=>).
     *
     * Validates: Requirement 4.7
     */
    @Query(value = "SELECT id FROM question_service.question WHERE state='PUBLISHED' AND 1 - (embedding_vector <=> cast(:embedding as vector)) > :threshold LIMIT 1", nativeQuery = true)
    Optional<UUID> findSimilarPublishedQuestion(@Param("embedding") String embedding, @Param("threshold") double threshold);
}
