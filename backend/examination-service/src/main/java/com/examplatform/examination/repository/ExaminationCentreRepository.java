package com.examplatform.examination.repository;

import com.examplatform.examination.domain.ExaminationCentre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExaminationCentreRepository extends JpaRepository<ExaminationCentre, UUID> {

    List<ExaminationCentre> findByTenantIdAndActiveTrue(String tenantId);

    Page<ExaminationCentre> findByTenantIdAndActiveTrue(String tenantId, Pageable pageable);

    Page<ExaminationCentre> findByTenantIdAndCentreNameContainingIgnoreCaseAndActiveTrue(
            String tenantId, String centreName, Pageable pageable);

    List<ExaminationCentre> findByTenantIdAndStateIgnoreCaseAndActiveTrue(String tenantId, String state);

    List<ExaminationCentre> findByTenantIdAndCityIgnoreCaseAndActiveTrue(String tenantId, String city);
}
