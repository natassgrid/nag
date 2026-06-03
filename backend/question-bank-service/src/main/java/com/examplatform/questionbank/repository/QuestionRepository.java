package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    List<Question> findBySubjectAndStateAndTenantId(String subject, String state, String tenantId);

    List<Question> findByAuthorIdAndTenantId(UUID authorId, String tenantId);
}
