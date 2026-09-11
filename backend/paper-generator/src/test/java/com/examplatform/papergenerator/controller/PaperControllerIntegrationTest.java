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

package com.examplatform.papergenerator.controller;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BlueprintRule;
import com.examplatform.papergenerator.dto.PaperGenerationRequest;
import com.examplatform.papergenerator.exception.InsufficientQuestionsException;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.examplatform.papergenerator.service.ExaminationLookupService;
import com.examplatform.papergenerator.service.PaperApprovalService;
import com.examplatform.papergenerator.service.PaperAssemblyService;
import com.examplatform.papergenerator.service.PaperSerializer;
import com.examplatform.papergenerator.support.AbstractIntegrationTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PaperController REST Endpoints E2E Tests (MockMvc)")
class PaperControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private PaperAssemblyService paperAssemblyService;

    @MockitoBean
    private PaperSerializer paperSerializer;

    @MockitoBean
    private PaperApprovalService paperApprovalService;

    @MockitoBean
    private PaperRepository paperRepository;

    @MockitoBean
    private QuestionBankClient questionBankClient;

    @MockitoBean
    private ExaminationLookupService examinationLookupService;

    private static final String TENANT_ID = "default";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PAPER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID EXAM_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private BlueprintRule sampleRule() {
        return BlueprintRule.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .difficulty("EASY")
                .cognitiveLevel("APPLY")
                .questionCount(5)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/papers (List Papers)")
    class ListPapersEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER lists papers - returns 200 OK")
        void examControllerCanListPapers() throws Exception {
            when(paperRepository.findPapers(any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));
            when(examinationLookupService.findExamNames(any())).thenReturn(Map.of());
            when(examinationLookupService.findShiftNames(any())).thenReturn(Map.of());

            mockMvc.perform(get("/api/v1/papers")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(0));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/papers")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/papers"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/papers/{paperId}")
    class GetPaperEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves paper details - returns 200 OK")
        void superAdminCanGetPaper() throws Exception {
            Paper paper = Paper.builder()
                    .name("Math Paper 1")
                    .examId(EXAM_ID)
                    .shiftId("SHIFT-1")
                    .status("DRAFT")
                    .isPractice(false)
                    .build();
            ReflectionTestUtils.setField(paper, "id", PAPER_ID);

            when(paperRepository.findByIdAndTenantId(eq(PAPER_ID), anyString()))
                    .thenReturn(Optional.of(paper));
            when(examinationLookupService.findExamNames(any())).thenReturn(Map.of(EXAM_ID, "Math Exam"));
            when(examinationLookupService.findShiftNames(any())).thenReturn(Map.of("SHIFT-1", "Morning Shift"));

            mockMvc.perform(get("/api/v1/papers/{paperId}", PAPER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(PAPER_ID.toString()))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("-ve: Nonexistent paper returns 404 Not Found")
        void notFoundReturns404() throws Exception {
            when(paperRepository.findByIdAndTenantId(eq(PAPER_ID), anyString()))
                    .thenReturn(Optional.empty());
            when(paperRepository.findById(eq(PAPER_ID)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/papers/{paperId}", PAPER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/papers/generate")
    class GeneratePaperEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER submits paper generation - returns 202 Accepted")
        void generatePaperAccepted() throws Exception {
            PaperGenerationRequest request = PaperGenerationRequest.builder()
                    .name("New Test Paper")
                    .examId(EXAM_ID)
                    .shiftId("SHIFT-A")
                    .isPractice(false)
                    .blueprintRules(List.of(sampleRule()))
                    .build();

            Paper paper = Paper.builder()
                    .name("New Test Paper")
                    .examId(EXAM_ID)
                    .shiftId("SHIFT-A")
                    .status("DRAFT")
                    .isPractice(false)
                    .build();
            ReflectionTestUtils.setField(paper, "id", PAPER_ID);

            when(paperAssemblyService.generatePaper(any(), any(), anyString())).thenReturn(paper);

            mockMvc.perform(post("/api/v1/papers/generate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.paperId").value(PAPER_ID.toString()))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
        }

        @Test
        @DisplayName("-ve: Insufficient questions returns 422 Unprocessable Entity")
        void insufficientQuestionsReturns422() throws Exception {
            PaperGenerationRequest request = PaperGenerationRequest.builder()
                    .name("Hard Paper")
                    .examId(EXAM_ID)
                    .shiftId("SHIFT-A")
                    .isPractice(false)
                    .blueprintRules(List.of(sampleRule()))
                    .build();

            when(paperAssemblyService.generatePaper(any(), any(), anyString()))
                    .thenThrow(new InsufficientQuestionsException("Insufficient questions in bank", Collections.emptyList()));

            mockMvc.perform(post("/api/v1/papers/generate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.title").value("Insufficient Questions"));
        }

        @Test
        @DisplayName("-ve: Missing required fields returns 400 Bad Request")
        void missingFieldsReturnsBadRequest() throws Exception {
            PaperGenerationRequest request = PaperGenerationRequest.builder().build();

            mockMvc.perform(post("/api/v1/papers/generate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Bad Request"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/papers/validate")
    class ValidatePaperEndpoint {

        @Test
        @DisplayName("+ve: Valid paper JSON returns 200 OK with valid:true")
        void validPaperJsonReturnsOk() throws Exception {
            when(paperSerializer.validate(anyString())).thenReturn(Collections.emptyList());

            mockMvc.perform(post("/api/v1/papers/validate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"questions\": []}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("-ve: Invalid paper JSON returns 422 Unprocessable Entity")
        void invalidPaperJsonReturns422() throws Exception {
            when(paperSerializer.validate(anyString())).thenReturn(List.of("Missing schema version"));

            mockMvc.perform(post("/api/v1/papers/validate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"bad\": \"data\"}"))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.errors.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/papers/{paperId}/approve")
    class ApprovePaperEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER approves paper - returns 200 OK")
        void examControllerCanApprovePaper() throws Exception {
            Paper paper = Paper.builder()
                    .name("Approved Paper")
                    .examId(EXAM_ID)
                    .shiftId("SHIFT-1")
                    .status("APPROVED")
                    .encryptionKeyId("key-12345")
                    .build();
            ReflectionTestUtils.setField(paper, "id", PAPER_ID);

            when(paperApprovalService.approvePaper(eq(PAPER_ID), anyString())).thenReturn(paper);

            mockMvc.perform(post("/api/v1/papers/{paperId}/approve", PAPER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.paperId").value(PAPER_ID.toString()))
                    .andExpect(jsonPath("$.status").value("APPROVED"))
                    .andExpect(jsonPath("$.encryptionKeyId").value("key-12345"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/papers/{paperId}/approve", PAPER_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(USER_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }
}
