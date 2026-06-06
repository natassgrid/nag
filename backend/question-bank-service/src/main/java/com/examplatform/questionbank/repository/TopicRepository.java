package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findBySubjectIdAndTenantId(UUID subjectId, String tenantId);

    Optional<Topic> findByNameAndSubjectIdAndTenantId(String name, UUID subjectId, String tenantId);

    boolean existsByNameAndSubjectIdAndTenantId(String name, UUID subjectId, String tenantId);
}
