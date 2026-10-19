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
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    private PaperFileStorageService paperFileStorageService;

    @Mock
    private AsyncPaperExtractionWorker asyncWorker;

    @Mock
    private QuestionPaperPdfParser pdfParser;

    private QuestionPaperExtractionService service;

    private final String tenantId = "tenant-test";
    private final UUID paperId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new QuestionPaperExtractionService(
                metadataRepository,
                extractedQuestionRepository,
                paperFileStorageService,
                asyncWorker,
                pdfParser
        );
    }

    @Test
    @DisplayName("uploadAndStartExtraction stores file and triggers async extraction")
    void testUploadAndStartExtraction() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "exam.pdf", "application/pdf", "%PDF-1.4 sample content".getBytes());

        PaperUploadRequest request = PaperUploadRequest.builder()
                .examinationName("UPSC Prelims 2024")
                .examYear(2024)
                .build();

        when(paperFileStorageService.computeSha256(any())).thenReturn("sha256-hash-123");
        when(metadataRepository.findFirstByTenantIdAndFileSha256OrderByCreatedAtDesc(eq(tenantId), eq("sha256-hash-123")))
                .thenReturn(Optional.empty());

        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> {
            QuestionPaperMetadata m = invocation.getArgument(0);
            if (m.getId() == null) {
                // simulate JPA prePersist
                org.springframework.test.util.ReflectionTestUtils.setField(m, "id", paperId);
            }
            return m;
        });

        when(paperFileStorageService.storePdf(eq(paperId), eq(tenantId), eq("exam.pdf"), any()))
                .thenReturn("/storage/tenant-test/" + paperId + ".pdf");

        QuestionPaperMetadataResponse response = service.uploadAndStartExtraction(file, request, UUID.randomUUID(), tenantId);

        assertThat(response).isNotNull();
        assertThat(response.getExaminationName()).isEqualTo("UPSC Prelims 2024");
        verify(asyncWorker).processPaperExtractionJob(eq(paperId), eq(tenantId));
    }

    @Test
    @DisplayName("cancelExtractionJob cancels active extraction")
    void testCancelExtractionJob() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("JEE Main 2024")
                .examYear(2024)
                .extractionStatus(PaperExtractionStatus.PROCESSING)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(paper, "id", paperId);

        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));
        when(metadataRepository.save(any(QuestionPaperMetadata.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionPaperMetadataResponse response = service.cancelExtractionJob(paperId, tenantId);

        assertThat(response.getExtractionStatus()).isEqualTo(PaperExtractionStatus.CANCELLED);
        verify(asyncWorker).requestCancel(paperId);
    }

    @Test
    @DisplayName("applyBulkAnswerKey updates keys and auto-approves questions")
    void testApplyBulkAnswerKey() {
        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName("GATE 2024")
                .examYear(2024)
                .build();
        when(metadataRepository.findByIdAndTenantId(paperId, tenantId)).thenReturn(Optional.of(paper));

        ExtractedQuestion eq = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(1)
                .content("What is Newton's third law?")
                .status(ExtractedQuestionStatus.EXTRACTED)
                .build();

        when(extractedQuestionRepository.findByPaperMetadataIdAndQuestionNumber(paperId, 1))
                .thenReturn(Optional.of(eq));

        BulkAnswerKeyRequest request = BulkAnswerKeyRequest.builder()
                .answersByQuestionNumber(Map.of(1, "B"))
                .autoApproveMatched(true)
                .build();

        int updated = service.applyBulkAnswerKey(paperId, request, UUID.randomUUID(), tenantId);

        assertThat(updated).isEqualTo(1);
        assertThat(eq.getAnswerKey()).isEqualTo("B");
        assertThat(eq.getStatus()).isEqualTo(ExtractedQuestionStatus.APPROVED);
        verify(extractedQuestionRepository).save(eq);
    }
}
