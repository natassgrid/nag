package com.examplatform.examination.repository;

import com.examplatform.examination.domain.ExaminationCentre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ExaminationCentreRepository extends JpaRepository<ExaminationCentre, UUID> {

    List<ExaminationCentre> findByTenantIdAndActiveTrue(String tenantId);

    List<ExaminationCentre> findByTenantIdAndStateIgnoreCaseAndActiveTrue(String tenantId, String state);

    List<ExaminationCentre> findByTenantIdAndCityIgnoreCaseAndActiveTrue(String tenantId, String city);
}
