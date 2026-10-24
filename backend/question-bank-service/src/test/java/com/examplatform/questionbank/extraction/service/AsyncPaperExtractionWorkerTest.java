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

package com.examplatform.questionbank.extraction.service;

import com.examplatform.questionbank.ai.generation.ModelRouter;
import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.extraction.ai.LiteLlmPageExtractionClient;
import com.examplatform.questionbank.extraction.ai.LlmPageExtractionResult;
import com.examplatform.questionbank.extraction.ai.LlmQuotaExhaustedException;
import com.examplatform.questionbank.extraction.ai.PdfPageContent;
import com.examplatform.questionbank.extraction.ai.QuestionPaperPdfParser;
import com.examplatform.questionbank.extraction.client.AssetServiceClient;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import com.examplatform.questionbank.extraction.repository.ExtractedQuestionRepository;
import com.examplatform.questionbank.extraction.repository.QuestionPaperMetadataRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.shared.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AsyncPaperExtractionWorker Unit Tests")
class AsyncPaperExtractionWorkerTest {

    @Mock
    private QuestionPaperMetadataRepository metadataRepository;

    @Mock
    private ExtractedQuestionRepository extractedQuestionRepository;

    @Mock
    private AssetServiceClient assetServiceClient;

    @Mock
    private LiteLlmPageExtractionClient extractionClient;

    @Mock
    private ModelRouter modelRouter;

    @Mock
    private SubjectRepository subjectRepository;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private SubtopicRepository subtopicRepository;

    private QuestionPaperPdfParser pdfParser;
    private ObjectMapper objectMapper;

    private AsyncPaperExtractionWorker worker;

