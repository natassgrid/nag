package com.examplatform.examination.repository;

import com.examplatform.examination.domain.Examination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, UUID> {

    List<Examination> findByTenantId(String tenantId);

    List<Examination> findByStatusAndTenantId(String status, String tenantId);
}
