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

package com.examplatform.papergenerator.service;

import com.examplatform.papergenerator.client.QuestionBankClient;
import com.examplatform.papergenerator.domain.Paper;
import com.examplatform.papergenerator.dto.BatchTranslationJobResponseDto;
import com.examplatform.papergenerator.dto.PaperTranslateRequest;
import com.examplatform.papergenerator.dto.PaperTranslateResponse;
import com.examplatform.papergenerator.repository.PaperRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service orchestrating batch translation for entire examination papers.
 * Extracts question IDs from the paper definition and triggers async batch translation
 * across the Question Bank service using IndicTrans2 AI with upsert and PUBLISHED status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperTranslationService {

    private final PaperRepository paperRepository;
    private final QuestionBankClient questionBankClient;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public PaperTranslateResponse translatePaper(
            UUID paperId,
            PaperTranslateRequest request,
            UUID initiatedBy,
            String tenantId) {

        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";

        Paper paper = paperRepository.findByIdAndTenantId(paperId, effectiveTenant)
                .or(() -> paperRepository.findById(paperId))
                .orElseThrow(() -> new EntityNotFoundException("Paper not found: " + paperId));

        List<UUID> questionIds = extractQuestionIds(paper);
        if (questionIds.isEmpty()) {
            throw new IllegalArgumentException("Paper " + paperId + " contains no questions to translate.");
        }

        PaperTranslateRequest effectiveRequest = (request != null) ? request : new PaperTranslateRequest();

        log.info("Initiating paper translation: paperId={}, questionCount={}, targetLang={}, overwrite={}, tenant={}",
                paperId, questionIds.size(), effectiveRequest.getTargetLanguage(), effectiveRequest.getOverwriteExisting(), effectiveTenant);

        BatchTranslationJobResponseDto jobResponse = questionBankClient.triggerBatchTranslation(
                paperId,
                questionIds,
                effectiveRequest,
                initiatedBy,
                effectiveTenant
        );

        return mapToPaperTranslateResponse(paperId, jobResponse, questionIds.size());
    }

    public PaperTranslateResponse getTranslationStatus(UUID paperId, UUID jobId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        BatchTranslationJobResponseDto jobResponse = questionBankClient.getBatchTranslationStatus(jobId, effectiveTenant);
        return mapToPaperTranslateResponse(paperId, jobResponse, jobResponse != null ? jobResponse.getTotalQuestions() : 0);
    }

    public List<UUID> extractQuestionIds(Paper paper) {
        List<UUID> questionIds = new ArrayList<>();
        if (paper.getPaperDefinitionJson() != null && !paper.getPaperDefinitionJson().isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(paper.getPaperDefinitionJson());
                JsonNode qIdsNode = root.get("questionIds");
                if (qIdsNode != null && qIdsNode.isArray()) {
                    for (JsonNode qNode : qIdsNode) {
                        try {
                            questionIds.add(UUID.fromString(qNode.asText()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse paperDefinitionJson for paper {}: {}", paper.getId(), e.getMessage());
            }
        }
        return questionIds;
    }

    private PaperTranslateResponse mapToPaperTranslateResponse(UUID paperId, BatchTranslationJobResponseDto dto, int fallbackTotal) {
        if (dto == null) {
            return PaperTranslateResponse.builder()
                    .paperId(paperId)
                    .status("UNKNOWN")
                    .totalQuestions(fallbackTotal)
                    .message("No batch job details returned from translation service")
                    .build();
        }

        return PaperTranslateResponse.builder()
                .jobId(dto.getId())
                .paperId(paperId)
                .status(dto.getStatus())
                .sourceLanguage(dto.getSourceLanguage())
                .targetLanguage(dto.getTargetLanguage())
                .targetStatus(dto.getTargetStatus())
                .overwriteExisting(dto.isOverwriteExisting())
                .totalQuestions(dto.getTotalQuestions() > 0 ? dto.getTotalQuestions() : fallbackTotal)
                .processedQuestions(dto.getProcessedQuestions())
                .successfulQuestions(dto.getSuccessfulQuestions())
                .failedQuestions(dto.getFailedQuestions())
                .progressPercentage(dto.getProgressPercentage())
                .message("Batch translation job " + (dto.getStatus() != null ? dto.getStatus() : "PENDING"))
                .startedAt(dto.getStartedAt())
                .completedAt(dto.getCompletedAt())
                .build();
    }
}
