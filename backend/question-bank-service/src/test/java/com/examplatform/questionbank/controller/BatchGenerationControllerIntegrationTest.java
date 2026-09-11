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

package com.examplatform.questionbank.controller;

import com.examplatform.questionbank.ai.batch.BatchGenerationJob;
import com.examplatform.questionbank.ai.batch.BatchGenerationJobRepository;
import com.examplatform.questionbank.ai.batch.BatchGenerationRequest;
import com.examplatform.questionbank.ai.batch.BatchJobResponse;
import com.examplatform.questionbank.ai.batch.BatchJobStatus;
import com.examplatform.questionbank.ai.batch.BedrockBatchService;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("BatchGenerationController REST Endpoints E2E Tests (MockMvc)")
class BatchGenerationControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private BedrockBatchService bedrockBatchService;

    @MockitoBean
    private BatchGenerationJobRepository jobRepository;

    private static final String TENANT_ID = "default";
    private static final UUID JOB_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID AUTHOR_ID = UUID.fromString("66666666-7777-8888-9999-000000000000");

    private BatchGenerationRequest validBatchRequest() {
        BatchGenerationRequest.BatchItem item = BatchGenerationRequest.BatchItem.builder()
                .subject("History")
                .topic("Ancient India")
                .difficulty("MEDIUM")
                .cognitiveLevel("REMEMBER")
                .questionType("SINGLE_MCQ")
                .count(5)
                .build();

        return BatchGenerationRequest.builder()
                .items(List.of(item))
                .avoidDuplicates(true)
                .build();
    }

    private BatchJobResponse sampleJobResponse() {
        return BatchJobResponse.builder()
                .id(JOB_ID)
                .status(BatchJobStatus.PENDING)
                .totalRequested(5)
                .totalGenerated(0)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/questions/batch")
    class SubmitBatchJobEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR submits batch generation job - returns 202 Accepted")
        void authorCanSubmitBatchJob() throws Exception {
            BatchGenerationRequest request = validBatchRequest();
            when(bedrockBatchService.submitBatchJob(any(BatchGenerationRequest.class), eq(AUTHOR_ID), eq(TENANT_ID)))
                    .thenReturn(sampleJobResponse());

            mockMvc.perform(post("/api/v1/questions/batch")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(JOB_ID.toString()));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden from submitting batch job - returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            BatchGenerationRequest request = validBatchRequest();

            mockMvc.perform(post("/api/v1/questions/batch")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString())))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/questions/batch/{jobId}")
    class GetJobStatusEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR checks job status - returns 200 OK")
        void authorCanGetJobStatus() throws Exception {
            BatchGenerationJob job = BatchGenerationJob.builder()
                    .initiatedBy(AUTHOR_ID)
                    .status(BatchJobStatus.PROCESSING)
                    .totalRequested(5)
                    .totalGenerated(2)
                    .build();
            ReflectionTestUtils.setField(job, "id", JOB_ID);
            ReflectionTestUtils.setField(job, "createdAt", Instant.now());
            job.setTenantId(TENANT_ID);

            when(jobRepository.findById(eq(JOB_ID))).thenReturn(Optional.of(job));

            mockMvc.perform(get("/api/v1/questions/batch/{jobId}", JOB_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.id").value(JOB_ID.toString()))
                    .andExpect(jsonPath("$.data.status").value("PROCESSING"));
        }

        @Test
        @DisplayName("-ve: Non-existent job returns 404 Not Found")
        void nonExistentJobReturnsNotFound() throws Exception {
            when(jobRepository.findById(eq(JOB_ID))).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/questions/batch/{jobId}", JOB_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value("error"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/questions/batch/{jobId}/cancel")
    class CancelJobEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR cancels job - returns 200 OK")
        void authorCanCancelJob() throws Exception {
            BatchJobResponse cancelledResponse = sampleJobResponse();
            cancelledResponse.setStatus(BatchJobStatus.CANCELLED);

            when(bedrockBatchService.cancelJob(eq(JOB_ID), eq(TENANT_ID))).thenReturn(cancelledResponse);

            mockMvc.perform(post("/api/v1/questions/batch/{jobId}/cancel", JOB_ID)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        }
    }
}
