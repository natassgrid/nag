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

package com.examplatform.analytics.controller;

import com.examplatform.analytics.domain.ExamAnalytics;
import com.examplatform.analytics.service.AnalyticsService;
import com.examplatform.analytics.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AnalyticsController REST Endpoints E2E Tests (MockMvc)")
class AnalyticsControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private AnalyticsService analyticsService;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID EXAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("GET /api/v1/analytics/exams/{id}")
    class GetExamAnalyticsEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER retrieves analytics - returns 200 OK")
        void examControllerCanGetAnalytics() throws Exception {
            ExamAnalytics analytics = ExamAnalytics.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .totalRegistered(1000L)
                    .totalAppeared(950L)
                    .top10PercentileThreshold(BigDecimal.valueOf(92.50))
                    .bottom10PercentileThreshold(BigDecimal.valueOf(35.00))
                    .computedAt(Instant.now())
                    .build();

            when(analyticsService.getAnalyticsForExam(eq(EXAM_ID))).thenReturn(analytics);

            mockMvc.perform(get("/api/v1/analytics/exams/{id}", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.examId").value(EXAM_ID.toString()))
                    .andExpect(jsonPath("$.totalRegistered").value(1000))
                    .andExpect(jsonPath("$.totalAppeared").value(950));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN retrieves analytics - returns 200 OK")
        void superAdminCanGetAnalytics() throws Exception {
            ExamAnalytics analytics = ExamAnalytics.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .totalRegistered(500L)
                    .totalAppeared(480L)
                    .computedAt(Instant.now())
                    .build();

            when(analyticsService.getAnalyticsForExam(eq(EXAM_ID))).thenReturn(analytics);

            mockMvc.perform(get("/api/v1/analytics/exams/{id}", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRegistered").value(500));
        }

        @Test
        @DisplayName("-ve: Nonexistent exam analytics returns 404 Not Found")
        void notFoundReturns404() throws Exception {
            when(analyticsService.getAnalyticsForExam(eq(EXAM_ID)))
                    .thenThrow(new IllegalArgumentException("Analytics not found for exam: " + EXAM_ID));

            mockMvc.perform(get("/api/v1/analytics/exams/{id}", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_CANDIDATE) returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/exams/{id}", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/exams/{id}", EXAM_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/analytics/exams/{id}/export")
    class ExportExamAnalyticsEndpoint {

        @Test
        @DisplayName("+ve: EXAM_CONTROLLER exports CSV - returns 200 OK with text/csv")
        void exportCsvReturnsOk() throws Exception {
            byte[] csvBytes = "metric,value\ntotal_appeared,950\n".getBytes();
            when(analyticsService.exportAnalytics(eq(EXAM_ID), eq("csv"))).thenReturn(csvBytes);

            mockMvc.perform(get("/api/v1/analytics/exams/{id}/export", EXAM_ID)
                            .param("format", "csv")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EXAM_CONTROLLER"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_" + EXAM_ID + ".csv"));
        }

        @Test
        @DisplayName("+ve: SUPER_ADMIN exports PDF - returns 200 OK with application/pdf")
        void exportPdfReturnsOk() throws Exception {
            byte[] pdfBytes = "%PDF-1.4 sample".getBytes();
            when(analyticsService.exportAnalytics(eq(EXAM_ID), eq("pdf"))).thenReturn(pdfBytes);

            mockMvc.perform(get("/api/v1/analytics/exams/{id}/export", EXAM_ID)
                            .param("format", "pdf")
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=analytics_" + EXAM_ID + ".pdf"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_PROCTOR) returns 403 Forbidden")
        void proctorForbidden() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/exams/{id}/export", EXAM_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/analytics/exams/{id}/export", EXAM_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
