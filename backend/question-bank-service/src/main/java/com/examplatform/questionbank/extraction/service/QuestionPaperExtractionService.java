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
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final AsyncPaperExtractionWorker asyncWorker;
    private final QuestionPaperPdfParser pdfParser;
    private final AssetServiceClient assetServiceClient;

    private final Map<String, byte[]> pageImageCache = Collections.synchronizedMap(
            new LinkedHashMap<>(50, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
                    return size() > 50;
                }
            }
    );

    /**
     * Validates the syntax of a page selection string (e.g. "1-5,8,10-12").
     * Throws {@link IllegalArgumentException} if invalid syntax is found.
     *
     * @param pageSelection page selection string
     */
    public static void validatePageSelection(String pageSelection) {
        if (pageSelection == null || pageSelection.isBlank()) {
            return;
        }
        String trimmed = pageSelection.trim();
        if (!Pattern.matches("^[0-9,\\-\\s]+$", trimmed)) {
            throw new IllegalArgumentException("Invalid page selection syntax: contains illegal characters in '" + pageSelection + "'");
        }
        String[] tokens = trimmed.split(",", -1);
        for (String token : tokens) {
            String item = token.trim();
            if (item.isEmpty()) {
                throw new IllegalArgumentException("Invalid page selection syntax: empty token in '" + pageSelection + "'");
            }
            if (item.contains("-")) {
                if (item.startsWith("-") || item.endsWith("-")) {
                    throw new IllegalArgumentException("Invalid page range syntax: '" + item + "'");
                }
                String[] parts = item.split("-", -1);
                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid page range syntax: '" + item + "'");
                }
                int start;
                int end;
                try {
                    start = Integer.parseInt(parts[0].trim());
                    end = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid page numbers in range: '" + item + "'", e);
                }
                if (start <= 0 || end <= 0) {
                    throw new IllegalArgumentException("Page numbers must be >= 1 in '" + item + "'");
                }
                if (start > end) {
                    throw new IllegalArgumentException("Invalid inverted page range: start " + start + " cannot exceed end " + end);
                }
            } else {
                int p;
                try {
                    p = Integer.parseInt(item);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid page number: '" + item + "'", e);
                }
                if (p <= 0) {
                    throw new IllegalArgumentException("Page numbers must be >= 1 in '" + item + "'");
                }
            }
        }
    }

    /**
     * Parses a page selection string (e.g. "1-5,8,10-12") into a sorted list of unique 1-based page numbers,
     * clamped to totalPages. If pageSelection is null or blank, returns all pages 1..totalPages.
     *
     * @param pageSelection page range specification string
     * @param totalPages total number of pages in the PDF document
     * @return sorted list of 1-based page numbers
     */
    public static List<Integer> parsePageSelection(String pageSelection, int totalPages) {
        if (totalPages <= 0) {
            return Collections.emptyList();
        }
        if (pageSelection == null || pageSelection.isBlank()) {
            return IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
        }

        validatePageSelection(pageSelection);

        Set<Integer> selected = new TreeSet<>();
        String[] tokens = pageSelection.trim().split(",", -1);
        for (String token : tokens) {
            String item = token.trim();
            if (item.contains("-")) {
                String[] parts = item.split("-", -1);
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                int clampedStart = Math.max(1, start);
                int clampedEnd = Math.min(totalPages, end);
                if (clampedStart <= clampedEnd && start <= totalPages) {
                    for (int p = clampedStart; p <= clampedEnd; p++) {
                        selected.add(p);
                    }
                }
            } else {
                int p = Integer.parseInt(item);
                if (p >= 1 && p <= totalPages) {
                    selected.add(p);
                }
            }
        }
        return new ArrayList<>(selected);
    }

    /**
     * Computes the subset of selected pages that need to be processed during extraction resume.
     * Only returns selected pages that have no extracted questions yet OR appear in failedPages.
     *
     * @param selectedPages list of selected page numbers for the paper
     * @param pagesWithQuestions set of page numbers having existing extracted questions
     * @param failedPages set of page numbers currently marked as failed
     * @return pages to extract
     */
    public static List<Integer> computeResumePages(
            List<Integer> selectedPages,
            Set<Integer> pagesWithQuestions,
            Set<Integer> failedPages) {

        if (selectedPages == null || selectedPages.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Integer> withQ = pagesWithQuestions != null ? pagesWithQuestions : Collections.emptySet();
        Set<Integer> failed = failedPages != null ? failedPages : Collections.emptySet();

        return selectedPages.stream()
                .filter(p -> !withQ.contains(p) || failed.contains(p))
                .collect(Collectors.toList());
    }

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
        validatePageSelection(request.getPageSelection());

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
                : assetServiceClient.computeSha256(bytes);

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
                .pageSelection(request.getPageSelection() != null && !request.getPageSelection().isBlank() ? request.getPageSelection().trim() : null)
                .fileAssetId(assetId)
                .fileStoragePath("asset://" + assetId)
                .originalFilename(filename)
                .fileSha256(sha256)
                .extractionStatus(PaperExtractionStatus.PENDING)
                .uploadedBy(resolvedUploader)
                .build();
        paper.setTenantId(tenantId);
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

        if (question.getStatus() == ExtractedQuestionStatus.PROMOTED || question.getPromotedQuestionId() != null) {
            throw new IllegalStateException("Cannot modify extracted question in PROMOTED status: " + extractedQuestionId);
        }

        if (request.getQuestionNumber() != null && !request.getQuestionNumber().equals(question.getQuestionNumber())) {
            Optional<ExtractedQuestion> conflict = extractedQuestionRepository
                    .findByPaperMetadataIdAndQuestionNumber(paperId, request.getQuestionNumber());
            if (conflict.isPresent() && !conflict.get().getId().equals(extractedQuestionId)) {
                throw new IllegalStateException("Question number " + request.getQuestionNumber() + " is already in use in paper " + paperId);
            }
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
        metadata.setLastCompletedPage(0);
        metadata.setQuestionsExtracted(0);
        metadata.setFailedPages(new ArrayList<>());
        metadata.setErrorMessage(null);
        metadata.setStartedAt(null);
        metadata.setCompletedAt(null);
        metadata = metadataRepository.save(metadata);

        asyncWorker.processPaperExtractionJob(paperId, tenantId);

        return QuestionPaperMetadataResponse.fromEntity(metadata);
    }

    /**
     * Resumes an extraction job from QUOTA_EXHAUSTED, CANCELLED, PARTIAL_FAILED, or FAILED status.
     * Re-processes only selected pages that have no extracted questions yet or appear in failedPages.
     *
     * @param paperId question paper metadata ID
     * @param tenantId examination authority identifier
     * @return refreshed QuestionPaperMetadataResponse with PROCESSING status
     */
    @Transactional
    public QuestionPaperMetadataResponse resumeExtractionJob(UUID paperId, String tenantId) {
        QuestionPaperMetadata metadata = metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        PaperExtractionStatus status = metadata.getExtractionStatus();
        if (status == PaperExtractionStatus.PROCESSING) {
            throw new IllegalStateException("Extraction job is currently processing and cannot be resumed");
        }
        if (status != PaperExtractionStatus.QUOTA_EXHAUSTED
                && status != PaperExtractionStatus.CANCELLED
                && status != PaperExtractionStatus.PARTIAL_FAILED
                && status != PaperExtractionStatus.FAILED) {
            throw new IllegalStateException("Extraction job cannot be resumed from status: " + status);
        }

        metadata.setExtractionStatus(PaperExtractionStatus.PROCESSING);
        metadata.setErrorMessage(null);
        metadata.setCompletedAt(null);
        metadata = metadataRepository.save(metadata);

        asyncWorker.processPaperExtractionJob(paperId, tenantId);

        return QuestionPaperMetadataResponse.fromEntity(metadata);
    }

    /**
     * Renders a specific page of a question paper PDF as a JPEG byte array (150 DPI).
     * Employs an in-memory LRU cache to avoid re-downloading/re-rendering identical pages.
     *
     * @param paperId question paper metadata ID
     * @param pageNumber 1-based page number
     * @param tenantId examination authority identifier
     * @return JPEG image bytes
     */
    @Transactional(readOnly = true)
    public byte[] renderPageImage(UUID paperId, int pageNumber, String tenantId) {
        if (pageNumber < 1) {
            throw new IllegalArgumentException("Page number must be >= 1");
        }
        QuestionPaperMetadata metadata = metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (metadata.getTotalPages() > 0 && pageNumber > metadata.getTotalPages()) {
            throw new IllegalArgumentException("Page number " + pageNumber + " exceeds total pages " + metadata.getTotalPages());
        }

        String cacheKey = paperId + ":" + pageNumber;
        byte[] cached = pageImageCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        UUID assetId = metadata.getFileAssetId();
        if (assetId == null) {
            throw new IllegalStateException("Question paper has no associated file asset");
        }

        byte[] pdfBytes = assetServiceClient.downloadAssetBytes(assetId, tenantId);
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalStateException("Failed to download PDF bytes for asset ID: " + assetId);
        }

        byte[] rendered = renderPdfPageToJpeg(pdfBytes, pageNumber);
        pageImageCache.put(cacheKey, rendered);
        return rendered;
    }

    private byte[] renderPdfPageToJpeg(byte[] pdfBytes, int pageNumber) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (pageNumber > document.getNumberOfPages()) {
                throw new IllegalArgumentException("Page number " + pageNumber + " exceeds total document pages " + document.getNumberOfPages());
            }
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 150.0f, ImageType.RGB);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "JPEG", baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to render PDF page " + pageNumber + " to image: " + e.getMessage(), e);
        }
    }

    /**
     * Merges two or more extracted staging questions in the given order into the first question.
     * Content and Hindi content are joined with double-newlines, options taken from first non-empty,
     * and non-target questions are deleted.
     *
     * @param paperId question paper metadata ID
     * @param request question IDs to merge in order
     * @param reviewerId user UUID executing the merge
     * @param tenantId examination authority identifier
     * @return merged ExtractedQuestionResponse
     */
    @Transactional
    public ExtractedQuestionResponse mergeQuestions(
            UUID paperId,
            MergeQuestionsRequest request,
            UUID reviewerId,
            String tenantId) {

        metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (request == null || request.getQuestionIds() == null || request.getQuestionIds().size() < 2) {
            throw new IllegalArgumentException("At least two question IDs are required to merge");
        }

        List<UUID> ids = request.getQuestionIds();
        List<ExtractedQuestion> questions = new ArrayList<>();
        for (UUID id : ids) {
            ExtractedQuestion q = extractedQuestionRepository
                    .findByIdAndPaperMetadataIdAndTenantId(id, paperId, tenantId)
                    .orElseThrow(() -> new EntityNotFoundException("Extracted question not found: " + id));
            if (q.getStatus() == ExtractedQuestionStatus.PROMOTED || q.getPromotedQuestionId() != null) {
                throw new IllegalStateException("Cannot merge question in PROMOTED status: " + id);
            }
            questions.add(q);
        }

        ExtractedQuestion target = questions.get(0);
        List<ExtractedQuestion> toDelete = questions.subList(1, questions.size());

        // Merge English content with \n\n
        String mergedContent = questions.stream()
                .map(ExtractedQuestion::getContent)
                .filter(c -> c != null && !c.isBlank())
                .collect(Collectors.joining("\n\n"));
        target.setContent(pdfParser.normalizeMathAndFormatting(mergedContent));

        // Merge Hindi content if present
        List<String> hindiContents = new ArrayList<>();
        for (ExtractedQuestion q : questions) {
            if (q.getTranslations() != null && q.getTranslations().containsKey("hi")) {
                Map<String, Object> hi = q.getTranslations().get("hi");
                if (hi != null && hi.get("content") != null) {
                    String hc = hi.get("content").toString().trim();
                    if (!hc.isEmpty()) {
                        hindiContents.add(hc);
                    }
                }
            }
        }
        if (!hindiContents.isEmpty()) {
            String mergedHindi = String.join("\n\n", hindiContents);
            Map<String, Map<String, Object>> trans = target.getTranslations() != null
                    ? target.getTranslations() : new HashMap<>();
            Map<String, Object> hiPayload = trans.computeIfAbsent("hi", k -> new HashMap<>());
            hiPayload.put("content", pdfParser.normalizeMathAndFormatting(mergedHindi));
            target.setTranslations(trans);
        }

        // Options taken from first non-empty
        for (ExtractedQuestion q : questions) {
            if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                target.setOptions(new ArrayList<>(q.getOptions()));
                break;
            }
        }
        for (ExtractedQuestion q : questions) {
            if (q.getTranslations() != null && q.getTranslations().containsKey("hi")) {
                Map<String, Object> hi = q.getTranslations().get("hi");
                if (hi != null && hi.get("options") instanceof List<?> opts && !opts.isEmpty()) {
                    target.getTranslations().computeIfAbsent("hi", k -> new HashMap<>()).put("options", opts);
                    break;
                }
            }
        }

        // Answer key & explanation fallback to first non-empty
        if (target.getAnswerKey() == null || target.getAnswerKey().isBlank()) {
            for (ExtractedQuestion q : questions) {
                if (q.getAnswerKey() != null && !q.getAnswerKey().isBlank()) {
                    target.setAnswerKey(q.getAnswerKey());
                    break;
                }
            }
        }
        if (target.getExplanation() == null || target.getExplanation().isBlank()) {
            for (ExtractedQuestion q : questions) {
                if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                    target.setExplanation(pdfParser.normalizeMathAndFormatting(q.getExplanation()));
                    break;
                }
            }
        }
        if (target.getPassageText() == null || target.getPassageText().isBlank()) {
            for (ExtractedQuestion q : questions) {
                if (q.getPassageText() != null && !q.getPassageText().isBlank()) {
                    target.setPassageText(pdfParser.normalizeMathAndFormatting(q.getPassageText()));
                    break;
                }
            }
        }

        // Merge media URLs & flags
        Set<String> mediaUrls = new LinkedHashSet<>(target.getMediaUrls() != null ? target.getMediaUrls() : Collections.emptyList());
        boolean hasImages = target.isHasImages();
        boolean hasTables = target.isHasTables();
        boolean hasFormulas = target.isHasFormulas();
        for (ExtractedQuestion q : toDelete) {
            if (q.getMediaUrls() != null) {
                mediaUrls.addAll(q.getMediaUrls());
            }
            hasImages = hasImages || q.isHasImages();
            hasTables = hasTables || q.isHasTables();
            hasFormulas = hasFormulas || q.isHasFormulas();
        }
        target.setMediaUrls(new ArrayList<>(mediaUrls));
        target.setHasImages(hasImages || !mediaUrls.isEmpty());
        target.setHasTables(hasTables);
        target.setHasFormulas(hasFormulas);

        target.setReviewerId(reviewerId);
        target = extractedQuestionRepository.save(target);

        extractedQuestionRepository.deleteAll(toDelete);

        refreshPaperApprovedCount(paperId, tenantId);

        return ExtractedQuestionResponse.fromEntity(target);
    }

    /**
     * Splits an extracted staging question into two questions.
     * The original question keeps firstContent; a new question is inserted at original+1 with secondContent,
     * and subsequent question numbers are safely shifted by +1 in two phases.
     *
     * @param paperId question paper metadata ID
     * @param questionId question ID to split
     * @param request firstContent and secondContent
     * @param reviewerId user UUID executing the split
     * @param tenantId examination authority identifier
     * @return list containing the updated original and new second question
     */
    @Transactional
    public List<ExtractedQuestionResponse> splitQuestion(
            UUID paperId,
            UUID questionId,
            SplitQuestionRequest request,
            UUID reviewerId,
            String tenantId) {

        metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (request == null || request.getFirstContent() == null || request.getFirstContent().isBlank()
                || request.getSecondContent() == null || request.getSecondContent().isBlank()) {
            throw new IllegalArgumentException("firstContent and secondContent must not be blank");
        }

        ExtractedQuestion original = extractedQuestionRepository
                .findByIdAndPaperMetadataIdAndTenantId(questionId, paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Extracted question not found: " + questionId));

        if (original.getStatus() == ExtractedQuestionStatus.PROMOTED || original.getPromotedQuestionId() != null) {
            throw new IllegalStateException("Cannot split question in PROMOTED status: " + questionId);
        }

        int origQNum = original.getQuestionNumber();

        // Shift subsequent questions (> origQNum) by +1 safely in two phases
        List<ExtractedQuestion> allQuestions = extractedQuestionRepository
                .findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId);
        List<ExtractedQuestion> subsequent = allQuestions.stream()
                .filter(q -> q.getQuestionNumber() > origQNum)
                .collect(Collectors.toList());

        int[] oldNumbers = subsequent.stream().mapToInt(ExtractedQuestion::getQuestionNumber).toArray();

        // Phase 1: Shift to unique temporary negative numbers
        for (int i = 0; i < subsequent.size(); i++) {
            subsequent.get(i).setQuestionNumber(-(100000 + i));
        }
        extractedQuestionRepository.saveAllAndFlush(subsequent);

        // Phase 2: Shift to oldNumber + 1
        for (int i = 0; i < subsequent.size(); i++) {
            subsequent.get(i).setQuestionNumber(oldNumbers[i] + 1);
        }
        extractedQuestionRepository.saveAllAndFlush(subsequent);

        // Update original question with first content
        original.setContent(pdfParser.normalizeMathAndFormatting(request.getFirstContent().trim()));
        original.setReviewerId(reviewerId);
        original = extractedQuestionRepository.saveAndFlush(original);

        // Create second question with number = origQNum + 1
        ExtractedQuestion second = ExtractedQuestion.builder()
                .paperMetadataId(paperId)
                .questionNumber(origQNum + 1)
                .sourcePageNumber(original.getSourcePageNumber())
                .sectionName(original.getSectionName())
                .questionType(original.getQuestionType())
                .content(pdfParser.normalizeMathAndFormatting(request.getSecondContent().trim()))
                .suggestedSubjectId(original.getSuggestedSubjectId())
                .suggestedTopicId(original.getSuggestedTopicId())
                .suggestedSubtopicId(original.getSuggestedSubtopicId())
                .suggestedDifficulty(original.getSuggestedDifficulty())
                .suggestedCognitiveLevel(original.getSuggestedCognitiveLevel())
                .extractionConfidence(original.getExtractionConfidence())
                .parsingTier(original.getParsingTier())
                .status(ExtractedQuestionStatus.EXTRACTED)
                .reviewerId(reviewerId)
                .build();
        second.setTenantId(tenantId);
        second = extractedQuestionRepository.saveAndFlush(second);

        refreshPaperApprovedCount(paperId, tenantId);

        return List.of(
                ExtractedQuestionResponse.fromEntity(original),
                ExtractedQuestionResponse.fromEntity(second)
        );
    }

    /**
     * Resequences extracted staging questions in the specified order starting from startNumber.
     * Uses a two-phase transaction to avoid unique constraint violations on (paper_metadata_id, question_number).
     *
     * @param paperId question paper metadata ID
     * @param request ordered question IDs and optional startNumber
     * @param reviewerId user UUID executing the resequencing
     * @param tenantId examination authority identifier
     * @return resequenced ExtractedQuestionResponse list in order
     */
    @Transactional
    public List<ExtractedQuestionResponse> resequenceQuestions(
            UUID paperId,
            ResequenceQuestionsRequest request,
            UUID reviewerId,
            String tenantId) {

        metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (request == null || request.getOrderedQuestionIds() == null || request.getOrderedQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("orderedQuestionIds must not be empty");
        }

        List<UUID> orderedIds = request.getOrderedQuestionIds();
        List<ExtractedQuestion> allQuestions = extractedQuestionRepository
                .findByPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(paperId, tenantId);

        Map<UUID, ExtractedQuestion> byId = allQuestions.stream()
                .collect(Collectors.toMap(ExtractedQuestion::getId, q -> q));

        List<ExtractedQuestion> targetQuestions = new ArrayList<>();
        for (UUID id : orderedIds) {
            ExtractedQuestion q = byId.get(id);
            if (q == null) {
                throw new EntityNotFoundException("Extracted question not found for paper: " + id);
            }
            if (q.getStatus() == ExtractedQuestionStatus.PROMOTED || q.getPromotedQuestionId() != null) {
                throw new IllegalStateException("Cannot resequence question in PROMOTED status: " + id);
            }
            targetQuestions.add(q);
        }

        // Two-phase renumbering
        // Phase 1: assign temporary negative numbers
        for (int i = 0; i < targetQuestions.size(); i++) {
            targetQuestions.get(i).setQuestionNumber(-(100000 + i));
            targetQuestions.get(i).setReviewerId(reviewerId);
        }
        extractedQuestionRepository.saveAllAndFlush(targetQuestions);

        // Phase 2: assign final sequenced numbers starting from startNumber
        int start = request.getStartNumber() > 0 ? request.getStartNumber() : 1;
        for (int i = 0; i < targetQuestions.size(); i++) {
            targetQuestions.get(i).setQuestionNumber(start + i);
        }
        extractedQuestionRepository.saveAllAndFlush(targetQuestions);

        return targetQuestions.stream()
                .map(ExtractedQuestionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a question paper and all associated staging extracted questions.
     *
     * @param paperId  unique question paper identifier
     * @param tenantId tenant identifier
     */
    @Transactional
    public void deletePaper(UUID paperId, String tenantId) {
        QuestionPaperMetadata paper = metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        if (paper.getExtractionStatus() == PaperExtractionStatus.PROCESSING) {
            paper.setExtractionStatus(PaperExtractionStatus.CANCELLED);
            paper.setCompletedAt(Instant.now());
            metadataRepository.save(paper);
        }

        extractedQuestionRepository.deleteByPaperMetadataIdAndTenantId(paperId, tenantId);
        metadataRepository.delete(paper);
        log.info("Deleted question paper metadata and staging questions for paperId={}, tenantId={}", paperId, tenantId);
    }

    /**
     * Deletes an individual extracted question from staging.
     *
     * @param paperId    unique question paper identifier
     * @param questionId extracted question identifier
     * @param tenantId   tenant identifier
     */
    @Transactional
    public void deleteExtractedQuestion(UUID paperId, UUID questionId, String tenantId) {
        ExtractedQuestion question = extractedQuestionRepository
                .findByIdAndPaperMetadataIdAndTenantId(questionId, paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Extracted question not found: " + questionId));

        if (question.getStatus() == ExtractedQuestionStatus.PROMOTED || question.getPromotedQuestionId() != null) {
            throw new IllegalStateException("Cannot delete a question that has already been promoted to the Question Bank");
        }

        extractedQuestionRepository.delete(question);
        refreshPaperQuestionCounts(paperId, tenantId);
        log.info("Deleted extracted question id={} from paperId={}, tenantId={}", questionId, paperId, tenantId);
    }

    /**
     * Deletes a batch of extracted questions from staging.
     *
     * @param paperId     unique question paper identifier
     * @param questionIds list of extracted question identifiers
     * @param tenantId    tenant identifier
     */
    @Transactional
    public void batchDeleteExtractedQuestions(UUID paperId, List<UUID> questionIds, String tenantId) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }

        List<ExtractedQuestion> questions = extractedQuestionRepository
                .findByIdInAndPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(questionIds, paperId, tenantId);

        if (questions.isEmpty()) {
            return;
        }

        for (ExtractedQuestion q : questions) {
            if (q.getStatus() == ExtractedQuestionStatus.PROMOTED || q.getPromotedQuestionId() != null) {
                throw new IllegalStateException("Cannot delete promoted question id: " + q.getId());
            }
        }

        extractedQuestionRepository.deleteAll(questions);
        refreshPaperQuestionCounts(paperId, tenantId);
        log.info("Batch deleted {} extracted questions from paperId={}, tenantId={}", questions.size(), paperId, tenantId);
    }

    private void refreshPaperApprovedCount(UUID paperId, String tenantId) {
        refreshPaperQuestionCounts(paperId, tenantId);
    }

    private void refreshPaperQuestionCounts(UUID paperId, String tenantId) {
        QuestionPaperMetadata paper = metadataRepository.findByIdAndTenantId(paperId, tenantId).orElse(null);
        if (paper != null) {
            int extractedCount = (int) extractedQuestionRepository.countByPaperMetadataIdAndTenantId(paperId, tenantId);
            int approvedCount = (int) extractedQuestionRepository.countByPaperMetadataIdAndStatusAndTenantId(
                    paperId, ExtractedQuestionStatus.APPROVED, tenantId);
            int promotedCount = (int) extractedQuestionRepository.countByPaperMetadataIdAndStatusAndTenantId(
                    paperId, ExtractedQuestionStatus.PROMOTED, tenantId);
            paper.setQuestionsExtracted(extractedCount);
            paper.setQuestionsApproved(approvedCount);
            paper.setQuestionsPromoted(promotedCount);
            metadataRepository.save(paper);
        }
    }
}
