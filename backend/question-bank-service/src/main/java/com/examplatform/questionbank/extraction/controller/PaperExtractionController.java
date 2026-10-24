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
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.security.UserContext;
import com.examplatform.shared.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for Question Paper PDF extraction lifecycle operations:
 * file uploading, telemetry status querying, extracted staging questions review,
 * bulk answer key application, and promotion to canonical question bank.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions/papers")
@RequiredArgsConstructor
public class PaperExtractionController {

    private static final String DEFAULT_USER_ID = "00000000-0000-0000-0000-000000000001";

    private final QuestionPaperExtractionService questionPaperExtractionService;
    private final PaperQuestionPromotionService paperQuestionPromotionService;

    /**
     * Initiates asynchronous question paper extraction for a PDF document pre-uploaded
     * to asset-service.
     * Consumes application/json (requires valid fileAssetId from asset-service).
     *
     * POST /api/v1/questions/papers
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<QuestionPaperMetadataResponse> initiateExtraction(
            @Valid @RequestBody PaperUploadRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID uploaderId = extractUserId(jwt);
        String tenantId = tenantId();
        log.info("Initiating question paper extraction for assetId={}, exam={}, year={}, tenantId={}",
                request.getFileAssetId(), request.getExaminationName(), request.getExamYear(), tenantId);

        QuestionPaperMetadataResponse response = questionPaperExtractionService
                .initiateExtraction(request, uploaderId, tenantId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Uploads/initiates a Question Paper extraction using pre-uploaded asset-service asset ID.
     * Consumes application/json.
     *
     * POST /api/v1/questions/papers/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<QuestionPaperMetadataResponse> uploadPaper(
            @Valid @RequestBody PaperUploadRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return initiateExtraction(request, jwt);
    }

    /**
     * Lists uploaded question papers with multi-criteria filtering and pagination.
     *
     * GET /api/v1/questions/papers
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<Page<QuestionPaperMetadataResponse>> listPapers(
            @RequestParam(required = false) String examinationName,
            @RequestParam(required = false) Integer examYear,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) PaperExtractionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<QuestionPaperMetadataResponse> response = questionPaperExtractionService.listPapers(
                tenantId(), examinationName, examYear, department, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves metadata, extraction telemetry, and status for a specific Question Paper.
     *
     * GET /api/v1/questions/papers/{paperId}
     */
    @GetMapping("/{paperId}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<QuestionPaperMetadataResponse> getPaper(
            @PathVariable UUID paperId) {

        QuestionPaperMetadataResponse response = questionPaperExtractionService.getPaperMetadata(paperId, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancels an ongoing or pending paper extraction batch job.
     *
     * POST /api/v1/questions/papers/{paperId}/cancel
     */
    @PostMapping("/{paperId}/cancel")
    @PreAuthorize("hasAnyRole('EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<QuestionPaperMetadataResponse> cancelExtraction(
            @PathVariable UUID paperId) {

        QuestionPaperMetadataResponse response = questionPaperExtractionService.cancelExtractionJob(paperId, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retries extraction on a failed, partially failed, or cancelled Question Paper.
     *
     * POST /api/v1/questions/papers/{paperId}/retry
     */
    @PostMapping("/{paperId}/retry")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<QuestionPaperMetadataResponse> retryExtraction(
            @PathVariable UUID paperId) {

        QuestionPaperMetadataResponse response = questionPaperExtractionService.retryExtractionJob(paperId, tenantId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Applies official or provisional answer keys in bulk by question number.
     *
     * POST /api/v1/questions/papers/{paperId}/answer-key
     */
    @PostMapping("/{paperId}/answer-key")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<QuestionPaperMetadataResponse> applyAnswerKey(
            @PathVariable UUID paperId,
            @Valid @RequestBody BulkAnswerKeyRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID reviewerId = extractUserId(jwt);
        String tenantId = tenantId();
        questionPaperExtractionService.applyBulkAnswerKey(paperId, request, reviewerId, tenantId);
        QuestionPaperMetadataResponse response = questionPaperExtractionService.getPaperMetadata(paperId, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists extracted staging questions for a paper with status and page filters.
     *
     * GET /api/v1/questions/papers/{paperId}/questions
     */
    @GetMapping("/{paperId}/questions")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<Page<ExtractedQuestionResponse>> listExtractedQuestions(
            @PathVariable UUID paperId,
            @RequestParam(required = false) ExtractedQuestionStatus status,
            @RequestParam(required = false) Integer pageNumber,
            @PageableDefault(size = 50, sort = "questionNumber", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<ExtractedQuestionResponse> response = questionPaperExtractionService.listExtractedQuestions(
                paperId, tenantId(), status, pageNumber, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single extracted staging question by ID.
     *
     * GET /api/v1/questions/papers/{paperId}/questions/{extractedQuestionId}
     */
    @GetMapping("/{paperId}/questions/{extractedQuestionId}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ExtractedQuestionResponse> getExtractedQuestion(
            @PathVariable UUID paperId,
            @PathVariable UUID extractedQuestionId) {

        ExtractedQuestionResponse response = questionPaperExtractionService.getExtractedQuestion(
                paperId, extractedQuestionId, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an extracted staging question during SME/reviewer verification.
     *
     * PUT /api/v1/questions/papers/{paperId}/questions/{extractedQuestionId}
     */
    @PutMapping("/{paperId}/questions/{extractedQuestionId}")
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<ExtractedQuestionResponse> updateExtractedQuestion(
            @PathVariable UUID paperId,
            @PathVariable UUID extractedQuestionId,
            @Valid @RequestBody UpdateExtractedQuestionRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID reviewerId = extractUserId(jwt);
        ExtractedQuestionResponse response = questionPaperExtractionService.updateExtractedQuestion(
                paperId, extractedQuestionId, request, reviewerId, tenantId());
        return ResponseEntity.ok(response);
    }

    /**
     * Promotes verified extracted questions to the canonical question bank.
     * Also accessible via alias endpoint POST /api/v1/questions/papers/{paperId}/questions/promote.
     *
     * POST /api/v1/questions/papers/{paperId}/promote
     * POST /api/v1/questions/papers/{paperId}/questions/promote
     */
    @PostMapping(value = {"/{paperId}/promote", "/{paperId}/questions/promote"})
    @PreAuthorize("hasAnyRole('QUESTION_AUTHOR', 'REVIEWER', 'EXAM_CONTROLLER', 'SUPER_ADMIN')")
    public ResponseEntity<PromoteQuestionsResponse> promoteQuestions(
            @PathVariable UUID paperId,
            @RequestBody(required = false) PromoteQuestionsRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        PromoteQuestionsRequest req = (request != null) ? request : new PromoteQuestionsRequest();
        UUID userId = extractUserId(jwt);
        String tenantId = tenantId();
        PromoteQuestionsResponse response = paperQuestionPromotionService.promoteQuestions(paperId, req, userId, tenantId);
        return ResponseEntity.ok(response);
    }

    private String tenantId() {
        return TenantContext.getTenantIdOrDefault();
    }

    private UUID extractUserId(Jwt jwt) {
        return UserContext.resolveUserIdOrDefault(jwt);
    }
}
