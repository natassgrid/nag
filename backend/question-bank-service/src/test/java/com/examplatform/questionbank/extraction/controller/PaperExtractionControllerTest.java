/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
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

package com.examplatform.questionbank.extraction.controller;

import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.exception.GlobalExceptionHandler;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.dto.BulkAnswerKeyRequest;
import com.examplatform.questionbank.extraction.dto.ExtractedQuestionResponse;
import com.examplatform.questionbank.extraction.dto.MergeQuestionsRequest;
import com.examplatform.questionbank.extraction.dto.PaperUploadRequest;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsRequest;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsResponse;
import com.examplatform.questionbank.extraction.dto.QuestionPaperMetadataResponse;
import com.examplatform.questionbank.extraction.dto.ResequenceQuestionsRequest;
import com.examplatform.questionbank.extraction.dto.SplitQuestionRequest;
import com.examplatform.questionbank.extraction.dto.UpdateExtractedQuestionRequest;
import com.examplatform.questionbank.extraction.service.PaperQuestionPromotionService;
import com.examplatform.questionbank.extraction.service.QuestionPaperExtractionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaperExtractionController Unit Tests")
class PaperExtractionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private QuestionPaperExtractionService extractionService;

    @Mock
    private PaperQuestionPromotionService promotionService;

    private ObjectMapper objectMapper;

    private final UUID samplePaperId = UUID.randomUUID();
    private final UUID sampleExtractedQId = UUID.randomUUID();
    private final UUID sampleUserId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID sampleAssetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        PaperExtractionController controller = new PaperExtractionController(
                extractionService,
                promotionService
        );

        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                        || parameter.getParameterType().equals(Jwt.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter,
                                          ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest,
                                          WebDataBinderFactory binderFactory) {
                return Jwt.withTokenValue("mock-token")
                        .header("alg", "none")
                        .subject(sampleUserId.toString())
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver(), jwtResolver)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers with JSON payload returns 202 Accepted")
    void testInitiateExtractionWithJsonPayload() throws Exception {
        PaperUploadRequest uploadReq = PaperUploadRequest.builder()
                .fileAssetId(sampleAssetId)
                .examinationName("UPSC Civil Services Preliminary Examination 2024")
                .examYear(2024)
                .department("UPSC")
                .paperCode("GS-PAPER-1")
                .languages(List.of("en", "hi"))
                .totalQuestionsExpected(100)
                .build();

        QuestionPaperMetadataResponse expectedResponse = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .fileAssetId(sampleAssetId)
                .examinationName("UPSC Civil Services Preliminary Examination 2024")
                .examYear(2024)
                .paperCode("GS-PAPER-1")
                .extractionStatus(PaperExtractionStatus.PENDING)
                .totalPages(24)
                .uploadedBy(sampleUserId)
                .build();

        when(extractionService.initiateExtraction(any(PaperUploadRequest.class), eq(sampleUserId), any()))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/questions/papers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.fileAssetId").value(sampleAssetId.toString()))
                .andExpect(jsonPath("$.examinationName").value("UPSC Civil Services Preliminary Examination 2024"))
                .andExpect(jsonPath("$.examYear").value(2024))
                .andExpect(jsonPath("$.paperCode").value("GS-PAPER-1"))
                .andExpect(jsonPath("$.extractionStatus").value("PENDING"));

        ArgumentCaptor<PaperUploadRequest> captor = ArgumentCaptor.forClass(PaperUploadRequest.class);
        verify(extractionService).initiateExtraction(captor.capture(), eq(sampleUserId), any());
        assertThat(captor.getValue().getExaminationName()).isEqualTo("UPSC Civil Services Preliminary Examination 2024");
        assertThat(captor.getValue().getFileAssetId()).isEqualTo(sampleAssetId);
    }

    @Test
    @DisplayName("POST /upload alias endpoint with JSON returns 202 Accepted")
    void testUploadPaperAliasEndpointWithJson() throws Exception {
        PaperUploadRequest uploadReq = PaperUploadRequest.builder()
                .fileAssetId(sampleAssetId)
                .examinationName("SSC CGL Tier 1")
                .examYear(2024)
                .department("Staff Selection Commission")
                .paperCode("SET-C")
                .subjectName("General Awareness")
                .defaultSubjectId(101L)
                .forceReExtract(true)
                .build();

        QuestionPaperMetadataResponse expectedResponse = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .fileAssetId(sampleAssetId)
                .examinationName("SSC CGL Tier 1")
                .examYear(2024)
                .department("Staff Selection Commission")
                .paperCode("SET-C")
                .subjectName("General Awareness")
                .defaultSubjectId(101L)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();

        when(extractionService.initiateExtraction(any(PaperUploadRequest.class), eq(sampleUserId), any()))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/v1/questions/papers/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(uploadReq)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.examinationName").value("SSC CGL Tier 1"))
                .andExpect(jsonPath("$.extractionStatus").value("PENDING"));

        ArgumentCaptor<PaperUploadRequest> captor = ArgumentCaptor.forClass(PaperUploadRequest.class);
        verify(extractionService).initiateExtraction(captor.capture(), eq(sampleUserId), any());
        assertThat(captor.getValue().getExaminationName()).isEqualTo("SSC CGL Tier 1");
        assertThat(captor.getValue().getFileAssetId()).isEqualTo(sampleAssetId);
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers without fileAssetId returns 400 Bad Request")
    void testInitiateExtractionMissingFileAssetIdReturns400() throws Exception {
        PaperUploadRequest invalidReq = PaperUploadRequest.builder()
                .examinationName("RRB NTPC CBT 1")
                .examYear(2024)
                .fileAssetId(null)
                .build();

        mockMvc.perform(post("/api/v1/questions/papers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers lists papers with pagination and filters")
    void testListPapers() throws Exception {
        QuestionPaperMetadataResponse paper = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("RRB NTPC CBT 1")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.COMPLETED)
                .questionsExtracted(100)
                .questionsApproved(95)
                .build();

        when(extractionService.listPapers(any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(paper), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/questions/papers")
                        .param("examinationName", "RRB")
                        .param("examYear", "2024")
                        .param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.content[0].examinationName").value("RRB NTPC CBT 1"))
                .andExpect(jsonPath("$.content[0].questionsExtracted").value(100));
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers/{paperId} returns paper metadata")
    void testGetPaperMetadata() throws Exception {
        QuestionPaperMetadataResponse response = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("GATE CS 2024")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.PROCESSING)
                .pagesProcessed(12)
                .totalPages(30)
                .build();

        when(extractionService.getPaperMetadata(eq(samplePaperId), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/questions/papers/{paperId}", samplePaperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.examinationName").value("GATE CS 2024"))
                .andExpect(jsonPath("$.pagesProcessed").value(12))
                .andExpect(jsonPath("$.totalPages").value(30));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/cancel cancels active extraction")
    void testCancelExtractionJob() throws Exception {
        QuestionPaperMetadataResponse cancelled = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("GATE CS 2024")
                .extractionStatus(PaperExtractionStatus.CANCELLED)
                .build();

        when(extractionService.cancelExtractionJob(eq(samplePaperId), any())).thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/cancel", samplePaperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.extractionStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/retry retries failed extraction")
    void testRetryExtractionJob() throws Exception {
        QuestionPaperMetadataResponse retried = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("GATE CS 2024")
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();

        when(extractionService.retryExtractionJob(eq(samplePaperId), any())).thenReturn(retried);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/retry", samplePaperId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.extractionStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/answer-key applies bulk answer key")
    void testApplyBulkAnswerKey() throws Exception {
        BulkAnswerKeyRequest req = BulkAnswerKeyRequest.builder()
                .answersByQuestionNumber(Map.of(1, "A", 2, "C", 3, "D"))
                .autoApproveMatched(true)
                .build();

        QuestionPaperMetadataResponse response = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .questionsApproved(3)
                .build();

        when(extractionService.applyBulkAnswerKey(eq(samplePaperId), any(BulkAnswerKeyRequest.class), eq(sampleUserId), any()))
                .thenReturn(3);
        when(extractionService.getPaperMetadata(eq(samplePaperId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/answer-key", samplePaperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionsApproved").value(3));
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers/{paperId}/questions lists extracted staging questions")
    void testListExtractedQuestions() throws Exception {
        ExtractedQuestionResponse q = ExtractedQuestionResponse.builder()
                .id(sampleExtractedQId)
                .paperMetadataId(samplePaperId)
                .questionNumber(1)
                .questionType("SINGLE_MCQ")
                .content("What is the speed of light in vacuum $$c$$?")
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();

        when(extractionService.listExtractedQuestions(eq(samplePaperId), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(q), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/questions/papers/{paperId}/questions", samplePaperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleExtractedQId.toString()))
                .andExpect(jsonPath("$.content[0].questionNumber").value(1))
                .andExpect(jsonPath("$.content[0].content").value("What is the speed of light in vacuum $$c$$?"));
    }

    @Test
    @DisplayName("PUT /api/v1/questions/papers/{paperId}/questions/{questionId} updates and approves question")
    void testUpdateExtractedQuestion() throws Exception {
        UpdateExtractedQuestionRequest req = UpdateExtractedQuestionRequest.builder()
                .questionNumber(1)
                .content("Updated stem $$c = 3 \\times 10^8 \\text{ m/s}$$")
                .answerKey("B")
                .status(ExtractedQuestionStatus.APPROVED)
                .reviewComments("Verified answer key")
                .build();

        ExtractedQuestionResponse response = ExtractedQuestionResponse.builder()
                .id(sampleExtractedQId)
                .paperMetadataId(samplePaperId)
                .questionNumber(1)
                .content("Updated stem $$c = 3 \\times 10^8 \\text{ m/s}$$")
                .answerKey("B")
                .status(ExtractedQuestionStatus.APPROVED)
                .reviewComments("Verified answer key")
                .build();

        when(extractionService.updateExtractedQuestion(eq(samplePaperId), eq(sampleExtractedQId), any(), eq(sampleUserId), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/questions/papers/{paperId}/questions/{questionId}", samplePaperId, sampleExtractedQId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleExtractedQId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.answerKey").value("B"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/promote promotes questions to canonical Question Bank")
    void testPromoteQuestions() throws Exception {
        PromoteQuestionsRequest req = PromoteQuestionsRequest.builder()
                .extractedQuestionIds(List.of(sampleExtractedQId))
                .fallbackSubjectId(10L)
                .fallbackTopicId(20L)
                .build();

        PromoteQuestionsResponse response = PromoteQuestionsResponse.builder()
                .paperMetadataId(samplePaperId)
                .totalRequested(1)
                .totalPromoted(1)
                .totalSkipped(0)
                .totalFailed(0)
                .promotedQuestionIds(List.of(UUID.randomUUID()))
                .build();

        when(promotionService.promoteQuestions(eq(samplePaperId), any(), eq(sampleUserId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/promote", samplePaperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperMetadataId").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.totalRequested").value(1))
                .andExpect(jsonPath("$.totalPromoted").value(1))
                .andExpect(jsonPath("$.totalFailed").value(0));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/resume resumes extraction")
    void testResumeExtraction() throws Exception {
        QuestionPaperMetadataResponse response = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("UPSC Prelims")
                .extractionStatus(PaperExtractionStatus.PROCESSING)
                .resumable(false)
                .build();

        when(extractionService.resumeExtractionJob(eq(samplePaperId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/resume", samplePaperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.extractionStatus").value("PROCESSING"))
                .andExpect(jsonPath("$.resumable").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers/{paperId}/pages/{pageNumber}/image renders JPEG image")
    void testGetPageImage() throws Exception {
        byte[] fakeJpeg = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};

        when(extractionService.renderPageImage(eq(samplePaperId), eq(1), any()))
                .thenReturn(fakeJpeg);

        mockMvc.perform(get("/api/v1/questions/papers/{paperId}/pages/{pageNumber}/image", samplePaperId, 1))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(fakeJpeg));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/questions/merge merges questions")
    void testMergeQuestions() throws Exception {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        MergeQuestionsRequest req = MergeQuestionsRequest.builder()
                .questionIds(List.of(q1, q2))
                .build();

        ExtractedQuestionResponse response = ExtractedQuestionResponse.builder()
                .id(q1)
                .paperMetadataId(samplePaperId)
                .questionNumber(1)
                .content("Merged content")
                .build();

        when(extractionService.mergeQuestions(eq(samplePaperId), any(), eq(sampleUserId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/questions/merge", samplePaperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(q1.toString()))
                .andExpect(jsonPath("$.content").value("Merged content"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/questions/{id}/split splits a question")
    void testSplitQuestion() throws Exception {
        UUID q1 = UUID.randomUUID();
        SplitQuestionRequest req = SplitQuestionRequest.builder()
                .firstContent("Part 1")
                .secondContent("Part 2")
                .build();

        ExtractedQuestionResponse r1 = ExtractedQuestionResponse.builder()
                .id(q1)
                .questionNumber(1)
                .content("Part 1")
                .build();
        ExtractedQuestionResponse r2 = ExtractedQuestionResponse.builder()
                .id(UUID.randomUUID())
                .questionNumber(2)
                .content("Part 2")
                .build();

        when(extractionService.splitQuestion(eq(samplePaperId), eq(q1), any(), eq(sampleUserId), any()))
                .thenReturn(List.of(r1, r2));

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/questions/{id}/split", samplePaperId, q1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Part 1"))
                .andExpect(jsonPath("$[1].content").value("Part 2"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/questions/resequence resequences questions")
    void testResequenceQuestions() throws Exception {
        UUID q1 = UUID.randomUUID();
        UUID q2 = UUID.randomUUID();
        ResequenceQuestionsRequest req = ResequenceQuestionsRequest.builder()
                .orderedQuestionIds(List.of(q2, q1))
                .startNumber(1)
                .build();

        ExtractedQuestionResponse r1 = ExtractedQuestionResponse.builder()
                .id(q2)
                .questionNumber(1)
                .build();
        ExtractedQuestionResponse r2 = ExtractedQuestionResponse.builder()
                .id(q1)
                .questionNumber(2)
                .build();

        when(extractionService.resequenceQuestions(eq(samplePaperId), any(), eq(sampleUserId), any()))
                .thenReturn(List.of(r1, r2));

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/questions/resequence", samplePaperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].questionNumber").value(1))
                .andExpect(jsonPath("$[1].questionNumber").value(2));
    }

    @Test
    @DisplayName("DELETE /api/v1/questions/papers/{paperId} deletes paper and returns 204 No Content")
    void testDeletePaperEndpoint() throws Exception {
        doNothing().when(extractionService).deletePaper(eq(samplePaperId), any());

        mockMvc.perform(delete("/api/v1/questions/papers/{paperId}", samplePaperId))
                .andExpect(status().isNoContent());

        verify(extractionService).deletePaper(eq(samplePaperId), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/questions/papers/{paperId}/questions/{id} deletes question and returns 204 No Content")
    void testDeleteExtractedQuestionEndpoint() throws Exception {
        doNothing().when(extractionService).deleteExtractedQuestion(eq(samplePaperId), eq(sampleExtractedQId), any());

        mockMvc.perform(delete("/api/v1/questions/papers/{paperId}/questions/{id}", samplePaperId, sampleExtractedQId))
                .andExpect(status().isNoContent());

        verify(extractionService).deleteExtractedQuestion(eq(samplePaperId), eq(sampleExtractedQId), any());
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/questions/batch-delete deletes batch and returns 204")
    void testBatchDeleteQuestionsEndpoint() throws Exception {
        com.examplatform.questionbank.extraction.dto.BatchDeleteQuestionsRequest req =
                com.examplatform.questionbank.extraction.dto.BatchDeleteQuestionsRequest.builder()
                        .questionIds(List.of(sampleExtractedQId))
                        .build();

        doNothing().when(extractionService).batchDeleteExtractedQuestions(eq(samplePaperId), eq(List.of(sampleExtractedQId)), any());

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/questions/batch-delete", samplePaperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        verify(extractionService).batchDeleteExtractedQuestions(eq(samplePaperId), eq(List.of(sampleExtractedQId)), any());
    }
}
