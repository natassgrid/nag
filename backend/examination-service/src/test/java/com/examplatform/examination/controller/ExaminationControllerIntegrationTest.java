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

import com.examplatform.examination.domain.Section;
import com.examplatform.examination.domain.enums.CalculatorPolicy;
import com.examplatform.examination.domain.enums.NavigationPolicy;
import com.examplatform.examination.dto.CreateExaminationRequest;
import com.examplatform.examination.dto.ExaminationResponse;
import com.examplatform.examination.exception.ExaminationNotFoundException;
import com.examplatform.examination.exception.SectionMarksValidationException;
import com.examplatform.examination.service.ExaminationService;
import com.examplatform.examination.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("ExaminationController REST Endpoints E2E Tests (MockMvc)")
class ExaminationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ExaminationService examinationService;

    private static final String TENANT_ID = "default";
    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private CreateExaminationRequest validCreateRequest() {
        return CreateExaminationRequest.builder()
                .name("National Eligibility Test 2026")
                .code("NET-2026")
                .durationMinutes(180)
                .totalMarks(300)
                .navigationPolicy(NavigationPolicy.FLEXIBLE)
                .calculatorPolicy(CalculatorPolicy.NONE)
                .sections(List.of(
                        Section.builder()
                                .name("Paper 1")
                                .subject("Teaching & Research Aptitude")
                                .questionCount(50)
                                .marksPerQuestion(2.0)
                                .build()
                ))
                .build();
    }

    private ExaminationResponse sampleExaminationResponse() {
        return ExaminationResponse.builder()
                .id(EXAM_ID)
                .name("National Eligibility Test 2026")
                .code("NET-2026")
                .durationMinutes(180)
                .totalMarks(300)
                .status("DRAFT")
                .build();
    }

    // =========================================================================
    // 1. GET /api/v1/examinations (List Examinations)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/examinations")
    class ListExaminationsEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves paginated list of examinations - returns 200 OK")
        void examControllerCanListExaminations() throws Exception {
            when(examinationService.listByTenantPaged(eq(TENANT_ID), any(), any(), any(), anyInt(), anyInt()))
                    .thenReturn(new PageImpl<>(List.of(sampleExaminationResponse())));

            mockMvc.perform(get("/api/v1/examinations")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.content[0].id").value(EXAM_ID.toString()))
                    .andExpect(jsonPath("$.data.content[0].name").value("National Eligibility Test 2026"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role cannot list all internal examinations - returns 403 Forbidden")
        void candidateRoleForbiddenFromListingInternalExaminations() throws Exception {
            mockMvc.perform(get("/api/v1/examinations")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/examinations")
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("-ve: Missing X-Tenant-Id header returns 400 Bad Request")
        void missingTenantHeaderReturnsBadRequest() throws Exception {
            mockMvc.perform(get("/api/v1/examinations")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // 2. POST /api/v1/examinations (Create Examination)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/examinations")
    class CreateExaminationEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER creates examination - returns 201 Created")
        void examControllerCanCreateExamination() throws Exception {
            CreateExaminationRequest request = validCreateRequest();
            ExaminationResponse response = sampleExaminationResponse();

            when(examinationService.create(any(CreateExaminationRequest.class), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/examinations")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(EXAM_ID.toString()))
                    .andExpect(jsonPath("$.message").value("Examination created successfully"));
        }

        @Test
        @DisplayName("-ve: Blank required fields return 400 Bad Request with fieldErrors")
        void blankNameReturnsBadRequest() throws Exception {
            CreateExaminationRequest invalidRequest = CreateExaminationRequest.builder()
                    .name("") // Blank
                    .durationMinutes(0) // < 1
                    .totalMarks(0) // < 1
                    .navigationPolicy(NavigationPolicy.FLEXIBLE)
                    .calculatorPolicy(CalculatorPolicy.NONE)
                    .sections(Collections.emptyList()) // Empty
                    .build();

            mockMvc.perform(post("/api/v1/examinations")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors.name").exists())
                    .andExpect(jsonPath("$.fieldErrors.durationMinutes").exists())
                    .andExpect(jsonPath("$.fieldErrors.totalMarks").exists())
                    .andExpect(jsonPath("$.fieldErrors.sections").exists());
        }

        @Test
        @DisplayName("-ve: Section marks mismatch throws SectionMarksValidationException - returns 422 Unprocessable Entity")
        void sectionMarksMismatchReturnsUnprocessableEntity() throws Exception {
            CreateExaminationRequest request = validCreateRequest();

            when(examinationService.create(any(CreateExaminationRequest.class), eq(TENANT_ID)))
                    .thenThrow(new SectionMarksValidationException(300, 100.0));

            mockMvc.perform(post("/api/v1/examinations")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.expectedTotalMarks").value(300))
                    .andExpect(jsonPath("$.actualTotalMarks").value(100.0));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from creating examinations - returns 403 Forbidden")
        void candidateCannotCreateExamination() throws Exception {
            CreateExaminationRequest request = validCreateRequest();

            mockMvc.perform(post("/api/v1/examinations")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 3. PUT /api/v1/examinations/{examId} (Update Examination)
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/v1/examinations/{examId}")
    class UpdateExaminationEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER updates examination - returns 200 OK")
        void examControllerCanUpdateExamination() throws Exception {
            CreateExaminationRequest request = validCreateRequest();
            ExaminationResponse response = sampleExaminationResponse();

            when(examinationService.update(eq(EXAM_ID), any(CreateExaminationRequest.class), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/examinations/{examId}", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(EXAM_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Non-existent examination returns 404 Not Found")
        void nonExistentExaminationReturnsNotFound() throws Exception {
            CreateExaminationRequest request = validCreateRequest();

            when(examinationService.update(eq(EXAM_ID), any(CreateExaminationRequest.class), eq(TENANT_ID)))
                    .thenThrow(new ExaminationNotFoundException(EXAM_ID));

            mockMvc.perform(put("/api/v1/examinations/{examId}", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role cannot update examination - returns 403 Forbidden")
        void candidateCannotUpdateExamination() throws Exception {
            CreateExaminationRequest request = validCreateRequest();

            mockMvc.perform(put("/api/v1/examinations/{examId}", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 4. PUT /api/v1/examinations/{examId}/publish (Publish Examination)
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/v1/examinations/{examId}/publish")
    class PublishExaminationEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER publishes examination - returns 200 OK")
        void examControllerCanPublishExamination() throws Exception {
            ExaminationResponse publishedResponse = ExaminationResponse.builder()
                    .id(EXAM_ID)
                    .status("PUBLISHED")
                    .build();

            when(examinationService.publish(eq(EXAM_ID), eq(TENANT_ID))).thenReturn(publishedResponse);

            mockMvc.perform(put("/api/v1/examinations/{examId}/publish", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
        }

        @Test
        @DisplayName("-ve: Publishing non-existent examination returns 404 Not Found")
        void publishingNonExistentReturnsNotFound() throws Exception {
            when(examinationService.publish(eq(EXAM_ID), eq(TENANT_ID)))
                    .thenThrow(new ExaminationNotFoundException(EXAM_ID));

            mockMvc.perform(put("/api/v1/examinations/{examId}/publish", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("-ve: CANDIDATE role cannot publish examination - returns 403 Forbidden")
        void candidateCannotPublishExamination() throws Exception {
            mockMvc.perform(put("/api/v1/examinations/{examId}/publish", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 5. GET /api/v1/examinations/{examId} (Get Examination By ID)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/examinations/{examId}")
    class GetExaminationByIdEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves examination by ID - returns 200 OK")
        void examControllerCanGetById() throws Exception {
            when(examinationService.getById(eq(EXAM_ID))).thenReturn(sampleExaminationResponse());

            mockMvc.perform(get("/api/v1/examinations/{examId}", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(EXAM_ID.toString()));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves examination by ID - returns 200 OK")
        void superAdminCanGetById() throws Exception {
            when(examinationService.getById(eq(EXAM_ID))).thenReturn(sampleExaminationResponse());

            mockMvc.perform(get("/api/v1/examinations/{examId}", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(EXAM_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role cannot retrieve internal exam by ID - returns 403 Forbidden")
        void candidateCannotGetById() throws Exception {
            mockMvc.perform(get("/api/v1/examinations/{examId}", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Non-existent examination returns 404 Not Found")
        void nonExistentReturnsNotFound() throws Exception {
            when(examinationService.getById(eq(EXAM_ID)))
                    .thenThrow(new ExaminationNotFoundException(EXAM_ID));

            mockMvc.perform(get("/api/v1/examinations/{examId}", EXAM_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
