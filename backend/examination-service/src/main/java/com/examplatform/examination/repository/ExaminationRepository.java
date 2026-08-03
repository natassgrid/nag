package com.examplatform.examination.repository;

import com.examplatform.examination.domain.Examination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExaminationRepository extends JpaRepository<Examination, UUID> {

    List<Examination> findByTenantId(String tenantId);

    Page<Examination> findByTenantId(String tenantId, Pageable pageable);

    Page<Examination> findByTenantIdAndNameContainingIgnoreCase(String tenantId, String name, Pageable pageable);

    List<Examination> findByStatusAndTenantId(String status, String tenantId);
}
