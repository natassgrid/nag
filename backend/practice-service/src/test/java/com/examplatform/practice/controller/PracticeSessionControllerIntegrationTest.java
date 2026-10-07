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
 */

package com.examplatform.practice.controller;

import com.examplatform.practice.client.QuestionBankClient;
import com.examplatform.practice.domain.PracticeSet;
import com.examplatform.practice.dto.AnswerKeyDto;
import com.examplatform.practice.dto.SaveResponseRequest;
import com.examplatform.practice.dto.StartSessionRequest;
import com.examplatform.practice.repository.PracticeResponseRepository;
import com.examplatform.practice.repository.PracticeSessionRepository;
import com.examplatform.practice.repository.PracticeSetRepository;
import com.examplatform.practice.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PracticeSessionController Integration Tests")
class PracticeSessionControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PracticeSetRepository practiceSetRepository;

    @Autowired
    private PracticeSessionRepository practiceSessionRepository;

    @Autowired
    private PracticeResponseRepository practiceResponseRepository;

    @Autowired
    private QuestionBankClient questionBankClient;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID CANDIDATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID OTHER_CANDIDATE_ID = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private PracticeSet practiceSet;
    private UUID question1;
    private UUID question2;

    @BeforeEach
    void setUp() {
        if (testcontainersAvailable) {
            practiceResponseRepository.deleteAll();
            practiceSessionRepository.deleteAll();
            practiceSetRepository.deleteAll();

            question1 = UUID.randomUUID();
            question2 = UUID.randomUUID();

            practiceSet = PracticeSet.builder()
                    .name("GATE CS Mock Assessment")
                    .description("Full Practice Set on Data Structures & Algorithms")
                    .durationMinutes(45)
                    .subjectSlug("cs")
                    .source("EXAM_CLONE")
                    .published(true)
                    .totalQuestions(2)
                    .questionIds("[\"" + question1 + "\",\"" + question2 + "\"]")
                    .createdBy(CREATOR_ID)
                    .build();
            practiceSet.setTenantId(TENANT_ID);
            practiceSet = practiceSetRepository.save(practiceSet);
        }
    }

    @Nested
    @DisplayName("POST /api/practice/sessions - Start Session")
    class StartSessionEndpoint {

        @Test
        @DisplayName("+ve: Candidate successfully initiates practice session")
        void startSessionSuccess() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest request = new StartSessionRequest(practiceSet.getId(), "TIMED");

            mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNotEmpty()
                    )
                    .andExpect(jsonPath("$.practiceSetId").value(practiceSet.getId().toString()))
                    .andExpect(jsonPath("$.mode").value("TIMED"))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("-ve: Attempting to start session with non-existent practice set returns 404")
        void startSessionNonExistentSet() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest request = new StartSessionRequest(UUID.randomUUID(), "TIMED");

            mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void startSessionUnauthenticated() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest request = new StartSessionRequest(practiceSet.getId(), "TIMED");

            mockMvc.perform(post("/api/practice/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/practice/sessions/{sessionId} & /questions")
    class GetSessionEndpoint {

        @Test
        @DisplayName("+ve: Candidate retrieves active session details")
        void getSessionSuccess() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest startReq = new StartSessionRequest(practiceSet.getId(), "TIMED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID sessionId = UUID.fromString(
                    objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText()
            );

            mockMvc.perform(get("/api/practice/sessions/{sessionId}", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(sessionId.toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        }

        @Test
        @DisplayName("+ve: Candidate retrieves session questions")
        void getSessionQuestionsSuccess() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest startReq = new StartSessionRequest(practiceSet.getId(), "TIMED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID sessionId = UUID.fromString(
                    objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText()
            );

            AnswerKeyDto ak1 = new AnswerKeyDto(
                    question1, "opt-A", "MCQ", "t-1", "Algorithms", "EASY", 2,
                    "What is O(1)?", "[{\"id\":\"opt-A\",\"text\":\"Constant\"}]",
                    "Explanation", "CS"
            );
            when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of(question1, ak1));

            mockMvc.perform(get("/api/practice/sessions/{sessionId}/questions", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(question1.toString()))
                    .andExpect(jsonPath("$[0].content").value("What is O(1)?"));
        }

        @Test
        @DisplayName("+ve: Candidate retrieves session questions falling back to subject when question_ids is empty")
        void getSessionQuestionsFallsBackToSubjectWhenQuestionIdsEmpty() throws Exception {
            if (!testcontainersAvailable) return;

            PracticeSet emptySet = PracticeSet.builder()
                    .name("Quantitative Aptitude Practice")
                    .description("Test Practice Set with dynamic questions")
                    .durationMinutes(30)
                    .subjectSlug("quantitative-aptitude")
                    .source("MANUAL")
                    .published(true)
                    .totalQuestions(1)
                    .questionIds("[]")
                    .createdBy(CREATOR_ID)
                    .build();
            emptySet.setTenantId(TENANT_ID);
            emptySet = practiceSetRepository.save(emptySet);

            StartSessionRequest startReq = new StartSessionRequest(emptySet.getId(), "TIMED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID sessionId = UUID.fromString(
                    objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText()
            );

            UUID fallbackQuestionId = UUID.randomUUID();
            when(questionBankClient.findQuestionIdsBySubject(eq("quantitative-aptitude"), anyInt()))
                    .thenReturn(List.of(fallbackQuestionId));

            AnswerKeyDto ak = new AnswerKeyDto(
                    fallbackQuestionId, "opt-B", "MCQ", "t-2", "Algebra", "MEDIUM", 2,
                    "Solve for x", "[{\"id\":\"opt-B\",\"text\":\"x=5\"}]",
                    "Explanation", "Quantitative Aptitude"
            );
            when(questionBankClient.getAnswerKeys(List.of(fallbackQuestionId)))
                    .thenReturn(Map.of(fallbackQuestionId, ak));

            mockMvc.perform(get("/api/practice/sessions/{sessionId}/questions", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(fallbackQuestionId.toString()))
                    .andExpect(jsonPath("$[0].content").value("Solve for x"));
        }

        @Test
        @DisplayName("-ve: Other candidate cannot retrieve someone else's session")
        void getSessionForbiddenForOtherCandidate() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest startReq = new StartSessionRequest(practiceSet.getId(), "TIMED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID sessionId = UUID.fromString(
                    objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText()
            );

            mockMvc.perform(get("/api/practice/sessions/{sessionId}", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(OTHER_CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/practice/sessions/{sessionId}/response - Save Responses")
    class SaveResponseEndpoint {

        @Test
        @DisplayName("+ve: Candidate saves and updates answers in practice mode")
        void saveAndUpdateResponse() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest startReq = new StartSessionRequest(practiceSet.getId(), "UNTYPED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID sessionId = UUID.fromString(
                    objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText()
            );

            // First answer save
            SaveResponseRequest resp1 = new SaveResponseRequest(question1, "[\"opt-A\"]", null, 12000L, false);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resp1)))
                    .andExpect(status().isNoContent());

            // Update same question answer
            SaveResponseRequest resp1Updated = new SaveResponseRequest(question1, "[\"opt-B\"]", null, 18000L, true);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resp1Updated)))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("POST /api/practice/sessions/{sessionId}/submit & GET /result & GET /history")
    class SubmissionAndResultLifecycle {

        @Test
        @DisplayName("+ve: Candidate submits practice test and receives immediate evaluated scorecard")
        void submitSessionAndRetrieveDetailedScorecard() throws Exception {
            if (!testcontainersAvailable) return;

            // 1. Start Session
            StartSessionRequest startReq = new StartSessionRequest(practiceSet.getId(), "TIMED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID sessionId = UUID.fromString(
                    objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText()
            );

            // 2. Answer question 1 correctly (opt-A) and question 2 incorrectly (opt-C when key is opt-B)
            SaveResponseRequest resp1 = new SaveResponseRequest(question1, "[\"opt-A\"]", null, 10000L, false);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resp1)))
                    .andExpect(status().isNoContent());

            SaveResponseRequest resp2 = new SaveResponseRequest(question2, "[\"opt-C\"]", null, 25000L, false);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(resp2)))
                    .andExpect(status().isNoContent());

            // 3. Mock QuestionBank answer keys with rich content & explanations
            AnswerKeyDto ak1 = new AnswerKeyDto(
                    question1, "opt-A", "MCQ", "t-1", "Algorithms", "EASY", 2,
                    "What is O(1)?", "[{\"id\":\"opt-A\",\"text\":\"Constant\"},{\"id\":\"opt-B\",\"text\":\"Linear\"}]",
                    "Constant time runs in fixed steps.", "CS"
            );
            AnswerKeyDto ak2 = new AnswerKeyDto(
                    question2, "opt-B", "MCQ", "t-2", "Data Structures", "MEDIUM", 2,
                    "What is a Stack?", "[{\"id\":\"opt-A\",\"text\":\"FIFO\"},{\"id\":\"opt-B\",\"text\":\"LIFO\"},{\"id\":\"opt-C\",\"text\":\"Random\"}]",
                    "Stack is a Last In First Out (LIFO) structure.", "CS"
            );
            when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of(question1, ak1, question2, ak2));

            // 4. Submit session
            mockMvc.perform(post("/api/practice/sessions/{sessionId}/submit", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.correctCount").value(1))
                    .andExpect(jsonPath("$.incorrectCount").value(1))
                    .andExpect(jsonPath("$.skippedCount").value(0))
                    .andExpect(jsonPath("$.obtainedMarks").value(1)) // +2 for Q1, -1 for Q2 = 1
                    .andExpect(jsonPath("$.accuracyPercent").value(50.0))
                    .andExpect(jsonPath("$.questionResults.length()").value(2))
                    .andExpect(jsonPath("$.practiceSetName").value("GATE CS Mock Assessment"));

            // 5. GET /result returns full question review
            mockMvc.perform(get("/api/practice/sessions/{sessionId}/result", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.correctCount").value(1))
                    .andExpect(jsonPath("$.questionResults[0].explanation").isNotEmpty())
                    .andExpect(jsonPath("$.practiceSetName").value("GATE CS Mock Assessment"));

            // 6. GET /history contains submitted attempt
            mockMvc.perform(get("/api/practice/history")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.content[0].practiceSetName").value("GATE CS Mock Assessment"))
                    .andExpect(jsonPath("$.content[0].correctCount").value(1))
                    .andExpect(jsonPath("$.content[0].accuracyPercent").value(50.0));
        }

        @Test
        @DisplayName("-ve: Attempting to submit already completed session returns 409 Conflict")
        void submitAlreadySubmittedSession() throws Exception {
            if (!testcontainersAvailable) return;

            StartSessionRequest startReq = new StartSessionRequest(practiceSet.getId(), "TIMED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andReturn();

            UUID sessionId = UUID.fromString(
                    objectMapper.readTree(startResult.getResponse().getContentAsString()).get("id").asText()
            );

            when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of());

            // First submission
            mockMvc.perform(post("/api/practice/sessions/{sessionId}/submit", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk());

            // Second submission attempt
            mockMvc.perform(post("/api/practice/sessions/{sessionId}/submit", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("+ve: Multi-attempt tracking records timestamps and distinct scorecards across multiple attempts")
        void multiAttemptHandlingAndTimestampRecording() throws Exception {
            if (!testcontainersAvailable) return;

            AnswerKeyDto ak1 = new AnswerKeyDto(
                    question1, "opt-A", "MCQ", "t-1", "Algorithms", "EASY", 2,
                    "Question 1", "[{\"id\":\"opt-A\",\"text\":\"A\"}]", "Exp", "CS"
            );
            AnswerKeyDto ak2 = new AnswerKeyDto(
                    question2, "opt-B", "MCQ", "t-2", "Data Structures", "MEDIUM", 2,
                    "Question 2", "[{\"id\":\"opt-B\",\"text\":\"B\"}]", "Exp", "CS"
            );
            when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of(question1, ak1, question2, ak2));

            // Attempt 1: Start, answer Q1 correctly, Q2 skipped, submit
            StartSessionRequest start1 = new StartSessionRequest(practiceSet.getId(), "TIMED");
            MvcResult res1 = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(start1)))
                    .andExpect(status().isCreated())
                    .andReturn();
            UUID session1Id = UUID.fromString(objectMapper.readTree(res1.getResponse().getContentAsString()).get("id").asText());

            SaveResponseRequest respA1 = new SaveResponseRequest(question1, "[\"opt-A\"]", null, 8000L, false);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", session1Id)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(respA1)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/practice/sessions/{sessionId}/submit", session1Id)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correctCount").value(1))
                    .andExpect(jsonPath("$.skippedCount").value(1));

            // Attempt 2: Start new attempt for same practice set, answer both Q1 and Q2 correctly
            StartSessionRequest start2 = new StartSessionRequest(practiceSet.getId(), "TIMED");
            MvcResult res2 = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(start2)))
                    .andExpect(status().isCreated())
                    .andReturn();
            UUID session2Id = UUID.fromString(objectMapper.readTree(res2.getResponse().getContentAsString()).get("id").asText());

            SaveResponseRequest respA2_1 = new SaveResponseRequest(question1, "[\"opt-A\"]", null, 6000L, false);
            SaveResponseRequest respA2_2 = new SaveResponseRequest(question2, "[\"opt-B\"]", null, 9000L, false);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", session2Id)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(respA2_1)))
                    .andExpect(status().isNoContent());
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", session2Id)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(respA2_2)))
                    .andExpect(status().isNoContent());

            mockMvc.perform(post("/api/practice/sessions/{sessionId}/submit", session2Id)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correctCount").value(2))
                    .andExpect(jsonPath("$.accuracyPercent").value(100.0));

            // Verify History lists both attempts in descending order with timestamps
            mockMvc.perform(get("/api/practice/history")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].sessionId").value(session2Id.toString()))
                    .andExpect(jsonPath("$.content[0].accuracyPercent").value(100.0))
                    .andExpect(jsonPath("$.content[0].submittedAt").isNotEmpty())
                    .andExpect(jsonPath("$.content[1].sessionId").value(session1Id.toString()))
                    .andExpect(jsonPath("$.content[1].accuracyPercent").value(50.0))
                    .andExpect(jsonPath("$.content[1].submittedAt").isNotEmpty());
        }

        @Test
        @DisplayName("+ve: Evaluation edge cases cover numerical tolerance, multi-MCQ set match, and flagged breakdown")
        void evaluationEdgeCasesAndFlaggedStatusBreakdown() throws Exception {
            if (!testcontainersAvailable) return;

            UUID numQ = UUID.randomUUID();
            UUID multiQ = UUID.randomUUID();

            PracticeSet advancedSet = PracticeSet.builder()
                    .name("STEM Advanced Practice")
                    .description("Advanced numerical and multi-select practice")
                    .durationMinutes(30)
                    .subjectSlug("stem")
                    .source("MANUAL")
                    .published(true)
                    .totalQuestions(2)
                    .questionIds("[\"" + numQ + "\",\"" + multiQ + "\"]")
                    .createdBy(CREATOR_ID)
                    .build();
            advancedSet.setTenantId(TENANT_ID);
            advancedSet = practiceSetRepository.save(advancedSet);

            StartSessionRequest start = new StartSessionRequest(advancedSet.getId(), "TIMED");
            MvcResult res = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(start)))
                    .andExpect(status().isCreated())
                    .andReturn();
            UUID sessionId = UUID.fromString(objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asText());

            // Save numerical response with decimal equivalent (3.140 for key 3.14) and flag for review
            SaveResponseRequest respNum = new SaveResponseRequest(numQ, null, "3.140", 15000L, true);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(respNum)))
                    .andExpect(status().isNoContent());

            // Save multi-MCQ response with order variation (["opt-B", "opt-A"] for key ["opt-A", "opt-B"])
            SaveResponseRequest respMulti = new SaveResponseRequest(multiQ, "[\"opt-B\", \"opt-A\"]", null, 22000L, false);
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(respMulti)))
                    .andExpect(status().isNoContent());

            AnswerKeyDto akNum = new AnswerKeyDto(
                    numQ, "3.14", "NUMERICAL", "t-math", "Calculus", "MEDIUM", 3,
                    "Approximate Pi to 2 decimal places", null, "Pi is approx 3.14", "Math"
            );
            AnswerKeyDto akMulti = new AnswerKeyDto(
                    multiQ, "[\"opt-A\", \"opt-B\"]", "MULTI_MCQ", "t-algo", "Algorithms", "HARD", 4,
                    "Select all comparison-based sorting algorithms",
                    "[{\"id\":\"opt-A\",\"text\":\"QuickSort\"},{\"id\":\"opt-B\",\"text\":\"MergeSort\"},{\"id\":\"opt-C\",\"text\":\"RadixSort\"}]",
                    "QuickSort and MergeSort are comparison-based.", "CS"
            );
            when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of(numQ, akNum, multiQ, akMulti));

            // Submit session and inspect result
            mockMvc.perform(post("/api/practice/sessions/{sessionId}/submit", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correctCount").value(2))
                    .andExpect(jsonPath("$.obtainedMarks").value(7)) // 3 + 4 = 7
                    .andExpect(jsonPath("$.flaggedCount").value(1))
                    .andExpect(jsonPath("$.questionResults[0].markedForReview").value(true));
        }
    }
}
