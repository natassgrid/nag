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

import com.examplatform.questionbank.ai.embedding.EmbeddingService;
import com.examplatform.questionbank.ai.generation.ClarifyRequirementsRequest;
import com.examplatform.questionbank.ai.generation.ClarifyRequirementsResponse;
import com.examplatform.questionbank.ai.generation.ExecutionMode;
import com.examplatform.questionbank.ai.generation.QuestionGenerationRequest;
import com.examplatform.questionbank.ai.generation.QuestionGenerationResponse;
import com.examplatform.questionbank.ai.generation.QuestionGenerationService;
import com.examplatform.questionbank.ai.parser.NormalizedSampleQuestion;
import com.examplatform.questionbank.ai.parser.SampleDocumentParserService;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("QuestionAiController REST Endpoints E2E Tests (MockMvc)")
class QuestionAiControllerIntegrationTest extends AbstractIntegrationTest {

    @MockitoBean
    private QuestionRepository questionRepository;

    @MockitoBean
    private EmbeddingService embeddingService;

    @MockitoBean
    private QuestionGenerationService questionGenerationService;

    @MockitoBean
    private SampleDocumentParserService sampleDocumentParserService;

    private static final String TENANT_ID = "default";
    private static final UUID AUTHOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private QuestionGenerationRequest validRequest() {
        return QuestionGenerationRequest.builder()
                .subject("Mathematics")
                .topic("Algebra")
                .difficulty("MEDIUM")
                .cognitiveLevel("APPLY")
                .questionType("SINGLE_MCQ")
                .count(3)
                .avoidDuplicate(true)
                .autoSave(false)
                .executionMode(ExecutionMode.AUTO)
                .build();
    }

    @Nested
    @DisplayName("POST /api/v1/questions/generate")
    class GenerateQuestionsEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR generates questions - returns 200 OK")
        void authorCanGenerateQuestions() throws Exception {
            QuestionGenerationRequest request = validRequest();
            QuestionGenerationResponse response = QuestionGenerationResponse.builder()
                    .modelUsed("qwen2.5-1.5b")
                    .totalGenerated(3)
                    .totalValid(3)
                    .totalDuplicates(0)
                    .executionMode("FAST")
                    .questions(List.of())
                    .build();

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.modelUsed").value("qwen2.5-1.5b"))
                    .andExpect(jsonPath("$.data.totalGenerated").value(3));
        }

        @Test
        @DisplayName("-ve: Invalid count (> 5) returns 400 Bad Request")
        void invalidCountReturnsBadRequest() throws Exception {
            QuestionGenerationRequest invalidRequest = QuestionGenerationRequest.builder()
                    .subject("Mathematics")
                    .topic("Algebra")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(10)
                    .build();

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden - returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            QuestionGenerationRequest request = validRequest();

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/questions/generate/with-samples (Multipart & JSON)")
    class GenerateWithSamplesEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR generates with multipart PDF upload - returns 200 OK")
        void authorCanGenerateWithUploadedFile() throws Exception {
            QuestionGenerationRequest request = validRequest();
            request.setExecutionMode(ExecutionMode.MULTI_AGENT);

            QuestionGenerationResponse response = QuestionGenerationResponse.builder()
                    .modelUsed("nova-lite")
                    .totalGenerated(3)
                    .totalValid(3)
                    .totalDuplicates(0)
                    .executionMode("MULTI_AGENT")
                    .triageRationale("Escalated to Multi-Agent due to EXPLICIT_EXECUTION_MODE_MULTI_AGENT")
                    .questions(List.of())
                    .build();

            when(sampleDocumentParserService.parseUploadedFile(any(MultipartFile.class)))
                    .thenReturn(List.of(NormalizedSampleQuestion.builder().stem("Sample").build()));

            when(questionGenerationService.generateWithSamples(any(QuestionGenerationRequest.class), anyList(), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(response);

            MockMultipartFile filePart = new MockMultipartFile(
                    "file", "sample.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes());
            MockMultipartFile requestPart = new MockMultipartFile(
                    "request", "", "application/json", objectMapper.writeValueAsBytes(request));

            mockMvc.perform(multipart("/api/v1/questions/generate/with-samples")
                            .file(filePart)
                            .file(requestPart)
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.executionMode").value("MULTI_AGENT"))
                    .andExpect(jsonPath("$.data.totalGenerated").value(3));
        }

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR generates with JSON sample questions - returns 200 OK")
        void authorCanGenerateWithJsonSamples() throws Exception {
            QuestionGenerationRequest request = validRequest();
            request.setSampleQuestions(List.of("Sample question text: What is momentum?"));

            QuestionGenerationResponse response = QuestionGenerationResponse.builder()
                    .modelUsed("nova-micro")
                    .totalGenerated(2)
                    .totalValid(2)
                    .totalDuplicates(0)
                    .executionMode("FAST")
                    .questions(List.of())
                    .build();

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/questions/generate/with-samples")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.executionMode").value("FAST"))
                    .andExpect(jsonPath("$.data.totalGenerated").value(2));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/questions/generate/clarify")
    class ClarifyRequirementsEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR requests requirement clarification - returns 200 OK")
        void authorCanRequestClarification() throws Exception {
            ClarifyRequirementsRequest request = ClarifyRequirementsRequest.builder()
                    .subject("Chemistry")
                    .topic("Electrochemistry")
                    .authorPrompt("Create tough questions like the sample")
                    .targetExam("JEE_ADV")
                    .build();

            ClarifyRequirementsResponse response = ClarifyRequirementsResponse.builder()
                    .clarificationNeeded(true)
                    .clarificationQuestions(List.of("What specific standard should this align with?"))
                    .suggestedSubtopics(List.of("Nernst Equation"))
                    .suggestedFormats(List.of("SINGLE_MCQ", "MULTI_MCQ"))
                    .recommendedBlueprint("Blueprint for JEE_ADV")
                    .resolvedTargetExam("JEE_ADV")
                    .build();

            when(questionGenerationService.clarifyRequirements(any(ClarifyRequirementsRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/questions/generate/clarify")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.clarificationNeeded").value(true))
                    .andExpect(jsonPath("$.data.resolvedTargetExam").value("JEE_ADV"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/questions/embeddings/backfill")
    class BackfillEmbeddingsEndpoint {

        @Test
        @DisplayName("+ve: SUPER_ADMIN runs backfill - returns 200 OK")
        void adminCanRunBackfill() throws Exception {
            when(embeddingService.backfillEmbeddings(eq(TENANT_ID)))
                    .thenReturn(Map.of("totalProcessed", 0, "totalFailed", 0, "failures", List.of()));

            mockMvc.perform(post("/api/v1/questions/embeddings/backfill")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
                                    .jwt(j -> j.claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.totalProcessed").value(0));
        }

        @Test
        @DisplayName("-ve: QUESTION_AUTHOR forbidden from running backfill - returns 403 Forbidden")
        void authorForbiddenFromBackfill() throws Exception {
            mockMvc.perform(post("/api/v1/questions/embeddings/backfill")
                            .header("X-Tenant-Id", TENANT_ID)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }
    }
}
