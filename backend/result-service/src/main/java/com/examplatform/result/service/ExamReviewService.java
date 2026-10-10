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

package com.examplatform.result.service;

import com.examplatform.result.client.EvaluationClient;
import com.examplatform.result.client.QuestionBankClient;
import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateEvaluationItemDto;
import com.examplatform.result.dto.ExamReviewResponse;
import com.examplatform.result.dto.QuestionAnalyticsResult;
import com.examplatform.result.dto.QuestionDetailDto;
import com.examplatform.result.dto.ReviewOptionDto;
import com.examplatform.result.dto.ReviewQuestionDto;
import com.examplatform.result.repository.ResultRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for assembling real cross-service exam review data.
 * Combines candidate evaluations, question bank content, and peer analytics.
 *
 * Validates: Requirements 13.1, 26.1, SPEC-UI3, Issue #110
 */
@Slf4j
@Service
public class ExamReviewService {

    private final EvaluationClient evaluationClient;
    private final QuestionBankClient questionBankClient;
    private final QuestionAnalyticsService questionAnalyticsService;
    private final ResultRepository resultRepository;

    public ExamReviewService(
            EvaluationClient evaluationClient,
            QuestionBankClient questionBankClient,
            QuestionAnalyticsService questionAnalyticsService) {
        this(evaluationClient, questionBankClient, questionAnalyticsService, null);
    }

    @Autowired
    public ExamReviewService(
            EvaluationClient evaluationClient,
            QuestionBankClient questionBankClient,
            QuestionAnalyticsService questionAnalyticsService,
            ResultRepository resultRepository) {
        this.evaluationClient = evaluationClient;
        this.questionBankClient = questionBankClient;
        this.questionAnalyticsService = questionAnalyticsService;
        this.resultRepository = resultRepository;
    }

    public Optional<Result> findByQrVerificationCode(String code) {
        return resultRepository != null ? resultRepository.findByQrVerificationCode(code) : Optional.empty();
    }

    /**
     * Assembles the full post-exam review for a candidate.
     *
     * @param candidateId candidate UUID
     * @param examId      exam UUID
     * @param tenantId    tenant identifier
     * @return ExamReviewResponse containing per-question review data
     */
    public ExamReviewResponse getExamReview(UUID candidateId, UUID examId, String tenantId) {
        log.info("Assembling exam review for candidate={}, exam={}, tenant={}", candidateId, examId, tenantId);

        // 1. Fetch evaluations from evaluation-service
        List<CandidateEvaluationItemDto> evaluations = evaluationClient.getEvaluationsForCandidate(
                candidateId, examId, tenantId);

        if (evaluations == null || evaluations.isEmpty()) {
            log.info("No evaluation records found for candidate={}, returning empty review list", candidateId);
            return ExamReviewResponse.builder()
                    .examId(examId)
                    .candidateId(candidateId)
                    .questions(Collections.emptyList())
                    .build();
        }

        // 2. Extract question IDs and fetch question content from question-bank-service
        List<UUID> questionIds = evaluations.stream()
                .map(CandidateEvaluationItemDto::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, QuestionDetailDto> questionMap = questionIds.isEmpty()
                ? Collections.emptyMap()
                : QuestionBankClient.toQuestionMap(questionBankClient.findQuestionsByIds(questionIds, tenantId));

        // 3. Retrieve peer accuracy from QuestionAnalyticsService
        Map<UUID, Double> peerAccuracyMap = new HashMap<>();
        try {
            List<QuestionAnalyticsResult> analytics = questionAnalyticsService.computeAnalytics(examId, tenantId);
            if (analytics != null) {
                for (QuestionAnalyticsResult qar : analytics) {
                    if (qar.getQuestionId() != null) {
                        peerAccuracyMap.put(qar.getQuestionId(), qar.getDifficultyIndex() * 100.0);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to compute question analytics for exam={}: {}", examId, e.getMessage());
        }

        // 4. Assemble ReviewQuestionDto list
        List<ReviewQuestionDto> reviewQuestions = new ArrayList<>();
        int questionNumber = 1;

        for (CandidateEvaluationItemDto eval : evaluations) {
            UUID qId = eval.getQuestionId();
            QuestionDetailDto qDetail = questionMap.get(qId);

            String content = qDetail != null && qDetail.getContent() != null ? qDetail.getContent() : "Question content";
            String subject = qDetail != null && qDetail.getSubject() != null ? qDetail.getSubject() : "General";
            String topic = qDetail != null && qDetail.getTopic() != null ? qDetail.getTopic() : "General";
            String difficulty = qDetail != null && qDetail.getDifficulty() != null ? qDetail.getDifficulty() : "MEDIUM";
            String bloomsLevel = qDetail != null && qDetail.getCognitiveLevel() != null ? qDetail.getCognitiveLevel() : "APPLY";
            List<ReviewOptionDto> options = qDetail != null && qDetail.getOptions() != null
                    ? qDetail.getOptions()
                    : Collections.emptyList();
            String explanation = qDetail != null && qDetail.getExplanation() != null
                    ? qDetail.getExplanation()
                    : (qDetail != null && qDetail.getAnswerKey() != null
                    ? "Correct answer: " + qDetail.getAnswerKey()
                    : "No explanation provided.");

            List<String> selectedOptions = eval.getCandidateSelectedOptionIds() != null
                    ? eval.getCandidateSelectedOptionIds()
                    : Collections.emptyList();

            boolean isCorrect = eval.getScore() > 0 && eval.getScore() >= eval.getMaxMarks();
            if (!isCorrect && !options.isEmpty() && !selectedOptions.isEmpty()) {
                List<String> correctOptionIds = options.stream()
                        .filter(ReviewOptionDto::isCorrect)
                        .map(ReviewOptionDto::getId)
                        .toList();
                if (!correctOptionIds.isEmpty() && correctOptionIds.containsAll(selectedOptions) && selectedOptions.containsAll(correctOptionIds)) {
                    isCorrect = true;
                }
            }

            double peerAccuracy = peerAccuracyMap.getOrDefault(qId, 50.0);

            reviewQuestions.add(ReviewQuestionDto.builder()
                    .questionId(qId)
                    .questionNumber(questionNumber++)
                    .content(content)
                    .subject(subject)
                    .topic(topic)
                    .difficulty(difficulty)
                    .bloomsLevel(bloomsLevel)
                    .options(options)
                    .candidateSelectedOptionIds(selectedOptions)
                    .isCorrect(isCorrect)
                    .marksAwarded(eval.getScore())
                    .timeSpentMs(eval.getTimeSpentMs())
                    .peerAccuracyPct(Math.round(peerAccuracy * 10.0) / 10.0)
                    .explanation(explanation)
                    .build());
        }

        log.info("Successfully assembled {} review questions for candidate={}, exam={}",
                reviewQuestions.size(), candidateId, examId);

        return ExamReviewResponse.builder()
                .examId(examId)
                .candidateId(candidateId)
                .questions(reviewQuestions)
                .build();
    }
}