    private final String tenantId = "tenant-test-extraction";
    private final UUID paperId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        pdfParser = new QuestionPaperPdfParser();
        objectMapper = new ObjectMapper();
        worker = new AsyncPaperExtractionWorker(
                metadataRepository,
                extractedQuestionRepository,
                assetServiceClient,
                pdfParser,
                extractionClient,
                modelRouter,
                subjectRepository,
                topicRepository,
                subtopicRepository,
                objectMapper
        );
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    private byte[] createTestPdf(int numPages) throws IOException {
        try (PDDocument document = new PDDocument()) {
            for (int i = 0; i < numPages; i++) {
                PDPage page = new PDPage();
                document.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                    cs.beginText();
                    cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    cs.newLineAtOffset(50, 700);
                    cs.showText("Page " + (i + 1) + " test content for PDF extraction verification.");
                    cs.endText();
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    @Test
    @DisplayName("End-to-end multi-page PDF extraction with cover page + bilingual math question transitions to COMPLETED")
    void testEndToEndMultiPageExtraction() throws IOException {
        byte[] pdfBytes = createTestPdf(2);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("UPSC Civil Services 2024")
                .examYear(2024)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .defaultSubjectId(100L)
                .defaultTopicId(200L)
                .defaultSubtopicId(300L)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("gemini-2.5-pro");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());
        when(extractedQuestionRepository.save(any(ExtractedQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Page 1: Cover page result
        LlmPageExtractionResult coverResult = LlmPageExtractionResult.builder()
                .coverPage(true)
                .detectedPaperCode("GS-PAPER-1")
                .detectedBookletSerialNo("BK-9988")
                .detectedSubjectName("General Studies")
                .detectedTotalQuestions(100)
                .detectedDurationMinutes(120)
                .detectedMaxMarks(200.0)
                .detectedNegativeMarkingRatio(0.33)
                .detectedSections(List.of(Map.of("name", "Section A", "totalQuestions", 50)))
                .questions(Collections.emptyList())
                .build();

        // Page 2: Question page with bilingual math & options
        LlmPageExtractionResult.ExtractedItemPayload q1 = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(1)
                .sectionName("Section A")
                .questionType("SINGLE_MCQ")
                .content("Calculate the force \\(F = ma\\) where \\(m = 2\\text{ kg}\\) and \\(a = 5\\text{ m/s}^2\\).")
                .options(List.of(
                        new QuestionOption("A", "$$5\\text{ N}$$", false, null, null),
                        new QuestionOption("B", "$$10\\text{ N}$$", true, null, null),
                        new QuestionOption("C", "$$15\\text{ N}$$", false, null, null),
                        new QuestionOption("D", "$$20\\text{ N}$$", false, null, null)
                ))
                .answerKey("B")
                .explanation("By Newton's second law, \\(F = 2 \\times 5 = 10\\text{ N}\\).")
                .hindiContent("बल \\(F = ma\\) की गणना करें जहाँ \\(m = 2\\text{ kg}\\) और \\(a = 5\\text{ m/s}^2\\) है।")
                .hindiOptions(List.of(
                        new QuestionOption("A", "$$5\\text{ N}$$", false, null, null),
                        new QuestionOption("B", "$$10\\text{ N}$$", true, null, null),
                        new QuestionOption("C", "$$15\\text{ N}$$", false, null, null),
                        new QuestionOption("D", "$$20\\text{ N}$$", false, null, null)
                ))
                .hasFormulas(true)
                .hasTables(false)
                .hasDiagram(false)
                .confidence(0.98)
                .suggestedDifficulty("EASY")
                .suggestedCognitiveLevel("APPLY")
                .build();

        LlmPageExtractionResult qPageResult = LlmPageExtractionResult.builder()
                .coverPage(false)
                .questions(List.of(q1))
                .build();

        when(extractionClient.extractPage(any(PdfPageContent.class), anyString()))
                .thenReturn(coverResult)
                .thenReturn(qPageResult);

        when(extractedQuestionRepository.findByPaperMetadataIdAndQuestionNumber(paperId, 1))
                .thenReturn(Optional.empty());
        when(extractedQuestionRepository.countByPaperMetadataIdAndTenantId(paperId, tenantId))
                .thenReturn(1L);

        worker.processPaperExtractionJob(paperId, tenantId);

        // Verification of status transitions and cover metadata enrichment
        assertThat(paper.getExtractionStatus()).isEqualTo(PaperExtractionStatus.COMPLETED);
        assertThat(paper.getTotalPages()).isEqualTo(2);
        assertThat(paper.getPagesProcessed()).isEqualTo(2);
        assertThat(paper.getModelUsed()).isEqualTo("gemini-2.5-pro");
        assertThat(paper.getPaperCode()).isEqualTo("GS-PAPER-1");
        assertThat(paper.getBookletSerialNo()).isEqualTo("BK-9988");
        assertThat(paper.getSubjectName()).isEqualTo("General Studies");
        assertThat(paper.getTotalQuestionsExpected()).isEqualTo(100);
        assertThat(paper.getDurationMinutes()).isEqualTo(120);
        assertThat(paper.getMaxMarks()).isEqualTo(200.0);
        assertThat(paper.getNegativeMarkingRatio()).isEqualTo(0.33);
        assertThat(paper.getCompletedAt()).isNotNull();

        // Verification of extracted question staging record
        ArgumentCaptor<ExtractedQuestion> eqCaptor = ArgumentCaptor.forClass(ExtractedQuestion.class);
        verify(extractedQuestionRepository).save(eqCaptor.capture());
        ExtractedQuestion savedEq = eqCaptor.getValue();

        assertThat(savedEq.getQuestionNumber()).isEqualTo(1);
        assertThat(savedEq.getSectionName()).isEqualTo("Section A");
        assertThat(savedEq.getQuestionType()).isEqualTo("SINGLE_MCQ");
        // Math normalization ensures delimiters are converted to $$...$$
        assertThat(savedEq.getContent()).contains("$$F = ma$$");
        assertThat(savedEq.getExplanation()).contains("$$F = 2 \\times 5 = 10\\text{ N}$$");
        assertThat(savedEq.getAnswerKey()).isEqualTo("B");
        assertThat(savedEq.getOptions()).hasSize(4);
        assertThat(savedEq.getOptions().get(1).isCorrect()).isTrue();
        assertThat(savedEq.isHasFormulas()).isTrue();
        assertThat(savedEq.getExtractionConfidence()).isEqualTo(0.98);
        assertThat(savedEq.getStatus()).isEqualTo(ExtractedQuestionStatus.EXTRACTED);
        assertThat(savedEq.getSuggestedSubjectId()).isEqualTo(100L);
        assertThat(savedEq.getSuggestedTopicId()).isEqualTo(200L);
        assertThat(savedEq.getSuggestedSubtopicId()).isEqualTo(300L);

        // Bilingual Hindi translation payload verification
        assertThat(savedEq.getTranslations()).containsKey("hi");
        Map<String, Object> hiMap = savedEq.getTranslations().get("hi");
        assertThat(hiMap.get("content").toString()).contains("$$F = ma$$");
        assertThat(hiMap.get("options")).isNotNull();

        verify(assetServiceClient).downloadAssetBytes(eq(assetId), eq(tenantId));
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Partial page failure populates failedPages and transitions paper to PARTIAL_FAILED")
    void testPartialPageFailureTransitionsToPartialFailed() throws IOException {
        byte[] pdfBytes = createTestPdf(2);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("GATE Physics 2024")
                .examYear(2024)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("claude-3-7-sonnet");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());

        // Page 1 succeeds
        LlmPageExtractionResult coverResult = LlmPageExtractionResult.builder()
                .coverPage(true)
                .detectedPaperCode("PH-2024")
                .questions(Collections.emptyList())
                .build();

        // Page 2 throws error (e.g. rate limit / LLM timeout)
        when(extractionClient.extractPage(any(PdfPageContent.class), anyString()))
                .thenReturn(coverResult)
                .thenThrow(new RuntimeException("Inference timeout on page 2"));

        worker.processPaperExtractionJob(paperId, tenantId);

        assertThat(paper.getExtractionStatus()).isEqualTo(PaperExtractionStatus.PARTIAL_FAILED);
        assertThat(paper.getTotalPages()).isEqualTo(2);
        assertThat(paper.getPagesProcessed()).isEqualTo(2);
        assertThat(paper.getFailedPages()).hasSize(1);
        assertThat(paper.getFailedPages().get(0).get("pageNumber")).isEqualTo(2);
        assertThat(paper.getFailedPages().get(0).get("error").toString()).contains("Inference timeout on page 2");
        assertThat(paper.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Total failure across all pages transitions paper to FAILED with error message")
    void testAllPagesFailureTransitionsToFailed() throws IOException {
        byte[] pdfBytes = createTestPdf(2);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("CAT 2024")
                .examYear(2024)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("gemini-2.5-pro");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());

        when(extractionClient.extractPage(any(PdfPageContent.class), anyString()))
                .thenThrow(new RuntimeException("Page 1 vision decode error"))
                .thenThrow(new RuntimeException("Page 2 vision decode error"));

        worker.processPaperExtractionJob(paperId, tenantId);

        assertThat(paper.getExtractionStatus()).isEqualTo(PaperExtractionStatus.FAILED);
        assertThat(paper.getFailedPages()).hasSize(2);
        assertThat(paper.getErrorMessage()).contains("All pages failed during extraction");
        assertThat(paper.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("Graceful cancellation via requestCancel halts extraction loop before next page")
    void testGracefulCancellationHaltsProcessing() throws IOException {
        byte[] pdfBytes = createTestPdf(3);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("NEET UG 2024")
                .examYear(2024)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("gemini-2.5-pro");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());

        LlmPageExtractionResult page1Result = LlmPageExtractionResult.builder()
                .coverPage(true)
                .questions(Collections.emptyList())
                .build();

        // During page 1 extraction, request cancellation
        when(extractionClient.extractPage(any(PdfPageContent.class), anyString())).thenAnswer(invocation -> {
            worker.requestCancel(paperId);
            return page1Result;
        });

        worker.processPaperExtractionJob(paperId, tenantId);

        assertThat(paper.getExtractionStatus()).isEqualTo(PaperExtractionStatus.CANCELLED);
        // Page 1 was processed before cancel was checked, but pages 2 and 3 were skipped
        verify(extractionClient, times(1)).extractPage(any(PdfPageContent.class), anyString());
    }

    @Test
    @DisplayName("Cancellation detected via database status breaks processing loop")
    void testCancellationViaDatabaseStatus() throws IOException {
        byte[] pdfBytes = createTestPdf(2);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("CLAT 2024")
                .examYear(2024)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        QuestionPaperMetadata cancelledPaper = QuestionPaperMetadata.builder()
                .examinationName("CLAT 2024")
                .examYear(2024)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .extractionStatus(PaperExtractionStatus.CANCELLED)
                .build();
        ReflectionTestUtils.setField(cancelledPaper, "id", paperId);
        cancelledPaper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId))
                .thenReturn(Optional.of(paper))
                .thenReturn(Optional.of(cancelledPaper));

        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("gemini-2.5-pro");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());

        worker.processPaperExtractionJob(paperId, tenantId);

        verify(extractionClient, never()).extractPage(any(), any());
    }

    @Test
    @DisplayName("Fatal file load exception invokes handleJobLevelFailure and marks paper FAILED")
    void testFatalFileLoadException() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("JEE Advanced 2024")
                .examYear(2024)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("gemini-2.5-pro");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId)))
                .thenThrow(new IllegalStateException("Asset download error"));

        worker.processPaperExtractionJob(paperId, tenantId);

        assertThat(paper.getExtractionStatus()).isEqualTo(PaperExtractionStatus.FAILED);
        assertThat(paper.getErrorMessage()).contains("Asset download error");
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("Paper not found logs warning and exits gracefully")
    void testPaperNotFoundExitsGracefully() {
        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.empty());

        worker.processPaperExtractionJob(paperId, tenantId);

        verify(assetServiceClient, never()).downloadAssetBytes(any(), any());
        verifyNoInteractions(extractionClient);
    }

