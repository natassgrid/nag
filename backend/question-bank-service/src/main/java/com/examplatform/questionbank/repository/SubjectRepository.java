package com.examplatform.questionbank.repository;

import com.examplatform.questionbank.domain.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {

    List<Subject> findByTenantId(String tenantId);

    Optional<Subject> findByNameAndTenantId(String name, String tenantId);

    boolean existsByNameAndTenantId(String name, String tenantId);
}
