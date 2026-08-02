package com.examplatform.examination.service;

import com.examplatform.examination.domain.ExamShift;
import com.examplatform.examination.domain.Examination;
import com.examplatform.examination.domain.ExaminationSchedule;
import com.examplatform.examination.dto.schedule.AmendScheduleRequest;
import com.examplatform.examination.dto.schedule.CreateScheduleRequest;
import com.examplatform.examination.dto.schedule.CreateShiftRequest;
import com.examplatform.examination.dto.schedule.ScheduleResponse;
import com.examplatform.examination.dto.schedule.ScheduleTransitionRequest;
import com.examplatform.examination.dto.schedule.ShiftResponse;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.ScheduleDateConflictException;
import com.examplatform.examination.exception.ScheduleWorkflowException;
import com.examplatform.examination.exception.ShiftTimingViolationException;
import com.examplatform.examination.repository.ExaminationRepository;
import com.examplatform.examination.repository.ExaminationScheduleRepository;
import com.examplatform.examination.repository.ExamShiftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExaminationScheduleService")
class ExaminationScheduleServiceTest {

    @Mock ExaminationRepository examinationRepository;
    @Mock ExaminationScheduleRepository scheduleRepository;
    @Mock ExamShiftRepository shiftRepository;
    @SuppressWarnings("rawtypes")
    @Mock KafkaTemplate kafkaTemplate;

    @InjectMocks ExaminationScheduleService service;

    private static final String TENANT    = "tenant-test";
    private static final UUID   EXAM_ID   = UUID.randomUUID();
    private static final UUID   ACTOR_ID  = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void stubKafka() {
        lenient().when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
    }

    // ── createSchedule ────────────────────────────────────────────────────────

    @Nested @DisplayName("createSchedule")
    class CreateSchedule {

        @Test
        @DisplayName("creates DRAFT schedule when exam exists and no date conflict")
        void createsDraftSchedule() {
            Examination exam = stubExam(EXAM_ID, TENANT);
            when(examinationRepository.findById(EXAM_ID)).thenReturn(Optional.of(exam));
            when(scheduleRepository.existsConflictingExamDate(any(), any(), any())).thenReturn(false);
            when(scheduleRepository.existsConflictingReserveDate(any(), any(), any())).thenReturn(false);
            when(scheduleRepository.save(any())).thenAnswer(inv -> {
                ExaminationSchedule s = inv.getArgument(0);
                setId(s, UUID.randomUUID());
                return s;
            });

            CreateScheduleRequest req = CreateScheduleRequest.builder()
                    .scheduleName("Phase 1")
                    .examDate(LocalDate.of(2027, 1, 10))
                    .build();

            ScheduleResponse resp = service.createSchedule(EXAM_ID, req, ACTOR_ID, TENANT);

            assertThat(resp.getStatus()).isEqualTo("DRAFT");
            assertThat(resp.getScheduleName()).isEqualTo("Phase 1");
            assertThat(resp.getScheduleVersion()).isEqualTo(1);
            assertThat(resp.getExaminationId()).isEqualTo(EXAM_ID);
        }

