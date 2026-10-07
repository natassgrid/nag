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
import com.examplatform.questionbank.ai.generation.QuestionGenerationRequest;
import com.examplatform.questionbank.ai.generation.QuestionGenerationResponse;
import com.examplatform.questionbank.ai.generation.QuestionGenerationService;
import com.examplatform.questionbank.ai.parser.NormalizedSampleQuestion;
import com.examplatform.questionbank.ai.parser.SampleDocumentParserService;
import com.examplatform.shared.api.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for AI-powered question operations: embedding backfill,
 * question generation, sample question upload, and requirement clarification.
 *
 * Validates: Requirements FR-3, FR-6, FR-9, Issue #272
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionAiController {

    private final EmbeddingService embeddingService;
    private final QuestionGenerationService questionGenerationService;
    private final SampleDocumentParserService sampleDocumentParserService;
    private final ObjectMapper objectMapper;

    /**
     * Generates questions using AI based on the provided parameters.
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionGenerationResponse>> generateQuestions(
            @Valid @RequestBody QuestionGenerationRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("AI question generation requested: subject={}, topic={}, count={}, tenant={}",
                request.getSubject(), request.getTopic(), request.getCount(), tenantId);

        UUID authorId = UUID.fromString(jwt.getSubject());
        QuestionGenerationResponse response = questionGenerationService.generate(request, tenantId, authorId);

        log.info("AI generation completed: model={}, generated={}, valid={}, duplicates={}, mode={}, tenant={}",
                response.getModelUsed(), response.getTotalGenerated(),
                response.getTotalValid(), response.getTotalDuplicates(), response.getExecutionMode(), tenantId);

        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Generated %d questions (%d valid, %d duplicates detected)",
                        response.getTotalGenerated(), response.getTotalValid(),
                        response.getTotalDuplicates())));
    }

    /**
     * Generates questions with sample document upload (multipart/form-data).
     * Parses PDF/images using tiered ladder and executes conditional complexity triage.
     */
    @PostMapping(value = "/generate/with-samples", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionGenerationResponse>> generateWithSamplesMultipart(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestPart("request") String requestJson,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) throws IOException {

        UUID authorId = UUID.fromString(jwt.getSubject());
        QuestionGenerationRequest request = objectMapper.readValue(requestJson, QuestionGenerationRequest.class);

        List<NormalizedSampleQuestion> sampleQuestions = new ArrayList<>();
        if (file != null && !file.isEmpty()) {
            try {
                sampleQuestions = sampleDocumentParserService.parseUploadedFile(file);
                log.info("Parsed {} sample question(s) from uploaded file {}", sampleQuestions.size(), file.getOriginalFilename());
            } catch (Exception e) {
                log.error("Failed to parse uploaded sample file {}: {}", file.getOriginalFilename(), e.getMessage());
                return ResponseEntity.badRequest().body(ApiResponse.error("Failed to parse uploaded sample document: " + e.getMessage()));
            }
        }

        QuestionGenerationResponse response = questionGenerationService.generateWithSamples(
                request, sampleQuestions, tenantId, authorId);

        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Generated %d questions with samples (%s mode)",
                        response.getTotalGenerated(), response.getExecutionMode())));
    }

    /**
     * Generates questions with sample questions payload in JSON format.
     */
    @PostMapping(value = "/generate/with-samples", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionGenerationResponse>> generateWithSamplesJson(
            @Valid @RequestBody QuestionGenerationRequest request,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID authorId = UUID.fromString(jwt.getSubject());
        QuestionGenerationResponse response = questionGenerationService.generate(request, tenantId, authorId);

        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Generated %d questions (%s mode)",
                        response.getTotalGenerated(), response.getExecutionMode())));
    }

    /**
     * Interactive requirement elicitation and refinement endpoint.
     */
    @PostMapping("/generate/clarify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<ClarifyRequirementsResponse>> clarifyRequirements(
            @Valid @RequestBody ClarifyRequirementsRequest request) {

        log.info("Clarification requested for subject={}, topic={}, prompt={}",
                request.getSubject(), request.getTopic(), request.getAuthorPrompt());

        ClarifyRequirementsResponse response = questionGenerationService.clarifyRequirements(request);

        return ResponseEntity.ok(ApiResponse.success(response,
                response.isClarificationNeeded()
                        ? "Requirement clarification questions generated"
                        : "Requirements clear and aligned with examination standard"));
    }

    /**
     * Generates embeddings for all questions that have a null embedding column.
     */
    @PostMapping("/embeddings/backfill")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> backfillEmbeddings(
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Starting embedding backfill for tenant={}", tenantId);

        Map<String, Object> summary = embeddingService.backfillEmbeddings(tenantId);
        int totalProcessed = (int) summary.getOrDefault("totalProcessed", 0);
        int totalFailed = (int) summary.getOrDefault("totalFailed", 0);

        String message = totalFailed == 0
                ? String.format("Embedding backfill completed: %d questions processed", totalProcessed)
                : String.format("Embedding backfill completed with errors: %d processed, %d failed",
                        totalProcessed, totalFailed);

        return ResponseEntity.ok(ApiResponse.success(summary, message));
    }
}
