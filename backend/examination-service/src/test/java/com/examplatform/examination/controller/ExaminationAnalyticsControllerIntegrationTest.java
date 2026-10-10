/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 */

package com.examplatform.examination.controller;

import com.examplatform.examination.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examplatform.examination.service.ExaminationService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Map;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("ExaminationAnalyticsController Integration Tests (MockMvc)")
class ExaminationAnalyticsControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ExaminationService examinationService;

    @Test
    @DisplayName("GET /api/v1/examinations/analytics/summary returns status breakdown")
    void getAnalyticsSummarySuccess() throws Exception {
        when(examinationService.getExaminationStatusBreakdown(anyString())).thenReturn(Map.of(
                "scheduled", 1L,
                "liveInProgress", 2L,
                "completed", 3L,
                "cancelled", 0L
        ));

        mockMvc.perform(get("/api/v1/examinations/analytics/summary")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                .jwt(j -> j.subject("11111111-1111-1111-1111-111111111111").claim("tenant_id", "default")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduled").exists())
                .andExpect(jsonPath("$.liveInProgress").exists())
                .andExpect(jsonPath("$.completed").exists())
                .andExpect(jsonPath("$.cancelled").exists());
    }
}
