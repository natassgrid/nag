/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.evaluation.controller;

import com.examplatform.evaluation.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("EvaluationAnalyticsController Integration Tests (MockMvc)")
class EvaluationAnalyticsControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/v1/evaluation/analytics/summary returns evaluation queue counts")
    void getAnalyticsSummarySuccess() throws Exception {
        mockMvc.perform(get("/api/v1/evaluation/analytics/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("11111111-1111-1111-1111-111111111111").claim("tenant_id", "default")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pending").exists())
                .andExpect(jsonPath("$.inProgress").exists())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.flagged").exists());
    }
}
