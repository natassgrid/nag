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

import com.examplatform.result.domain.Result;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.result.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ScorecardController REST Endpoints E2E Tests (MockMvc)")
class ScorecardControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private ResultRepository resultRepository;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Nested
    @DisplayName("GET /api/v1/results/{candidateId}/scorecard")
    class DownloadScorecardEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE downloads scorecard - returns 200 OK with PDF")
        void candidateCanDownloadScorecard(@TempDir Path tempDir) throws Exception {
            Path pdfPath = tempDir.resolve("scorecard.pdf");
            Files.write(pdfPath, "%PDF-1.4 test scorecard content".getBytes());

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef(pdfPath.toAbsolutePath().toString())
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scorecard-" + CANDIDATE_ID + ".pdf\""));
        }

        @Test
        @DisplayName("+ve: ADMIN downloads scorecard - returns 200 OK with PDF")
        void adminCanDownloadScorecard(@TempDir Path tempDir) throws Exception {
            Path pdfPath = tempDir.resolve("scorecard.pdf");
            Files.write(pdfPath, "%PDF-1.4 admin download".getBytes());

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef(pdfPath.toAbsolutePath().toString())
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))
                                    .jwt(j -> j.subject(UUID.randomUUID().toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF));
        }

        @Test
        @DisplayName("-ve: Candidate result not found returns 404 Not Found")
        void resultNotFoundReturns404() throws Exception {
            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("-ve: Scorecard file not on disk returns 404 Not Found")
        void scorecardFileMissingReturns404() throws Exception {
            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef("nonexistent/path/scorecard.pdf")
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }

        @Test
        @DisplayName("-ve: Unauthorized role (ROLE_PROCTOR) returns 403 Forbidden")
        void proctorForbiddenFromDownloading() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_PROCTOR"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("-ve: Unauthenticated request returns 401 Unauthorized")
        void unauthenticatedReturnsUnauthorized() throws Exception {
            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID))
                    .andExpect(status().isUnauthorized());
        }
    }
}
