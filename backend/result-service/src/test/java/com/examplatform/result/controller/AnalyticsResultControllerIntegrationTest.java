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

package com.examplatform.result.controller;

import com.examplatform.result.dto.QuestionAnalyticsResult;
import com.examplatform.result.service.QuestionAnalyticsService;
import com.examplatform.result.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AnalyticsResultController REST Endpoints E2E Tests (MockMvc)")
class AnalyticsResultControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private QuestionAnalyticsService questionAnalyticsService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID QUESTION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("GET /api/v1/results/analytics/exam/{examId}")
    class GetExamAnalyticsEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves analytics - returns 200 OK")
        void examControllerCanRetrieveAnalytics() throws Exception {
            QuestionAnalyticsResult analytics = QuestionAnalyticsResult.builder()
                    .questionId(QUESTION_ID)
                    .difficultyIndex(0.65)
                    .discriminationIndex(0.42)
                    .responseDistribution(Map.of("A", 15, "B", 60, "C", 20, "D", 5))
                    .build();

            when(questionAnalyticsService.computeAnalytics(eq(EXAM_ID), anyString()))
                    .thenReturn(List.of(analytics));

            mockMvc.perform(get("/api/v1/results/analytics/exam/{examId}", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].questionId").value(QUESTION_ID.toString()))
                    .andExpect(jsonPath("$[0].difficultyIndex").value(0.65))
                    .andExpect(jsonPath("$[0].discriminationIndex").value(0.42))
                    .andExpect(jsonPath("$[0].responseDistribution.B").value(60));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbiddenFromRetrievingAnalytics() throws Exception {
            mockMvc.perform(get("/api/v1/results/analytics/exam/{examId}", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/results/analytics/exam/{examId}", EXAM_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
