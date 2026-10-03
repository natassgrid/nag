/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.PassageRequest;
import com.examplatform.questionbank.dto.PassageResponse;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.dto.SubQuestionRequest;
import com.examplatform.questionbank.dto.TransitionRequest;
import com.examplatform.questionbank.service.PassageLifecycleService;
import com.examplatform.questionbank.service.PassageService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PassageController REST Endpoints E2E Tests (MockMvc)")
class PassageControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private PassageService passageService;

    @MockitoBean
    private PassageLifecycleService passageLifecycleService;

    private static final String TENANT_ID = "default";
    private static final UUID PASSAGE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID REVIEWER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private PassageResponse buildSamplePassageResponse(String state) {
        return PassageResponse.builder()
                .id(PASSAGE_ID)
                .tenantId(TENANT_ID)
                .title("Reading Comprehension: Indian Economy Reforms")
                .content("In 1991, India initiated comprehensive structural reforms...")
                .contentFormat("MIXED")
                .subjectId(101L)
                .topicId(201L)
                .subject("Economics")
                .topic("Liberalization & Fiscal Policies")
                .hasImages(false)
                .state(state)
                .authorId(AUTHOR_ID)
                .subQuestionCount(2)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1L)
                .build();
    }

    private PassageRequest buildSamplePassageRequest() {
        return PassageRequest.builder()
                .title("Reading Comprehension: Indian Economy Reforms")
                .content("In 1991, India initiated comprehensive structural reforms...")
                .contentFormat("MIXED")
                .subjectId(101L)
                .topicId(201L)
                .subject("Economics")
                .topic("Liberalization & Fiscal Policies")
                .hasImages(false)
                .subQuestions(List.of(
                        SubQuestionRequest.builder()
                                .questionType(QuestionType.SINGLE_MCQ)
                                .content("What was the primary driver of the 1991 reforms?")
                                .difficulty(DifficultyLevel.MEDIUM)
                                .cognitiveLevel(CognitiveLevel.UNDERSTAND)
                                .options(List.of(
                                        QuestionOption.builder().id("A").text("Balance of payments crisis").correct(true).build(),
                                        QuestionOption.builder().id("B").text("Trade surplus").correct(false).build()
                                ))
                                .build(),
                        SubQuestionRequest.builder()
                                .questionType(QuestionType.SINGLE_MCQ)
                                .content("Which financial institutions played a key role?")
                                .difficulty(DifficultyLevel.EASY)
                                .cognitiveLevel(CognitiveLevel.REMEMBER)
                                .options(List.of(
                                        QuestionOption.builder().id("A").text("IMF and World Bank").correct(true).build(),
                                        QuestionOption.builder().id("B").text("None").correct(false).build()
                                ))
                                .build()
                ))
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/passages - Passage Creation")
    class CreatePassageTests {

        @Test
        @DisplayName("201 CREATED - Author successfully creates a reading comprehension passage")
        void createPassage_AuthorRole_Returns201() throws Exception {
            PassageRequest request = buildSamplePassageRequest();
            PassageResponse response = buildSamplePassageResponse("DRAFT");

            when(passageService.createPassage(any(PassageRequest.class), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/passages")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(AUTHOR_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Passage created successfully"))
                    .andExpect(jsonPath("$.data.id").value(PASSAGE_ID.toString()))
                    .andExpect(jsonPath("$.data.title").value("Reading Comprehension: Indian Economy Reforms"))
                    .andExpect(jsonPath("$.data.state").value("DRAFT"));
        }

        @Test
        @DisplayName("403 FORBIDDEN - Reviewer cannot create a passage")
        void createPassage_ReviewerRole_Returns403() throws Exception {
            PassageRequest request = buildSamplePassageRequest();

            mockMvc.perform(post("/api/v1/passages")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(REVIEWER_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_REVIEWER")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("400 BAD REQUEST - Validation fails when content is empty or subQuestions is invalid")
        void createPassage_InvalidPayload_Returns400() throws Exception {
            PassageRequest request = PassageRequest.builder()
                    .title("Invalid Passage")
                    .content("") // Blank
                    .subjectId(null) // Null
                    .subQuestions(List.of()) // Less than 2
                    .build();

            mockMvc.perform(post("/api/v1/passages")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(AUTHOR_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/passages - Listing & Retrieval")
    class GetPassagesTests {

        @Test
        @DisplayName("200 OK - Author/Reviewer lists passages with pagination")
        void listPassages_ValidRoles_Returns200() throws Exception {
            PassageResponse item = buildSamplePassageResponse("DRAFT");
            when(passageService.listPassages(any(), any(), any(), anyInt(), anyInt(), eq(TENANT_ID)))
                    .thenReturn(new PageImpl<>(List.of(item)));

            mockMvc.perform(get("/api/v1/passages")
                            .header("X-Tenant-Id", TENANT_ID)
                            .param("subjectId", "101")
                            .param("state", "DRAFT")
                            .param("page", "0")
                            .param("size", "10")
                            .with(jwt().jwt(j -> j.subject(REVIEWER_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.content[0].id").value(PASSAGE_ID.toString()));
        }

        @Test
        @DisplayName("200 OK - Retrieve passage by ID")
        void getPassage_Exists_Returns200() throws Exception {
            PassageResponse response = buildSamplePassageResponse("UNDER_REVIEW");
            when(passageService.getPassage(eq(PASSAGE_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/passages/{id}", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(AUTHOR_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(PASSAGE_ID.toString()))
                    .andExpect(jsonPath("$.data.state").value("UNDER_REVIEW"));
        }

        @Test
        @DisplayName("404 NOT FOUND - Passage does not exist")
        void getPassage_NotFound_Returns404() throws Exception {
            when(passageService.getPassage(eq(PASSAGE_ID), eq(TENANT_ID)))
                    .thenThrow(new EntityNotFoundException("Passage not found"));

            mockMvc.perform(get("/api/v1/passages/{id}", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(AUTHOR_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT / DELETE /api/v1/passages/{id} - Modification and Removal")
    class ModifyPassageTests {

        @Test
        @DisplayName("200 OK - Author updates passage")
        void updatePassage_AuthorRole_Returns200() throws Exception {
            PassageRequest request = buildSamplePassageRequest();
            PassageResponse response = buildSamplePassageResponse("DRAFT");
            when(passageService.updatePassage(eq(PASSAGE_ID), any(PassageRequest.class), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/passages/{id}", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(AUTHOR_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Passage updated successfully"));
        }

        @Test
        @DisplayName("200 OK - Author deletes draft passage")
        void deletePassage_AuthorRole_Returns200() throws Exception {
            doNothing().when(passageService).deletePassage(eq(PASSAGE_ID), eq(AUTHOR_ID), eq(TENANT_ID));

            mockMvc.perform(delete("/api/v1/passages/{id}", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(AUTHOR_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Passage deleted successfully"));
        }
    }

    @Nested
    @DisplayName("Passage Lifecycle Transitions - Submit, Approve, Reject, Transition")
    class LifecycleTests {

        @Test
        @DisplayName("200 OK - Author submits passage for review")
        void submitForReview_AuthorRole_Returns200() throws Exception {
            PassageResponse response = buildSamplePassageResponse("UNDER_REVIEW");
            when(passageLifecycleService.submitForReview(eq(PASSAGE_ID), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/passages/{id}/submit", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(AUTHOR_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Passage submitted for review"))
                    .andExpect(jsonPath("$.data.state").value("UNDER_REVIEW"));
        }

        @Test
        @DisplayName("200 OK - Approver/Reviewer approves passage")
        void approve_ReviewerRole_Returns200() throws Exception {
            PassageResponse response = buildSamplePassageResponse("APPROVED");
            when(passageLifecycleService.approve(eq(PASSAGE_ID), eq(REVIEWER_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/passages/{id}/approve", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().jwt(j -> j.subject(REVIEWER_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_REVIEWER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Passage approved successfully"))
                    .andExpect(jsonPath("$.data.state").value("APPROVED"));
        }

        @Test
        @DisplayName("200 OK - Approver/Reviewer rejects passage with comments")
        void reject_ReviewerRole_Returns200() throws Exception {
            PassageResponse response = buildSamplePassageResponse("REJECTED");
            when(passageLifecycleService.reject(eq(PASSAGE_ID), eq(REVIEWER_ID), eq("Needs better sub-questions"), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(put("/api/v1/passages/{id}/reject", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("comments", "Needs better sub-questions")))
                            .with(jwt().jwt(j -> j.subject(REVIEWER_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_APPROVER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Passage rejected"))
                    .andExpect(jsonPath("$.data.state").value("REJECTED"));
        }

        @Test
        @DisplayName("200 OK - Generic lifecycle transition endpoint")
        void transition_ApproverRole_Returns200() throws Exception {
            TransitionRequest request = TransitionRequest.builder()
                    .targetState("APPROVED")
                    .comments("All verified")
                    .build();
            PassageResponse response = buildSamplePassageResponse("APPROVED");
            when(passageLifecycleService.transition(eq(PASSAGE_ID), any(TransitionRequest.class), eq(REVIEWER_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/passages/{id}/transition", PASSAGE_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().jwt(j -> j.subject(REVIEWER_ID.toString()))
                                    .authorities(new SimpleGrantedAuthority("ROLE_APPROVER"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.state").value("APPROVED"));
        }
    }
}
