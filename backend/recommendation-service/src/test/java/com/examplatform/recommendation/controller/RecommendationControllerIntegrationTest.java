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

package com.examplatform.recommendation.controller;

import com.examplatform.recommendation.dto.LearnerProfileDto;
import com.examplatform.recommendation.dto.RecommendationDto;
import com.examplatform.recommendation.service.LearnerProfileService;
import com.examplatform.recommendation.service.RecommendationService;
import com.examplatform.recommendation.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("RecommendationController REST Endpoints E2E Tests (MockMvc)")
class RecommendationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private LearnerProfileService learnerProfileService;

    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RECOMMENDATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private RecommendationDto buildSampleRecommendation() {
        return new RecommendationDto(
                RECOMMENDATION_ID,
                CANDIDATE_ID,
                UUID.randomUUID(),
                "ACTIVE",
                Instant.now(),
                "Focus on Quantitative Aptitude - Number Systems",
                "Daily 30m practice on LCM and HCF",
                "[\"ps-1\", \"ps-2\"]",
                "Great work! Keep practicing to improve accuracy."
        );
    }

    @Nested
    @DisplayName("GET /api/recommendations/latest - Get Latest AI Recommendation")
    class GetLatestEndpoint {

        @Test
        @DisplayName("200 OK - Authenticated candidate retrieves their latest recommendation")
        void candidateCanGetLatestRecommendation() throws Exception {
            RecommendationDto dto = buildSampleRecommendation();
            when(recommendationService.getLatest(eq(CANDIDATE_ID))).thenReturn(Optional.of(dto));

            mockMvc.perform(get("/api/recommendations/latest")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(RECOMMENDATION_ID.toString()))
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"))
                    .andExpect(jsonPath("$.weakTopicRecommendations").value("Focus on Quantitative Aptitude - Number Systems"));
        }

        @Test
        @DisplayName("204 NO CONTENT - When no recommendations exist yet for candidate")
        void noContentWhenEmpty() throws Exception {
            when(recommendationService.getLatest(eq(CANDIDATE_ID))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/recommendations/latest")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("401 UNAUTHORIZED - Unauthenticated request is rejected")
        void unauthenticatedReturns401() throws Exception {
            mockMvc.perform(get("/api/recommendations/latest"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/history - Recommendation History")
    class GetHistoryEndpoint {

        @Test
        @DisplayName("200 OK - Candidate retrieves paginated recommendation history")
        void candidateCanGetHistory() throws Exception {
            RecommendationDto dto = buildSampleRecommendation();
            when(recommendationService.getHistory(eq(CANDIDATE_ID), any(PageRequest.class)))
                    .thenReturn(new PageImpl<>(List.of(dto)));

            mockMvc.perform(get("/api/recommendations/history")
                            .param("page", "0")
                            .param("size", "10")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(RECOMMENDATION_ID.toString()))
                    .andExpect(jsonPath("$.content[0].candidateId").value(CANDIDATE_ID.toString()));
        }
    }

    @Nested
    @DisplayName("POST /api/recommendations/{id}/dismiss - Dismiss Recommendation")
    class DismissEndpoint {

        @Test
        @DisplayName("204 NO CONTENT - Candidate dismisses recommendation")
        void candidateCanDismiss() throws Exception {
            doNothing().when(recommendationService).dismiss(eq(RECOMMENDATION_ID), eq(CANDIDATE_ID));

            mockMvc.perform(post("/api/recommendations/{id}/dismiss", RECOMMENDATION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("GET /api/recommendations/learner-profile - Learner Profile Analytics")
    class LearnerProfileEndpoint {

        @Test
        @DisplayName("200 OK - Candidate retrieves their personalized learner profile")
        void candidateCanGetLearnerProfile() throws Exception {
            LearnerProfileDto profile = new LearnerProfileDto(
                    CANDIDATE_ID,
                    15,
                    78.5,
                    "[\"Permutation & Combination\", \"Trigonometry\"]",
                    "[\"Modern History\", \"Indian Polity\"]",
                    "{\"Quant\": 65.0, \"General Studies\": 88.0}"
            );

            when(learnerProfileService.getProfile(eq(CANDIDATE_ID))).thenReturn(profile);

            mockMvc.perform(get("/api/recommendations/learner-profile")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.totalPracticeSessions").value(15))
                    .andExpect(jsonPath("$.overallAccuracy").value(78.5))
                    .andExpect(jsonPath("$.weakTopics").value("[\"Permutation & Combination\", \"Trigonometry\"]"));
        }
    }
}
