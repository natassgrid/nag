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

import com.examplatform.questionbank.domain.Subject;
import com.examplatform.questionbank.domain.Subtopic;
import com.examplatform.questionbank.domain.Topic;
import com.examplatform.questionbank.domain.enums.CognitiveLevel;
import com.examplatform.questionbank.domain.enums.DifficultyLevel;
import com.examplatform.questionbank.domain.enums.QuestionType;
import com.examplatform.questionbank.dto.CreateQuestionRequest;
import com.examplatform.questionbank.dto.QuestionOption;
import com.examplatform.questionbank.dto.QuestionResponse;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestion;
import com.examplatform.questionbank.extraction.domain.ExtractedQuestionStatus;
import com.examplatform.questionbank.extraction.domain.QuestionPaperMetadata;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsRequest;
import com.examplatform.questionbank.extraction.dto.PromoteQuestionsResponse;
import com.examplatform.questionbank.extraction.repository.ExtractedQuestionRepository;
import com.examplatform.questionbank.extraction.repository.QuestionPaperMetadataRepository;
import com.examplatform.questionbank.repository.SubjectRepository;
import com.examplatform.questionbank.repository.SubtopicRepository;
import com.examplatform.questionbank.repository.TopicRepository;
import com.examplatform.questionbank.service.QuestionService;
import com.examplatform.questionbank.translation.domain.Translation;
import com.examplatform.questionbank.translation.dto.TranslatedOptionDto;
import com.examplatform.questionbank.translation.service.TranslationWorkflowService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service managing promotion of verified extracted questions from staging records
 * into the canonical {@code question_service.question} repository and creating
 * corresponding bilingual translations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperQuestionPromotionService {

    private static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final QuestionPaperMetadataRepository metadataRepository;
    private final ExtractedQuestionRepository extractedQuestionRepository;
    private final QuestionService questionService;
    private final TranslationWorkflowService translationWorkflowService;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;

    /**
     * Promotes verified extracted questions to the canonical question bank.
     *
     * @param paperId question paper metadata ID
     * @param request promotion configuration and target questions
     * @param promoterUserId UUID of the reviewing/promoting user
     * @param tenantId examination authority identifier
     * @return summary response containing promoted question IDs and failure diagnostics
     */
    @Transactional
    public PromoteQuestionsResponse promoteQuestions(
            UUID paperId,
            PromoteQuestionsRequest request,
            UUID promoterUserId,
            String tenantId) {

        QuestionPaperMetadata paper = metadataRepository.findByIdAndTenantId(paperId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException("Question paper not found: " + paperId));

        List<ExtractedQuestion> candidates;
        if (request != null && request.getExtractedQuestionIds() != null && !request.getExtractedQuestionIds().isEmpty()) {
            candidates = extractedQuestionRepository.findByIdInAndPaperMetadataIdAndTenantIdOrderByQuestionNumberAsc(
                    request.getExtractedQuestionIds(), paperId, tenantId);
        } else {
            candidates = extractedQuestionRepository.findByPaperMetadataIdAndStatusAndTenantIdOrderByQuestionNumberAsc(
                    paperId, ExtractedQuestionStatus.APPROVED, tenantId);
        }

        UUID resolvedPromoterId = promoterUserId != null ? promoterUserId : SYSTEM_USER_ID;
        List<UUID> promotedQuestionIds = new ArrayList<>();
        Map<UUID, String> failureReasons = new HashMap<>();
        int totalSkipped = 0;
        int totalPromoted = 0;
        int totalFailed = 0;

        for (ExtractedQuestion eq : candidates) {
            if (eq.getStatus() == ExtractedQuestionStatus.PROMOTED && eq.getPromotedQuestionId() != null) {
                totalSkipped++;
                continue;
            }

            try {
                Long subjectId = resolveSubjectId(eq, request, paper, tenantId);
                Long topicId = resolveTopicId(eq, request, paper, subjectId, tenantId);
                Long subtopicId = resolveSubtopicId(eq, request, paper, topicId, tenantId);

                if (subjectId == null || topicId == null || subtopicId == null) {
                    failureReasons.put(eq.getId(), "Unable to resolve Subject -> Topic -> Subtopic hierarchy");
                    totalFailed++;
                    continue;
                }

                QuestionType questionType = resolveQuestionType(eq.getQuestionType());
                DifficultyLevel difficulty = resolveDifficulty(eq.getSuggestedDifficulty());
                CognitiveLevel cognitiveLevel = resolveCognitiveLevel(eq.getSuggestedCognitiveLevel());

                List<QuestionOption> options = prepareOptionsForPromotion(eq, questionType);

                String targetState = request != null && request.getTargetQuestionState() != null
                        ? request.getTargetQuestionState() : "APPROVED";

                CreateQuestionRequest createReq = CreateQuestionRequest.builder()
                        .subjectId(subjectId)
                        .topicId(topicId)
                        .subtopicId(subtopicId)
                        .questionType(questionType)
                        .difficulty(difficulty)
                        .cognitiveLevel(cognitiveLevel)
                        .content(eq.getContent())
                        .options(options)
                        .answerKey(eq.getAnswerKey())
                        .explanation(eq.getExplanation())
                        .state(targetState)
                        .build();

                QuestionResponse created = questionService.createQuestion(createReq, resolvedPromoterId, tenantId);

                boolean shouldCreateTranslations = request == null || request.isCreateTranslations();
                if (shouldCreateTranslations && eq.getTranslations() != null && !eq.getTranslations().isEmpty()) {
                    persistExtractedTranslations(created.getId(), eq, resolvedPromoterId, tenantId, paper.getExaminationName());
                }

                eq.setPromotedQuestionId(created.getId());
                eq.setStatus(ExtractedQuestionStatus.PROMOTED);
                extractedQuestionRepository.save(eq);

                promotedQuestionIds.add(created.getId());
                totalPromoted++;
            } catch (Exception ex) {
                log.warn("Failed to promote extracted question {}: {}", eq.getId(), ex.getMessage(), ex);
                failureReasons.put(eq.getId(), ex.getMessage() != null ? ex.getMessage() : "Promotion creation failed");
                totalFailed++;
            }
        }

        int promotedCount = (int) extractedQuestionRepository.countByPaperMetadataIdAndStatusAndTenantId(
                paperId, ExtractedQuestionStatus.PROMOTED, tenantId);
        int approvedCount = (int) extractedQuestionRepository.countByPaperMetadataIdAndStatusAndTenantId(
                paperId, ExtractedQuestionStatus.APPROVED, tenantId);
        paper.setQuestionsPromoted(promotedCount);
        paper.setQuestionsApproved(approvedCount);
        metadataRepository.save(paper);

        return PromoteQuestionsResponse.builder()
                .paperMetadataId(paperId)
                .totalRequested(candidates.size())
                .totalPromoted(totalPromoted)
                .totalSkipped(totalSkipped)
                .totalFailed(totalFailed)
                .promotedQuestionIds(promotedQuestionIds)
                .failureReasons(failureReasons)
                .build();
    }

    private Long resolveSubjectId(ExtractedQuestion eq, PromoteQuestionsRequest request, QuestionPaperMetadata paper, String tenantId) {
        if (eq.getSuggestedSubjectId() != null) {
            return eq.getSuggestedSubjectId();
        }
        if (request != null && request.getFallbackSubjectId() != null) {
            return request.getFallbackSubjectId();
        }
        if (paper.getDefaultSubjectId() != null) {
            return paper.getDefaultSubjectId();
        }
        return subjectRepository.findByTenantId(tenantId).stream()
                .findFirst()
                .map(Subject::getId)
                .orElseGet(() -> subjectRepository.findByTenantIdIsNull().stream()
                        .findFirst()
                        .map(Subject::getId)
                        .orElse(null));
    }

    private Long resolveTopicId(ExtractedQuestion eq, PromoteQuestionsRequest request, QuestionPaperMetadata paper, Long subjectId, String tenantId) {
        if (eq.getSuggestedTopicId() != null) {
            return eq.getSuggestedTopicId();
        }
        if (request != null && request.getFallbackTopicId() != null) {
            return request.getFallbackTopicId();
        }
        if (paper.getDefaultTopicId() != null) {
            return paper.getDefaultTopicId();
        }
        if (subjectId != null) {
            return topicRepository.findBySubjectIdAndTenantId(subjectId, tenantId).stream()
                    .findFirst()
                    .map(Topic::getId)
                    .orElseGet(() -> topicRepository.findBySubjectIdAndTenantIdIsNull(subjectId).stream()
                            .findFirst()
                            .map(Topic::getId)
                            .orElse(null));
        }
        return null;
    }

    private Long resolveSubtopicId(ExtractedQuestion eq, PromoteQuestionsRequest request, QuestionPaperMetadata paper, Long topicId, String tenantId) {
        if (eq.getSuggestedSubtopicId() != null) {
            return eq.getSuggestedSubtopicId();
        }
        if (request != null && request.getFallbackSubtopicId() != null) {
            return request.getFallbackSubtopicId();
        }
        if (paper.getDefaultSubtopicId() != null) {
            return paper.getDefaultSubtopicId();
        }
        if (topicId != null) {
            return subtopicRepository.findByTopicIdAndTenantId(topicId, tenantId).stream()
                    .findFirst()
                    .map(Subtopic::getId)
                    .orElseGet(() -> subtopicRepository.findByTopicIdAndTenantIdIsNull(topicId).stream()
                            .findFirst()
                            .map(Subtopic::getId)
                            .orElse(null));
        }
        return null;
    }

    private QuestionType resolveQuestionType(String typeStr) {
        if (typeStr == null || typeStr.isBlank()) {
            return QuestionType.SINGLE_MCQ;
        }
        QuestionType resolved = QuestionType.fromString(typeStr);
        return resolved != null ? resolved : QuestionType.SINGLE_MCQ;
    }

    private DifficultyLevel resolveDifficulty(String diffStr) {
        if (diffStr == null || diffStr.isBlank()) {
            return DifficultyLevel.MEDIUM;
        }
        DifficultyLevel resolved = DifficultyLevel.fromString(diffStr);
        return resolved != null ? resolved : DifficultyLevel.MEDIUM;
    }

    private CognitiveLevel resolveCognitiveLevel(String cogStr) {
        if (cogStr == null || cogStr.isBlank()) {
            return CognitiveLevel.UNDERSTAND;
        }
        CognitiveLevel resolved = CognitiveLevel.fromString(cogStr);
        return resolved != null ? resolved : CognitiveLevel.UNDERSTAND;
    }

    private List<QuestionOption> prepareOptionsForPromotion(ExtractedQuestion eq, QuestionType questionType) {
        if (questionType != QuestionType.SINGLE_MCQ && questionType != QuestionType.MULTI_MCQ) {
            return eq.getOptions() != null ? new ArrayList<>(eq.getOptions()) : new ArrayList<>();
        }

        List<QuestionOption> options = eq.getOptions() != null ? new ArrayList<>(eq.getOptions()) : new ArrayList<>();
        if (options.size() < 2) {
            options = new ArrayList<>(List.of(
                    new QuestionOption("A", "Option A", true, null, null),
                    new QuestionOption("B", "Option B", false, null, null),
                    new QuestionOption("C", "Option C", false, null, null),
                    new QuestionOption("D", "Option D", false, null, null)
            ));
        }

        String[] ids = {"A", "B", "C", "D", "E", "F"};
        for (int i = 0; i < options.size(); i++) {
            options.get(i).setId(i < ids.length ? ids[i] : String.valueOf((char) ('A' + i)));
        }

        String key = eq.getAnswerKey();
        if (key != null && !key.isBlank()) {
            String cleanKey = key.trim();
            if (cleanKey.matches("^[1-6]$")) {
                int idx = Integer.parseInt(cleanKey) - 1;
                cleanKey = ids[idx];
            }
            for (QuestionOption opt : options) {
                opt.setCorrect(opt.getId().equalsIgnoreCase(cleanKey));
            }
        }

        long correctCount = options.stream().filter(QuestionOption::isCorrect).count();
        if (questionType == QuestionType.SINGLE_MCQ && correctCount != 1) {
            for (int i = 0; i < options.size(); i++) {
                options.get(i).setCorrect(i == 0);
            }
        } else if (questionType == QuestionType.MULTI_MCQ && correctCount < 1) {
            options.get(0).setCorrect(true);
        }

        return options;
    }

    private void persistExtractedTranslations(
            UUID createdQuestionId,
            ExtractedQuestion eq,
            UUID promoterId,
            String tenantId,
            String examName) {

        Map<String, Map<String, Object>> translations = eq.getTranslations();
        for (Map.Entry<String, Map<String, Object>> entry : translations.entrySet()) {
            String langCode = entry.getKey();
            if (!TranslationWorkflowService.SUPPORTED_LANGUAGES.contains(langCode.toLowerCase())) {
                continue;
            }

            Map<String, Object> payload = entry.getValue();
            if (payload == null) {
                continue;
            }

            String content = (String) payload.get("content");
            String explanation = (String) payload.get("explanation");
            Object rawOptions = payload.get("options");

            List<TranslatedOptionDto> optionDtos = new ArrayList<>();
            if (rawOptions instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof QuestionOption opt) {
                        optionDtos.add(new TranslatedOptionDto(opt.getId(), opt.getText(), opt.getImageUrl(), opt.getImageAltText()));
                    } else if (item instanceof Map<?, ?> map) {
                        String id = (String) map.get("id");
                        String text = (String) map.get("text");
                        String imageUrl = (String) map.get("imageUrl");
                        String imageAltText = (String) map.get("imageAltText");
                        optionDtos.add(new TranslatedOptionDto(id, text, imageUrl, imageAltText));
                    }
                }
            }

            try {
                translationWorkflowService.upsertTranslation(
                        createdQuestionId,
                        langCode,
                        content,
                        optionDtos,
                        explanation,
                        Translation.TranslationStatus.PUBLISHED,
                        promoterId,
                        "Auto-promoted from Question Paper: " + examName,
                        tenantId
                );
            } catch (Exception e) {
                log.warn("Failed to create {} translation for promoted question {}: {}", langCode, createdQuestionId, e.getMessage());
            }
        }
    }
}
