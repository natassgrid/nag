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

import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.extraction.ai.QuestionPaperPdfParser;
import com.examplatform.questionbank.extraction.client.AssetMetadataDto;
import com.examplatform.questionbank.extraction.client.AssetServiceClient;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import com.examplatform.questionbank.extraction.dto.*;
import com.examplatform.questionbank.extraction.repository.ExtractedQuestionRepository;
import com.examplatform.questionbank.extraction.repository.QuestionPaperMetadataRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionPaperExtractionService Unit Tests")
class QuestionPaperExtractionServiceTest {

    @Mock
    private QuestionPaperMetadataRepository metadataRepository;

    @Mock
    private ExtractedQuestionRepository extractedQuestionRepository;

    @Mock
    private AsyncPaperExtractionWorker asyncWorker;

    @Mock
    private QuestionPaperPdfParser pdfParser;

    @Mock
    private AssetServiceClient assetServiceClient;

    private QuestionPaperExtractionService service;

    private final String tenantId = "tenant-test";
    private final UUID paperId = UUID.randomUUID();
    private final UUID assetId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QuestionPaperExtractionService(
                metadataRepository,
                extractedQuestionRepository,
                asyncWorker,
                pdfParser,
                assetServiceClient
        );
        lenient().when(pdfParser.normalizeMathAndFormatting(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("initiateExtraction fetches asset binary, registers paper and triggers async worker")
    void testInitiateExtraction() {
        byte[] pdfBytes = "%PDF-1.4 sample content".getBytes();

        PaperUploadRequest request = PaperUploadRequest.builder()
                .fileAssetId(assetId)
                .examinationName("UPSC Prelims 2024")
                .examYear(2024)
                .department("UPSC")
                .paperCode("GS-1")
                .build();

        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(assetServiceClient.getAssetMetadata(eq(assetId), eq(tenantId)))
                .thenReturn(AssetMetadataDto.builder()
                        .id(assetId)
                        .originalFilename("upsc_2024.pdf")
                        .sha256Hash("sha256-hash-123")
                        .fileSize((long) pdfBytes.length)
                        .build());

        when(metadataRepository.findFirstByTenantIdAndFileSha256OrderByCreatedAtDesc(eq(tenantId), eq("sha256-hash-123")))
                .thenReturn(Optional.empty());

        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> {
            QuestionPaperMetadata m = invocation.getArgument(0);
            if (m.getId() == null) {
                org.springframework.test.util.ReflectionTestUtils.setField(m, "id", paperId);
            }
            return m;
        });

        QuestionPaperMetadataResponse response = service.initiateExtraction(
                request, UUID.randomUUID(), tenantId);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(paperId);
        assertThat(response.getExaminationName()).isEqualTo("UPSC Prelims 2024");
        assertThat(response.getFileAssetId()).isEqualTo(assetId);
        assertThat(response.getFileStoragePath()).isEqualTo("asset://" + assetId);

        verify(assetServiceClient).downloadAssetBytes(eq(assetId), eq(tenantId));
        verify(assetServiceClient).createReference(eq(assetId), eq("QUESTION_PAPER"), eq(paperId), eq(tenantId));
        verify(asyncWorker).processPaperExtractionJob(eq(paperId), eq(tenantId));
    }

    @Test
    @DisplayName("initiateExtraction throws when fileAssetId is missing")
    void testInitiateExtractionMissingFileAssetId() {
        PaperUploadRequest request = PaperUploadRequest.builder()
                .examinationName("UPSC Prelims 2024")
                .examYear(2024)
                .build();

        assertThatThrownBy(() -> service.initiateExtraction(request, UUID.randomUUID(), tenantId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("fileAssetId");
    }

    @Test
    @DisplayName("initiateExtraction returns existing paper when SHA-256 matches and forceReExtract is false")
    void testInitiateExtractionIdempotent() {
        byte[] pdfBytes = "%PDF-1.4 sample content".getBytes();

        PaperUploadRequest request = PaperUploadRequest.builder()
                .fileAssetId(assetId)
                .examinationName("UPSC Prelims 2024")
                .examYear(2024)
                .forceReExtract(false)
                .build();

        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);
        when(assetServiceClient.getAssetMetadata(eq(assetId), eq(tenantId)))
                .thenReturn(AssetMetadataDto.builder()
                        .id(assetId)
                        .originalFilename("upsc_2024.pdf")
                        .sha256Hash("sha256-existing-hash")
                        .build());

        QuestionPaperMetadata existing = QuestionPaperMetadata.builder()
                .examinationName("UPSC Prelims 2024")
                .examYear(2024)
                .fileSha256("sha256-existing-hash")
                .extractionStatus(PaperExtractionStatus.COMPLETED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(existing, "id", paperId);

        when(metadataRepository.findFirstByTenantIdAndFileSha256OrderByCreatedAtDesc(eq(tenantId), eq("sha256-existing-hash")))
                .thenReturn(Optional.of(existing));

        QuestionPaperMetadataResponse response = service.initiateExtraction(
                request, UUID.randomUUID(), tenantId);

        assertThat(response.getId()).isEqualTo(paperId);
        verify(asyncWorker, never()).processPaperExtractionJob(any(), any());
    }

    @Test
    @DisplayName("applyBulkAnswerKey updates extracted questions and approves them")
    void testApplyBulkAnswerKey() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("RRB NTPC")
                .examYear(2024)
                .questionsExtracted(2)
                .questionsApproved(0)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        ExtractedQuestion q1 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.findByPaperMetadataIdAndQuestionNumber(eq(paperId), eq(1)))
                .thenReturn(Optional.of(q1));
        when(extractedQuestionRepository.findByPaperMetadataIdAndQuestionNumber(eq(paperId), eq(2)))
                .thenReturn(Optional.empty());
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(i -> i.getArgument(0));

        BulkAnswerKeyRequest req = BulkAnswerKeyRequest.builder()
                .answersByQuestionNumber(Map.of(1, "B", 2, "C"))
                .autoApproveMatched(true)
                .build();

        int result = service.applyBulkAnswerKey(
                paperId, req, UUID.randomUUID(), tenantId);

        assertThat(result).isEqualTo(1);
        assertThat(q1.getAnswerKey()).isEqualTo("B");
        assertThat(q1.getStatus()).isEqualTo(ExtractedQuestionStatus.APPROVED);
        verify(extractedQuestionRepository).save(q1);
    }

    @Test
    @DisplayName("updateExtractedQuestion updates question fields and review status")
    void testUpdateExtractedQuestion() {
        UUID qId = UUID.randomUUID();
        ExtractedQuestion q = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(5)
                .content("Original content")
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q, "id", qId);

        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(qId), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(q));
        when(extractedQuestionRepository.save(any(ExtractedQuestion.class))).thenAnswer(i -> i.getArgument(0));