        @Test
        @DisplayName("throws ExaminationNotFoundException when exam not found")
        void throwsWhenExamNotFound() {
            when(examinationRepository.findById(EXAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createSchedule(EXAM_ID,
                    CreateScheduleRequest.builder()
                            .scheduleName("S")
                            .examDate(LocalDate.now().plusDays(30))
                            .build(),
                    ACTOR_ID, TENANT))
                    .isInstanceOf(ExaminationNotFoundException.class);
        }

        @Test
        @DisplayName("throws ScheduleDateConflictException when exam date conflicts")
        void throwsOnDateConflict() {
            when(examinationRepository.findById(EXAM_ID)).thenReturn(Optional.of(stubExam(EXAM_ID, TENANT)));
            when(scheduleRepository.existsConflictingExamDate(any(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> service.createSchedule(EXAM_ID,
                    CreateScheduleRequest.builder()
                            .scheduleName("S")
                            .examDate(LocalDate.of(2027, 1, 10))
                            .build(),
                    ACTOR_ID, TENANT))
                    .isInstanceOf(ScheduleDateConflictException.class)
                    .hasMessageContaining("conflict");
        }
    }

    // ── transitionSchedule ────────────────────────────────────────────────────

    @Nested @DisplayName("transitionSchedule")
    class TransitionSchedule {

        @Test
        @DisplayName("DRAFT → SCHEDULER_REVIEW succeeds for EXAM_CONTROLLER")
        void draftToSchedulerReview() {
            ExaminationSchedule schedule = stubSchedule(UUID.randomUUID(), EXAM_ID, "DRAFT", 1);
            when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
            when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ScheduleResponse resp = service.transitionSchedule(
                    schedule.getId(),
                    ScheduleTransitionRequest.builder().targetStatus("SCHEDULER_REVIEW").build(),
                    ACTOR_ID, Set.of("EXAM_CONTROLLER"), TENANT);

            assertThat(resp.getStatus()).isEqualTo("SCHEDULER_REVIEW");
        }

        @Test
        @DisplayName("DRAFT → PUBLISHED is rejected (skips states)")
        void draftToPublishedRejected() {
            ExaminationSchedule schedule = stubSchedule(UUID.randomUUID(), EXAM_ID, "DRAFT", 1);
            when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

            assertThatThrownBy(() -> service.transitionSchedule(
                    schedule.getId(),
                    ScheduleTransitionRequest.builder().targetStatus("PUBLISHED").build(),
                    ACTOR_ID, Set.of("SUPER_ADMIN"), TENANT))
                    .isInstanceOf(ScheduleWorkflowException.class)
                    .hasMessageContaining("DRAFT")
                    .hasMessageContaining("PUBLISHED");
        }

        @Test
        @DisplayName("PUBLISHED → CANCELLED succeeds for EXAM_CONTROLLER")
        void publishedToCancelled() {
            ExaminationSchedule schedule = stubSchedule(UUID.randomUUID(), EXAM_ID, "PUBLISHED", 1);
            when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
            when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            ScheduleResponse resp = service.transitionSchedule(
                    schedule.getId(),
                    ScheduleTransitionRequest.builder().targetStatus("CANCELLED").build(),
                    ACTOR_ID, Set.of("EXAM_CONTROLLER"), TENANT);

            assertThat(resp.getStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("full approval chain: DRAFT → … → PUBLISHED")
        void fullApprovalChain() {
            UUID sid = UUID.randomUUID();
            // Simulate progressive state changes by returning updated schedule each time
            ExaminationSchedule s = stubSchedule(sid, EXAM_ID, "DRAFT", 1);
            when(scheduleRepository.findById(sid)).thenReturn(Optional.of(s));
            when(scheduleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            String[] chain = {
                "SCHEDULER_REVIEW", "CONTROLLER_APPROVED",
                "SECURITY_REVIEW", "CHAIRMAN_APPROVED", "PUBLISHED"
            };
            Set<String>[] roles = new Set[]{
                Set.of("EXAM_CONTROLLER"), Set.of("EXAM_CONTROLLER"),
                Set.of("SECURITY_ADMIN"),  Set.of("SUPER_ADMIN"), Set.of("SUPER_ADMIN")
            };

            for (int i = 0; i < chain.length; i++) {
                ScheduleResponse resp = service.transitionSchedule(
                        sid,
                        ScheduleTransitionRequest.builder().targetStatus(chain[i]).build(),
                        ACTOR_ID, roles[i], TENANT);
                assertThat(resp.getStatus()).isEqualTo(chain[i]);
                s.setStatus(chain[i]); // advance state for next iteration
            }
        }
    }

    // ── amendSchedule ─────────────────────────────────────────────────────────

    @Nested @DisplayName("amendSchedule")
    class AmendSchedule {

        @Test
        @DisplayName("creates new version row with incremented version and previousVersionId")
        void createsNewVersionRow() {
            UUID sid = UUID.randomUUID();
            ExaminationSchedule published = stubSchedule(sid, EXAM_ID, "PUBLISHED", 2);
            when(scheduleRepository.findById(sid)).thenReturn(Optional.of(published));
            when(scheduleRepository.existsConflictingExamDate(any(), any(), any())).thenReturn(false);
            when(scheduleRepository.existsConflictingReserveDate(any(), any(), any())).thenReturn(false);
            when(scheduleRepository.save(any())).thenAnswer(inv -> {
                ExaminationSchedule s = inv.getArgument(0);
                setId(s, UUID.randomUUID());
                return s;
            });

            AmendScheduleRequest req = AmendScheduleRequest.builder()
                    .changeReason("Date rescheduled due to national holiday")
                    .scheduleName("Phase 1 (Revised)")
                    .examDate(LocalDate.of(2027, 2, 5))
                    .build();

            ScheduleResponse resp = service.amendSchedule(sid, req, ACTOR_ID, TENANT);

            assertThat(resp.getScheduleVersion()).isEqualTo(3);
            assertThat(resp.getPreviousVersionId()).isEqualTo(sid);
            assertThat(resp.getStatus()).isEqualTo("DRAFT"); // amendment restarts workflow
            assertThat(resp.getChangeReason()).isEqualTo("Date rescheduled due to national holiday");
        }

        @Test
        @DisplayName("rejects amendment of non-PUBLISHED schedule")
        void rejectsNonPublished() {
            UUID sid = UUID.randomUUID();
            when(scheduleRepository.findById(sid))
                    .thenReturn(Optional.of(stubSchedule(sid, EXAM_ID, "DRAFT", 1)));

            assertThatThrownBy(() -> service.amendSchedule(sid,
                    AmendScheduleRequest.builder()
                            .changeReason("reason")
                            .scheduleName("S")
                            .examDate(LocalDate.now().plusDays(10))
                            .build(),
                    ACTOR_ID, TENANT))
                    .isInstanceOf(ScheduleWorkflowException.class)
                    .hasMessageContaining("PUBLISHED");
        }
    }

    // ── validateShiftTimings ──────────────────────────────────────────────────

    @Nested @DisplayName("validateShiftTimings")
    class ValidateShiftTimings {

        @Test
        @DisplayName("passes for a valid morning shift")
        void passesValidShift() {
            CreateShiftRequest req = validShiftRequest();
            // Should not throw
            service.validateShiftTimings(req);
        }

        @Test
        @DisplayName("rejects when reportingTime >= gateClosingTime")
        void rejectsReportingAfterGate() {
            CreateShiftRequest req = validShiftRequest();
            req.setReportingTime(LocalTime.of(9, 0));
            req.setGateClosingTime(LocalTime.of(8, 30)); // before reporting

            assertThatThrownBy(() -> service.validateShiftTimings(req))
                    .isInstanceOf(ShiftTimingViolationException.class)
                    .hasMessageContaining("reportingTime < gateClosingTime");
        }

        @Test
        @DisplayName("rejects when gateClosingTime >= loginStartTime")
        void rejectsGateAfterLogin() {
            CreateShiftRequest req = validShiftRequest();
            req.setGateClosingTime(LocalTime.of(9, 0));
            req.setLoginStartTime(LocalTime.of(8, 45)); // before gate

            assertThatThrownBy(() -> service.validateShiftTimings(req))
                    .isInstanceOf(ShiftTimingViolationException.class)
                    .hasMessageContaining("gateClosingTime < loginStartTime");
        }

        @Test
        @DisplayName("rejects when loginStartTime >= examStartTime")
        void rejectsLoginAfterStart() {
            CreateShiftRequest req = validShiftRequest();
            req.setLoginStartTime(LocalTime.of(9, 15));
            req.setExamStartTime(LocalTime.of(9, 0)); // before login

            assertThatThrownBy(() -> service.validateShiftTimings(req))
                    .isInstanceOf(ShiftTimingViolationException.class)
                    .hasMessageContaining("loginStartTime < examStartTime");
        }

        @Test
        @DisplayName("rejects when examStartTime >= examEndTime")
        void rejectsStartAfterEnd() {
            CreateShiftRequest req = validShiftRequest();
            req.setExamStartTime(LocalTime.of(12, 0));
            req.setExamEndTime(LocalTime.of(9, 0));    // before start
            req.setDurationMinutes(180);

            assertThatThrownBy(() -> service.validateShiftTimings(req))
                    .isInstanceOf(ShiftTimingViolationException.class)
                    .hasMessageContaining("examStartTime < examEndTime");
        }

        @Test
        @DisplayName("rejects when declared durationMinutes does not match computed duration")
        void rejectsDurationMismatch() {
            CreateShiftRequest req = validShiftRequest();
            req.setDurationMinutes(120); // actual is 180

            assertThatThrownBy(() -> service.validateShiftTimings(req))
                    .isInstanceOf(ShiftTimingViolationException.class)
                    .hasMessageContaining("durationMinutes");
        }

        @Test
        @DisplayName("accepts shift where duration matches exactly")
        void acceptsExactDurationMatch() {
            // 09:00 → 12:00 = 180 minutes exactly
            CreateShiftRequest req = validShiftRequest();
            service.validateShiftTimings(req); // no exception
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static Examination stubExam(UUID id, String tenantId) {
        Examination e = Examination.builder()
                .name("Test Exam")
                .durationMinutes(180)
                .totalMarks(100)
                .navigationPolicy("FLEXIBLE")
                .calculatorPolicy("NONE")
                .status("DRAFT")
                .build();
        setId(e, id);
        e.setTenantId(tenantId);
        return e;
    }

    private static ExaminationSchedule stubSchedule(UUID id, UUID examId,
                                                     String status, int version) {
        ExaminationSchedule s = ExaminationSchedule.builder()
                .examinationId(examId)
                .scheduleName("Test Schedule")
                .scheduleVersion(version)
                .examDate(LocalDate.of(2027, 1, 10))
                .timeZone("Asia/Kolkata")
                .status(status)
                .build();
        setId(s, id);
        s.setTenantId(TENANT);
        return s;
    }

    /**
     * A valid morning shift: 07:30 report → 08:30 gate → 08:45 login → 09:00 start → 12:00 end (180 min).
     */
    private static CreateShiftRequest validShiftRequest() {
        return CreateShiftRequest.builder()
                .shiftNumber(1)
                .shiftName("Morning")
                .reportingTime(LocalTime.of(7, 30))
                .gateClosingTime(LocalTime.of(8, 30))
                .loginStartTime(LocalTime.of(8, 45))
                .examStartTime(LocalTime.of(9, 0))
                .examEndTime(LocalTime.of(12, 0))
                .durationMinutes(180)
                .bufferMinutes(30)
                .build();
    }

    private static void setId(Object entity, UUID id) {
        try {
            var field = entity.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            var createdAt = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(entity, Instant.now());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
