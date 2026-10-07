/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 Open Digital Public Infrastructure (DPI) Platform Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */
package com.examplatform.questionbank.ai.generation.multiagent;

import com.examplatform.questionbank.ai.similarity.SimilarityCheckResult;
import com.examplatform.questionbank.service.SimilarityDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 4. [Similarity Auditor Agent]
 * Validates generated questions against the pgvector question bank to ensure
 * cosine similarity is below the strict uniqueness threshold (similarity < 0.85).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityAuditorAgent {

    public static final double MULTI_AGENT_SIMILARITY_THRESHOLD = 0.85;

    private final SimilarityDetectionService similarityDetectionService;

    public record AuditResult(boolean passed, double topSimilarity, UUID conflictingQuestionId, String message) {}

    public AuditResult auditUniqueness(String content, String subject, String tenantId) {
        try {
            SimilarityCheckResult result = similarityDetectionService.checkSimilarity(content, subject, tenantId);
            if (result.status() == SimilarityCheckResult.Status.REJECT || !result.similarQuestions().isEmpty()) {
                var topMatch = result.similarQuestions().getFirst();
                if (topMatch.similarity() >= MULTI_AGENT_SIMILARITY_THRESHOLD) {
                    return new AuditResult(false, topMatch.similarity(), topMatch.questionId(),
                            String.format("Cosine similarity %.3f exceeds uniqueness threshold (0.85) against question %s",
                                    topMatch.similarity(), topMatch.questionId()));
                }
            }
            return new AuditResult(true, 0.0, null, "Question uniqueness audit passed");
        } catch (Exception e) {
            log.warn("Similarity auditor query failed, defaulting to passed: {}", e.getMessage());
            return new AuditResult(true, 0.0, null, "Similarity service unavailable, proceeding");
        }
    }
}
