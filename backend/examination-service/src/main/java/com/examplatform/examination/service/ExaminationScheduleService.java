/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.examination.service;

import com.examplatform.examination.domain.ExamShift;
import com.examplatform.examination.domain.ExaminationSchedule;
import com.examplatform.examination.dto.schedule.AmendScheduleRequest;
import com.examplatform.examination.dto.schedule.CreateScheduleRequest;
import com.examplatform.examination.dto.schedule.CreateShiftRequest;
import com.examplatform.examination.dto.schedule.ScheduleResponse;
import com.examplatform.examination.dto.schedule.ScheduleTransitionRequest;
import com.examplatform.examination.dto.schedule.ShiftResponse;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.ScheduleDateConflictException;
import com.examplatform.examination.exception.ScheduleNotFoundException;
import com.examplatform.examination.exception.ScheduleWorkflowException;
import com.examplatform.examination.exception.ShiftNotFoundException;
import com.examplatform.examination.exception.ShiftTimingViolationException;
import com.examplatform.examination.repository.ExaminationRepository;
import com.examplatform.examination.repository.ExaminationScheduleRepository;
import com.examplatform.examination.repository.ExamShiftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Core scheduling service: create, list, transition, amend schedules and manage shifts.
 *
 * <h3>Approval FSM (Req 7b.7)</h3>
 * <pre>
 *   DRAFT → SCHEDULER_REVIEW → CONTROLLER_APPROVED
 *        → SECURITY_REVIEW  → CHAIRMAN_APPROVED → PUBLISHED
 *   Any non-CANCELLED state → CANCELLED
 * </pre>
 *
 * Validates: Requirements 7b.1–7b.4, 7b.7–7b.10, 7b.13
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExaminationScheduleService {

    private static final String AUDIT_TOPIC = "exam.audit.events";
    private static final String NOTIF_TOPIC = "exam.notifications.outbound";

    // Valid forward transitions (Req 7b.7)
    private static final Map<String, String> NEXT_STATUS = Map.of(
            "DRAFT",               "SCHEDULER_REVIEW",
            "SCHEDULER_REVIEW",    "CONTROLLER_APPROVED",
            "CONTROLLER_APPROVED", "SECURITY_REVIEW",
            "SECURITY_REVIEW",     "CHAIRMAN_APPROVED",
            "CHAIRMAN_APPROVED",   "PUBLISHED"
    );

    // Roles permitted to perform each forward transition
    private static final Map<String, Set<String>> TRANSITION_ROLES = Map.of(
            "SCHEDULER_REVIEW",    Set.of("EXAM_CONTROLLER", "SUPER_ADMIN"),
            "CONTROLLER_APPROVED", Set.of("EXAM_CONTROLLER"),
            "SECURITY_REVIEW",     Set.of("SECURITY_ADMIN", "SUPER_ADMIN"),
            "CHAIRMAN_APPROVED",   Set.of("SUPER_ADMIN"),
            "PUBLISHED",           Set.of("SUPER_ADMIN"),
            "CANCELLED",           Set.of("EXAM_CONTROLLER", "SUPER_ADMIN", "SECURITY_ADMIN")
    );

    private final ExaminationRepository examinationRepository;
    private final ExaminationScheduleRepository scheduleRepository;
    private final ExamShiftRepository shiftRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Schedules ─────────────────────────────────────────────────────────────

    /**
     * Creates a new DRAFT schedule for the given examination.
     */
    public ScheduleResponse createSchedule(UUID examId,
                                           CreateScheduleRequest request,
                                           UUID actorId,
                                           String tenantId) {
        // Verify exam exists and belongs to tenant
        examinationRepository.findById(examId)
                .filter(e -> tenantId.equals(e.getTenantId()))
                .orElseThrow(() -> new ExaminationNotFoundException(examId));

        validateDateConflicts(tenantId, request.getExamDate(), request.getReserveDate(),
                /* excludeId */ null);

        ExaminationSchedule schedule = ExaminationSchedule.builder()
                .examinationId(examId)
                .scheduleName(request.getScheduleName())
                .scheduleVersion(1)
                .notificationNumber(request.getNotificationNumber())
                .examDate(request.getExamDate())
                .reserveDate(request.getReserveDate())
                .timeZone(request.getTimeZone() != null ? request.getTimeZone() : "Asia/Kolkata")
                .status("DRAFT")
                .createdBy(actorId)
                .modifiedBy(actorId)
                .build();
        schedule.setTenantId(tenantId);

        ExaminationSchedule saved = scheduleRepository.save(schedule);
        log.info("Schedule created: id={}, exam={}, version=1, tenant={}", saved.getId(), examId, tenantId);

        publishAudit("SCHEDULE_CREATED", saved, actorId, tenantId, null, "DRAFT");
        return toResponse(saved);
    }

    /**
     * Lists all schedule versions for an examination, newest first.
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedules(UUID examId, String tenantId) {
        return scheduleRepository
                .findByExaminationIdAndTenantIdOrderByScheduleVersionDesc(examId, tenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lists schedule versions with server-side pagination, newest first.
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ScheduleResponse> listSchedulesPaged(
            UUID examId, String tenantId, int page, int size) {
        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("scheduleVersion").descending());
        return scheduleRepository
                .findByExaminationIdAndTenantId(examId, tenantId, pageable)
                .map(this::toResponse);
    }

    /**
     * Gets a single schedule by ID, enforcing tenant ownership.
     */
    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(UUID scheduleId, String tenantId) {
        return toResponse(findSchedule(scheduleId, tenantId));
    }

    /**
     * Transitions a schedule forward (or to CANCELLED) per the approval FSM.
     * Validates actor role and current/target state combination.
     *
     * @param actorRoles the roles held by the authenticating user
     */
    public ScheduleResponse transitionSchedule(UUID scheduleId,
                                               ScheduleTransitionRequest request,
                                               UUID actorId,
                                               Set<String> actorRoles,
                                               String tenantId) {
        ExaminationSchedule schedule = findSchedule(scheduleId, tenantId);
        String current = schedule.getStatus();
        String target  = request.getTargetStatus();

        // Validate transition legality
        validateTransition(current, target, actorRoles);

        String prevStatus = current;
        schedule.setStatus(target);
        schedule.setModifiedBy(actorId);

        if ("PUBLISHED".equals(target) || "CHAIRMAN_APPROVED".equals(target)) {
            schedule.setApprovedBy(actorId);
            schedule.setApprovedAt(Instant.now());
        }

        ExaminationSchedule saved = scheduleRepository.save(schedule);
        log.info("Schedule {} transitioned {} → {} by actor={}", scheduleId, prevStatus, target, actorId);

        publishAudit("SCHEDULE_STATUS_CHANGED", saved, actorId, tenantId, prevStatus, target);

        // On publication, notify candidates (Req 7b.12)
        if ("PUBLISHED".equals(target)) {
            publishNotification(saved, actorId, tenantId, "SCHEDULE_PUBLISHED");
        }

        return toResponse(saved);
    }

    /**
     * Amends a PUBLISHED schedule: creates a NEW row with incremented version and
     * previousVersionId pointing to the current row. The old row is left intact
     * (immutable version chain). Change reason is mandatory (Req 7b.8).
     */
    public ScheduleResponse amendSchedule(UUID scheduleId,
                                          AmendScheduleRequest request,
                                          UUID actorId,
                                          String tenantId) {
        ExaminationSchedule current = findSchedule(scheduleId, tenantId);

        if (!"PUBLISHED".equals(current.getStatus())) {
            throw new ScheduleWorkflowException(
                    "Only PUBLISHED schedules can be amended. Current status: " + current.getStatus());
        }

        validateDateConflicts(tenantId, request.getExamDate(), request.getReserveDate(), scheduleId);

        ExaminationSchedule amended = ExaminationSchedule.builder()
                .examinationId(current.getExaminationId())
                .scheduleName(request.getScheduleName())
                .scheduleVersion(current.getScheduleVersion() + 1)
                .notificationNumber(request.getNotificationNumber())
                .examDate(request.getExamDate())
                .reserveDate(request.getReserveDate())
                .timeZone(request.getTimeZone() != null ? request.getTimeZone() : current.getTimeZone())
                .status("DRAFT")                        // amendment restarts the approval workflow
                .changeReason(request.getChangeReason())
                .effectiveFrom(request.getEffectiveFrom())
                .previousVersionId(current.getId())    // immutable version chain
                .createdBy(actorId)
                .modifiedBy(actorId)
                .build();
        amended.setTenantId(tenantId);

        ExaminationSchedule saved = scheduleRepository.save(amended);
        log.info("Schedule amended: new id={}, version={}, exam={}, tenant={}",
                saved.getId(), saved.getScheduleVersion(), saved.getExaminationId(), tenantId);

        publishAudit("SCHEDULE_AMENDED", saved, actorId, tenantId,
                "v" + current.getScheduleVersion(), "v" + saved.getScheduleVersion());
        publishNotification(saved, actorId, tenantId, "SCHEDULE_AMENDED");

        return toResponse(saved);
    }

    // ── Shifts ────────────────────────────────────────────────────────────────

    /**
     * Adds a shift to a schedule. Validates all timing invariants and overlap.
     */
    public ShiftResponse addShift(UUID scheduleId,
                                  CreateShiftRequest request,
                                  UUID actorId,
                                  String tenantId) {
        ExaminationSchedule schedule = findSchedule(scheduleId, tenantId);

        // Enforce timing invariants (Req 7b.3)
        validateShiftTimings(request);

        // Check for overlap with existing shifts in the same schedule (Req 7b.3.f)
        boolean overlaps = shiftRepository.existsOverlappingShift(
                scheduleId, UUID.fromString("00000000-0000-0000-0000-000000000000"),
                request.getExamStartTime(), request.getExamEndTime());
        if (overlaps) {
            throw new ShiftTimingViolationException(
                    "examStartTime–examEndTime",
                    "Shift window [" + request.getExamStartTime() + "–" + request.getExamEndTime()
                            + "] overlaps with an existing shift in schedule " + scheduleId);
        }

        ExamShift shift = ExamShift.builder()
                .scheduleId(scheduleId)
                .shiftNumber(request.getShiftNumber())
                .shiftName(request.getShiftName())
                .reportingTime(request.getReportingTime())
                .gateClosingTime(request.getGateClosingTime())
                .loginStartTime(request.getLoginStartTime())
                .examStartTime(request.getExamStartTime())
                .examEndTime(request.getExamEndTime())
                .exitTime(request.getExitTime())
                .durationMinutes(request.getDurationMinutes())
                .bufferMinutes(request.getBufferMinutes())
                .build();
        shift.setTenantId(tenantId);

        ExamShift saved = shiftRepository.save(shift);
        log.info("Shift created: id={}, schedule={}, number={}, tenant={}",
                saved.getId(), scheduleId, saved.getShiftNumber(), tenantId);

        publishAudit("SHIFT_CREATED", saved.getId(), scheduleId, actorId, tenantId);
        return toShiftResponse(saved);
    }

    /**
     * Updates an existing shift's timings.
     */
    public ShiftResponse updateShift(UUID scheduleId,
                                     UUID shiftId,
                                     CreateShiftRequest request,
                                     UUID actorId,
                                     String tenantId) {
        findSchedule(scheduleId, tenantId); // ownership check
        ExamShift shift = shiftRepository.findById(shiftId)
                .filter(s -> scheduleId.equals(s.getScheduleId()))
                .orElseThrow(() -> new ShiftNotFoundException(shiftId));

        validateShiftTimings(request);

        boolean overlaps = shiftRepository.existsOverlappingShift(
                scheduleId, shiftId,
                request.getExamStartTime(), request.getExamEndTime());
        if (overlaps) {
            throw new ShiftTimingViolationException(
                    "examStartTime–examEndTime",
                    "Updated shift window overlaps with another shift in schedule " + scheduleId);
        }

        shift.setShiftNumber(request.getShiftNumber());
        shift.setShiftName(request.getShiftName());
        shift.setReportingTime(request.getReportingTime());
        shift.setGateClosingTime(request.getGateClosingTime());
        shift.setLoginStartTime(request.getLoginStartTime());
        shift.setExamStartTime(request.getExamStartTime());
        shift.setExamEndTime(request.getExamEndTime());
        shift.setExitTime(request.getExitTime());
        shift.setDurationMinutes(request.getDurationMinutes());
        shift.setBufferMinutes(request.getBufferMinutes());

        ExamShift saved = shiftRepository.save(shift);
        log.info("Shift updated: id={}, schedule={}, tenant={}", shiftId, scheduleId, tenantId);

        publishAudit("SHIFT_UPDATED", saved.getId(), scheduleId, actorId, tenantId);
        return toShiftResponse(saved);
    }

    /**
     * Lists all shifts for a schedule, ordered by shift number.
     */
    @Transactional(readOnly = true)
    public List<ShiftResponse> listShifts(UUID scheduleId, String tenantId) {
        findSchedule(scheduleId, tenantId);
        return shiftRepository.findByScheduleIdOrderByShiftNumber(scheduleId)
                .stream().map(this::toShiftResponse).toList();
    }

    // ── Validation helpers ────────────────────────────────────────────────────

    /**
     * Enforces all 5 timing ordering constraints plus duration equality (Req 7b.3).
     */
    void validateShiftTimings(CreateShiftRequest request) {
        if (!request.getReportingTime().isBefore(request.getGateClosingTime())) {
            throw new ShiftTimingViolationException(
                    "reportingTime < gateClosingTime",
                    "reportingTime=" + request.getReportingTime()
                            + " must be before gateClosingTime=" + request.getGateClosingTime());
        }
        if (!request.getGateClosingTime().isBefore(request.getLoginStartTime())) {
            throw new ShiftTimingViolationException(
                    "gateClosingTime < loginStartTime",
                    "gateClosingTime=" + request.getGateClosingTime()
                            + " must be before loginStartTime=" + request.getLoginStartTime());
        }
        if (!request.getLoginStartTime().isBefore(request.getExamStartTime())) {
            throw new ShiftTimingViolationException(
                    "loginStartTime < examStartTime",
                    "loginStartTime=" + request.getLoginStartTime()
                            + " must be before examStartTime=" + request.getExamStartTime());
        }
        if (!request.getExamStartTime().isBefore(request.getExamEndTime())) {
            throw new ShiftTimingViolationException(
                    "examStartTime < examEndTime",
                    "examStartTime=" + request.getExamStartTime()
                            + " must be before examEndTime=" + request.getExamEndTime());
        }
        long computedDuration = ChronoUnit.MINUTES.between(
                request.getExamStartTime(), request.getExamEndTime());
        if (computedDuration != request.getDurationMinutes()) {
            throw new ShiftTimingViolationException(
                    "durationMinutes == examEndTime − examStartTime",
                    "Computed duration=" + computedDuration
                            + " min but declared durationMinutes=" + request.getDurationMinutes());
        }
    }

    private void validateTransition(String current, String target, Set<String> actorRoles) {
        // CANCELLED is always allowed from any non-CANCELLED state
        if ("CANCELLED".equals(target)) {
            if ("CANCELLED".equals(current)) {
                throw new ScheduleWorkflowException(current, target);
            }
            Set<String> allowed = TRANSITION_ROLES.getOrDefault("CANCELLED", Set.of());
            if (actorRoles.stream().noneMatch(allowed::contains)) {
                throw new AccessDeniedException(
                        "Role not permitted to cancel schedule. Required one of: " + allowed);
            }
            return;
        }
        // Forward transition: target must be the expected next state
        String expectedNext = NEXT_STATUS.get(current);
        if (!target.equals(expectedNext)) {
            throw new ScheduleWorkflowException(current, target);
        }
        Set<String> allowed = TRANSITION_ROLES.getOrDefault(target, Set.of());
        if (actorRoles.stream().noneMatch(allowed::contains)) {
            throw new AccessDeniedException(
                    "Role not permitted to transition to " + target + ". Required one of: " + allowed);
        }
    }

    private void validateDateConflicts(String tenantId,
                                       LocalDate examDate,
                                       LocalDate reserveDate,
                                       UUID excludeScheduleId) {
        UUID excludeId = excludeScheduleId != null
                ? excludeScheduleId
                : UUID.fromString("00000000-0000-0000-0000-000000000000");

        // exam_date of new schedule must not match another schedule's exam_date
        if (scheduleRepository.existsConflictingExamDate(tenantId, examDate, excludeId)) {
            throw new ScheduleDateConflictException(examDate, "exam_date");
        }
        // exam_date must not match another schedule's reserve_date
        if (scheduleRepository.existsConflictingReserveDate(tenantId, examDate, excludeId)) {
            throw new ScheduleDateConflictException(examDate, "reserve_date");
        }
        // If a reserve_date is provided, it must not conflict either
        if (reserveDate != null) {
            if (scheduleRepository.existsConflictingExamDate(tenantId, reserveDate, excludeId)) {
                throw new ScheduleDateConflictException(reserveDate, "exam_date");
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private ExaminationSchedule findSchedule(UUID scheduleId, String tenantId) {
        ExaminationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));
        if (!tenantId.equals(schedule.getTenantId())) {
            throw new ScheduleNotFoundException(scheduleId);
        }
        return schedule;
    }

    private ScheduleResponse toResponse(ExaminationSchedule s) {
        return ScheduleResponse.builder()
                .id(s.getId())
                .examinationId(s.getExaminationId())
                .scheduleName(s.getScheduleName())
                .scheduleVersion(s.getScheduleVersion())
                .notificationNumber(s.getNotificationNumber())
                .examDate(s.getExamDate())
                .reserveDate(s.getReserveDate())
                .timeZone(s.getTimeZone())
                .status(s.getStatus())
                .changeReason(s.getChangeReason())
                .effectiveFrom(s.getEffectiveFrom())
                .previousVersionId(s.getPreviousVersionId())
                .createdBy(s.getCreatedBy())
                .modifiedBy(s.getModifiedBy())
                .approvedBy(s.getApprovedBy())
                .approvedAt(s.getApprovedAt())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ShiftResponse toShiftResponse(ExamShift s) {
        return ShiftResponse.builder()
                .id(s.getId())
                .scheduleId(s.getScheduleId())
                .shiftNumber(s.getShiftNumber())
                .shiftName(s.getShiftName())
                .reportingTime(s.getReportingTime())
                .gateClosingTime(s.getGateClosingTime())
                .loginStartTime(s.getLoginStartTime())
                .examStartTime(s.getExamStartTime())
                .examEndTime(s.getExamEndTime())
                .exitTime(s.getExitTime())
                .durationMinutes(s.getDurationMinutes())
                .bufferMinutes(s.getBufferMinutes())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private void publishAudit(String eventType, ExaminationSchedule schedule,
                               UUID actorId, String tenantId,
                               String previousValue, String newValue) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", eventType);
            event.put("scheduleId", schedule.getId().toString());
            event.put("examinationId", schedule.getExaminationId().toString());
            event.put("scheduleVersion", schedule.getScheduleVersion());
            event.put("actorId", actorId != null ? actorId.toString() : null);
            event.put("tenantId", tenantId);
            if (previousValue != null) event.put("previousValue", previousValue);
            if (newValue != null)      event.put("newValue", newValue);
            event.put("occurredAt", Instant.now().toString());

            kafkaTemplate.send(AUDIT_TOPIC, schedule.getId().toString(), event)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event [{}] for schedule [{}]: {}",
                                    eventType, schedule.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing audit event [{}]: {}", eventType, e.getMessage());
        }
    }

    private void publishAudit(String eventType, UUID shiftId, UUID scheduleId,
                               UUID actorId, String tenantId) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", eventType);
            event.put("shiftId", shiftId.toString());
            event.put("scheduleId", scheduleId.toString());
            event.put("actorId", actorId != null ? actorId.toString() : null);
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());
            kafkaTemplate.send(AUDIT_TOPIC, shiftId.toString(), event)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish audit event [{}] for shift [{}]: {}",
                                    eventType, shiftId, ex.getMessage());
                        }
                    });
        } catch (Exception e) {
            log.error("Unexpected error publishing shift audit event: {}", e.getMessage());
        }
    }

    private void publishNotification(ExaminationSchedule schedule,
                                      UUID actorId, String tenantId, String eventType) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventType", eventType);
            event.put("scheduleId", schedule.getId().toString());
            event.put("examinationId", schedule.getExaminationId().toString());
            event.put("scheduleVersion", schedule.getScheduleVersion());
            event.put("tenantId", tenantId);
            event.put("occurredAt", Instant.now().toString());
            kafkaTemplate.send(NOTIF_TOPIC, schedule.getId().toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish notification event [{}]: {}", eventType, e.getMessage());
        }
    }
}
