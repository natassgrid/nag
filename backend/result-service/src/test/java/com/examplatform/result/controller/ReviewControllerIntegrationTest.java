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

package com.examplatform.result.controller;

import com.examplatform.result.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ReviewController REST Endpoints E2E Tests (MockMvc)")
class ReviewControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID EXAM_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("GET /api/v1/results/{candidateId}/review - Candidate Exam Review")
    class GetExamReviewEndpoint {

        @Test
        @DisplayName("200 OK - CANDIDATE retrieves full post-exam review with question breakdown and solutions")
        void candidateCanGetExamReview() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()))
                    .andExpect(jsonPath("$.questions[0].questionNumber").value(1))
                    .andExpect(jsonPath("$.questions[0].subject").value("Physics"))
                    .andExpect(jsonPath("$.questions[0].topic").value("Thermodynamics"))
                    .andExpect(jsonPath("$.questions[0].marksAwarded").value(-0.25))
                    .andExpect(jsonPath("$.questions[0].options.length()").value(4));
        }

        @Test
        @DisplayName("200 OK - EXAM_CONTROLLER retrieves post-exam review")
        void examControllerCanGetExamReview() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()));
        }

        @Test
        @DisplayName("403 FORBIDDEN - ROLE_PROCTOR cannot access exam review")
        void proctorForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString())
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("401 UNAUTHORIZED - Unauthenticated request returns 401")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/review", CANDIDATE_ID)
                            .param("examId", EXAM_ID.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/results/verify - QR Code Scorecard Verification")
    class VerifyScorecardEndpoint {

        @Test
        @DisplayName("200 OK - Public QR verification returns scorecard status")
        void publicVerifyEndpoint() throws Exception {
            String qrCode = "qr-token-abc-123";

            mockMvc.perform(get("/api/v1/results/verify")
                            .param("code", qrCode))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(qrCode))
                    .andExpect(jsonPath("$.valid").exists())
                    .andExpect(jsonPath("$.message").exists());
        }
    }
}
