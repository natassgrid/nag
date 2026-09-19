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
import com.examplatform.result.storage.ScorecardStorageProvider;
import com.examplatform.result.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

    @MockitoBean
    private ScorecardStorageProvider scorecardStorageProvider;

    private static final String TENANT_ID = "tenant-test";
    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID RESULT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Nested
    @DisplayName("GET /api/v1/results/{id}/scorecard")
    class GetScorecardEndpoint {

        @Test
        @DisplayName("+ve: CANDIDATE requests scorecard JSON - returns presigned URL")
        void candidateCanGetPresignedUrl() throws Exception {
            String storageKey = "scorecards/scorecard-" + RESULT_ID + ".pdf";
            String presignedUrl = "https://s3.ap-south-1.amazonaws.com/exam-scorecards/" + storageKey + "?signature=abc";

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef(storageKey)
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));
            when(scorecardStorageProvider.name()).thenReturn("s3");
            when(scorecardStorageProvider.generatePresignedUrl(eq(storageKey), any()))
                    .thenReturn(presignedUrl);

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .accept(MediaType.APPLICATION_JSON)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.downloadUrl").value(presignedUrl))
                    .andExpect(jsonPath("$.candidateId").value(CANDIDATE_ID.toString()))
                    .andExpect(jsonPath("$.storageProvider").value("s3"))
                    .andExpect(jsonPath("$.expiresInSeconds").value(900));
        }

        @Test
        @DisplayName("+ve: CANDIDATE downloads binary PDF directly with Accept: application/pdf")
        void candidateCanDownloadScorecardPdf() throws Exception {
            String storageKey = "scorecards/scorecard-" + RESULT_ID + ".pdf";
            byte[] pdfBytes = "%PDF-1.4 test scorecard content".getBytes(StandardCharsets.UTF_8);

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef(storageKey)
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));
            when(scorecardStorageProvider.download(eq(storageKey)))
                    .thenReturn(Optional.of(new ByteArrayInputStream(pdfBytes)));

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .accept(MediaType.APPLICATION_PDF)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scorecard-" + CANDIDATE_ID + ".pdf\""));
        }

        @Test
        @DisplayName("+ve: ADMIN downloads scorecard binary PDF directly")
        void adminCanDownloadScorecard() throws Exception {
            String storageKey = "scorecards/scorecard-" + RESULT_ID + ".pdf";
            byte[] pdfBytes = "%PDF-1.4 admin download".getBytes(StandardCharsets.UTF_8);

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef(storageKey)
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));
            when(scorecardStorageProvider.download(eq(storageKey)))
                    .thenReturn(Optional.of(new ByteArrayInputStream(pdfBytes)));

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .param("download", "true")
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
        @DisplayName("-ve: Scorecard file not found in storage returns 404 Not Found")
        void scorecardFileMissingReturns404() throws Exception {
            String storageKey = "scorecards/missing.pdf";
            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef(storageKey)
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));
            when(scorecardStorageProvider.download(eq(storageKey))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard", CANDIDATE_ID)
                            .accept(MediaType.APPLICATION_PDF)
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

    @Nested
    @DisplayName("GET /api/v1/results/{id}/scorecard/presigned")
    class PresignedEndpoint {

        @Test
        @DisplayName("+ve: Explicit presigned URL endpoint returns 200 OK")
        void explicitPresignedEndpoint() throws Exception {
            String storageKey = "scorecards/scorecard-" + RESULT_ID + ".pdf";
            String presignedUrl = "https://s3.ap-south-1.amazonaws.com/exam-scorecards/" + storageKey;

            Result result = Result.builder()
                    .candidateId(CANDIDATE_ID)
                    .examId(UUID.randomUUID())
                    .scorecardPdfRef(storageKey)
                    .build();

            when(resultRepository.findByCandidateIdAndTenantId(eq(CANDIDATE_ID), eq(TENANT_ID)))
                    .thenReturn(List.of(result));
            when(scorecardStorageProvider.name()).thenReturn("s3");
            when(scorecardStorageProvider.generatePresignedUrl(eq(storageKey), any()))
                    .thenReturn(presignedUrl);

            mockMvc.perform(get("/api/v1/results/{candidateId}/scorecard/presigned", CANDIDATE_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(CANDIDATE_ID.toString()).claim("tenant_id", TENANT_ID)))
                            .header("X-Tenant-Id", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.downloadUrl").value(presignedUrl));
        }
    }
}
