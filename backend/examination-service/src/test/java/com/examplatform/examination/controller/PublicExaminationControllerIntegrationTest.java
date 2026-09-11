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

import com.examplatform.examination.dto.*;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.service.ExamApplicationService;
import com.examplatform.examination.service.ExaminationService;
import com.examplatform.examination.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PublicExaminationController REST Endpoints E2E Tests (MockMvc)")
class PublicExaminationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ExaminationService examinationService;

    @MockitoBean
    private ExamApplicationService examApplicationService;

    private static final String TENANT_ID = "default";
    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CANDIDATE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID APPLICATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private ExaminationResponse sampleExamResponse() {
        return ExaminationResponse.builder()
                .id(EXAM_ID)
                .name("Civil Services Examination 2026")
                .code("CSE-2026")
                .status("PUBLISHED")
                .durationMinutes(120)
                .totalMarks(200)
                .build();
    }

    private ExamApplicationResponse sampleApplicationResponse() {
        return ExamApplicationResponse.builder()
                .applicationId(APPLICATION_ID)
                .examId(EXAM_ID)
                .candidateId(CANDIDATE_ID)
                .status("APPLIED")
                .applicationDate(LocalDateTime.now())
                .examName("Civil Services Examination 2026")
                .examCode("CSE-2026")
                .build();
    }

    private AdmitCardResponse sampleAdmitCardResponse() {
        return AdmitCardResponse.builder()
                .applicationId(APPLICATION_ID)
                .hallTicketNumber("HT-2026-99999")
                .candidateId(CANDIDATE_ID)
                .candidateName("John Doe")
                .examId(EXAM_ID)
                .examName("Civil Services Examination 2026")
                .examCode("CSE-2026")
                .examDate(LocalDate.of(2026, 7, 20))
                .shiftName("Morning Shift")
                .build();
    }

    // =========================================================================
    // 1. Public Examination Discovery (No Authentication Required)
    // =========================================================================
    @Nested
    @DisplayName("Public Catalog Endpoints")
    class PublicCatalogEndpoints {

        @Test
        @DisplayName("+ve: Public access to listPublished() returns 200 OK without JWT")
        void publicCanListPublishedExams() throws Exception {
            when(examinationService.listPublishedPaged(any(), any(), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(sampleExamResponse())));

            mockMvc.perform(get("/api/v1/examinations/public"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.content[0].id").value(EXAM_ID.toString()))
                    .andExpect(jsonPath("$.data.content[0].status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("+ve: Public access to getPublishedById() returns 200 OK without JWT")
        void publicCanGetExamById() throws Exception {
            when(examinationService.getById(eq(EXAM_ID))).thenReturn(sampleExamResponse());

            mockMvc.perform(get("/api/v1/examinations/public/{examId}", EXAM_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.name").value("Civil Services Examination 2026"));
        }

        @Test
        @DisplayName("-ve: Public access to non-existent exam returns 404 Not Found")
        void nonExistentExamReturnsNotFound() throws Exception {
            when(examinationService.getById(eq(EXAM_ID)))
                    .thenThrow(new ExaminationNotFoundException(EXAM_ID));

            mockMvc.perform(get("/api/v1/examinations/public/{examId}", EXAM_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("+ve: Public access to list centres returns 200 OK without JWT")
        void publicCanListCentres() throws Exception {
            PublicCentreResponse centre = PublicCentreResponse.builder()
                    .id(UUID.randomUUID())
                    .centreName("Centre 101")
                    .state("Delhi")
                    .city("New Delhi")
                    .build();

            when(examApplicationService.listPublicCentres(any(), any(), any()))
                    .thenReturn(List.of(centre));

            mockMvc.perform(get("/api/v1/examinations/centres/public"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].centreName").value("Centre 101"));
        }
    }

    // =========================================================================
    // 2. Candidate Application Endpoints
    // =========================================================================
    @Nested
    @DisplayName("Candidate Application Endpoints")
    class CandidateApplicationEndpoints {

        @Test
        @DisplayName("+ve: CANDIDATE applies for examination - returns 201 Created")
        void candidateCanApplyForExam() throws Exception {
            ExamApplicationRequest request = ExamApplicationRequest.builder()
                    .pwdRequired(false)
                    .scribeRequired(false)
                    .build();

            when(examApplicationService.apply(eq(EXAM_ID), eq(CANDIDATE_ID), any(), any()))
                    .thenReturn(sampleApplicationResponse());

            mockMvc.perform(post("/api/v1/examinations/{examId}/apply", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.applicationId").value(APPLICATION_ID.toString()))
                    .andExpect(jsonPath("$.data.status").value("APPLIED"));
        }

        @Test
        @DisplayName("-ve: Duplicate application throws DuplicateKeyException - returns 409 Conflict")
        void duplicateApplicationReturnsConflict() throws Exception {
            when(examApplicationService.apply(eq(EXAM_ID), eq(CANDIDATE_ID), any(), any()))
                    .thenThrow(new DuplicateKeyException("Already applied for this examination"));

            mockMvc.perform(post("/api/v1/examinations/{examId}/apply", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Already applied for this examination"));
        }

        @Test
        @DisplayName("-ve: Closed exam application throws IllegalStateException - returns 400 Bad Request")
        void closedApplicationReturnsBadRequest() throws Exception {
            when(examApplicationService.apply(eq(EXAM_ID), eq(CANDIDATE_ID), any(), any()))
                    .thenThrow(new IllegalStateException("Application window has closed for this examination"));

            mockMvc.perform(post("/api/v1/examinations/{examId}/apply", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Application window has closed for this examination"));
        }

        @Test
        @DisplayName("-ve: Unauthenticated request to apply returns 401 Unauthorized")
        void unauthenticatedApplyReturnsUnauthorized() throws Exception {
            mockMvc.perform(post("/api/v1/examinations/{examId}/apply", EXAM_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("+ve: CANDIDATE retrieves own applications via /my-exams - returns 200 OK")
        void candidateCanGetMyExams() throws Exception {
            when(examApplicationService.getMyApplications(eq(CANDIDATE_ID), any()))
                    .thenReturn(List.of(sampleApplicationResponse()));

            mockMvc.perform(get("/api/v1/examinations/my-exams")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].applicationId").value(APPLICATION_ID.toString()));
        }

        @Test
        @DisplayName("+ve: CANDIDATE retrieves application status for single exam - returns 200 OK")
        void candidateCanGetMyApplicationForExam() throws Exception {
            when(examApplicationService.getMyApplication(eq(EXAM_ID), eq(CANDIDATE_ID), any()))
                    .thenReturn(sampleApplicationResponse());

            mockMvc.perform(get("/api/v1/examinations/{examId}/my-application", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.examId").value(EXAM_ID.toString()));
        }
    }

    // =========================================================================
    // 3. Hall Ticket / Admit Card Endpoints
    // =========================================================================
    @Nested
    @DisplayName("Admit Card Endpoints")
    class AdmitCardEndpoints {

        @Test
        @DisplayName("+ve: CANDIDATE retrieves admit card by exam ID - returns 200 OK")
        void candidateCanGetAdmitCardByExamId() throws Exception {
            when(examApplicationService.getAdmitCard(eq(EXAM_ID), eq(CANDIDATE_ID), any()))
                    .thenReturn(sampleAdmitCardResponse());

            mockMvc.perform(get("/api/v1/examinations/{examId}/admit-card", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.hallTicketNumber").value("HT-2026-99999"))
                    .andExpect(jsonPath("$.data.candidateName").value("John Doe"));
        }

        @Test
        @DisplayName("+ve: CANDIDATE retrieves admit card by application ID - returns 200 OK")
        void candidateCanGetAdmitCardByApplicationId() throws Exception {
            when(examApplicationService.getAdmitCardByApplicationId(eq(APPLICATION_ID), eq(CANDIDATE_ID), any()))
                    .thenReturn(sampleAdmitCardResponse());

            mockMvc.perform(get("/api/v1/examinations/applications/{applicationId}/admit-card", APPLICATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.hallTicketNumber").value("HT-2026-99999"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves candidate admit card by application ID - returns 200 OK")
        void examControllerCanGetAdmitCardByApplicationId() throws Exception {
            when(examApplicationService.getAdmitCardByApplicationId(eq(APPLICATION_ID), any(), any()))
                    .thenReturn(sampleAdmitCardResponse());

            mockMvc.perform(get("/api/v1/examinations/applications/{applicationId}/admit-card", APPLICATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }
    }
}
