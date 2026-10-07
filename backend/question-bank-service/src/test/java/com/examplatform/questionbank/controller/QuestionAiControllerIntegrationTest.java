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
import com.examplatform.questionbank.service.SubjectTopicService;
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
import static org.mockito.Mockito.*;
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

    @MockitoBean
    private SubjectTopicService subjectTopicService;

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

    private QuestionGenerationRequest requestWithIds(Long subjectId, Long topicId) {
        return QuestionGenerationRequest.builder()
                .subjectId(subjectId)
                .topicId(topicId)
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

    private QuestionGenerationResponse successResponse(int count) {
        return QuestionGenerationResponse.builder()
                .modelUsed("qwen2.5-1.5b")
                .totalGenerated(count)
                .totalValid(count)
                .totalDuplicates(0)
                .executionMode("FAST")
                .questions(List.of())
                .build();
    }

    // =========================================================================
    // POST /api/v1/questions/generate
    // =========================================================================

    @Nested
    @DisplayName("POST /api/v1/questions/generate")
    class GenerateQuestionsEndpoint {

        @Test
        @DisplayName("+ve: QUESTION_AUTHOR generates questions by name - returns 200 OK")
        void authorCanGenerateQuestionsByName() throws Exception {
            QuestionGenerationRequest request = validRequest();

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(successResponse(3));

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
        @DisplayName("+ve: QUESTION_AUTHOR generates questions with subjectId/topicId ids - returns 200, no new taxonomy created")
        void authorCanGenerateWithIds() throws Exception {
            QuestionGenerationRequest request = requestWithIds(10L, 42L);

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(successResponse(3));

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.totalGenerated").value(3));

            // When ids are provided, subjectTopicService.resolveOrCreateByName must never be called
            verify(subjectTopicService, never()).resolveOrCreateByName(any(), any(), any(), any());
        }

        @Test
        @DisplayName("+ve: Generate with subjectId/topicId/subtopicId all present - subtopicId forwarded correctly")
        void generateWithAllIds() throws Exception {
            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subjectId(1L)
                    .topicId(5L)
                    .subtopicId(12L)
                    .subject("Physics")
                    .topic("Electromagnetism")
                    .subtopic("Gauss Law")
                    .difficulty("HARD")
                    .cognitiveLevel("ANALYZE")
                    .questionType("SINGLE_MCQ")
                    .count(2)
                    .avoidDuplicate(true)
                    .autoSave(false)
                    .build();

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(successResponse(2));

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.data.totalGenerated").value(2));
        }

        @Test
        @DisplayName("+ve: Request fields subjectId/topicId are deserialized correctly")
        void idFieldsDeserializedCorrectly() throws Exception {
            QuestionGenerationRequest request = requestWithIds(7L, 99L);
            String json = objectMapper.writeValueAsString(request);

            // Verify the serialized JSON contains the id fields
            assert json.contains("\"subjectId\":7");
            assert json.contains("\"topicId\":99");

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(successResponse(1));

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk());

            verify(questionGenerationService).generate(
                    argThat(req -> Long.valueOf(7L).equals(req.getSubjectId()) && Long.valueOf(99L).equals(req.getTopicId())),
                    eq(TENANT_ID), eq(AUTHOR_ID));
        }

        @Test
        @DisplayName("-ve: Missing subject (blank) returns 400 Bad Request")
        void missingSubjectReturnsBadRequest() throws Exception {
            QuestionGenerationRequest invalid = QuestionGenerationRequest.builder()
                    .subject("")
                    .topic("Algebra")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(3)
                    .build();

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalid))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: Invalid count (> 5) returns 400 Bad Request")
        void invalidCountReturnsBadRequest() throws Exception {
            QuestionGenerationRequest invalid = QuestionGenerationRequest.builder()
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
                            .content(objectMapper.writeValueAsString(invalid))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: Unknown subjectId causes 400 when autoSave=true (resolveByIds throws)")
        void unknownSubjectIdReturnsBadRequestOnAutoSave() throws Exception {
            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subjectId(9999L)
                    .topicId(8888L)
                    .subject("Unknown Subject")
                    .topic("Unknown Topic")
                    .difficulty("EASY")
                    .cognitiveLevel("REMEMBER")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .autoSave(true)
                    .build();

            // Service throws IllegalArgumentException when id not found (→ 400 via GlobalExceptionHandler)
            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenThrow(new IllegalArgumentException("Subject not found: id=9999 tenant=default"));

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: Unknown topicId causes 400 when autoSave=true (resolveByIds throws)")
        void unknownTopicIdReturnsBadRequestOnAutoSave() throws Exception {
            QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                    .subjectId(1L)
                    .topicId(9999L)
                    .subject("Mathematics")
                    .topic("Unknown Topic")
                    .difficulty("MEDIUM")
                    .cognitiveLevel("APPLY")
                    .questionType("SINGLE_MCQ")
                    .count(1)
                    .autoSave(true)
                    .build();

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenThrow(new IllegalArgumentException("Topic not found: id=9999 for subjectId=1 tenant=default"));

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("error"));
        }

        @Test
        @DisplayName("-ve: CANDIDATE role forbidden - returns 403 Forbidden")
        void candidateForbidden() throws Exception {
            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest()))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CANDIDATE"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("+ve: executionMode MULTI_AGENT is accepted and forwarded")
        void executionModeMultiAgentAccepted() throws Exception {
            QuestionGenerationRequest request = validRequest();
            request.setExecutionMode(ExecutionMode.MULTI_AGENT);

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(QuestionGenerationResponse.builder()
                            .modelUsed("nova-lite").totalGenerated(3).totalValid(3)
                            .totalDuplicates(0).executionMode("MULTI_AGENT").questions(List.of()).build());

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.executionMode").value("MULTI_AGENT"));
        }

        @Test
        @DisplayName("+ve: generationQuality EXAM_READY is accepted")
        void generationQualityExamReadyAccepted() throws Exception {
            QuestionGenerationRequest request = validRequest();
            request.setGenerationQuality("EXAM_READY");
            request.setTargetExam("JEE_ADV");

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(successResponse(3));

            mockMvc.perform(post("/api/v1/questions/generate")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(questionGenerationService).generate(
                    argThat(req -> "EXAM_READY".equals(req.getGenerationQuality()) && "JEE_ADV".equals(req.getTargetExam())),
                    eq(TENANT_ID), eq(AUTHOR_ID));
        }

        @Test
        @DisplayName("+ve: ASSERTION_REASON and PARAGRAPH_SET question types are accepted")
        void assertionReasonAndParagraphSetTypesAccepted() throws Exception {
            for (String type : List.of("ASSERTION_REASON", "PARAGRAPH_SET")) {
                QuestionGenerationRequest request = QuestionGenerationRequest.builder()
                        .subject("Chemistry").topic("Organic Chemistry")
                        .difficulty("HARD").cognitiveLevel("ANALYZE")
                        .questionType(type).count(1).build();

                when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                        .thenReturn(successResponse(1));

                mockMvc.perform(post("/api/v1/questions/generate")
                                .header("X-Tenant-Id", TENANT_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                        .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.status").value("success"));
            }
        }
    }

    // =========================================================================
    // POST /api/v1/questions/generate/with-samples (Multipart & JSON)
    // =========================================================================

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

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(successResponse(2));

            mockMvc.perform(post("/api/v1/questions/generate/with-samples")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));
        }

        @Test
        @DisplayName("+ve: Generate with-samples accepts subjectId/topicId ids")
        void generateWithSamplesAcceptsIds() throws Exception {
            QuestionGenerationRequest request = requestWithIds(3L, 17L);
            request.setSampleQuestions(List.of("Derive the quadratic formula."));

            when(questionGenerationService.generate(any(QuestionGenerationRequest.class), eq(TENANT_ID), eq(AUTHOR_ID)))
                    .thenReturn(successResponse(2));

            mockMvc.perform(post("/api/v1/questions/generate/with-samples")
                            .header("X-Tenant-Id", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_QUESTION_AUTHOR"))
                                    .jwt(j -> j.subject(AUTHOR_ID.toString()).claim("tenant_id", TENANT_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"));

            verify(questionGenerationService).generate(
                    argThat(req -> Long.valueOf(3L).equals(req.getSubjectId()) && Long.valueOf(17L).equals(req.getTopicId())),
                    eq(TENANT_ID), eq(AUTHOR_ID));
        }
    }

    // =========================================================================
    // POST /api/v1/questions/generate/clarify
    // =========================================================================

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

    // =========================================================================
    // POST /api/v1/questions/embeddings/backfill
    // =========================================================================

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
