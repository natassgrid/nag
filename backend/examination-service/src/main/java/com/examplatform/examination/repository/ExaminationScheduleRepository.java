package com.examplatform.examination.repository;

import com.examplatform.examination.domain.ExaminationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExaminationScheduleRepository extends JpaRepository<ExaminationSchedule, UUID> {

    /** All schedules for an examination, ordered newest first. */
    List<ExaminationSchedule> findByExaminationIdAndTenantIdOrderByScheduleVersionDesc(
            UUID examinationId, String tenantId);

    /** Find the latest (highest version) published schedule for an examination. */
    Optional<ExaminationSchedule> findFirstByExaminationIdAndStatusAndTenantIdOrderByScheduleVersionDesc(
            UUID examinationId, String status, String tenantId);

    /**
     * Check whether any other schedule in the same tenant has an exam_date
     * that conflicts with a proposed reserve date (Req 7b.10).
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM ExaminationSchedule s
            WHERE s.tenantId = :tenantId
              AND s.examDate = :reserveDate
              AND s.id <> :excludeId
              AND s.status <> 'CANCELLED'
            """)
    boolean existsConflictingExamDate(
            @Param("tenantId") String tenantId,
            @Param("reserveDate") LocalDate reserveDate,
            @Param("excludeId") UUID excludeId);

    /**
     * Check whether any other schedule in the same tenant has a reserve_date
     * equal to a proposed exam_date (Req 7b.10).
     */
    @Query("""
            SELECT COUNT(s) > 0 FROM ExaminationSchedule s
            WHERE s.tenantId = :tenantId
              AND s.reserveDate = :examDate
              AND s.id <> :excludeId
              AND s.status <> 'CANCELLED'
            """)
    boolean existsConflictingReserveDate(
            @Param("tenantId") String tenantId,
            @Param("examDate") LocalDate examDate,
            @Param("excludeId") UUID excludeId);
}
