package com.examplatform.examination.repository;

import com.examplatform.examination.domain.ExamShift;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExamShiftRepository extends JpaRepository<ExamShift, UUID> {

    /** All shifts for a schedule, ordered by shift number. */
    List<ExamShift> findByScheduleIdOrderByShiftNumber(UUID scheduleId);

    /** Check for time-window overlap with any other shift in the same schedule (Req 7b.3.f). */
    @Query("""
            SELECT COUNT(s) > 0 FROM ExamShift s
            WHERE s.scheduleId = :scheduleId
              AND s.id <> :excludeId
              AND s.examStartTime < :newEnd
              AND s.examEndTime   > :newStart
            """)
    boolean existsOverlappingShift(
            @Param("scheduleId") UUID scheduleId,
            @Param("excludeId") UUID excludeId,
            @Param("newStart") LocalTime newStart,
            @Param("newEnd") LocalTime newEnd);

    /**
     * Find all shifts for a specific centre across schedules on the same exam_date
     * to detect cross-examination centre conflicts (Req 7b.11).
     */
    @Query("""
            SELECT sh FROM ExamShift sh
            JOIN ExaminationSchedule sc ON sc.id = sh.scheduleId
            WHERE sc.tenantId   = :tenantId
              AND sc.examDate   = (SELECT sc2.examDate FROM ExaminationSchedule sc2 WHERE sc2.id = :scheduleId)
              AND sc.status    <> 'CANCELLED'
              AND sh.id        <> :excludeShiftId
            """)
    List<ExamShift> findShiftsOnSameDateForTenant(
            @Param("tenantId") String tenantId,
            @Param("scheduleId") UUID scheduleId,
            @Param("excludeShiftId") UUID excludeShiftId);
}
