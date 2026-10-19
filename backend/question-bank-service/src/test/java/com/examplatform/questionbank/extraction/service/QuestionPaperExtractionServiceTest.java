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

import com.examplatform.questionbank.extraction.ai.QuestionPaperPdfParser;
import com.examplatform.questionbank.extraction.client.AssetMetadataDto;
import com.examplatform.questionbank.extraction.client.AssetServiceClient;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import com.examplatform.questionbank.extraction.domain.PaperExtractionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import com.examplatform.questionbank.extraction.dto.BulkAnswerKeyRequest;
import com.examplatform.questionbank.extraction.dto.PaperUploadRequest;
import com.examplatform.questionbank.extraction.dto.QuestionPaperMetadataResponse;
import com.examplatform.questionbank.extraction.dto.UpdateExtractedQuestionRequest;
import com.examplatform.questionbank.extraction.repository.ExtractedQuestionRepository;
import com.examplatform.questionbank.extraction.repository.QuestionPaperMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
}
