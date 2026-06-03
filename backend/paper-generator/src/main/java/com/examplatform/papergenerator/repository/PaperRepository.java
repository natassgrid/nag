package com.examplatform.papergenerator.repository;

import com.examplatform.papergenerator.domain.Paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaperRepository extends JpaRepository<Paper, UUID> {

    List<Paper> findByExamIdAndTenantId(UUID examId, String tenantId);

    List<Paper> findByExamIdAndShiftIdAndTenantId(UUID examId, String shiftId, String tenantId);

    List<Paper> findByStatusAndTenantId(String status, String tenantId);
}