        UpdateExtractedQuestionRequest req = UpdateExtractedQuestionRequest.builder()
                .content("Corrected content with $$E = mc^2$$")
                .answerKey("A")
                .status(ExtractedQuestionStatus.APPROVED)
                .reviewComments("Verified stem")
                .build();

        var result = service.updateExtractedQuestion(paperId, qId, req, UUID.randomUUID(), tenantId);

        assertThat(result.getContent()).isEqualTo("Corrected content with $$E = mc^2$$");
        assertThat(result.getAnswerKey()).isEqualTo("A");
        assertThat(result.getStatus()).isEqualTo(ExtractedQuestionStatus.APPROVED);
    }

    @Test
    @DisplayName("cancelExtractionJob delegates cancellation to async worker")
    void testCancelExtractionJob() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("GATE")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.PROCESSING)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(i -> i.getArgument(0));

        QuestionPaperMetadataResponse response = service.cancelExtractionJob(paperId, tenantId);

        assertThat(response.getExtractionStatus()).isEqualTo(PaperExtractionStatus.CANCELLED);
        verify(asyncWorker).requestCancel(paperId);
    }

    @Test
    @DisplayName("parsePageSelection correctly parses ranges, lists, and clamps to totalPages")
    void testParsePageSelection() {
        List<Integer> parsed = QuestionPaperExtractionService.parsePageSelection("1-5,8,10-12", 15);
        assertThat(parsed).containsExactly(1, 2, 3, 4, 5, 8, 10, 11, 12);

        List<Integer> clamped = QuestionPaperExtractionService.parsePageSelection("1-5,8,10-12", 6);
        assertThat(clamped).containsExactly(1, 2, 3, 4, 5);

        List<Integer> allPages = QuestionPaperExtractionService.parsePageSelection(null, 4);
        assertThat(allPages).containsExactly(1, 2, 3, 4);

        List<Integer> blankPages = QuestionPaperExtractionService.parsePageSelection("   ", 3);
        assertThat(blankPages).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("parsePageSelection throws IllegalArgumentException on invalid syntax")
    void testParsePageSelectionInvalidSyntaxThrows() {
        assertThatThrownBy(() -> QuestionPaperExtractionService.parsePageSelection("abc", 10))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> QuestionPaperExtractionService.parsePageSelection("5-2", 10))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> QuestionPaperExtractionService.parsePageSelection("0-5", 10))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> QuestionPaperExtractionService.parsePageSelection("1,,3", 10))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> QuestionPaperExtractionService.parsePageSelection("-3", 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("computeResumePages returns selected pages without extracted questions OR in failedPages")
    void testComputeResumePages() {
        List<Integer> selectedPages = List.of(1, 2, 3, 4, 5);
        Set<Integer> pagesWithQuestions = Set.of(1, 2, 3);
        Set<Integer> failedPages = Set.of(3);

        List<Integer> toProcess = QuestionPaperExtractionService.computeResumePages(
                selectedPages, pagesWithQuestions, failedPages);

        assertThat(toProcess).containsExactly(3, 4, 5);
    }

    @Test
    @DisplayName("resumeExtractionJob transitions eligible paper from QUOTA_EXHAUSTED to PROCESSING")
    void testResumeExtractionJobEligible() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("UPSC Prelims")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.QUOTA_EXHAUSTED)
                .lastCompletedPage(2)
                .errorMessage("LLM quota exceeded")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(i -> i.getArgument(0));

        QuestionPaperMetadataResponse response = service.resumeExtractionJob(paperId, tenantId);

        assertThat(response.getExtractionStatus()).isEqualTo(PaperExtractionStatus.PROCESSING);
        assertThat(response.getErrorMessage()).isNull();
        verify(asyncWorker).processPaperExtractionJob(eq(paperId), eq(tenantId));
    }

    @Test
    @DisplayName("resumeExtractionJob rejects paper not in resumable status")
    void testResumeExtractionJobIneligible() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("UPSC Prelims")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.COMPLETED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));

        assertThatThrownBy(() -> service.resumeExtractionJob(paperId, tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot be resumed from status: COMPLETED");

        verify(asyncWorker, never()).processPaperExtractionJob(any(), any());
    }

    @Test
    @DisplayName("renderPageImage downloads PDF, renders JPEG bytes and caches subsequent calls")
    void testRenderPageImage() throws IOException {
        byte[] pdfBytes;
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            pdfBytes = baos.toByteArray();
        }

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("Sample Exam")
                .examYear(2024)
                .fileAssetId(assetId)
                .totalPages(1)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(assetServiceClient.downloadAssetBytes(eq(assetId), eq(tenantId))).thenReturn(pdfBytes);

        byte[] firstCall = service.renderPageImage(paperId, 1, tenantId);
        assertThat(firstCall).isNotEmpty();

        // Second call should hit in-memory LRU cache
        byte[] secondCall = service.renderPageImage(paperId, 1, tenantId);
        assertThat(secondCall).isSameAs(firstCall);

        verify(assetServiceClient, times(1)).downloadAssetBytes(eq(assetId), eq(tenantId));
    }

    @Test
    @DisplayName("mergeQuestions combines content with \\n\\n, preserves first non-empty options and deletes merged items")
    void testMergeQuestions() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("Exam")
                .examYear(2024)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        ExtractedQuestion q1 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .content("Statement 1: The sun rises in the east.")
                .options(new ArrayList<>())
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q1, "id", q1Id);

        ExtractedQuestion q2 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(2)
                .content("Statement 2: The moon orbits the earth.")
                .options(List.of(QuestionOption.builder().id("A").text("Both true").build()))
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q2, "id", q2Id);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(q1Id), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(q1));
        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(q2Id), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(q2));
        when(extractedQuestionRepository.save(any(ExtractedQuestion.class))).thenAnswer(i -> i.getArgument(0));

        MergeQuestionsRequest req = MergeQuestionsRequest.builder()
                .questionIds(List.of(q1Id, q2Id))
                .build();

        ExtractedQuestionResponse merged = service.mergeQuestions(paperId, req, UUID.randomUUID(), tenantId);

        assertThat(merged.getContent()).isEqualTo("Statement 1: The sun rises in the east.\n\nStatement 2: The moon orbits the earth.");
        assertThat(merged.getOptions()).hasSize(1);
        assertThat(merged.getOptions().get(0).getText()).isEqualTo("Both true");
        verify(extractedQuestionRepository).deleteAll(List.of(q2));
    }

    @Test
    @DisplayName("mergeQuestions throws IllegalStateException when any question is PROMOTED")
    void testMergeQuestionsRefusesPromoted() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("Exam")
                .examYear(2024)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        ExtractedQuestion q1 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .status(ExtractedQuestionStatus.PROMOTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q1, "id", q1Id);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(q1Id), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(q1));

        MergeQuestionsRequest req = MergeQuestionsRequest.builder()
                .questionIds(List.of(q1Id, q2Id))
                .build();

        assertThatThrownBy(() -> service.mergeQuestions(paperId, req, UUID.randomUUID(), tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROMOTED");
    }

    @Test
    @DisplayName("splitQuestion updates original with firstContent and inserts second question with shifted subsequent")
    void testSplitQuestion() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("Exam")
                .examYear(2024)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        ExtractedQuestion q1 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .content("Combined content")
                .sourcePageNumber(1)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q1, "id", q1Id);

        ExtractedQuestion q2 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(2)
                .content("Subsequent question")
                .sourcePageNumber(1)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q2, "id", q2Id);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(q1Id), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(q1));
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(eq(paperId), eq(tenantId)))
                .thenReturn(List.of(q1, q2));
        when(extractedQuestionRepository.saveAndFlush(any(ExtractedQuestion.class))).thenAnswer(i -> i.getArgument(0));

        SplitQuestionRequest req = SplitQuestionRequest.builder()
                .firstContent("First part")
                .secondContent("Second part")
                .build();

        List<ExtractedQuestionResponse> result = service.splitQuestion(paperId, q1Id, req, UUID.randomUUID(), tenantId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("First part");
        assertThat(result.get(0).getQuestionNumber()).isEqualTo(1);
        assertThat(result.get(1).getContent()).isEqualTo("Second part");
        assertThat(result.get(1).getQuestionNumber()).isEqualTo(2);

        // Subsequent question q2 shifted from 2 to 3
        assertThat(q2.getQuestionNumber()).isEqualTo(3);
    }

    @Test
    @DisplayName("splitQuestion throws IllegalStateException when question is PROMOTED")
    void testSplitQuestionRefusesPromoted() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder().build();
        UUID q1Id = UUID.randomUUID();
        ExtractedQuestion q1 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .status(ExtractedQuestionStatus.PROMOTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q1, "id", q1Id);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(q1Id), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(q1));

        SplitQuestionRequest req = SplitQuestionRequest.builder()
                .firstContent("A")
                .secondContent("B")
                .build();

        assertThatThrownBy(() -> service.splitQuestion(paperId, q1Id, req, UUID.randomUUID(), tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROMOTED");
    }

    @Test
    @DisplayName("resequenceQuestions renumbers questions sequentially using two-phase shift")
    void testResequenceQuestions() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder().build();
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();

        ExtractedQuestion q1 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(10)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q1, "id", q1Id);

        ExtractedQuestion q2 = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(20)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q2, "id", q2Id);

        when(metadataRepository.findByIdAndTenantId(eq(paperId), eq(tenantId))).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(eq(paperId), eq(tenantId)))
                .thenReturn(List.of(q1, q2));

        ResequenceQuestionsRequest req = ResequenceQuestionsRequest.builder()
                .orderedQuestionIds(List.of(q2Id, q1Id))
                .startNumber(1)
                .build();

        List<ExtractedQuestionResponse> resequenced = service.resequenceQuestions(paperId, req, UUID.randomUUID(), tenantId);

        assertThat(resequenced).hasSize(2);
        assertThat(resequenced.get(0).getId()).isEqualTo(q2Id);
        assertThat(resequenced.get(0).getQuestionNumber()).isEqualTo(1);
        assertThat(resequenced.get(1).getId()).isEqualTo(q1Id);
        assertThat(resequenced.get(1).getQuestionNumber()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateExtractedQuestion throws IllegalStateException when question is PROMOTED or number collides")
    void testUpdateExtractedQuestionValidations() {
        UUID qId = UUID.randomUUID();
        ExtractedQuestion q = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .status(ExtractedQuestionStatus.PROMOTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(q, "id", qId);

        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(qId), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(q));

        UpdateExtractedQuestionRequest req = UpdateExtractedQuestionRequest.builder()
                .content("New content")
                .build();

        assertThatThrownBy(() -> service.updateExtractedQuestion(paperId, qId, req, UUID.randomUUID(), tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROMOTED");

        // Conflict check
        ExtractedQuestion qEditable = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(qEditable, "id", qId);

        UUID otherId = UUID.randomUUID();
        ExtractedQuestion conflictQ = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(2)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(conflictQ, "id", otherId);

        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(eq(qId), eq(paperId), eq(tenantId)))
                .thenReturn(Optional.of(qEditable));
        when(extractedQuestionRepository.findByPaperMetadataIdAndQuestionNumber(eq(paperId), eq(2)))
                .thenReturn(Optional.of(conflictQ));

        UpdateExtractedQuestionRequest conflictReq = UpdateExtractedQuestionRequest.builder()
                .questionNumber(2)
                .build();

        assertThatThrownBy(() -> service.updateExtractedQuestion(paperId, qId, conflictReq, UUID.randomUUID(), tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already in use");
    }

    @Test
    @DisplayName("deletePaper deletes metadata and cascades deletion of extracted questions")
    void testDeletePaperSuccess() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("UPSC Prelims")
                .extractionStatus(PaperExtractionStatus.PROCESSING)
                .build();
        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));

        service.deletePaper(paperId, tenantId);

        assertThat(paper.getExtractionStatus()).isEqualTo(PaperExtractionStatus.CANCELLED);
        verify(extractedQuestionRepository).deleteByPaperMetadataIdAndTenantId(paperId, tenantId);
        verify(metadataRepository).delete(paper);
    }

    @Test
    @DisplayName("deletePaper throws EntityNotFoundException when paper does not exist")
    void testDeletePaperNotFoundThrows() {
        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deletePaper(paperId, tenantId))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    @DisplayName("deleteExtractedQuestion deletes staging item and refreshes counters")
    void testDeleteExtractedQuestionSuccess() {
        UUID qId = UUID.randomUUID();
        ExtractedQuestion q = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .questionsExtracted(5)
                .build();

        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(qId, paperId, tenantId))
                .thenReturn(Optional.of(q));
        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.countByPaperMetadataIdAndTenantId(paperId, tenantId)).thenReturn(4L);
        when(extractedQuestionRepository.countByPaperMetadataIdAndStatusAndTenantId(paperId, ExtractedQuestionStatus.APPROVED, tenantId)).thenReturn(0L);
        when(extractedQuestionRepository.countByPaperMetadataIdAndStatusAndTenantId(paperId, ExtractedQuestionStatus.PROMOTED, tenantId)).thenReturn(0L);

        service.deleteExtractedQuestion(paperId, qId, tenantId);

        verify(extractedQuestionRepository).delete(q);
        assertThat(paper.getQuestionsExtracted()).isEqualTo(4);
        verify(metadataRepository).save(paper);
    }

    @Test
    @DisplayName("deleteExtractedQuestion refuses to delete PROMOTED question")
    void testDeleteExtractedQuestionPromotedThrows() {
        UUID qId = UUID.randomUUID();
        ExtractedQuestion q = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .status(ExtractedQuestionStatus.PROMOTED)
                .build();

        when(extractedQuestionRepository.findByIdAndPaperMetadataIdAndTenantId(qId, paperId, tenantId))
                .thenReturn(Optional.of(q));

        assertThatThrownBy(() -> service.deleteExtractedQuestion(paperId, qId, tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been promoted");
    }

    @Test
    @DisplayName("batchDeleteExtractedQuestions deletes multiple questions and updates counters")
    void testBatchDeleteExtractedQuestionsSuccess() {
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();
        ExtractedQuestion q1 = ExtractedQuestion.builder().paperMetadataId(paperId).questionNumber(1).status(ExtractedQuestionStatus.EXTRACTED).build();
        ExtractedQuestion q2 = ExtractedQuestion.builder().paperMetadataId(paperId).questionNumber(2).status(ExtractedQuestionStatus.EXTRACTED).build();
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder().questionsExtracted(10).build();

        when(extractedQuestionRepository.findByIdInAndPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(List.of(q1Id, q2Id), paperId, tenantId))
                .thenReturn(List.of(q1, q2));
        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(extractedQuestionRepository.countByPaperMetadataIdAndTenantId(paperId, tenantId)).thenReturn(8L);

        service.batchDeleteExtractedQuestions(paperId, List.of(q1Id, q2Id), tenantId);

        verify(extractedQuestionRepository).deleteAll(List.of(q1, q2));
        assertThat(paper.getQuestionsExtracted()).isEqualTo(8);
    }

    @Test
    @DisplayName("batchDeleteExtractedQuestions throws when any question is PROMOTED")
    void testBatchDeleteExtractedQuestionsPromotedThrows() {
        UUID q1Id = UUID.randomUUID();
        UUID q2Id = UUID.randomUUID();
        ExtractedQuestion q1 = ExtractedQuestion.builder().paperMetadataId(paperId).questionNumber(1).status(ExtractedQuestionStatus.EXTRACTED).build();
        ExtractedQuestion q2 = ExtractedQuestion.builder().paperMetadataId(paperId).questionNumber(2).status(ExtractedQuestionStatus.PROMOTED).build();
        org.springframework.test.util.ReflectionTestUtils.setField(q2, "id", q2Id);

        when(extractedQuestionRepository.findByIdInAndPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(List.of(q1Id, q2Id), paperId, tenantId))
                .thenReturn(List.of(q1, q2));

        assertThatThrownBy(() -> service.batchDeleteExtractedQuestions(paperId, List.of(q1Id, q2Id), tenantId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("promoted question");
    }
}
