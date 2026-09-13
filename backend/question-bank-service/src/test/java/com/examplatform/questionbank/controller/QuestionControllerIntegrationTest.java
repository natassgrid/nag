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

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.domain.QuestionVersion;
import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.exception.FourEyesPrincipleViolationException;
import com.examplatform.questionbank.exception.InvalidTransitionException;
import com.examplatform.questionbank.service.QuestionLifecycleService;
import com.examplatform.questionbank.service.QuestionSearchService;
import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.questionbank.service.QuestionUpdateService;
import com.examplatform.questionbank.service.QuestionVersioningService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("QuestionController REST Endpoints E2E Tests (MockMvc)")
class QuestionControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private QuestionService questionService;

    @MockitoBean
    private QuestionUpdateService questionUpdateService;

    @MockitoBean
    private QuestionVersioningService questionVersioningService;

    @MockitoBean
    private QuestionLifecycleService questionLifecycleService;

    @MockitoBean
    private QuestionSearchService questionSearchService;

    private static final String TENANT_ID = "default";
    private static final UUID QUESTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REVIEWER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private CreateQuestionRequest validCreateRequest() {
        return CreateQuestionRequest.builder()
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .subject("Physics")
                .topic("Thermodynamics")
                .subtopic("Carnot Engine")
                .difficulty(DifficultyLevel.MEDIUM)
                .cognitiveLevel(CognitiveLevel.APPLY)
                .questionType(QuestionType.SINGLE_MCQ)
                .content("What is the efficiency of an ideal Carnot engine operating between 300K and 600K?")
                .answerKey("B")
                .explanation("Efficiency = 1 - (Tc/Th) = 1 - (300/600) = 0.5 or 50%")
                .options(List.of(
                        QuestionOption.builder().id("A").text("25%").build(),
                        QuestionOption.builder().id("B").text("50%").correct(true).build(),
                        QuestionOption.builder().id("C").text("75%").build(),
                        QuestionOption.builder().id("D").text("100%").build()
                ))
                .build();
    }

    private QuestionResponse sampleQuestionResponse() {
        return QuestionResponse.builder()
                .id(QUESTION_ID)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .subject("Physics")
                .topic("Thermodynamics")
                .subtopic("Carnot Engine")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .questionType("SINGLE_MCQ")
                .content("What is the efficiency of an ideal Carnot engine operating between 300K and 600K?")
                .answerKey("B")
                .state("DRAFT")
                .authorId(AUTHOR_ID)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // 1. POST /api/v1/questions (Create Question)
    // =========================================================================
    @Nested
    @DisplayName("POST /api/v1/questions")
    class CreateQuestionEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR creates question - returns 201 Created")
        void authorCanCreateQuestion() throws Exception {
            CreateQuestionRequest request = validCreateRequest();
            when(questionService.createQuestion(any(CreateQuestionRequest.class), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(sampleQuestionResponse());

            mockMvc.perform(post("/api/v1/questions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(QUESTION_ID.toString()))
                    .andExpect(jsonPath("$.message").value("Question created successfully"));
        }

        @Test
        @DisplayName("-ve: Blank required fields return 400 Bad Request with validation message")
        void blankRequiredFieldsReturnBadRequest() throws Exception {
            CreateQuestionRequest invalidRequest = CreateQuestionRequest.builder()
                    .content("") // Blank
                    .build();

            mockMvc.perform(post("/api/v1/questions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from creating question - returns 403 Forbidden")
        void candidateCannotCreateQuestion() throws Exception {
            CreateQuestionRequest request = validCreateRequest();

            mockMvc.perform(post("/api/v1/questions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Missing X-Tenant-Id header returns 400 Bad Request")
        void missingTenantHeaderReturnsBadRequest() throws Exception {
            CreateQuestionRequest request = validCreateRequest();

            mockMvc.perform(post("/api/v1/questions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // 2. GET /api/v1/questions (List Questions)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/questions")
    class ListQuestionsEndpoint {

        @Test
        @DisplayName("+ve: REVIEWER retrieves paginated list of questions - returns 200 OK")
        void reviewerCanListQuestions() throws Exception {
            when(questionService.listQuestions(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt(), eq(TENANT_ID)))
                    .thenReturn(new PageImpl<>(List.of(sampleQuestionResponse())));

            mockMvc.perform(get("/api/v1/questions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.content[0].id").value(QUESTION_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from listing questions - returns 403 Forbidden")
        void candidateForbiddenFromListing() throws Exception {
            mockMvc.perform(get("/api/v1/questions")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 3. GET /api/v1/questions/{id} (Get Question by ID)
    // =========================================================================
    @Nested
    @DisplayName("GET /api/v1/questions/{id}")
    class GetQuestionByIdEndpoint {

        @Test
        @DisplayName("+ve: APPROVER retrieves question by ID - returns 200 OK")
        void approverCanGetQuestionById() throws Exception {
            when(questionService.getQuestion(eq(QUESTION_ID))).thenReturn(sampleQuestionResponse());

            mockMvc.perform(get("/api/v1/questions/{id}", QUESTION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_APPROVER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(QUESTION_ID.toString()));
        }

        @Test
        @DisplayName("-ve: Non-existent question returns 404 Not Found")
        void nonExistentQuestionReturnsNotFound() throws Exception {
            when(questionService.getQuestion(eq(QUESTION_ID)))
                    .thenThrow(new EntityNotFoundException("Question not found with ID: " + QUESTION_ID));

            mockMvc.perform(get("/api/v1/questions/{id}", QUESTION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_APPROVER"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Question not found with ID: " + QUESTION_ID));
        }
    }

    // =========================================================================
    // 4. PUT /api/v1/questions/{id} (Update Question)
    // =========================================================================
    @Nested
    @DisplayName("PUT /api/v1/questions/{id}")
    class UpdateQuestionEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR updates question - returns 200 OK")
        void authorCanUpdateQuestion() throws Exception {
            CreateQuestionRequest request = validCreateRequest();
            when(questionUpdateService.updateQuestion(eq(QUESTION_ID), any(CreateQuestionRequest.class), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(sampleQuestionResponse());

            mockMvc.perform(put("/api/v1/questions/{id}", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(QUESTION_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE cannot update question - returns 403 Forbidden")
        void candidateCannotUpdateQuestion() throws Exception {
            CreateQuestionRequest request = validCreateRequest();

            mockMvc.perform(put("/api/v1/questions/{id}", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================================================================
    // 5. Lifecycle Transitions (Submit, Transition, Approve, Reject)
    // =========================================================================
    @Nested
    @DisplayName("Question Lifecycle Endpoints")
    class LifecycleEndpoints {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR submits question for review - returns 200 OK")
        void authorCanSubmitForReview() throws Exception {
            QuestionResponse inReview = sampleQuestionResponse();
            inReview.setState("REVIEW");

            when(questionService.submitForReview(eq(QUESTION_ID), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(inReview);

            mockMvc.perform(put("/api/v1/questions/{id}/submit", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.state").value("REVIEW"));
        }

        @Test
        @DisplayName("+ve: REVIEWER transitions question - returns 200 OK")
        void reviewerCanTransitionQuestion() throws Exception {
            TransitionRequest request = TransitionRequest.builder()
                    .targetState("APPROVED")
                    .comments("Looks solid")
                    .build();

            QuestionResponse approved = sampleQuestionResponse();
            approved.setState("APPROVED");

            when(questionLifecycleService.transition(eq(QUESTION_ID), any(TransitionRequest.class), eq(REVIEWER_ID), eq(TENANT_ID)))
                    .thenReturn(approved);

            mockMvc.perform(post("/api/v1/questions/{id}/transition", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.state").value("APPROVED"));
        }

        @Test
        @DisplayName("-ve: Invalid transition throws InvalidTransitionException - returns 422 Unprocessable Entity")
        void invalidTransitionReturnsUnprocessableEntity() throws Exception {
            TransitionRequest request = TransitionRequest.builder()
                    .targetState("PUBLISHED")
                    .build();

            when(questionLifecycleService.transition(eq(QUESTION_ID), any(TransitionRequest.class), eq(REVIEWER_ID), eq(TENANT_ID)))
                    .thenThrow(new InvalidTransitionException("DRAFT", "PUBLISHED"));

            mockMvc.perform(post("/api/v1/questions/{id}/transition", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: Four-eyes violation throws FourEyesPrincipleViolationException - returns 403 Forbidden")
        void fourEyesViolationReturnsForbidden() throws Exception {
            TransitionRequest request = TransitionRequest.builder()
                    .targetState("APPROVED")
                    .build();

            when(questionLifecycleService.transition(eq(QUESTION_ID), any(TransitionRequest.class), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenThrow(new FourEyesPrincipleViolationException());

            mockMvc.perform(post("/api/v1/questions/{id}/transition", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("+ve: REVIEWER approves question - returns 200 OK")
        void reviewerCanApproveQuestion() throws Exception {
            QuestionResponse approved = sampleQuestionResponse();
            approved.setState("APPROVED");

            when(questionLifecycleService.approve(eq(QUESTION_ID), eq(REVIEWER_ID), eq(TENANT_ID)))
                    .thenReturn(approved);

            mockMvc.perform(put("/api/v1/questions/{id}/approve", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.state").value("APPROVED"));
        }

        @Test
        @DisplayName("+ve: REVIEWER rejects question - returns 200 OK")
        void reviewerCanRejectQuestion() throws Exception {
            QuestionResponse rejected = sampleQuestionResponse();
            rejected.setState("DRAFT");

            when(questionLifecycleService.reject(eq(QUESTION_ID), eq(REVIEWER_ID), eq("Needs revision"), eq(TENANT_ID)))
                    .thenReturn(rejected);

            mockMvc.perform(put("/api/v1/questions/{id}/reject", QUESTION_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))
                                    .jwt(j -> j.subject(REVIEWER_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("comments", "Needs revision"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.state").value("DRAFT"));
        }
    }

    // =========================================================================
    // 6. Versions & Search
    // =========================================================================
    @Nested
    @DisplayName("Versions and Search Endpoints")
    class VersionsAndSearchEndpoints {

        @Test
        @DisplayName("+ve: REVIEWER retrieves version history - returns 200 OK")
        void reviewerCanGetVersions() throws Exception {
            QuestionVersion version = QuestionVersion.builder()
                    .questionId(QUESTION_ID)
                    .authorId(AUTHOR_ID)
                    .changedAt(Instant.now())
                    .versionNumber(1)
                    .diffJson("{}")
                    .build();

            when(questionVersioningService.getVersions(eq(QUESTION_ID))).thenReturn(List.of(version));

            mockMvc.perform(get("/api/v1/questions/{id}/versions", QUESTION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data[0].versionNumber").value(1));
        }

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR searches questions - returns 200 OK")
        void authorCanSearchQuestions() throws Exception {
            when(questionSearchService.search(anyString(), any(), any(), anyInt(), anyInt(), eq(TENANT_ID)))
                    .thenReturn(new PageImpl<>(List.of(sampleQuestionResponse())));

            mockMvc.perform(get("/api/v1/questions/search")
                            .header("X-Tenant-Id", TENANT_ID)
                            .param("query", "Carnot")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.content[0].id").value(QUESTION_ID.toString()));
        }
    }
}
