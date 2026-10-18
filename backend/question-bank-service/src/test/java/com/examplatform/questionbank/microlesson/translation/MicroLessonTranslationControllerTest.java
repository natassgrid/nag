/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.microlesson.translation;

import com.examplatform.questionbank.microlesson.dto.MicroLessonResponse;
import com.examplatform.questionbank.microlesson.dto.StepByStepBreakdownDto;
import com.examplatform.questionbank.microlesson.translation.controller.MicroLessonTranslationController;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonBatchTranslationJobResponse;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonBatchTranslationRequest;
import com.examplatform.questionbank.microlesson.translation.dto.MicroLessonTranslationResponse;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonBatchTranslationService;
import com.examplatform.questionbank.microlesson.translation.service.MicroLessonTranslationWorkflowService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class MicroLessonTranslationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MicroLessonTranslationWorkflowService workflowService;

    @Mock
    private MicroLessonBatchTranslationService batchService;

    private ObjectMapper objectMapper;

    private final UUID sampleLessonId = UUID.randomUUID();
    private final UUID sampleJobId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        MicroLessonTranslationController controller =
                new MicroLessonTranslationController(workflowService, batchService);

        org.springframework.web.method.support.HandlerMethodArgumentResolver jwtResolver =
                new org.springframework.web.method.support.HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(org.springframework.core.MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(org.springframework.security.core.annotation.AuthenticationPrincipal.class)
                                || parameter.getParameterType().equals(org.springframework.security.oauth2.jwt.Jwt.class);
                    }

                    @Override
                    public Object resolveArgument(org.springframework.core.MethodParameter parameter,
                                                  org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                                                  org.springframework.web.context.request.NativeWebRequest webRequest,
                                                  org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                        return org.springframework.security.oauth2.jwt.Jwt.withTokenValue("mock-jwt-token")
                                .header("alg", "none")
                                .subject("00000000-0000-0000-0000-000000000001")
                                .build();
                    }
                };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(), jwtResolver)
                .build();
    }

    @Test
    @DisplayName("POST /{id}/auto-translate/{lang} returns 200 OK with MicroLessonTranslationResponse")
    void shouldAutoTranslateSingleLesson() throws Exception {
        MicroLessonTranslationResponse response = MicroLessonTranslationResponse.builder()
                .id(UUID.randomUUID())
                .microlessonId(sampleLessonId)
                .languageCode("hi")
                .coreConcept("अनुवादित मुख्य अवधारणा: $$F = ma$$")
                .trap("अनुवादित जाल")
                .cheatSheet(List.of("फॉर्मूला: $$E = mc^2$$"))
                .status("PUBLISHED")
                .sourceVersion(1)
                .build();

        when(workflowService.autoTranslateSingleLesson(eq(sampleLessonId), eq("hi"), any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/microlessons/{id}/auto-translate/{lang}", sampleLessonId, "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.microlessonId").value(sampleLessonId.toString()))
                .andExpect(jsonPath("$.languageCode").value("hi"))
                .andExpect(jsonPath("$.coreConcept").value("अनुवादित मुख्य अवधारणा: $$F = ma$$"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(workflowService).autoTranslateSingleLesson(eq(sampleLessonId), eq("hi"), any(), any());
    }

    @Test
    @DisplayName("POST /batch/auto-translate returns 202 Accepted with both id and jobId in response")
    void shouldTriggerBatchJob() throws Exception {
        MicroLessonBatchTranslationRequest request = MicroLessonBatchTranslationRequest.builder()
                .subject("Physics")
                .topic("Optics")
                .targetExam("JEE_MAIN")
                .targetLanguage("hi")
                .batchSize(20)
                .overwriteExisting(true)
                .build();

        MicroLessonBatchTranslationJobResponse jobResponse = MicroLessonBatchTranslationJobResponse.builder()
                .id(sampleJobId)
                .status("PENDING")
                .targetLanguage("hi")
                .subjectFilter("Physics")
                .batchSize(20)
                .totalLessons(10)
                .progressPercentage(0.0)
                .build();

        when(batchService.startBatchJob(any(MicroLessonBatchTranslationRequest.class), any(), any()))
                .thenReturn(jobResponse);

        mockMvc.perform(post("/api/v1/microlessons/batch/auto-translate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(sampleJobId.toString()))
                .andExpect(jsonPath("$.jobId").value(sampleJobId.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.targetLanguage").value("hi"));
    }

    @Test
    @DisplayName("GET /batch/{jobId} returns 200 OK with live progress")
    void shouldGetBatchJobStatus() throws Exception {
        MicroLessonBatchTranslationJobResponse jobResponse = MicroLessonBatchTranslationJobResponse.builder()
                .id(sampleJobId)
                .status("IN_PROGRESS")
                .targetLanguage("hi")
                .totalLessons(20)
                .processedLessons(10)
                .successfulLessons(10)
                .failedLessons(0)
                .progressPercentage(50.0)
                .build();

        when(batchService.getJobStatus(eq(sampleJobId), any())).thenReturn(jobResponse);

        mockMvc.perform(get("/api/v1/microlessons/batch/{jobId}", sampleJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleJobId.toString()))
                .andExpect(jsonPath("$.jobId").value(sampleJobId.toString()))
                .andExpect(jsonPath("$.progressPercentage").value(50.0))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /batch returns 200 OK with page of jobs")
    void shouldListBatchJobs() throws Exception {
        MicroLessonBatchTranslationJobResponse job = MicroLessonBatchTranslationJobResponse.builder()
                .id(sampleJobId)
                .status("COMPLETED")
                .targetLanguage("hi")
                .build();

        when(batchService.listJobs(any(), any()))
                .thenReturn(new PageImpl<>(List.of(job), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/microlessons/batch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleJobId.toString()))
                .andExpect(jsonPath("$.content[0].jobId").value(sampleJobId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("POST /batch/{jobId}/cancel returns 200 OK with CANCELLED status")
    void shouldCancelBatchJob() throws Exception {
        MicroLessonBatchTranslationJobResponse jobResponse = MicroLessonBatchTranslationJobResponse.builder()
                .id(sampleJobId)
                .status("CANCELLED")
                .build();

        when(batchService.cancelJob(eq(sampleJobId), any())).thenReturn(jobResponse);

        mockMvc.perform(post("/api/v1/microlessons/batch/{jobId}/cancel", sampleJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleJobId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("GET /{id}/language/{lang} returns 200 OK with localized lesson and fallback metadata")
    void shouldGetLocalizedMicroLesson() throws Exception {
        MicroLessonResponse localized = new MicroLessonResponse(
                sampleLessonId, 1L, 2L, 3L, "Physics", "Mechanics", "Laws", "JEE_MAIN",
                "हिंदी अवधारणा: $$F = ma$$", "हिंदी जाल", List.of("सूत्र"),
                new StepByStepBreakdownDto("प्रश्न", List.of(), "उत्तर"), "PUBLISHED",
                null, Instant.now(), null, 1, Instant.now(), Instant.now(),
                "hi", false
        );

        when(workflowService.getLocalizedMicroLesson(eq(sampleLessonId), eq("hi"), any()))
                .thenReturn(localized);

        mockMvc.perform(get("/api/v1/microlessons/{id}/language/{lang}", sampleLessonId, "hi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleLessonId.toString()))
                .andExpect(jsonPath("$.languageCode").value("hi"))
                .andExpect(jsonPath("$.fallbackToEnglish").value(false))
                .andExpect(jsonPath("$.coreConcept").value("हिंदी अवधारणा: $$F = ma$$"));
    }

    @Test
    @DisplayName("GET /{id}/translations returns 200 OK with all translations")
    void shouldGetLessonTranslations() throws Exception {
        MicroLessonTranslationResponse t = MicroLessonTranslationResponse.builder()
                .id(UUID.randomUUID())
                .microlessonId(sampleLessonId)
                .languageCode("hi")
                .status("PUBLISHED")
                .build();

        when(workflowService.getTranslationsForLesson(eq(sampleLessonId), any()))
                .thenReturn(List.of(t));

        mockMvc.perform(get("/api/v1/microlessons/{id}/translations", sampleLessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].microlessonId").value(sampleLessonId.toString()))
                .andExpect(jsonPath("$[0].languageCode").value("hi"));
    }

    @Test
    @DisplayName("POST /translations/{id}/approve returns 200 OK with APPROVED status")
    void shouldApproveTranslation() throws Exception {
        UUID translationId = UUID.randomUUID();
        MicroLessonTranslationResponse response = MicroLessonTranslationResponse.builder()
                .id(translationId)
                .status("APPROVED")
                .reviewComments("Verified LaTeX formulas")
                .build();

        when(workflowService.approveTranslation(eq(translationId), any(), any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/microlessons/translations/{id}/approve", translationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reviewComments\": \"Verified LaTeX formulas\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(translationId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("POST /translations/{id}/reject returns 200 OK with REJECTED status")
    void shouldRejectTranslation() throws Exception {
        UUID translationId = UUID.randomUUID();
        MicroLessonTranslationResponse response = MicroLessonTranslationResponse.builder()
                .id(translationId)
                .status("REJECTED")
                .reviewComments("Math symbol missing in step 2")
                .build();

        when(workflowService.rejectTranslation(eq(translationId), any(), eq("Math symbol missing in step 2"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/microlessons/translations/{id}/reject", translationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comments\": \"Math symbol missing in step 2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(translationId.toString()))
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.reviewComments").value("Math symbol missing in step 2"));
    }

    @Test
    @DisplayName("Also supports alternative /api/v1/micro-lessons route prefix")
    void shouldSupportAlternativeRoutePrefix() throws Exception {
        MicroLessonTranslationResponse response = MicroLessonTranslationResponse.builder()
                .id(UUID.randomUUID())
                .microlessonId(sampleLessonId)
                .languageCode("bn")
                .status("PUBLISHED")
                .build();

        when(workflowService.autoTranslateSingleLesson(eq(sampleLessonId), eq("bn"), any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/micro-lessons/{id}/auto-translate/{lang}", sampleLessonId, "bn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("bn"));
    }
}
