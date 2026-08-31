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
import com.examplatform.questionbank.ai.generation.QuestionGenerationRequest;
import com.examplatform.questionbank.ai.generation.QuestionGenerationResponse;
import com.examplatform.questionbank.ai.generation.QuestionGenerationService;
import com.examplatform.questionbank.domain.Question;
import com.examplatform.questionbank.repository.QuestionRepository;
import com.examplatform.questionbank.util.EmbeddingUtils;
import com.examplatform.shared.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST controller for AI-powered question operations: embedding backfill,
 * question generation, and PDF import.
 *
 * Validates: Requirements FR-3, FR-6, FR-9
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionAiController {

    private static final int BATCH_SIZE = 50;

    private final QuestionRepository questionRepository;
    private final EmbeddingService embeddingService;
    private final QuestionGenerationService questionGenerationService;

    /**
     * Generates questions using AI based on the provided parameters.
     *
     * <p>Selects the appropriate LLM model for the subject, retrieves
     * relevant existing questions for RAG context, invokes the model,
     * validates generated output, performs duplicate detection, and optionally
     * auto-saves valid questions as DRAFT.</p>
     *
     * Validates: Requirements FR-3 (AI Question Generation)
     *
     * @param request  the generation parameters (subject, topic, difficulty, count, etc.)
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return generation response with questions, validation results, and metadata
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'QUESTION_AUTHOR')")
    public ResponseEntity<ApiResponse<QuestionGenerationResponse>> generateQuestions(@Valid @RequestBody QuestionGenerationRequest request, @RequestHeader("X-Tenant-Id") String tenantId, @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.oauth2.jwt.Jwt jwt) {

        log.info("AI question generation requested: subject={}, topic={}, count={}, tenant={}",
                request.getSubject(), request.getTopic(), request.getCount(), tenantId);

        java.util.UUID authorId = java.util.UUID.fromString(jwt.getSubject());
        QuestionGenerationResponse response = questionGenerationService.generate(request, tenantId, authorId);

        log.info("AI generation completed: model={}, generated={}, valid={}, duplicates={}, tenant={}",
                response.getModelUsed(), response.getTotalGenerated(),
                response.getTotalValid(), response.getTotalDuplicates(), tenantId);

        return ResponseEntity.ok(ApiResponse.success(response,
                String.format("Generated %d questions (%d valid, %d duplicates detected)",
                        response.getTotalGenerated(), response.getTotalValid(),
                        response.getTotalDuplicates())));
    }

    /**
     * Generates embeddings for all questions that have a null embedding column.
     * Processes in batches of 50 to avoid overwhelming the embedding service.
     *
     * <p>On failure of a batch, the error is logged and processing continues
     * with the next batch. The response includes total processed count and
     * any batch failures encountered.</p>
     *
     * Validates: Requirements FR-9 (Batch Embedding Backfill)
     *
     * @param tenantId tenant identifier from the X-Tenant-Id header
     * @return summary with total processed count and failures
     */
    @PostMapping("/embeddings/backfill")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> backfillEmbeddings(
            @RequestHeader("X-Tenant-Id") String tenantId) {

        log.info("Starting embedding backfill for tenant={}", tenantId);

        int totalProcessed = 0;
        int totalFailed = 0;
        List<String> failures = new ArrayList<>();

        Page<Question> batch;
        do {
            // Always fetch page 0 since processed questions no longer have null embedding
            batch = questionRepository.findQuestionsWithNullEmbedding(
                    tenantId, PageRequest.of(0, BATCH_SIZE));

            if (batch.isEmpty()) {
                break;
            }

            try {
                List<String> contents = batch.getContent().stream()
                        .map(Question::getContent)
                        .map(content -> content != null ? content : "")
                        .toList();

                List<float[]> embeddings = embeddingService.embedBatch(contents);

                List<Question> questions = batch.getContent();
                for (int i = 0; i < questions.size(); i++) {
                    questionRepository.updateEmbedding(
                            questions.get(i).getId(), EmbeddingUtils.embeddingToString(embeddings.get(i)));
                }

                totalProcessed += questions.size();

                log.info("Backfill batch completed: processed={}, remaining={}",
                        questions.size(), batch.getTotalElements() - questions.size());

            } catch (Exception e) {
                totalFailed += batch.getContent().size();
                String failureMessage = String.format(
                        "Batch failed (%d questions): %s", batch.getContent().size(), e.getMessage());
                failures.add(failureMessage);
                log.error("Embedding backfill batch failed for tenant={}: {}", tenantId, e.getMessage(), e);
                // Break on failure to avoid infinite loop retrying the same batch
                break;
            }

        } while (batch.hasNext() || !batch.isEmpty());

        Map<String, Object> summary = Map.of(
                "totalProcessed", totalProcessed,
                "totalFailed", totalFailed,
                "failures", failures
        );

        String message = totalFailed == 0
                ? String.format("Embedding backfill completed: %d questions processed", totalProcessed)
                : String.format("Embedding backfill completed with errors: %d processed, %d failed",
                        totalProcessed, totalFailed);

        log.info("Embedding backfill finished for tenant={}: processed={}, failed={}",
                tenantId, totalProcessed, totalFailed);

        return ResponseEntity.ok(ApiResponse.success(summary, message));
    }
}
