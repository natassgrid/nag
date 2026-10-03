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
import com.examplatform.result.dto.CandidateExamResponseDto;
import com.examplatform.result.dto.QuestionAnalyticsResult;
import com.examplatform.result.dto.QuestionDetailDto;
import com.examplatform.result.dto.ReviewOptionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service responsible for computing per-question psychometric analytics for an exam.
 * Calculates difficulty index, discrimination index (using Kelley's 27% method),
 * response distribution, and average time spent with 1-hour in-memory/Redis caching.
 *
 * Validates: Requirements 26.1, 26.5, Issue #111
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAnalyticsService {

    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final EvaluationClient evaluationClient;
    private final QuestionBankClient questionBankClient;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * Computes analytics for all questions in an exam with real cross-service data.
     *
     * Psychometric Formulas:
     * - Difficulty index (P) = correct responses / total attempted responses
     * - Discrimination index (D) = (top 27% correct rate) - (bottom 27% correct rate)
     * - Response distribution = count per option selected
     *
     * @param examId   the exam identifier
     * @param tenantId the tenant identifier
     * @return list of per-question analytics results
     */
    public List<QuestionAnalyticsResult> computeAnalytics(UUID examId, String tenantId) {
        String effectiveTenant = (tenantId != null && !tenantId.isBlank()) ? tenantId : "default";
        String cacheKey = effectiveTenant + ":" + examId;

        // 1. Check cache
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            log.debug("Returning cached question analytics for exam={}, tenant={}", examId, effectiveTenant);
            return cached.results();
        }

        log.info("Computing real question analytics for exam={}, tenant={}", examId, effectiveTenant);

        // 2. Fetch candidate responses for this exam from evaluation & response services
        List<CandidateExamResponseDto> responses = evaluationClient != null
                ? evaluationClient.getResponsesForExam(examId, effectiveTenant)
                : Collections.emptyList();

        if (responses.isEmpty()) {
            log.info("No responses found for exam={}, returning empty analytics", examId);
            return Collections.emptyList();
        }

        // 3. Fetch question definitions and options from question-bank-service
        List<UUID> questionIds = responses.stream()
                .map(CandidateExamResponseDto::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, QuestionDetailDto> questionMap = Collections.emptyMap();
        if (questionBankClient != null && !questionIds.isEmpty()) {
            List<QuestionDetailDto> questionDetails = questionBankClient.findQuestionsByIds(questionIds, effectiveTenant);
            if (questionDetails != null) {
                questionMap = questionDetails.stream()
                        .filter(q -> q.getId() != null)
                        .collect(Collectors.toMap(QuestionDetailDto::getId, Function.identity(), (a, b) -> a));
            }
        }

        // 4. Group all candidates and sort by exam total score to identify top/bottom 27% groups
        Map<UUID, Double> candidateTotalScoreMap = new HashMap<>();
        for (CandidateExamResponseDto r : responses) {
            candidateTotalScoreMap.putIfAbsent(r.getCandidateId(), r.getExamTotalScore());
        }

        List<Map.Entry<UUID, Double>> sortedCandidates = candidateTotalScoreMap.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        int totalCandidates = sortedCandidates.size();
        Set<UUID> top27CandidateIds;
        Set<UUID> bottom27CandidateIds;

        if (totalCandidates >= 2) {
            int groupSize = Math.max(1, (int) Math.round(totalCandidates * 0.27));
            // Ensure top and bottom groups do not overlap
            if (groupSize > totalCandidates / 2) {
                groupSize = totalCandidates / 2;
            }

            top27CandidateIds = sortedCandidates.subList(0, groupSize).stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            bottom27CandidateIds = sortedCandidates.subList(totalCandidates - groupSize, totalCandidates).stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());
        } else {
            top27CandidateIds = Collections.emptySet();
            bottom27CandidateIds = Collections.emptySet();
        }

        // 5. Group responses by questionId and compute indices
        Map<UUID, List<CandidateExamResponseDto>> responsesByQuestion = responses.stream()
                .filter(r -> r.getQuestionId() != null)
                .collect(Collectors.groupingBy(CandidateExamResponseDto::getQuestionId, LinkedHashMap::new, Collectors.toList()));

        List<QuestionAnalyticsResult> results = new ArrayList<>();

        for (Map.Entry<UUID, List<CandidateExamResponseDto>> entry : responsesByQuestion.entrySet()) {
            UUID qId = entry.getKey();
            List<CandidateExamResponseDto> qAttempts = entry.getValue();
            QuestionDetailDto qDetail = questionMap.get(qId);

            int totalAttempted = qAttempts.size();
            long totalCorrect = qAttempts.stream()
                    .filter(a -> isAttemptCorrect(a, qDetail))
                    .count();

            // Difficulty Index: correct / attempted
            double difficultyIndex = totalAttempted > 0 ? (double) totalCorrect / totalAttempted : 0.0;
            difficultyIndex = Math.round(difficultyIndex * 1000.0) / 1000.0;

            // Discrimination Index: top 27% correct rate - bottom 27% correct rate
            double discriminationIndex;
            if (!top27CandidateIds.isEmpty() && !bottom27CandidateIds.isEmpty()) {
                List<CandidateExamResponseDto> topAttempts = qAttempts.stream()
                        .filter(a -> top27CandidateIds.contains(a.getCandidateId()))
                        .toList();

                List<CandidateExamResponseDto> bottomAttempts = qAttempts.stream()
                        .filter(a -> bottom27CandidateIds.contains(a.getCandidateId()))
                        .toList();

                double topCorrectRate = !topAttempts.isEmpty()
                        ? (double) topAttempts.stream().filter(a -> isAttemptCorrect(a, qDetail)).count() / topAttempts.size()
                        : 0.0;

                double bottomCorrectRate = !bottomAttempts.isEmpty()
                        ? (double) bottomAttempts.stream().filter(a -> isAttemptCorrect(a, qDetail)).count() / bottomAttempts.size()
                        : 0.0;

                discriminationIndex = topCorrectRate - bottomCorrectRate;
                discriminationIndex = Math.max(-1.0, Math.min(1.0, discriminationIndex));
                discriminationIndex = Math.round(discriminationIndex * 1000.0) / 1000.0;
            } else {
                discriminationIndex = 0.0;
            }

            // Response Distribution
            Map<String, Integer> distribution = new LinkedHashMap<>();
            if (qDetail != null && qDetail.getOptions() != null) {
                for (ReviewOptionDto opt : qDetail.getOptions()) {
                    if (opt.getId() != null) {
                        distribution.put(opt.getId(), 0);
                    }
                }
            }

            for (CandidateExamResponseDto a : qAttempts) {
                if (a.getSelectedOptionIds() != null) {
                    for (String optId : a.getSelectedOptionIds()) {
                        distribution.put(optId, distribution.getOrDefault(optId, 0) + 1);
                    }
                }
            }

            // Average time spent
            long totalTimeMs = qAttempts.stream().mapToLong(CandidateExamResponseDto::getTimeSpentMs).sum();
            double avgTimeSpentMs = totalAttempted > 0 ? (double) totalTimeMs / totalAttempted : 0.0;

            String solutionExplanation = qDetail != null ? qDetail.getExplanation() : null;
            String correctAnswer = resolveCorrectAnswer(qDetail);

            results.add(QuestionAnalyticsResult.builder()
                    .questionId(qId)
                    .difficultyIndex(difficultyIndex)
                    .discriminationIndex(discriminationIndex)
                    .responseDistribution(distribution)
                    .avgTimeSpentMs(avgTimeSpentMs)
                    .totalAttempted(totalAttempted)
                    .totalCorrect((int) totalCorrect)
                    .solutionExplanation(solutionExplanation)
                    .correctAnswer(correctAnswer)
                    .build());
        }

        // Cache computed results
        cache.put(cacheKey, new CacheEntry(results, Instant.now().plus(CACHE_TTL)));
        log.info("Computed and cached analytics for {} questions in exam={}", results.size(), examId);

        return results;
    }

    /**
     * Determines whether an attempt is correct.
     */
    private boolean isAttemptCorrect(CandidateExamResponseDto attempt, QuestionDetailDto qDetail) {
        if (attempt.isCorrect()) {
            return true;
        }
        if (attempt.getQuestionScore() > 0 && attempt.getQuestionScore() >= attempt.getMaxMarks()) {
            return true;
        }
        if (qDetail != null && qDetail.getOptions() != null && attempt.getSelectedOptionIds() != null && !attempt.getSelectedOptionIds().isEmpty()) {
            List<String> correctOptionIds = qDetail.getOptions().stream()
                    .filter(ReviewOptionDto::isCorrect)
                    .map(ReviewOptionDto::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (!correctOptionIds.isEmpty() &&
                    correctOptionIds.containsAll(attempt.getSelectedOptionIds()) &&
                    attempt.getSelectedOptionIds().containsAll(correctOptionIds)) {
                return true;
            }
        }
        return false;
    }

    private String resolveCorrectAnswer(QuestionDetailDto qDetail) {
        if (qDetail == null) return null;
        if (qDetail.getOptions() != null) {
            List<String> correctOptions = qDetail.getOptions().stream()
                    .filter(ReviewOptionDto::isCorrect)
                    .map(ReviewOptionDto::getId)
                    .filter(Objects::nonNull)
                    .toList();
            if (!correctOptions.isEmpty()) {
                return String.join(", ", correctOptions);
            }
        }
        return qDetail.getAnswerKey();
    }

    /**
     * Invalidates cached analytics for a specific exam.
     */
    public void invalidateCache(UUID examId) {
        if (examId != null) {
            cache.keySet().removeIf(k -> k.endsWith(":" + examId));
            log.info("Invalidated question analytics cache for exam={}", examId);
        }
    }

    /**
     * Clears all cached question analytics.
     */
    public void clearCache() {
        cache.clear();
        log.info("Cleared all question analytics cache");
    }

    private record CacheEntry(List<QuestionAnalyticsResult> results, Instant expiry) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}
