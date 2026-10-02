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
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("PracticeSetController & Session Lifecycle Testcontainers Integration Tests")
class PracticeSetControllerIntegrationTest extends AbstractIntegrationTest {

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
    private static final UUID CREATOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private PracticeSet publishedSet;
    private PracticeSet draftSet;
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

            publishedSet = PracticeSet.builder()
                    .name("National Science Practice Test")
                    .description("Physics & Chemistry mock test")
                    .durationMinutes(30)
                    .subjectSlug("science")
                    .source("EXAM_CLONE")
                    .published(true)
                    .totalQuestions(2)
                    .questionIds("[\"" + question1 + "\",\"" + question2 + "\"]")
                    .createdBy(CREATOR_ID)
                    .build();
            publishedSet.setTenantId(TENANT_ID);
            publishedSet = practiceSetRepository.save(publishedSet);

            draftSet = PracticeSet.builder()
                    .name("Draft Biology Test")
                    .description("Unpublished draft")
                    .durationMinutes(20)
                    .subjectSlug("biology")
                    .source("MANUAL")
                    .published(false)
                    .totalQuestions(10)
                    .questionIds("[]")
                    .createdBy(CREATOR_ID)
                    .build();
            draftSet.setTenantId(TENANT_ID);
            draftSet = practiceSetRepository.save(draftSet);
        }
    }

    @Nested
    @DisplayName("GET /api/practice/sets")
    class GetPracticeSets {

        @Test
        @DisplayName("+ve: CANDIDATE retrieves only published practice sets - returns 200 OK")
        void candidateCanRetrievePublishedSets() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(get("/api/practice/sets")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("National Science Practice Test"))
                    .andExpect(jsonPath("$[0].source").value("EXAM_CLONE"))
                    .andExpect(jsonPath("$[0].totalQuestions").value(2));
        }

        @Test
        @DisplayName("+ve: CANDIDATE retrieves single published set by ID - returns 200 OK")
        void candidateCanRetrieveSingleSet() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(get("/api/practice/sets/{setId}", publishedSet.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(publishedSet.getId().toString()))
                    .andExpect(jsonPath("$.name").value("National Science Practice Test"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE accessing unpublished draft set returns 404 Not Found")
        void unpublishedSetReturnsNotFound() throws Exception {
            if (!testcontainersAvailable) return;

            mockMvc.perform(get("/api/practice/sets/{setId}", draftSet.getId())
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Practice Session Full Lifecycle (Start -> Save Response -> Submit -> Get Result)")
    class SessionLifecycle {

        @Test
        @DisplayName("+ve: Candidate completes end-to-end practice flow with auto-evaluation")
        void candidateCompletesPracticeLifecycle() throws Exception {
            if (!testcontainersAvailable) return;

            // 1. Start Session
            StartSessionRequest startReq = new StartSessionRequest(publishedSet.getId(), "TIMED");
            MvcResult startResult = mockMvc.perform(post("/api/practice/sessions")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(startReq)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.practiceSetId").value(publishedSet.getId().toString()))
                    .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                    .andReturn();

            JsonNode sessionNode = objectMapper.readTree(startResult.getResponse().getContentAsString());
            UUID sessionId = UUID.fromString(sessionNode.get("id").asText());

            // 2. Save Response for Question 1
            SaveResponseRequest respReq1 = new SaveResponseRequest(
                    question1, "[\"opt-A\"]", null, 15000L, false
            );
            mockMvc.perform(put("/api/practice/sessions/{sessionId}/response", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(respReq1)))
                    .andExpect(status().isNoContent());

            // 3. Mock QuestionBank Answer Keys for evaluation
            AnswerKeyDto ak1 = new AnswerKeyDto(question1, "opt-A", "MCQ", "t-1", "Mechanics", "EASY", 4);
            AnswerKeyDto ak2 = new AnswerKeyDto(question2, "opt-B", "MCQ", "t-2", "Optics", "MEDIUM", 4);
            when(questionBankClient.getAnswerKeys(any())).thenReturn(Map.of(question1, ak1, question2, ak2));

            // 4. Submit Session
            mockMvc.perform(post("/api/practice/sessions/{sessionId}/submit", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.correctCount").value(1))
                    .andExpect(jsonPath("$.skippedCount").value(1))
                    .andExpect(jsonPath("$.obtainedMarks").value(4));

            // 5. Retrieve Result
            mockMvc.perform(get("/api/practice/sessions/{sessionId}/result", sessionId)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(sessionId.toString()))
                    .andExpect(jsonPath("$.correctCount").value(1))
                    .andExpect(jsonPath("$.obtainedMarks").value(4));
        }
    }
}
