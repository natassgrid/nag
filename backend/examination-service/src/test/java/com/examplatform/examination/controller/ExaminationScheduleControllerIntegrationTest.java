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

package com.examplatform.examination.controller;

import com.examplatform.examination.dto.schedule.*;
import com.examplatform.examination.exception.ScheduleDateConflictException;
import com.examplatform.examination.exception.ScheduleNotFoundException;
import com.examplatform.examination.exception.ScheduleWorkflowException;
import com.examplatform.examination.exception.ShiftTimingViolationException;
import com.examplatform.examination.service.ExaminationScheduleService;
import com.examplatform.examination.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ExaminationScheduleController REST Endpoints E2E Tests (MockMvc)")
class ExaminationScheduleControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ExaminationScheduleService scheduleService;

    private static final String TENANT_ID = "default";
    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SCHEDULE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SHIFT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private CreateScheduleRequest sampleCreateScheduleRequest() {
        return CreateScheduleRequest.builder()
                .scheduleName("Phase 1 Regular Schedule")
                .notificationNumber("NOTIF/2026/01")
                .examDate(LocalDate.of(2026, 6, 15))
                .reserveDate(LocalDate.of(2026, 6, 20))
                .timeZone("Asia/Kolkata")
                .build();
    }

    private ScheduleResponse sampleScheduleResponse() {
        return ScheduleResponse.builder()
                .id(SCHEDULE_ID)
                .examinationId(EXAM_ID)
                .scheduleName("Phase 1 Regular Schedule")
                .scheduleVersion(1)
                .examDate(LocalDate.of(2026, 6, 15))
                .status("DRAFT")
                .build();
    }

    private CreateShiftRequest sampleCreateShiftRequest() {
        return CreateShiftRequest.builder()
                .shiftNumber(1)
                .shiftName("Morning Shift")
                .reportingTime(LocalTime.of(7, 30))
                .gateClosingTime(LocalTime.of(8, 30))
                .loginStartTime(LocalTime.of(8, 45))
                .examStartTime(LocalTime.of(9, 0))
                .examEndTime(LocalTime.of(12, 0))
                .durationMinutes(180)
                .build();
    }

    private ShiftResponse sampleShiftResponse() {
        return ShiftResponse.builder()
                .id(SHIFT_ID)
                .scheduleId(SCHEDULE_ID)
                .shiftNumber(1)
                .shiftName("Morning Shift")
                .reportingTime(LocalTime.of(7, 30))
                .gateClosingTime(LocalTime.of(8, 30))
                .loginStartTime(LocalTime.of(8, 45))
                .examStartTime(LocalTime.of(9, 0))
                .examEndTime(LocalTime.of(12, 0))
                .durationMinutes(180)
                .build();
    }

    // =========================================================================
    // 1. POST /api/v1/examinations/{examId}/schedules (Create Schedule)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/examinations/{examId}/schedules")
    class CreateScheduleEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER creates examination schedule - returns 201 Created")
        void examControllerCanCreateSchedule() throws Exception {
            CreateScheduleRequest request = sampleCreateScheduleRequest();
            when(scheduleService.createSchedule(eq(EXAM_ID), any(CreateScheduleRequest.class), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenReturn(sampleScheduleResponse());

            mockMvc.perform(post("/api/v1/examinations/{examId}/schedules", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(SCHEDULE_ID.toString()))
                    .andExpect(jsonPath("$.data.scheduleName").value("Phase 1 Regular Schedule"));
        }

        @Test
        @DisplayName("-ve: Date conflict throws ScheduleDateConflictException - returns 409 Conflict")
        void dateConflictReturnsConflict() throws Exception {
            CreateScheduleRequest request = sampleCreateScheduleRequest();
            when(scheduleService.createSchedule(eq(EXAM_ID), any(CreateScheduleRequest.class), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenThrow(new ScheduleDateConflictException(LocalDate.of(2026, 6, 15), "EXAM_DATE"));

            mockMvc.perform(post("/api/v1/examinations/{examId}/schedules", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from creating schedules - returns 403 Forbidden")
        void candidateCannotCreateSchedule() throws Exception {
            CreateScheduleRequest request = sampleCreateScheduleRequest();

            mockMvc.perform(post("/api/v1/examinations/{examId}/schedules", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 2. GET /api/v1/examinations/{examId}/schedules (List Schedules)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/examinations/{examId}/schedules")
    class ListSchedulesEndpoint {

        @Test
        @DisplayName("+ve: SECURITY_ADMIN lists schedules - returns 200 OK")
        void securityAdminCanListSchedules() throws Exception {
            when(scheduleService.listSchedulesPaged(eq(EXAM_ID), eq(TENANT_ID), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(sampleScheduleResponse())));

            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.content[0].id").value(SCHEDULE_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE forbidden from listing schedules - returns 403 Forbidden")
        void candidateCannotListSchedules() throws Exception {
            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 3. GET /api/v1/examinations/{examId}/schedules/{scheduleId}
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/examinations/{examId}/schedules/{scheduleId}")
    class GetScheduleByIdEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves schedule by ID - returns 200 OK")
        void examControllerCanGetSchedule() throws Exception {
            when(scheduleService.getSchedule(eq(SCHEDULE_ID), eq(TENANT_ID))).thenReturn(sampleScheduleResponse());

            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules/{scheduleId}", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(SCHEDULE_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Non-existent schedule returns 404 Not Found")
        void nonExistentScheduleReturnsNotFound() throws Exception {
            when(scheduleService.getSchedule(eq(SCHEDULE_ID), eq(TENANT_ID)))
                    .thenThrow(new ScheduleNotFoundException(SCHEDULE_ID));

            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules/{scheduleId}", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // =========================================================================
    // 4. PUT /api/v1/examinations/{examId}/schedules/{scheduleId}/transition
    // =========================================================================
    @Nested
    @DisplayName("PUT transition & amend endpoints")
    class TransitionAndAmendEndpoints {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER transitions schedule status - returns 200 OK")
        void examControllerCanTransitionSchedule() throws Exception {
            ScheduleTransitionRequest request = ScheduleTransitionRequest.builder()
                    .targetStatus("SCHEDULER_REVIEW")
                    .comment("Ready for review")
                    .build();

            ScheduleResponse transitionedResponse = ScheduleResponse.builder()
                    .id(SCHEDULE_ID)
                    .examinationId(EXAM_ID)
                    .status("SCHEDULER_REVIEW")
                    .build();

            when(scheduleService.transitionSchedule(eq(SCHEDULE_ID), any(ScheduleTransitionRequest.class),
                    eq(ACTOR_ID), any(), eq(TENANT_ID)))
                    .thenReturn(transitionedResponse);

            mockMvc.perform(put("/api/v1/examinations/{examId}/schedules/{scheduleId}/transition", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString()).claim("realm_access", Map.of("roles", List.of("EXAM_CONTROLLER")))))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.status").value("SCHEDULER_REVIEW"));
        }

        @Test
        @DisplayName("-ve: Invalid transition throws ScheduleWorkflowException - returns 422 Unprocessable Entity")
        void invalidTransitionReturnsUnprocessableEntity() throws Exception {
            ScheduleTransitionRequest request = ScheduleTransitionRequest.builder()
                    .targetStatus("PUBLISHED") // Cannot jump directly from DRAFT to PUBLISHED
                    .build();

            when(scheduleService.transitionSchedule(eq(SCHEDULE_ID), any(ScheduleTransitionRequest.class),
                    eq(ACTOR_ID), any(), eq(TENANT_ID)))
                    .thenThrow(new ScheduleWorkflowException("DRAFT", "PUBLISHED"));

            mockMvc.perform(put("/api/v1/examinations/{examId}/schedules/{scheduleId}/transition", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.currentStatus").value("DRAFT"))
                    .andExpect(jsonPath("$.targetStatus").value("PUBLISHED"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER amends published schedule - returns 201 Created with new version")
        void examControllerCanAmendSchedule() throws Exception {
            AmendScheduleRequest request = AmendScheduleRequest.builder()
                    .scheduleName("Phase 1 Amended")
                    .examDate(LocalDate.of(2026, 6, 18))
                    .changeReason("Centre maintenance requested date shift")
                    .build();

            ScheduleResponse amendedResponse = ScheduleResponse.builder()
                    .id(UUID.randomUUID())
                    .scheduleName("Phase 1 Amended")
                    .scheduleVersion(2)
                    .build();

            when(scheduleService.amendSchedule(eq(SCHEDULE_ID), any(AmendScheduleRequest.class), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenReturn(amendedResponse);

            mockMvc.perform(put("/api/v1/examinations/{examId}/schedules/{scheduleId}/amend", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.scheduleVersion").value(2));
        }
    }

    // =========================================================================
    // 5. Shift Endpoints (CRUD)
    // =========================================================================
    @Nested
    @DisplayName("Shift Management Endpoints")
    class ShiftManagementEndpoints {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER adds shift to schedule - returns 201 Created")
        void examControllerCanAddShift() throws Exception {
            CreateShiftRequest request = sampleCreateShiftRequest();
            when(scheduleService.addShift(eq(SCHEDULE_ID), any(CreateShiftRequest.class), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenReturn(sampleShiftResponse());

            mockMvc.perform(post("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(SHIFT_ID.toString()))
                    .andExpect(jsonPath("$.data.shiftName").value("Morning Shift"));
        }

        @Test
        @DisplayName("-ve: Shift timing violation throws ShiftTimingViolationException - returns 422 Unprocessable Entity")
        void timingViolationReturnsUnprocessableEntity() throws Exception {
            CreateShiftRequest request = sampleCreateShiftRequest();
            when(scheduleService.addShift(eq(SCHEDULE_ID), any(CreateShiftRequest.class), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenThrow(new ShiftTimingViolationException("reporting_before_gate", "Reporting time must be before gate closing time"));

            mockMvc.perform(post("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.violatedConstraint").value("reporting_before_gate"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER lists shifts for schedule - returns 200 OK")
        void examControllerCanListShifts() throws Exception {
            when(scheduleService.listShifts(eq(SCHEDULE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(sampleShiftResponse()));

            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].id").value(SHIFT_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE cannot view shifts - returns 403 Forbidden")
        void candidateCannotListShifts() throws Exception {
            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts", EXAM_ID, SCHEDULE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }
}
