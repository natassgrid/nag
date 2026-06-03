package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.QuestionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QuestionVersionRepository extends JpaRepository<QuestionVersion, UUID> {

    List<QuestionVersion> findByQuestionIdOrderByVersionNumberDesc(UUID questionId);

    Optional<QuestionVersion> findTopByQuestionIdOrderByVersionNumberDesc(UUID questionId);
}
