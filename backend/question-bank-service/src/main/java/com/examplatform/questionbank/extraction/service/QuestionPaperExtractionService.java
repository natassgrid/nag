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
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Primary orchestrator service for Question Paper PDF extraction operations:
 * file uploading, job querying, extracted question review, bulk answer key injection,
 * and lifecycle retry/cancellation commands.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionPaperExtractionService {

    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final QuestionPaperMetadataRepository metadataRepository;
    private final ExtractedQuestionRepository extractedQuestionRepository;
    private final PaperFileStorageService paperFileStorageService;
    private final AsyncPaperExtractionWorker asyncWorker;
    private final QuestionPaperPdfParser pdfParser;
    private final AssetServiceClient assetServiceClient;

    /**
     * Initiates asynchronous question paper extraction for a PDF document pre-uploaded
     * to asset-service.
     *
     * @param request paper metadata and extraction configurations containing fileAssetId
     * @param uploadedBy user UUID initiating the extraction
     * @param tenantId examination authority identifier
     * @return initialized QuestionPaperMetadataResponse
     */
    public QuestionPaperMetadataResponse initiateExtraction(
            PaperUploadRequest request,
            UUID uploadedBy,
            String tenantId) {

        if (request == null) {
            throw new IllegalArgumentException("PaperUploadRequest must not be null");
        }
        if (request.getFileAssetId() == null) {
            throw new IllegalArgumentException("fileAssetId from asset-service upload is required");
        }
        if (request.getExaminationName() == null || request.getExaminationName().isBlank()) {
            throw new IllegalArgumentException("examinationName is required");
        }
        if (request.getExamYear() == null) {
            throw new IllegalArgumentException("examYear is required");
        }

        UUID assetId = request.getFileAssetId();
        byte[] bytes = assetServiceClient.downloadAssetBytes(assetId, tenantId);
        if (bytes == null || bytes.length == 0) {
            throw new IllegalStateException("Failed to download PDF bytes for asset ID: " + assetId);
        }

        AssetMetadataDto assetMeta = assetServiceClient.getAssetMetadata(assetId, tenantId);
        String filename = (assetMeta != null && assetMeta.getOriginalFilename() != null && !assetMeta.getOriginalFilename().isBlank())
                ? assetMeta.getOriginalFilename()
                : (request.getPaperCode() != null && !request.getPaperCode().isBlank() ? request.getPaperCode() + ".pdf" : "paper-" + assetId + ".pdf");

        String sha256 = (assetMeta != null && assetMeta.getSha256Hash() != null && !assetMeta.getSha256Hash().isBlank())
                ? assetMeta.getSha256Hash()
                : paperFileStorageService.computeSha256(bytes);

        if (!request.isForceReExtract()) {
            Optional<QuestionPaperMetadata> existingOpt = metadataRepository
                    .findFirstByTenantIdAndFileSha256OrderByCreatedAtDesc(tenantId, sha256);
            if (existingOpt.isPresent()) {
                QuestionPaperMetadata existing = existingOpt.get();
                if (existing.getExtractionStatus() == PaperExtractionStatus.COMPLETED
                        || existing.getExtractionStatus() == PaperExtractionStatus.PROCESSING) {
                    log.info("Identical PDF already exists (status={}) for tenantId={}. Returning existing record {}.",
                            existing.getExtractionStatus(), tenantId, existing.getId());
                    return QuestionPaperMetadataResponse.fromEntity(existing);
                }
            }
        }

        UUID resolvedUploader = uploadedBy != null ? uploadedBy : SYSTEM_USER_ID;

        QuestionPaperMetadata paper = QuestionPaperMetadata.builder()
                .examinationName(request.getExaminationName().trim())
                .examYear(request.getExamYear())
                .department(request.getDepartment() != null ? request.getDepartment().trim() : null)
                .paperCode(request.getPaperCode() != null ? request.getPaperCode().trim() : null)
                .bookletSerialNo(request.getBookletSerialNo() != null ? request.getBookletSerialNo().trim() : null)
                .subjectName(request.getSubjectName() != null ? request.getSubjectName().trim() : null)
                .defaultSubjectId(request.getDefaultSubjectId())
                .defaultTopicId(request.getDefaultTopicId())
                .defaultSubtopicId(request.getDefaultSubtopicId())
                .languages(request.getLanguages() != null && !request.getLanguages().isEmpty() ? request.getLanguages() : List.of("en", "hi"))
                .totalQuestionsExpected(request.getTotalQuestionsExpected() != null ? request.getTotalQuestionsExpected() : 0)
                .durationMinutes(request.getDurationMinutes())
                .maxMarks(request.getMaxMarks())
                .negativeMarkingRatio(request.getNegativeMarkingRatio())
                .fileAssetId(assetId)
                .originalFilename(filename)
                .fileSha256(sha256)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .uploadedBy(resolvedUploader)
                .build();
        paper.setTenantId(tenantId);
        paper = metadataRepository.save(paper);

        String storagePath = paperFileStorageService.storePdf(paper.getId(), tenantId, filename, bytes);
        paper.setFileStoragePath(storagePath);
        paper = metadataRepository.save(paper);

        // Register asset reference in asset-service
        assetServiceClient.createReference(assetId, "QUESTION_PAPER", paper.getId(), tenantId);

        // Launch async extraction worker
        asyncWorker.processPaperExtractionJob(paper.getId(), tenantId);

        return QuestionPaperMetadataResponse.fromEntity(paper);
    }

    /**
     * Retrieves extraction status, telemetry, and metadata for a specific Question Paper.
     *
     * @param paperId question paper metadata ID
     * @param tenantId examination authority identifier
     * @return QuestionPaperMetadataResponse
     */
    @Transactional(readOnly = true)
    public QuestionPaperMetadataResponse getPaperMetadata(UUID paperId, String tenantId) {
        QuestionPaperMetadata metadata = metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));
        return QuestionPaperMetadataResponse.fromEntity(metadata);
    }

    /**
     * Lists uploaded question papers for the tenant with multi-criteria filtering and pagination.
     *
     * @param tenantId examination authority identifier
     * @param examinationName optional case-insensitive substring filter
     * @param examYear optional year filter
     * @param department optional department filter
     * @param status optional extraction status filter
     * @param pageable pagination parameters
     * @return page of QuestionPaperMetadataResponse
     */
    @Transactional(readOnly = true)
    public Page<QuestionPaperMetadataResponse> listPapers(
            String tenantId,
            String examinationName,
            Integer examYear,
            String department,
            PaperExtractionStatus status,
            Pageable pageable) {

        Specification<QuestionPaperMetadata> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (examinationName != null && !examinationName.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("examinationName")), "%" + examinationName.toLowerCase().trim() + "%"));
            }
            if (examYear != null) {
                predicates.add(cb.equal(root.get("examYear"), examYear));
            }
            if (department != null && !department.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("department")), "%" + department.toLowerCase().trim() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("extractionStatus"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return metadataRepository.findAll(spec, pageable).map(QuestionPaperMetadataResponse::fromEntity);
    }

    /**
     * Lists extracted staging questions for a given paper with status and page filters.
     *
     * @param paperId question paper metadata ID
     * @param tenantId examination authority identifier
     * @param status optional ExtractedQuestionStatus filter
     * @param pageNumber optional source PDF page number filter
     * @param pageable pagination parameters
     * @return page of ExtractedQuestionResponse
     */
    @Transactional(readOnly = true)
    public Page<ExtractedQuestionResponse> listExtractedQuestions(
            UUID paperId,
            String tenantId,
            ExtractedQuestionStatus status,
            Integer pageNumber,
            Pageable pageable) {

        metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        Specification<ExtractedQuestion> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("paperMetadataId"), paperId));
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (pageNumber != null) {
                predicates.add(cb.equal(root.get("sourcePageNumber"), pageNumber));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return extractedQuestionRepository.findAll(spec, pageable).map(ExtractedQuestionResponse::fromEntity);
    }

    /**
     * Retrieves a single extracted staging question by ID.
     *
     * @param paperId question paper metadata ID
     * @param extractedQuestionId staging question ID
     * @param tenantId examination authority identifier
     * @return ExtractedQuestionResponse
     */
    @Transactional(readOnly = true)
    public ExtractedQuestionResponse getExtractedQuestion(UUID paperId, UUID extractedQuestionId, String tenantId) {
        ExtractedQuestion question = extractedQuestionRepository
                .findByIdAndPaperMetadataIdAndTenantId(extractedQuestionId, paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Extracted question not found: " + extractedQuestionId));
        return ExtractedQuestionResponse.fromEntity(question);
    }

    /**
     * Updates an extracted staging question during SME/reviewer verification.
     *
     * @param paperId question paper metadata ID
     * @param extractedQuestionId staging question ID
     * @param request update payload
     * @param reviewerId user UUID making the update
     * @param tenantId examination authority identifier
     * @return updated ExtractedQuestionResponse
     */
    @Transactional
    public ExtractedQuestionResponse updateExtractedQuestion(
            UUID paperId,
            UUID extractedQuestionId,
            UpdateExtractedQuestionRequest request,
            UUID reviewerId,
            String tenantId) {

        ExtractedQuestion question = extractedQuestionRepository
                .findByIdAndPaperMetadataIdAndTenantId(extractedQuestionId, paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Extracted question not found: " + extractedQuestionId));

        if (request.getQuestionNumber() != null) {
            question.setQuestionNumber(request.getQuestionNumber());
        }
        if (request.getSectionName() != null) {
            question.setSectionName(request.getSectionName());
        }
        if (request.getQuestionType() != null) {
            question.setQuestionType(request.getQuestionType());
        }
        if (request.getContent() != null) {
            question.setContent(pdfParser.normalizeMathAndFormatting(request.getContent()));
        }
        if (request.getOptions() != null) {
            List<QuestionOption> options = request.getOptions();
            for (QuestionOption opt : options) {
                if (opt.getText() != null) {
                    opt.setText(pdfParser.normalizeMathAndFormatting(opt.getText()));
                }
            }
            question.setOptions(options);
        }
        if (request.getAnswerKey() != null) {
            question.setAnswerKey(request.getAnswerKey());
        }
        if (request.getExplanation() != null) {
            question.setExplanation(pdfParser.normalizeMathAndFormatting(request.getExplanation()));
        }
        if (request.getPassageText() != null) {
            question.setPassageText(pdfParser.normalizeMathAndFormatting(request.getPassageText()));
        }
        if (request.getTranslations() != null) {
            question.setTranslations(request.getTranslations());
        }
        if (request.getSuggestedSubjectId() != null) {
            question.setSuggestedSubjectId(request.getSuggestedSubjectId());
        }
        if (request.getSuggestedTopicId() != null) {
            question.setSuggestedTopicId(request.getSuggestedTopicId());
        }
        if (request.getSuggestedSubtopicId() != null) {
            question.setSuggestedSubtopicId(request.getSuggestedSubtopicId());
        }
        if (request.getSuggestedDifficulty() != null) {
            question.setSuggestedDifficulty(request.getSuggestedDifficulty());
        }
        if (request.getSuggestedCognitiveLevel() != null) {
            question.setSuggestedCognitiveLevel(request.getSuggestedCognitiveLevel());
        }
        if (request.getStatus() != null) {
            question.setStatus(request.getStatus());
        }
        if (request.getReviewComments() != null) {
            question.setReviewComments(request.getReviewComments());
        }
        question.setReviewerId(reviewerId);

        question = extractedQuestionRepository.save(question);

        refreshPaperApprovedCount(paperId, tenantId);

        return ExtractedQuestionResponse.fromEntity(question);
    }

    /**
     * Applies official or provisional answer keys to extracted questions in bulk by question number.
     *
     * @param paperId question paper metadata ID
     * @param request bulk key mappings and optional auto-approve flag
     * @param reviewerId user UUID applying the keys
     * @param tenantId examination authority identifier
     * @return count of questions updated
     */
    @Transactional
    public int applyBulkAnswerKey(
            UUID paperId,
            BulkAnswerKeyRequest request,
            UUID reviewerId,
            String tenantId) {

        metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (request == null || request.getAnswersByQuestionNumber() == null || request.getAnswersByQuestionNumber().isEmpty()) {
            return 0;
        }

        int updatedCount = 0;
        for (Map.Entry<Integer, String> entry : request.getAnswersByQuestionNumber().entrySet()) {
            Integer qNum = entry.getKey();
            String key = entry.getValue();
            if (qNum == null || key == null) {
                continue;
            }

            Optional<ExtractedQuestion> eqOpt = extractedQuestionRepository
                    .findByPaperMetadataIdAndQuestionNumber(paperId, qNum);
            if (eqOpt.isPresent()) {
                ExtractedQuestion eq = eqOpt.get();
                eq.setAnswerKey(key);

                if (request.getExplanationsByQuestionNumber() != null) {
                    String explanation = request.getExplanationsByQuestionNumber().get(qNum);
                    if (explanation != null) {
                        eq.setExplanation(pdfParser.normalizeMathAndFormatting(explanation));
                    }
                }

                if (eq.getOptions() != null && !eq.getOptions().isEmpty()) {
                    for (QuestionOption opt : eq.getOptions()) {
                        opt.setCorrect(opt.getId() != null && opt.getId().trim().equalsIgnoreCase(key.trim()));
                    }
                }

                if (request.isAutoApproveMatched()) {
                    eq.setStatus(ExtractedQuestionStatus.APPROVED);
                }
                eq.setReviewerId(reviewerId);
                extractedQuestionRepository.save(eq);
                updatedCount++;
            }
        }

        refreshPaperApprovedCount(paperId, tenantId);

        return updatedCount;
    }

    /**
     * Cancels an active or pending extraction batch job.
     *
     * @param paperId question paper metadata ID
     * @param tenantId examination authority identifier
     * @return updated QuestionPaperMetadataResponse
     */
    @Transactional
    public QuestionPaperMetadataResponse cancelExtractionJob(UUID paperId, String tenantId) {
        QuestionPaperMetadata metadata = metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (metadata.getExtractionStatus() == PaperExtractionStatus.PENDING
                || metadata.getExtractionStatus() == PaperExtractionStatus.PROCESSING) {
            asyncWorker.requestCancel(paperId);
            metadata.setExtractionStatus(PaperExtractionStatus.CANCELLED);
            metadata.setCompletedAt(Instant.now());
            metadata = metadataRepository.save(metadata);
        }

        return QuestionPaperMetadataResponse.fromEntity(metadata);
    }

    /**
     * Retries extraction on a failed, partially failed, or cancelled Question Paper.
     *
     * @param paperId question paper metadata ID
     * @param tenantId examination authority identifier
     * @return refreshed QuestionPaperMetadataResponse
     */
    @Transactional
    public QuestionPaperMetadataResponse retryExtractionJob(UUID paperId, String tenantId) {
        QuestionPaperMetadata metadata = metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (metadata.getExtractionStatus() == PaperExtractionStatus.PROCESSING) {
            throw new IllegalStateException("Extraction job is currently processing and cannot be retried");
        }

        metadata.setExtractionStatus(PaperExtractionStatus.PENDING);
        metadata.setPagesProcessed(0);
        metadata.setQuestionsExtracted(0);
        metadata.setFailedPages(new ArrayList<>());
        metadata.setErrorMessage(null);
        metadata.setStartedAt(null);
        metadata.setCompletedAt(null);
        metadata = metadataRepository.save(metadata);

        asyncWorker.processPaperExtractionJob(paperId, tenantId);

        return QuestionPaperMetadataResponse.fromEntity(metadata);
    }

    private void refreshPaperApprovedCount(UUID paperId, String tenantId) {
        QuestionPaperMetadata paper = metadataRepository.findByIdAndTenantId(paperId, tenantId).orElse(null);
        if (paper != null) {
            int approvedCount = (int) extractedQuestionRepository.countByPaperMetadataIdAndStatusAndTenantId(
                    paperId, ExtractedQuestionStatus.APPROVED, tenantId);
            paper.setQuestionsApproved(approvedCount);
            metadataRepository.save(paper);
        }
    }
}
