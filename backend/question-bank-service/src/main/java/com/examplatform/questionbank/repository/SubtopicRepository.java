package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Subtopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubtopicRepository extends JpaRepository<Subtopic, UUID> {

    List<Subtopic> findByTopicIdAndTenantId(UUID topicId, String tenantId);

    Optional<Subtopic> findByNameAndTopicIdAndTenantId(String name, UUID topicId, String tenantId);

    boolean existsByNameAndTopicIdAndTenantId(String name, UUID topicId, String tenantId);
}
