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

package com.examplatform.delivery.controller;

import com.examplatform.delivery.dto.NavigationRequest;
import com.examplatform.delivery.dto.NavigationResponse;
import com.examplatform.delivery.dto.NavigationResponse.NavigationAction;
import com.examplatform.delivery.dto.NavigationResponse.NavigationPolicy;
import com.examplatform.delivery.exception.NavigationPolicyViolationException;
import com.examplatform.delivery.service.NavigationService;
import com.examplatform.delivery.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("NavigationController REST Endpoints E2E Tests (MockMvc)")
class NavigationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private NavigationService navigationService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Nested
    @DisplayName("POST /api/v1/sessions/{sessionId}/navigate")
    class NavigateEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE navigates to target question index - returns 200 OK")
        void candidateCanNavigateToQuestion() throws Exception {
            NavigationRequest request = NavigationRequest.builder()
                    .sessionId(SESSION_ID)
                    .targetQuestionIndex(3)
                    .targetSectionIndex(0)
                    .build();

            NavigationResponse response = NavigationResponse.builder()
                    .sessionId(SESSION_ID)
                    .currentQuestionIndex(3)
                    .questionContent("{\"text\":\"Question 4\"}")
                    .navigationPolicy(NavigationPolicy.FLEXIBLE)
                    .allowedActions(List.of(NavigationAction.NEXT, NavigationAction.PREV, NavigationAction.JUMP))
                    .totalQuestions(25)
                    .build();

            when(navigationService.navigate(any(NavigationRequest.class), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/navigate", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sessionId").value(SESSION_ID.toString()))
                    .andExpect(jsonPath("$.currentQuestionIndex").value(3))
                    .andExpect(jsonPath("$.navigationPolicy").value("FLEXIBLE"))
                    .andExpect(jsonPath("$.totalQuestions").value(25))
                    .andExpect(jsonPath("$.allowedActions.length()").value(3));
        }

        @Test
        @DisplayName("-ve: Navigation policy violation throws NavigationPolicyViolationException - returns 422 Unprocessable Entity")
        void navigationViolationReturnsUnprocessableEntity() throws Exception {
            NavigationRequest request = NavigationRequest.builder()
                    .sessionId(SESSION_ID)
                    .targetQuestionIndex(10)
                    .build();

            when(navigationService.navigate(any(NavigationRequest.class), eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenThrow(new NavigationPolicyViolationException("Cannot jump forward in SEQUENTIAL navigation mode"));

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/navigate", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error.code").value("NAVIGATION_POLICY_VIOLATION"));
        }

        @Test
        @DisplayName("-ve: Missing sessionId in request body returns 400 Bad Request")
        void missingSessionIdReturnsBadRequest() throws Exception {
            NavigationRequest request = NavigationRequest.builder()
                    .sessionId(null)
                    .targetQuestionIndex(2)
                    .build();

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/navigate", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("-ve: Non-CANDIDATE role returns 403 Forbidden")
        void nonCandidateForbiddenFromNavigation() throws Exception {
            NavigationRequest request = NavigationRequest.builder()
                    .sessionId(SESSION_ID)
                    .targetQuestionIndex(1)
                    .build();

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/navigate", SESSION_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            NavigationRequest request = NavigationRequest.builder()
                    .sessionId(SESSION_ID)
                    .targetQuestionIndex(1)
                    .build();

            mockMvc.perform(post("/api/v1/sessions/{sessionId}/navigate", SESSION_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
