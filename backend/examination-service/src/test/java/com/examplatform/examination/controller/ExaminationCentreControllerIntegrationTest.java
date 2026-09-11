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

import com.examplatform.examination.dto.schedule.CentreResponse;
import com.examplatform.examination.dto.schedule.CreateCentreRequest;
import com.examplatform.examination.dto.schedule.SeatAllocationRequest;
import com.examplatform.examination.dto.schedule.SeatAllocationResponse;
import com.examplatform.examination.exception.CentreNotFoundException;
import com.examplatform.examination.exception.SeatAllocationException;
import com.examplatform.examination.service.ExaminationCentreService;
import com.examplatform.examination.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ExaminationCentreController REST Endpoints E2E Tests (MockMvc)")
class ExaminationCentreControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ExaminationCentreService centreService;

    private static final String TENANT_ID = "default";
    private static final UUID CENTRE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXAM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID SCHEDULE_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID SHIFT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ALLOCATION_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ACTOR_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private CreateCentreRequest sampleCreateCentreRequest() {
        return CreateCentreRequest.builder()
                .centreName("National Institute of Technology Test Centre")
                .state("Delhi")
                .city("New Delhi")
                .building("Main Campus Block A")
                .floor("2nd Floor")
                .totalCapacity(500)
                .active(true)
                .build();
    }

    private CentreResponse sampleCentreResponse() {
        return CentreResponse.builder()
                .id(CENTRE_ID)
                .centreName("National Institute of Technology Test Centre")
                .state("Delhi")
                .city("New Delhi")
                .totalCapacity(500)
                .active(true)
                .build();
    }

    // =========================================================================
    // 1. POST /api/v1/examinations/centres (Create Centre)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/examinations/centres")
    class CreateCentreEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER creates examination centre - returns 201 Created")
        void examControllerCanCreateCentre() throws Exception {
            CreateCentreRequest request = sampleCreateCentreRequest();
            when(centreService.createCentre(any(CreateCentreRequest.class), eq(TENANT_ID)))
                    .thenReturn(sampleCentreResponse());

            mockMvc.perform(post("/api/v1/examinations/centres")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(CENTRE_ID.toString()))
                    .andExpect(jsonPath("$.data.centreName").value("National Institute of Technology Test Centre"));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN creates examination centre - returns 201 Created")
        void superAdminCanCreateCentre() throws Exception {
            CreateCentreRequest request = sampleCreateCentreRequest();
            when(centreService.createCentre(any(CreateCentreRequest.class), eq(TENANT_ID)))
                    .thenReturn(sampleCentreResponse());

            mockMvc.perform(post("/api/v1/examinations/centres")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("-ve: Blank required fields return 400 Bad Request with fieldErrors")
        void blankCentreFieldsReturnBadRequest() throws Exception {
            CreateCentreRequest invalidRequest = CreateCentreRequest.builder()
                    .centreName("") // Blank
                    .state("") // Blank
                    .city("") // Blank
                    .totalCapacity(-1) // Min 0
                    .build();

            mockMvc.perform(post("/api/v1/examinations/centres")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.centreName").exists())
                    .andExpect(jsonPath("$.fieldErrors.state").exists())
                    .andExpect(jsonPath("$.fieldErrors.city").exists());
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from creating centre - returns 403 Forbidden")
        void candidateCannotCreateCentre() throws Exception {
            CreateCentreRequest request = sampleCreateCentreRequest();

            mockMvc.perform(post("/api/v1/examinations/centres")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            CreateCentreRequest request = sampleCreateCentreRequest();

            mockMvc.perform(post("/api/v1/examinations/centres")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    // =========================================================================
    // 2. GET /api/v1/examinations/centres (List Centres)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/examinations/centres")
    class ListCentresEndpoint {

        @Test
        @DisplayName("+ve: SECURITY_ADMIN lists centres - returns 200 OK")
        void securityAdminCanListCentres() throws Exception {
            when(centreService.listCentresPaged(eq(TENANT_ID), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(sampleCentreResponse())));

            mockMvc.perform(get("/api/v1/examinations/centres")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SECURITY_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.content[0].id").value(CENTRE_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from listing internal centres - returns 403 Forbidden")
        void candidateCannotListCentres() throws Exception {
            mockMvc.perform(get("/api/v1/examinations/centres")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 3. GET & PUT /api/v1/examinations/centres/{centreId}
    // =========================================================================
    @Nested
    @DisplayName("Centre Details and Deactivation")
    class CentreDetailsAndDeactivationEndpoints {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves centre by ID - returns 200 OK")
        void examControllerCanGetCentre() throws Exception {
            when(centreService.getCentre(eq(CENTRE_ID), eq(TENANT_ID))).thenReturn(sampleCentreResponse());

            mockMvc.perform(get("/api/v1/examinations/centres/{centreId}", CENTRE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(CENTRE_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Non-existent centre returns 404 Not Found")
        void nonExistentCentreReturnsNotFound() throws Exception {
            when(centreService.getCentre(eq(CENTRE_ID), eq(TENANT_ID)))
                    .thenThrow(new CentreNotFoundException(CENTRE_ID));

            mockMvc.perform(get("/api/v1/examinations/centres/{centreId}", CENTRE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER deactivates centre - returns 200 OK")
        void examControllerCanDeactivateCentre() throws Exception {
            CentreResponse deactivated = CentreResponse.builder()
                    .id(CENTRE_ID)
                    .centreName("National Institute of Technology Test Centre")
                    .active(false)
                    .build();

            when(centreService.deactivateCentre(eq(CENTRE_ID), eq(TENANT_ID))).thenReturn(deactivated);

            mockMvc.perform(put("/api/v1/examinations/centres/{centreId}/deactivate", CENTRE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.active").value(false));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role cannot deactivate centre - returns 403 Forbidden")
        void candidateCannotDeactivateCentre() throws Exception {
            mockMvc.perform(put("/api/v1/examinations/centres/{centreId}/deactivate", CENTRE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 4. Shift Seat Allocation Endpoints
    // =========================================================================
    @Nested
    @DisplayName("Seat Allocation Endpoints")
    class SeatAllocationEndpoints {

        private SeatAllocationRequest validAllocationRequest() {
            return SeatAllocationRequest.builder()
                    .centreId(CENTRE_ID)
                    .totalSeats(200)
                    .availableSeats(150)
                    .reservedSeats(30)
                    .pwdSeats(10)
                    .emergencyBufferSeats(10)
                    .femaleReservedSeats(20)
                    .specialCategorySeats(10)
                    .build();
        }

        private SeatAllocationResponse sampleAllocationResponse() {
            return SeatAllocationResponse.builder()
                    .id(ALLOCATION_ID)
                    .shiftId(SHIFT_ID)
                    .centreId(CENTRE_ID)
                    .totalSeats(200)
                    .availableSeats(150)
                    .build();
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER upserts seat allocation - returns 200 OK")
        void examControllerCanUpsertAllocation() throws Exception {
            SeatAllocationRequest request = validAllocationRequest();
            when(centreService.upsertAllocation(eq(SHIFT_ID), any(SeatAllocationRequest.class), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenReturn(sampleAllocationResponse());

            mockMvc.perform(post("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations",
                            EXAM_ID, SCHEDULE_ID, SHIFT_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(ALLOCATION_ID.toString()))
                    .andExpect(jsonPath("$.data.totalSeats").value(200));
        }

        @Test
        @DisplayName("-ve: Seat allocation exceeding capacity throws SeatAllocationException - returns 422 Unprocessable Entity")
        void allocationExceedingCapacityReturnsUnprocessableEntity() throws Exception {
            SeatAllocationRequest request = validAllocationRequest();
            when(centreService.upsertAllocation(eq(SHIFT_ID), any(SeatAllocationRequest.class), eq(ACTOR_ID), eq(TENANT_ID)))
                    .thenThrow(new SeatAllocationException("Allocated seats exceed centre capacity"));

            mockMvc.perform(post("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations",
                            EXAM_ID, SCHEDULE_ID, SHIFT_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(ACTOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER lists allocations for shift - returns 200 OK")
        void examControllerCanListAllocations() throws Exception {
            when(centreService.listAllocations(eq(SHIFT_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(sampleAllocationResponse()));

            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations",
                            EXAM_ID, SCHEDULE_ID, SHIFT_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].id").value(ALLOCATION_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role cannot view allocations - returns 403 Forbidden")
        void candidateCannotViewAllocations() throws Exception {
            mockMvc.perform(get("/api/v1/examinations/{examId}/schedules/{scheduleId}/shifts/{shiftId}/allocations",
                            EXAM_ID, SCHEDULE_ID, SHIFT_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }
}
