/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Public License as published
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

package com.examplatform.questionbank.translation.controller;

import com.examplatform.questionbank.translation.ai.IndicTrans2Service;


import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import com.examplatform.questionbank.translation.domain.BatchTranslationJobStatus;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.*;
import com.examplatform.questionbank.translation.service.*;
import com.examplatform.shared.constants.QuestionStateConstants;
import com.examplatform.shared.constants.SharedConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("TranslationController REST Endpoints E2E Tests (MockMvc)")
class TranslationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private TranslationWorkflowService translationWorkflowService;

    @MockitoBean
    private TranslationReviewService translationReviewService;

    @MockitoBean
    private TranslationQueryService translationQueryService;

    @MockitoBean
    private IndicTrans2Service indicTrans2Service;

    @MockitoBean
    private BatchTranslationService batchTranslationService;

    @MockitoBean
    private QuestionService questionService;

    private static final UUID QUESTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID TRANSLATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REVIEWER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TRANSLATOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID JOB_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private TranslationRequest validTranslationRequest() {
        TranslationRequest request = new TranslationRequest();
        request.setQuestionId(QUESTION_ID);
        request.setLanguageCode("hi");
        request.setTranslatorId(TRANSLATOR_ID);
        request.setTranslatedContent("हिंदी में प्रश्न");
        request.setTranslatedOptions(List.of(
                new TranslatedOptionDto("A", "विकल्प A"),
                new TranslatedOptionDto("B", "विकल्प B")
        ));
        return request;
    }

    @Nested
    @DisplayName("Auto-Translation via IndicTrans2")
    class AutoTranslateEndpoint {

        @Test
        @DisplayName("+ve: TRANSLATOR triggers auto-translate - returns 200 OK")
        void translatorCanAutoTranslate() throws Exception {
            AutoTranslateResponse response = AutoTranslateResponse.builder()
                    .questionId(QUESTION_ID)
                    .languageCode("hi")
                    .translatedContent("हिंदी में प्रश्न")
                    .translatedOptions(List.of(
                            new TranslatedOptionDto("A", "विकल्प A"),
                            new TranslatedOptionDto("B", "विकल्प B")
                    ))
                    .model("IndicTrans2-v1")
                    .build();

            when(indicTrans2Service.autoTranslateQuestion(eq(QUESTION_ID), eq("hi"))).thenReturn(response);

            mockMvc.perform(post("/api/v1/translations/question/{questionId}/auto-translate/{lang}", QUESTION_ID, "hi")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.questionId").value(QUESTION_ID.toString()))
                    .andExpect(jsonPath("$.languageCode").value("hi"))
                    .andExpect(jsonPath("$.translatedContent").value("हिंदी में प्रश्न"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from auto-translate - returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/translations/question/{questionId}/auto-translate/{lang}", QUESTION_ID, "hi")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Batch Auto-Translation Endpoints")
    class BatchAutoTranslateEndpoints {

        @Test
        @DisplayName("+ve: SUPER_ADMIN triggers batch auto-translate - returns 202 Accepted")
        void superAdminCanTriggerBatchAutoTranslate() throws Exception {
            BatchTranslationJobResponse jobResponse = BatchTranslationJobResponse.builder()
                    .id(JOB_ID)
                    .tenantId("default")
                    .status(BatchTranslationJobStatus.PENDING)
                    .sourceLanguage("en")
                    .targetLanguage("hi")
                    .targetStatus(QuestionStateConstants.PUBLISHED)
                    .build();

            when(batchTranslationService.startBatchJob(any(BatchTranslationRequest.class), any(), anyString()))
                    .thenReturn(jobResponse);

            BatchTranslationRequest request = BatchTranslationRequest.builder()
                    .targetLanguage("hi")
                    .targetStatus(QuestionStateConstants.PUBLISHED)
                    .batchSize(20)
                    .build();

            mockMvc.perform(post("/api/v1/translations/batch/auto-translate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.id").value(JOB_ID.toString()))
                    .andExpect(jsonPath("$.status").value("PENDING"))
                    .andExpect(jsonPath("$.targetLanguage").value("hi"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER gets batch job status - returns 200 OK")
        void examControllerCanGetBatchStatus() throws Exception {
            BatchTranslationJobResponse jobResponse = BatchTranslationJobResponse.builder()
                    .id(JOB_ID)
                    .tenantId("default")
                    .status(BatchTranslationJobStatus.IN_PROGRESS)
                    .totalQuestions(100)
                    .processedQuestions(50)
                    .progressPercentage(50.0)
                    .build();

            when(batchTranslationService.getJobStatus(eq(JOB_ID), anyString()))
                    .thenReturn(jobResponse);

            mockMvc.perform(get("/api/v1/translations/batch/{jobId}", JOB_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(JOB_ID.toString()))
                    .andExpect(jsonPath("$.progressPercentage").value(50.0))
                    .andExpect(jsonPath("$.processedQuestions").value(50));
        }


        @Test
        @DisplayName("+ve: SUPER_ADMIN cancels running batch job - returns 200 OK")
        void superAdminCanCancelBatchJob() throws Exception {
            BatchTranslationJobResponse jobResponse = BatchTranslationJobResponse.builder()
                    .id(JOB_ID)
                    .status(BatchTranslationJobStatus.CANCELLED)
                    .build();

            when(batchTranslationService.cancelJob(eq(JOB_ID), anyString()))
                    .thenReturn(jobResponse);

            mockMvc.perform(post("/api/v1/translations/batch/{jobId}/cancel", JOB_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(SharedConstants.CANCELLED));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN lists paginated batch jobs - returns 200 OK with Page")
        void superAdminCanListBatchJobs() throws Exception {
            BatchTranslationJobResponse jobResp = BatchTranslationJobResponse.builder()
                    .id(JOB_ID)
                    .status(BatchTranslationJobStatus.COMPLETED)
                    .sourceLanguage("en")
                    .targetLanguage("hi")
                    .totalQuestions(10)
                    .processedQuestions(10)
                    .successfulQuestions(10)
                    .progressPercentage(100.0)
                    .build();

            Page<BatchTranslationJobResponse> pagedResponse = new PageImpl<>(List.of(jobResp), PageRequest.of(0, 20), 1);
            when(batchTranslationService.listJobs(anyString(), any(Pageable.class)))
                    .thenReturn(pagedResponse);

            mockMvc.perform(get("/api/v1/translations/batch?page=0&size=20")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(JOB_ID.toString()))
                    .andExpect(jsonPath("$.content[0].status").value(SharedConstants.COMPLETED))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role cannot trigger batch auto-translate - returns 403 Forbidden")
        void candidateCannotTriggerBatch() throws Exception {
            mockMvc.perform(post("/api/v1/translations/batch/auto-translate")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Translation Workflow Write Endpoints")
    class WorkflowWriteEndpoints {

        @Test
        @DisplayName("+ve: TRANSLATOR submits translation request - returns 201 Created")
        void translatorCanSubmitTranslation() throws Exception {
            Translation translation = Translation.builder()
                    .questionId(QUESTION_ID)
                    .languageCode("hi")
                    .status(Translation.TranslationStatus.DRAFT)
                    .translatorId(TRANSLATOR_ID)
                    .build();
            ReflectionTestUtils.setField(translation, "id", TRANSLATION_ID);

            when(translationWorkflowService.requestTranslation(any(TranslationRequest.class), anyString()))
                    .thenReturn(translation);

            mockMvc.perform(post("/api/v1/translations")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR"))
                                    .jwt(j -> j.subject(TRANSLATOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validTranslationRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.translationId").value(TRANSLATION_ID.toString()))
                    .andExpect(jsonPath("$.status").value(QuestionStateConstants.DRAFT));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN submits translation without translatorId in body - defaults translatorId from JWT")
        void adminCanSubmitTranslationWithoutTranslatorId() throws Exception {
            Translation translation = Translation.builder()
                    .questionId(QUESTION_ID)
                    .languageCode("hi")
                    .status(Translation.TranslationStatus.DRAFT)
                    .translatorId(TRANSLATOR_ID)
                    .build();
            ReflectionTestUtils.setField(translation, "id", TRANSLATION_ID);

            when(translationWorkflowService.requestTranslation(any(TranslationRequest.class), anyString()))
                    .thenReturn(translation);

            // Payload with translatorId omitted
            String jsonPayload = """
                    {
                      "questionId": "11111111-1111-1111-1111-111111111111",
                      "languageCode": "hi",
                      "translatedContent": "हिंदी में प्रश्न",
                      "translatedOptions": [
                        {"id": "A", "text": "विकल्प A"},
                        {"id": "B", "text": "विकल्प B"}
                      ]
                    }
                    """;

            mockMvc.perform(post("/api/v1/translations")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(TRANSLATOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonPayload))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.translationId").value(TRANSLATION_ID.toString()))
                    .andExpect(jsonPath("$.status").value(QuestionStateConstants.DRAFT));

            ArgumentCaptor<TranslationRequest> captor = ArgumentCaptor.forClass(TranslationRequest.class);
            verify(translationWorkflowService).requestTranslation(captor.capture(), anyString());
            assertThat(captor.getValue().getTranslatorId()).isEqualTo(TRANSLATOR_ID);
        }

        @Test
        @DisplayName("+ve: REVIEWER approves translation - returns 200 OK")
        void reviewerCanApproveTranslation() throws Exception {
            Translation translation = Translation.builder()
                    .questionId(QUESTION_ID)
                    .languageCode("hi")
                    .status(Translation.TranslationStatus.APPROVED)
                    .reviewerId(REVIEWER_ID)
                    .build();
            ReflectionTestUtils.setField(translation, "id", TRANSLATION_ID);

            when(translationReviewService.approve(eq(TRANSLATION_ID), eq(REVIEWER_ID), anyString()))
                    .thenReturn(translation);

            mockMvc.perform(post("/api/v1/translations/{id}/approve", TRANSLATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(SharedConstants.REVIEWER_ID, REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.translationId").value(TRANSLATION_ID.toString()))
                    .andExpect(jsonPath("$.status").value(QuestionStateConstants.APPROVED));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN approves translation with empty body - defaults reviewerId from JWT")
        void adminCanApproveTranslationWithEmptyBody() throws Exception {
            Translation translation = Translation.builder()
                    .questionId(QUESTION_ID)
                    .languageCode("hi")
                    .status(Translation.TranslationStatus.APPROVED)
                    .reviewerId(REVIEWER_ID)
                    .build();
            ReflectionTestUtils.setField(translation, "id", TRANSLATION_ID);

            when(translationReviewService.approve(eq(TRANSLATION_ID), eq(REVIEWER_ID), anyString()))
                    .thenReturn(translation);

            mockMvc.perform(post("/api/v1/translations/{id}/approve", TRANSLATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.translationId").value(TRANSLATION_ID.toString()))
                    .andExpect(jsonPath("$.status").value(QuestionStateConstants.APPROVED));

            verify(translationReviewService).approve(eq(TRANSLATION_ID), eq(REVIEWER_ID), anyString());
        }

        @Test
        @DisplayName("+ve: REVIEWER rejects translation with comments - returns 200 OK")
        void reviewerCanRejectTranslation() throws Exception {
            TranslationReviewRequest request = new TranslationReviewRequest();
            request.setReviewerId(REVIEWER_ID);
            request.setComments("Terminology in option B is inaccurate");

            mockMvc.perform(post("/api/v1/translations/{id}/reject", TRANSLATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.translationId").value(TRANSLATION_ID.toString()))
                    .andExpect(jsonPath("$.status").value(QuestionStateConstants.DRAFT));
        }
    }

    @Nested
    @DisplayName("Translation Query Endpoints")
    class QueryEndpoints {

        @Test
        @DisplayName("+ve: TRANSLATOR lists questions for translation - returns 200 OK with page")
        void translatorCanListQuestionsForTranslation() throws Exception {
            QuestionResponse resp = QuestionResponse.builder()
                    .id(QUESTION_ID)
                    .subject("Physics")
                    .topic("Thermodynamics")
                    .content("What is entropy?")
                    .translatedLanguages(List.of("hi"))
                    .translationStatusMap(Map.of("hi", QuestionStateConstants.APPROVED))
                    .translationStatus(QuestionStateConstants.APPROVED)
                    .build();

            when(questionService.listQuestions(
                    any(), any(), any(), any(), any(), any(), any(),
                    eq("hi"), eq(QuestionStateConstants.APPROVED), any(), any(), anyInt(), anyInt(), anyString()))
                    .thenReturn(new PageImpl<>(List.of(resp)));

            mockMvc.perform(get("/api/v1/translations/questions?targetLang=hi&translationStatus=APPROVED")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TRANSLATOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(QUESTION_ID.toString()))
                    .andExpect(jsonPath("$.data.content[0].translationStatus").value(QuestionStateConstants.APPROVED))
                    .andExpect(jsonPath("$.data.content[0].translatedLanguages[0]").value("hi"));
        }

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER fetches approved translation - returns 200 OK")
        void deliveryServiceCanGetApprovedTranslation() throws Exception {
            TranslationResponse response = TranslationResponse.builder()
                    .translationId(TRANSLATION_ID)
                    .questionId(QUESTION_ID)
                    .languageCode("hi")
                    .translatedContent("हिंदी में प्रश्न")
                    .status(Translation.TranslationStatus.APPROVED)
                    .build();

            when(translationQueryService.getApprovedTranslation(eq(QUESTION_ID), eq("hi"), anyString()))
                    .thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/v1/translations/question/{questionId}/language/{lang}", QUESTION_ID, "hi")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.translationId").value(TRANSLATION_ID.toString()))
                    .andExpect(jsonPath("$.languageCode").value("hi"))
                    .andExpect(jsonPath("$.status").value(QuestionStateConstants.APPROVED));
        }

        @Test
        @DisplayName("+ve: CANDIDATE fetches approved translation - returns 200 OK")
        void candidateCanGetApprovedTranslation() throws Exception {
            TranslationResponse response = TranslationResponse.builder()
                    .translationId(TRANSLATION_ID)
                    .questionId(QUESTION_ID)
                    .languageCode("hi")
                    .translatedContent("हिंदी में प्रश्न")
                    .status(Translation.TranslationStatus.APPROVED)
                    .build();

            when(translationQueryService.getApprovedTranslation(eq(QUESTION_ID), eq("hi"), anyString()))
                    .thenReturn(Optional.of(response));

            mockMvc.perform(get("/api/v1/translations/question/{questionId}/language/{lang}", QUESTION_ID, "hi")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.translationId").value(TRANSLATION_ID.toString()))
                    .andExpect(jsonPath("$.languageCode").value("hi"))
                    .andExpect(jsonPath("$.status").value(QuestionStateConstants.APPROVED));
        }

        @Test
        @DisplayName("-ve: Missing approved translation returns 404 Not Found")
        void missingApprovedTranslationReturnsNotFound() throws Exception {
            when(translationQueryService.getApprovedTranslation(eq(QUESTION_ID), eq("ta"), anyString()))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/translations/question/{questionId}/language/{lang}", QUESTION_ID, "ta")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))))
                    .andExpect(status().isNotFound());
        }
    }
}
