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
import com.examplatform.questionbank.extraction.dto.PaperUploadRequest;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsRequest;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsResponse;
import com.examplatform.questionbank.extraction.dto.QuestionPaperMetadataResponse;
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
import org.springframework.mock.web.MockMultipartFile;
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

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        PaperExtractionController controller = new PaperExtractionController(
                extractionService,
                promotionService,
                objectMapper
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
    @DisplayName("POST /upload with metadata JSON part returns 202 Accepted")
    void testUploadPaperWithMetadataJsonPart() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "upsc_prelims_2024.pdf", "application/pdf", "%PDF-1.4 test bytes".getBytes());

        PaperUploadRequest uploadReq = PaperUploadRequest.builder()
                .examinationName("UPSC Civil Services Preliminary Examination 2024")
                .examYear(2024)
                .department("UPSC")
                .paperCode("GS-PAPER-1")
                .languages(List.of("en", "hi"))
                .totalQuestionsExpected(100)
                .build();

        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata", "", "application/json", objectMapper.writeValueAsBytes(uploadReq));

        QuestionPaperMetadataResponse expectedResponse = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("UPSC Civil Services Preliminary Examination 2024")
                .examYear(2024)
                .paperCode("GS-PAPER-1")
                .extractionStatus(PaperExtractionStatus.PENDING)
                .totalPages(24)
                .uploadedBy(sampleUserId)
                .build();

        when(extractionService.uploadAndStartExtraction(any(), any(PaperUploadRequest.class), eq(sampleUserId), any()))
                .thenReturn(expectedResponse);

        mockMvc.perform(multipart("/api/v1/questions/papers/upload")
                        .file(file)
                        .file(metadataPart))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.examinationName").value("UPSC Civil Services Preliminary Examination 2024"))
                .andExpect(jsonPath("$.examYear").value(2024))
                .andExpect(jsonPath("$.paperCode").value("GS-PAPER-1"))
                .andExpect(jsonPath("$.extractionStatus").value("PENDING"));

        ArgumentCaptor<PaperUploadRequest> captor = ArgumentCaptor.forClass(PaperUploadRequest.class);
        verify(extractionService).uploadAndStartExtraction(any(), captor.capture(), eq(sampleUserId), any());
        assertThat(captor.getValue().getExaminationName()).isEqualTo("UPSC Civil Services Preliminary Examination 2024");
        assertThat(captor.getValue().getPaperCode()).isEqualTo("GS-PAPER-1");
    }

    @Test
    @DisplayName("POST /upload with form parameters returns 202 Accepted")
    void testUploadPaperWithFormParameters() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "ssc_cgl_tier1_2024.pdf", "application/pdf", "%PDF-1.4 test bytes".getBytes());

        QuestionPaperMetadataResponse expectedResponse = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("SSC CGL Tier 1")
                .examYear(2024)
                .department("Staff Selection Commission")
                .paperCode("SET-C")
                .subjectName("General Awareness")
                .defaultSubjectId(101L)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();

        when(extractionService.uploadAndStartExtraction(any(), any(PaperUploadRequest.class), eq(sampleUserId), any()))
                .thenReturn(expectedResponse);

        mockMvc.perform(multipart("/api/v1/questions/papers/upload")
                        .file(file)
                        .param("examinationName", "SSC CGL Tier 1")
                        .param("examYear", "2024")
                        .param("department", "Staff Selection Commission")
                        .param("paperCode", "SET-C")
                        .param("subjectName", "General Awareness")
                        .param("defaultSubjectId", "101")
                        .param("forceReExtract", "true"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.examinationName").value("SSC CGL Tier 1"))
                .andExpect(jsonPath("$.extractionStatus").value("PENDING"));

        ArgumentCaptor<PaperUploadRequest> captor = ArgumentCaptor.forClass(PaperUploadRequest.class);
        verify(extractionService).uploadAndStartExtraction(any(), captor.capture(), eq(sampleUserId), any());
        assertThat(captor.getValue().getExaminationName()).isEqualTo("SSC CGL Tier 1");
        assertThat(captor.getValue().getExamYear()).isEqualTo(2024);
        assertThat(captor.getValue().getPaperCode()).isEqualTo("SET-C");
        assertThat(captor.getValue().isForceReExtract()).isTrue();
    }

    @Test
    @DisplayName("POST /upload with malformed metadata JSON returns 400 Bad Request")
    void testUploadPaperWithMalformedMetadataJson() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", "%PDF-1.4 test bytes".getBytes());

        MockMultipartFile badMetadataPart = new MockMultipartFile(
                "metadata", "", "application/json", "{malformed-json:".getBytes());

        mockMvc.perform(multipart("/api/v1/questions/papers/upload")
                        .file(file)
                        .file(badMetadataPart))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers lists papers with pagination and filters")
    void testListPapers() throws Exception {
        QuestionPaperMetadataResponse paper = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("NDA 2024")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.COMPLETED)
                .build();

        when(extractionService.listPapers(any(), eq("NDA"), eq(2024), any(), eq(PaperExtractionStatus.COMPLETED), any()))
                .thenReturn(new PageImpl<>(List.of(paper), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/questions/papers")
                        .param("examinationName", "NDA")
                        .param("examYear", "2024")
                        .param("status", "COMPLETED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.content[0].examinationName").value("NDA 2024"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers/{paperId} retrieves specific paper metadata")
    void testGetPaperMetadata() throws Exception {
        QuestionPaperMetadataResponse response = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .examinationName("GATE Computer Science 2024")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.PROCESSING)
                .pagesProcessed(10)
                .totalPages(20)
                .progressPercentage(50.0)
                .build();

        when(extractionService.getPaperMetadata(eq(samplePaperId), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/questions/papers/{paperId}", samplePaperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.examinationName").value("GATE Computer Science 2024"))
                .andExpect(jsonPath("$.progressPercentage").value(50.0))
                .andExpect(jsonPath("$.extractionStatus").value("PROCESSING"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/cancel cancels ongoing job")
    void testCancelExtraction() throws Exception {
        QuestionPaperMetadataResponse cancelled = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .extractionStatus(PaperExtractionStatus.CANCELLED)
                .build();

        when(extractionService.cancelExtractionJob(eq(samplePaperId), any()))
                .thenReturn(cancelled);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/cancel", samplePaperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.extractionStatus").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/retry retries failed job and returns 202 Accepted")
    void testRetryExtraction() throws Exception {
        QuestionPaperMetadataResponse retried = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .pagesProcessed(0)
                .build();

        when(extractionService.retryExtractionJob(eq(samplePaperId), any()))
                .thenReturn(retried);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/retry", samplePaperId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.extractionStatus").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/answer-key applies bulk answer key")
    void testApplyBulkAnswerKey() throws Exception {
        BulkAnswerKeyRequest req = BulkAnswerKeyRequest.builder()
                .answersByQuestionNumber(Map.of(1, "A", 2, "C"))
                .explanationsByQuestionNumber(Map.of(1, "Formula derivation", 2, "Direct definition"))
                .autoApproveMatched(true)
                .build();

        QuestionPaperMetadataResponse metadata = QuestionPaperMetadataResponse.builder()
                .id(samplePaperId)
                .questionsApproved(2)
                .build();

        when(extractionService.applyBulkAnswerKey(eq(samplePaperId), any(BulkAnswerKeyRequest.class), eq(sampleUserId), any()))
                .thenReturn(2);
        when(extractionService.getPaperMetadata(eq(samplePaperId), any()))
                .thenReturn(metadata);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/answer-key", samplePaperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.questionsApproved").value(2));

        verify(extractionService).applyBulkAnswerKey(eq(samplePaperId), any(BulkAnswerKeyRequest.class), eq(sampleUserId), any());
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers/{paperId}/questions lists extracted questions")
    void testListExtractedQuestions() throws Exception {
        ExtractedQuestionResponse q = ExtractedQuestionResponse.builder()
                .id(sampleExtractedQId)
                .paperMetadataId(samplePaperId)
                .questionNumber(1)
                .sourcePageNumber(2)
                .content("What is the speed of light in vacuum?")
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();

        when(extractionService.listExtractedQuestions(eq(samplePaperId), any(), eq(ExtractedQuestionStatus.EXTRACTED), eq(2), any()))
                .thenReturn(new PageImpl<>(List.of(q), PageRequest.of(0, 50), 1));

        mockMvc.perform(get("/api/v1/questions/papers/{paperId}/questions", samplePaperId)
                        .param("status", "EXTRACTED")
                        .param("pageNumber", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(sampleExtractedQId.toString()))
                .andExpect(jsonPath("$.content[0].questionNumber").value(1))
                .andExpect(jsonPath("$.content[0].sourcePageNumber").value(2))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/questions/papers/{paperId}/questions/{extractedQuestionId} retrieves single question")
    void testGetExtractedQuestion() throws Exception {
        ExtractedQuestionResponse response = ExtractedQuestionResponse.builder()
                .id(sampleExtractedQId)
                .paperMetadataId(samplePaperId)
                .questionNumber(5)
                .content("Calculate the determinant of $$A$$.")
                .hasFormulas(true)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();

        when(extractionService.getExtractedQuestion(eq(samplePaperId), eq(sampleExtractedQId), any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/questions/papers/{paperId}/questions/{extractedQuestionId}",
                        samplePaperId, sampleExtractedQId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleExtractedQId.toString()))
                .andExpect(jsonPath("$.questionNumber").value(5))
                .andExpect(jsonPath("$.hasFormulas").value(true));
    }

    @Test
    @DisplayName("PUT /api/v1/questions/papers/{paperId}/questions/{extractedQuestionId} updates question")
    void testUpdateExtractedQuestion() throws Exception {
        UpdateExtractedQuestionRequest request = UpdateExtractedQuestionRequest.builder()
                .questionNumber(5)
                .content("Calculate the determinant of $$A$$ where $$A$$ is singular.")
                .options(List.of(
                        new QuestionOption("A", "$$0$$", true, null, null),
                        new QuestionOption("B", "$$1$$", false, null, null)
                ))
                .answerKey("A")
                .status(ExtractedQuestionStatus.APPROVED)
                .reviewComments("Verified by SME")
                .build();

        ExtractedQuestionResponse response = ExtractedQuestionResponse.builder()
                .id(sampleExtractedQId)
                .paperMetadataId(samplePaperId)
                .questionNumber(5)
                .content("Calculate the determinant of $$A$$ where $$A$$ is singular.")
                .status(ExtractedQuestionStatus.APPROVED)
                .reviewComments("Verified by SME")
                .build();

        when(extractionService.updateExtractedQuestion(eq(samplePaperId), eq(sampleExtractedQId),
                any(UpdateExtractedQuestionRequest.class), eq(sampleUserId), any()))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/questions/papers/{paperId}/questions/{extractedQuestionId}",
                        samplePaperId, sampleExtractedQId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(sampleExtractedQId.toString()))
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.reviewComments").value("Verified by SME"));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/promote promotes approved questions")
    void testPromoteQuestionsPrimaryEndpoint() throws Exception {
        PromoteQuestionsRequest request = PromoteQuestionsRequest.builder()
                .createTranslations(true)
                .targetQuestionState("APPROVED")
                .build();

        UUID promotedCanonicalId = UUID.randomUUID();
        PromoteQuestionsResponse response = PromoteQuestionsResponse.builder()
                .paperMetadataId(samplePaperId)
                .totalRequested(10)
                .totalPromoted(10)
                .totalSkipped(0)
                .totalFailed(0)
                .promotedQuestionIds(List.of(promotedCanonicalId))
                .build();

        when(promotionService.promoteQuestions(eq(samplePaperId), any(PromoteQuestionsRequest.class), eq(sampleUserId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/promote", samplePaperId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperMetadataId").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.totalRequested").value(10))
                .andExpect(jsonPath("$.totalPromoted").value(10))
                .andExpect(jsonPath("$.promotedQuestionIds[0]").value(promotedCanonicalId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/questions/papers/{paperId}/questions/promote alias route promotes questions")
    void testPromoteQuestionsAliasEndpoint() throws Exception {
        UUID promotedCanonicalId = UUID.randomUUID();
        PromoteQuestionsResponse response = PromoteQuestionsResponse.builder()
                .paperMetadataId(samplePaperId)
                .totalRequested(5)
                .totalPromoted(5)
                .promotedQuestionIds(List.of(promotedCanonicalId))
                .build();

        when(promotionService.promoteQuestions(eq(samplePaperId), any(PromoteQuestionsRequest.class), eq(sampleUserId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/questions/papers/{paperId}/questions/promote", samplePaperId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paperMetadataId").value(samplePaperId.toString()))
                .andExpect(jsonPath("$.totalPromoted").value(5));
    }
}
