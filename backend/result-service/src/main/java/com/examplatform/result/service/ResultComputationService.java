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

package com.examplatform.result.service;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateScoreInput;
import com.examplatform.result.dto.SectionBreakdownDto;
import com.examplatform.result.dto.TaxonomyBreakdownDto;
import com.examplatform.result.repository.ResultRepository;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.messaging.EventPublisher;
import com.examplatform.shared.util.DataConversionUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

import static com.examplatform.shared.constants.MessagingConstants.AUDIT_EVENTS_TOPIC;

/**
 * Service responsible for computing examination results including total scores,
 * section-wise scores, rankings, percentiles, and optional shift normalization.
 *
 * Validates: Requirements 13.1, 13.2
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ResultComputationService {

    private static final double TARGET_MEAN = 50.0;
    private static final double TARGET_STD_DEV = 10.0;

    private final ResultRepository resultRepository;
    private final ObjectMapper objectMapper;
    private final EventPublisher eventPublisher;

    /**
     * Computes results for all candidates in an exam.
     *
     * <ol>
     *   <li>If normalizeShifts is true, applies shift normalization formula</li>
     *   <li>Sorts candidates by final totalScore descending</li>
     *   <li>Assigns overallRank (1-based, ties get same rank)</li>
     *   <li>Computes overallPercentile</li>
     *   <li>Serializes sectionScores to JSONB</li>
     *   <li>Builds and saves Result records</li>
     * </ol>
     *
     * @param examId           the exam identifier
     * @param candidateScores  list of candidate score inputs
     * @param normalizeShifts  whether to apply shift normalization
     * @param tenantId         the tenant identifier
     * @return the saved Result records
     */
    public List<Result> computeResults(UUID examId,
                                       List<CandidateScoreInput> candidateScores,
                                       boolean normalizeShifts,
                                       String tenantId) {
        log.info("Computing results for exam={} with {} candidates, normalization={}",
                examId, candidateScores.size(), normalizeShifts);

        int totalCandidates = candidateScores.size();
        if (totalCandidates == 0) {
            return List.of();
        }

        // Step 1: Compute final scores (apply normalization if configured)
        List<ScoredCandidate> scoredCandidates = new ArrayList<>();
        for (CandidateScoreInput input : candidateScores) {
            double finalScore;
            if (normalizeShifts && input.getShiftStdDev() > 0) {
                finalScore = applyShiftNormalization(input.getTotalRawScore(),
                        input.getShiftMean(), input.getShiftStdDev());
            } else {
                finalScore = input.getTotalRawScore();
            }
            scoredCandidates.add(new ScoredCandidate(input, finalScore));
        }

        // Step 2: Sort by final totalScore descending
        scoredCandidates.sort(Comparator.comparingDouble(ScoredCandidate::finalScore).reversed());

        // Step 3: Assign ranks (1-based, ties get same rank)
        assignRanks(scoredCandidates);

        // Step 4: Compute percentiles
        computePercentiles(scoredCandidates, totalCandidates);

        // Step 5 & 6: Build and save Result records
        List<Result> results = new ArrayList<>();
        for (ScoredCandidate sc : scoredCandidates) {
            Result result = buildResult(sc, examId, tenantId);
            results.add(result);
        }

        List<Result> savedResults = resultRepository.saveAll(results);
        log.info("Saved {} result records for exam={}", savedResults.size(), examId);

        // Publish RESULT_PUBLISHED audit event (fire-and-forget)
        publishResultAuditEvent(examId, savedResults.size(), tenantId);

        return savedResults;
    }

    /**
     * Retrieves a result for a specific candidate in an exam within a tenant.
     *
     * @param candidateId the candidate identifier
     * @param examId      the exam identifier
     * @param tenantId    the tenant identifier
     * @return the Result entity
     * @throws EntityNotFoundException if no result found
     */
    @Transactional(readOnly = true)
    public Result getResult(UUID candidateId, UUID examId, String tenantId) {
        return resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, examId, tenantId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("Result not found for candidate=%s, exam=%s, tenant=%s",
                                candidateId, examId, tenantId)));
    }

    /**
     * Retrieves all results for a candidate within a tenant.
     *
     * @param candidateId the candidate identifier
     * @param tenantId    the tenant identifier
     * @return the list of results
     */
    @Transactional(readOnly = true)
    public List<Result> getCandidateResults(UUID candidateId, String tenantId) {
        return resultRepository.findByCandidateIdAndTenantId(candidateId, tenantId);
    }

    /**
     * Publishes a RESULT_PUBLISHED audit event (fire-and-forget).
     */
     private void publishResultAuditEvent(UUID examId, int candidateCount, String tenantId) {
        try {
            Map<String, Object> event = Map.of(
                SharedConstants.EVENT_TYPE, "RESULT_PUBLISHED",
                    "examId", examId.toString(),
                    "candidateCount", candidateCount,
                SharedConstants.TENANT_ID, tenantId,
                SharedConstants.OCCURRED_AT, Instant.now().toString()
            );
            eventPublisher.publish(AUDIT_EVENTS_TOPIC, examId.toString(), event);
        } catch (Exception e) {
            log.error("Unexpected error publishing RESULT_PUBLISHED audit event: {}", e.getMessage());
        }
    }

    /**
     * Applies the shift normalization formula:
     * normalizedScore = (score - shiftMean) / shiftStdDev * targetStdDev + targetMean
     */
    double applyShiftNormalization(double rawScore, double shiftMean, double shiftStdDev) {
        return ((rawScore - shiftMean) / shiftStdDev) * TARGET_STD_DEV + TARGET_MEAN;
    }

    private void assignRanks(List<ScoredCandidate> scoredCandidates) {
        int rank = 1;
        for (int i = 0; i < scoredCandidates.size(); i++) {
            if (i > 0 && Double.compare(scoredCandidates.get(i).finalScore(),
                    scoredCandidates.get(i - 1).finalScore()) != 0) {
                rank = i + 1;
            }
            scoredCandidates.get(i).setRank(rank);
        }
    }

    private void computePercentiles(List<ScoredCandidate> scoredCandidates, int totalCandidates) {
        for (ScoredCandidate sc : scoredCandidates) {
            long candidatesBelowThisScore = scoredCandidates.stream()
                    .filter(other -> other.finalScore() < sc.finalScore())
                    .count();
            double percentile = ((double) candidatesBelowThisScore / totalCandidates) * 100.0;
            sc.setPercentile(percentile);
        }
    }

    private Result buildResult(ScoredCandidate sc, UUID examId, String tenantId) {
        String sectionScoresJson = serializeSectionScores(sc.input().getSectionScores());
        String sectionalStatusJson = serializeSectionBreakdowns(sc.input().getSectionBreakdowns(), sc.input().getQuestionLevelScores());
        String topicBreakdownJson = serializeTopicBreakdowns(sc.input().getTopicBreakdowns(), sc.input().getSubtopicBreakdowns(), sc.input().getQuestionLevelScores());
        BigDecimal accuracyRate = calculateAccuracyRate(sc.input());

        Result result = Result.builder()
                .candidateId(sc.input().getCandidateId())
                .examId(examId)
                .totalScore(BigDecimal.valueOf(sc.finalScore()).setScale(2, RoundingMode.HALF_UP))
                .sectionScoresJson(sectionScoresJson)
                .sectionalStatusJson(sectionalStatusJson)
                .topicBreakdownJson(topicBreakdownJson)
                .accuracyRate(accuracyRate)
                .overallRank(sc.rank())
                .overallPercentile(BigDecimal.valueOf(sc.percentile()).setScale(3, RoundingMode.HALF_UP))
                .scorecardPdfRef(null)
                .digiLockerPushed(false)
                .build();
        result.setTenantId(tenantId);
        return result;
    }

    private String serializeSectionScores(Map<String, Double> sectionScores) {
        if (sectionScores == null || sectionScores.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(sectionScores);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize section scores", e);
            return "{}";
        }
    }

    private String serializeSectionBreakdowns(List<SectionBreakdownDto> sectionBreakdowns,
                                             List<Map<String, Object>> questionLevelScores) {
        List<SectionBreakdownDto> breakdowns = sectionBreakdowns;
        if ((breakdowns == null || breakdowns.isEmpty()) && questionLevelScores != null && !questionLevelScores.isEmpty()) {
            breakdowns = deriveSectionBreakdownsFromQuestions(questionLevelScores);
        }
        if (breakdowns == null || breakdowns.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(breakdowns);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize section breakdowns", e);
            return null;
        }
    }

    private String serializeTopicBreakdowns(List<TaxonomyBreakdownDto> topicBreakdowns,
                                           List<TaxonomyBreakdownDto> subtopicBreakdowns,
                                           List<Map<String, Object>> questionLevelScores) {
        List<TaxonomyBreakdownDto> breakdowns = subtopicBreakdowns;
        if (breakdowns == null || breakdowns.isEmpty()) {
            breakdowns = topicBreakdowns;
        }
        if ((breakdowns == null || breakdowns.isEmpty()) && questionLevelScores != null && !questionLevelScores.isEmpty()) {
            breakdowns = deriveTaxonomyBreakdownsFromQuestions(questionLevelScores);
        }
        if (breakdowns == null || breakdowns.isEmpty()) {
            return null;
        }
        try {
            for (TaxonomyBreakdownDto dto : breakdowns) {
                if (dto != null) {
                    dto.normalize();
                }
            }
            return objectMapper.writeValueAsString(breakdowns);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize topic/subtopic breakdowns", e);
            return null;
        }
    }

    private BigDecimal calculateAccuracyRate(CandidateScoreInput input) {
        if (input.getAccuracyRate() != null) {
            return BigDecimal.valueOf(input.getAccuracyRate()).setScale(2, RoundingMode.HALF_UP);
        }
        List<Map<String, Object>> questions = input.getQuestionLevelScores();
        if (questions == null || questions.isEmpty()) {
            return null;
        }
        long attempted = questions.stream()
                .filter(q -> {
                    String status = Objects.toString(q.get("status"), "");
                    return !"SKIPPED".equalsIgnoreCase(status) && !"UNATTEMPTED".equalsIgnoreCase(status);
                })
                .count();
        if (attempted == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long correct = questions.stream()
                .filter(q -> {
                    String status = Objects.toString(q.get("status"), "");
                    double score = DataConversionUtils.toDouble(q.get("score"));
                    return "CORRECT".equalsIgnoreCase(status) || score > 0;
                })
                .count();
        return BigDecimal.valueOf(correct * 100.0 / attempted).setScale(2, RoundingMode.HALF_UP);
    }

    private List<SectionBreakdownDto> deriveSectionBreakdownsFromQuestions(List<Map<String, Object>> questions) {
        Map<String, SectionAccumulator> map = new LinkedHashMap<>();
        for (Map<String, Object> q : questions) {
            UUID sectionId = DataConversionUtils.parseUUID(q.get("sectionId"));
            String sectionName = Objects.toString(q.get("sectionName"), null);
            String key = sectionId != null ? sectionId.toString() : (sectionName != null ? sectionName : "default");

            SectionAccumulator acc = map.computeIfAbsent(key, k -> {
                SectionAccumulator a = new SectionAccumulator();
                a.sectionId = sectionId;
                a.sectionName = sectionName != null ? sectionName : "Section";
                Object subjIdObj = q.get("subjectId");
                if (subjIdObj instanceof Number num) {
                    a.subjectId = num.longValue();
                }
                a.subject = Objects.toString(q.get("subject"), null);
                return a;
            });

            double maxMarks = DataConversionUtils.toDouble(q.get("maxMarks"));
            double score = DataConversionUtils.toDouble(q.get("score"));
            String status = Objects.toString(q.get("status"), "");

            acc.questionCount++;
            acc.maxMarks += maxMarks;
            acc.score += score;

            boolean isAttempted = !"SKIPPED".equalsIgnoreCase(status) && !"UNATTEMPTED".equalsIgnoreCase(status);
            if (isAttempted) {
                acc.attemptedCount++;
                if ("CORRECT".equalsIgnoreCase(status) || score > 0) {
                    acc.correctCount++;
                }
            }
        }

        List<SectionBreakdownDto> result = new ArrayList<>();
        for (SectionAccumulator acc : map.values()) {
            double accuracy = acc.attemptedCount > 0 ? (acc.correctCount * 100.0 / acc.attemptedCount) : 0.0;
            result.add(SectionBreakdownDto.builder()
                    .sectionId(acc.sectionId)
                    .sectionName(acc.sectionName)
                    .subjectId(acc.subjectId)
                    .subject(acc.subject)
                    .score(Math.round(acc.score * 100.0) / 100.0)
                    .maxMarks(Math.round(acc.maxMarks * 100.0) / 100.0)
                    .accuracyRate(Math.round(accuracy * 100.0) / 100.0)
                    .questionCount(acc.questionCount)
                    .attemptedCount(acc.attemptedCount)
                    .correctCount(acc.correctCount)
                    .build());
        }
        return result;
    }

    private List<TaxonomyBreakdownDto> deriveTaxonomyBreakdownsFromQuestions(List<Map<String, Object>> questions) {
        Map<String, TaxonomyAccumulator> map = new LinkedHashMap<>();
        for (Map<String, Object> q : questions) {
            Object subIdObj = q.get("subtopicId");
            Long subtopicId = subIdObj instanceof Number num ? num.longValue() : null;
            String subtopic = Objects.toString(q.get("subtopic"), null);

            Object topIdObj = q.get("topicId");
            Long topicId = topIdObj instanceof Number num ? num.longValue() : null;
            String topic = Objects.toString(q.get("topic"), null);

            Object subjIdObj = q.get("subjectId");
            Long subjectId = subjIdObj instanceof Number num ? num.longValue() : null;
            String subject = Objects.toString(q.get("subject"), null);

            String key = subtopicId != null ? ("sub-" + subtopicId)
                    : (subtopic != null ? ("subname-" + subtopic)
                    : (topicId != null ? ("top-" + topicId)
                    : (topic != null ? ("topname-" + topic) : "general")));

            TaxonomyAccumulator acc = map.computeIfAbsent(key, k -> {
                TaxonomyAccumulator a = new TaxonomyAccumulator();
                a.subjectId = subjectId;
                a.subject = subject != null ? subject : SharedConstants.GENERAL;
                a.topicId = topicId;
                a.topic = topic != null ? topic : SharedConstants.GENERAL;
                a.subtopicId = subtopicId;
                a.subtopic = subtopic != null ? subtopic : a.topic;
                return a;
            });

            double maxMarks = DataConversionUtils.toDouble(q.get("maxMarks"));
            double score = DataConversionUtils.toDouble(q.get("score"));
            String status = Objects.toString(q.get("status"), "");
            long timeSpent = q.get("timeSpentMs") instanceof Number n ? n.longValue() : 0L;

            acc.questionCount++;
            acc.maxMarks += maxMarks;
            acc.score += score;
            acc.totalTimeSpentMs += timeSpent;

            boolean isAttempted = !"SKIPPED".equalsIgnoreCase(status) && !"UNATTEMPTED".equalsIgnoreCase(status);
            if (isAttempted) {
                acc.attemptedCount++;
                if ("CORRECT".equalsIgnoreCase(status) || score > 0) {
                    acc.correctCount++;
                } else {
                    acc.incorrectCount++;
                }
            } else {
                acc.skippedCount++;
            }
        }

        List<TaxonomyBreakdownDto> result = new ArrayList<>();
        for (TaxonomyAccumulator acc : map.values()) {
            double accuracy = acc.attemptedCount > 0 ? (acc.correctCount * 100.0 / acc.attemptedCount) : 0.0;
            double skipRate = acc.questionCount > 0 ? (acc.skippedCount * 100.0 / acc.questionCount) : 0.0;
            long avgTime = acc.attemptedCount > 0 ? (acc.totalTimeSpentMs / acc.attemptedCount) : 0L;

            result.add(TaxonomyBreakdownDto.builder()
                    .subjectId(acc.subjectId)
                    .subject(acc.subject)
                    .subjectName(acc.subject)
                    .topicId(acc.topicId)
                    .topic(acc.topic)
                    .topicName(acc.topic)
                    .subtopicId(acc.subtopicId)
                    .subtopic(acc.subtopic)
                    .subtopicName(acc.subtopic)
                    .score(Math.round(acc.score * 100.0) / 100.0)
                    .maxMarks(Math.round(acc.maxMarks * 100.0) / 100.0)
                    .accuracyRate(Math.round(accuracy * 100.0) / 100.0)
                    .accuracyPercent(Math.round(accuracy * 100.0) / 100.0)
                    .questionCount(acc.questionCount)
                    .totalQuestions(acc.questionCount)
                    .attemptedCount(acc.attemptedCount)
                    .correctCount(acc.correctCount)
                    .incorrectCount(acc.incorrectCount)
                    .skippedCount(acc.skippedCount)
                    .totalTimeSpentMs(acc.totalTimeSpentMs)
                    .avgTimeSpentMs(avgTime)
                    .benchmarkTimeMs(90000L)
                    .skipRatePercent(Math.round(skipRate * 100.0) / 100.0)
                    .paceRatio(1.0)
                    .build());
        }
        return result;
    }

    private static class SectionAccumulator {
        UUID sectionId;
        String sectionName;
        Long subjectId;
        String subject;
        double score;
        double maxMarks;
        int questionCount;
        int attemptedCount;
        int correctCount;
    }

    private static class TaxonomyAccumulator {
        Long subjectId;
        String subject;
        Long topicId;
        String topic;
        Long subtopicId;
        String subtopic;
        double score;
        double maxMarks;
        int questionCount;
        int attemptedCount;
        int correctCount;
        int incorrectCount;
        int skippedCount;
        long totalTimeSpentMs;
    }

    /**
     * Internal mutable record used during computation.
     */
    static class ScoredCandidate {
        private final CandidateScoreInput input;
        private final double finalScore;
        private int rank;
        private double percentile;

        ScoredCandidate(CandidateScoreInput input, double finalScore) {
            this.input = input;
            this.finalScore = finalScore;
        }

        CandidateScoreInput input() {
            return input;
        }

        double finalScore() {
            return finalScore;
        }

        int rank() {
            return rank;
        }

        void setRank(int rank) {
            this.rank = rank;
        }

        double percentile() {
            return percentile;
        }

        void setPercentile(double percentile) {
            this.percentile = percentile;
        }
    }
}