    @Test
    @DisplayName("Taxonomy resolves hierarchy from Subject Name when default IDs are null")
    void testTaxonomyResolutionByName() throws IOException {
        byte[] pdfBytes = createTestPdf(1);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("Board Exam 2024")
                .examYear(2024)
                .subjectName("Mathematics")
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("gemini-2.5-pro");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());
        when(extractedQuestionRepository.save(any(ExtractedQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Subject mathSub = Subject.builder().name("Mathematics").build();
        ReflectionTestUtils.setField(mathSub, "id", 555L);
        when(subjectRepository.findByNameAndTenantId("Mathematics", tenantId)).thenReturn(Optional.of(mathSub));

        Topic algebraTopic = Topic.builder().name("Algebra").build();
        ReflectionTestUtils.setField(algebraTopic, "id", 777L);
        when(topicRepository.findBySubjectIdAndTenantId(555L, tenantId)).thenReturn(List.of(algebraTopic));

        Subtopic quadSubtopic = Subtopic.builder().name("Quadratic Equations").build();
        ReflectionTestUtils.setField(quadSubtopic, "id", 888L);
        when(subtopicRepository.findByTopicIdAndTenantId(777L, tenantId)).thenReturn(List.of(quadSubtopic));

        LlmPageExtractionResult.ExtractedItemPayload q = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(1)
                .content("Solve $$x^2 - 4 = 0$$.")
                .options(List.of(new QuestionOption("A", "$$x = \\pm 2$$", true, null, null)))
                .answerKey("A")
                .build();

        when(extractionClient.extractPage(any(PdfPageContent.class), anyString()))
                .thenReturn(LlmPageExtractionResult.builder().coverPage(false).questions(List.of(q)).build());

        worker.processPaperExtractionJob(paperId, tenantId);

        ArgumentCaptor<ExtractedQuestion> captor = ArgumentCaptor.forClass(ExtractedQuestion.class);
        verify(extractedQuestionRepository).save(captor.capture());
        ExtractedQuestion saved = captor.getValue();

        assertThat(saved.getSuggestedSubjectId()).isEqualTo(555L);
        assertThat(saved.getSuggestedTopicId()).isEqualTo(777L);
        assertThat(saved.getSuggestedSubtopicId()).isEqualTo(888L);
    }

    @Test
    @DisplayName("LlmQuotaExhaustedException pauses job in QUOTA_EXHAUSTED status without marking page failed")
    void testQuotaExhaustedExceptionTransitionsToQuotaExhausted() throws IOException {
        byte[] pdfBytes = createTestPdf(2);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("UPSC Prelims")
                .examYear(2024)
                .fileAssetId(assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(i -> i.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("gemini-2.5-flash");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());

        when(extractionClient.extractPage(any(PdfPageContent.class), anyString()))
                .thenThrow(new LlmQuotaExhaustedException(1, "Rate limit reached 429", null));

        worker.processPaperExtractionJob(paperId, tenantId);

        assertThat(paper.getExtractionStatus()).isEqualTo(PaperExtractionStatus.QUOTA_EXHAUSTED);
        assertThat(paper.getErrorMessage()).contains("LLM quota exhausted on page 1");
        assertThat(paper.getFailedPages()).isEmpty();
    }

    @Test
    @DisplayName("Active passage propagates to subsequent questions in range across pages")
    void testPassagePropagationAcrossPages() throws IOException {
        byte[] pdfBytes = createTestPdf(3);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("English Comprehension")
                .examYear(2024)
                .fileAssetId(assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(i -> i.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("mock-model");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());

        // Page 1: Q1 with passage range 1..2
        LlmPageExtractionResult.ExtractedItemPayload q1 = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(1)
                .content("What is the central theme?")
                .passageText("Artificial Intelligence is evolving rapidly.")
                .passageRangeStart(1)
                .passageRangeEnd(2)
                .build();

        // Page 2: Q2 without passage
        LlmPageExtractionResult.ExtractedItemPayload q2 = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(2)
                .content("According to the author, what is key?")
                .passageText(null)
                .build();

        // Page 3: Q3 outside range without passage
        LlmPageExtractionResult.ExtractedItemPayload q3 = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(3)
                .content("Standalone question.")
                .passageText(null)
                .build();

        when(extractionClient.extractPage(any(PdfPageContent.class), anyString()))
                .thenReturn(LlmPageExtractionResult.builder().questions(List.of(q1)).build())
                .thenReturn(LlmPageExtractionResult.builder().questions(List.of(q2)).build())
                .thenReturn(LlmPageExtractionResult.builder().questions(List.of(q3)).build());

        worker.processPaperExtractionJob(paperId, tenantId);

        ArgumentCaptor<ExtractedQuestion> captor = ArgumentCaptor.forClass(ExtractedQuestion.class);
        verify(extractedQuestionRepository, times(3)).save(captor.capture());
        List<ExtractedQuestion> saved = captor.getAllValues();

        assertThat(saved.get(0).getPassageText()).isEqualTo("Artificial Intelligence is evolving rapidly.");
        assertThat(saved.get(1).getPassageText()).isEqualTo("Artificial Intelligence is evolving rapidly.");
        assertThat(saved.get(2).getPassageText()).isNull();
    }

    @Test
    @DisplayName("Cropped diagram data URIs are attached to question mediaUrls and flag hasImages")
    void testCroppedDiagramDataUrisAddedToMediaUrls() throws IOException {
        byte[] pdfBytes = createTestPdf(1);

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("Physics")
                .examYear(2024)
                .fileAssetId(assetId)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(paper, "id", paperId);
        paper.setTenantId(tenantId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(i -> i.getArgument(0));
        when(modelRouter.selectPaperExtractionModel(true)).thenReturn("mock-model");
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId))
                .thenReturn(Collections.emptyList());

        LlmPageExtractionResult.ExtractedItemPayload q = LlmPageExtractionResult.ExtractedItemPayload.builder()
                .questionNumber(1)
                .content("Refer to the figure below:")
                .croppedDiagramDataUris(List.of("data:image/jpeg;base64,sampleCrop"))
                .build();

        when(extractionClient.extractPage(any(PdfPageContent.class), anyString()))
                .thenReturn(LlmPageExtractionResult.builder().questions(List.of(q)).build());

        worker.processPaperExtractionJob(paperId, tenantId);

        ArgumentCaptor<ExtractedQuestion> captor = ArgumentCaptor.forClass(ExtractedQuestion.class);
        verify(extractedQuestionRepository).save(captor.capture());
        ExtractedQuestion saved = captor.getValue();

        assertThat(saved.isHasImages()).isTrue();
        assertThat(saved.getMediaUrls()).contains("data:image/jpeg;base64,sampleCrop");
    }
}
