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

package com.examplatform.analytics.service;

import com.examplatform.analytics.domain.CandidateAnalyticsResult;
import com.examplatform.analytics.domain.ExamAnalytics;
import com.examplatform.analytics.repository.CandidateAnalyticsResultRepository;
import com.examplatform.analytics.repository.ExamAnalyticsRepository;
import com.examplatform.shared.util.DataConversionUtils;
import com.examplatform.shared.util.UuidV7Generator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for computing, persisting, and exporting exam analytics.
 * Ingests Kafka/RabbitMQ result events, aggregates distribution statistics,
 * and generates structured PDF/CSV reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ExamAnalyticsRepository examAnalyticsRepository;
    private final CandidateAnalyticsResultRepository candidateResultRepository;
    private final AnalyticsPdfService analyticsPdfService;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<UUID, Object> examLocks = new ConcurrentHashMap<>();

    /**
     * Retrieves the latest computed analytics for the given exam.
     *
     * @param examId the exam identifier
     * @return the computed ExamAnalytics record
     * @throws IllegalArgumentException if no analytics record exists for the exam
     */
    @Transactional(readOnly = true)
    public ExamAnalytics getAnalyticsForExam(UUID examId) {
        return examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(examId)
                .orElseThrow(() -> new IllegalArgumentException("Analytics not found for exam: " + examId));
    }

    /**
     * Recomputes analytics for an exam on-demand from all persisted candidate evaluation records.
     *
     * @param examId the exam identifier
     * @return the newly computed and persisted ExamAnalytics record
     * @throws IllegalArgumentException if no candidate evaluation records exist for the exam
     */
    @Transactional
    public ExamAnalytics computeAnalyticsForExam(UUID examId) {
        log.info("Batch recomputation triggered for exam: {}", examId);

        List<CandidateAnalyticsResult> candidateResults = candidateResultRepository.findByExamId(examId);
        if (candidateResults.isEmpty()) {
            throw new IllegalArgumentException("No candidate evaluation records found for exam: " + examId);
        }

        return aggregateAndPersist(examId, candidateResults);
    }

    /**
     * Ingests an evaluation completed event, persists the candidate result record,
     * and incrementally updates the exam analytics.
     *
     * @param examId              the exam identifier
     * @param candidateId         the candidate identifier
     * @param sessionId           the exam session identifier
     * @param totalRawScore       the total score evaluated
     * @param sectionScores       section-wise scores map
     * @param questionLevelScores question-level evaluation details
     * @param tenantId            the tenant identifier
     * @param evaluatedAt         evaluation completion timestamp
     * @return the updated ExamAnalytics record
     */
    @Transactional
    public ExamAnalytics processEvaluationCompleted(UUID examId,
                                                    UUID candidateId,
                                                    UUID sessionId,
                                                    double totalRawScore,
                                                    Map<String, Object> sectionScores,
                                                    List<Map<String, Object>> questionLevelScores,
                                                    String tenantId,
                                                    Instant evaluatedAt) {
        log.info("Processing evaluation completed event for candidate={}, exam={}", candidateId, examId);

        Object examLock = examLocks.computeIfAbsent(examId, k -> new Object());
        synchronized (examLock) {
            // 1. Idempotently persist / update candidate evaluation record
            Optional<CandidateAnalyticsResult> existingResult =
                    candidateResultRepository.findByCandidateIdAndExamId(candidateId, examId);

            String sectionScoresJson = serializeJson(sectionScores);
            String questionScoresJson = serializeJson(questionLevelScores);

            CandidateAnalyticsResult candidateRecord = existingResult.orElseGet(() ->
                    CandidateAnalyticsResult.builder()
                            .id(UuidV7Generator.generate())
                            .examId(examId)
                            .candidateId(candidateId)
                            .sessionId(sessionId)
                            .tenantId(tenantId)
                            .createdAt(Instant.now())
                            .build()
            );

            candidateRecord.setSessionId(sessionId);
            candidateRecord.setTotalRawScore(BigDecimal.valueOf(totalRawScore).setScale(2, RoundingMode.HALF_UP));
            candidateRecord.setSectionScoresJson(sectionScoresJson);
            candidateRecord.setQuestionLevelScoresJson(questionScoresJson);
            candidateRecord.setTenantId(tenantId);
            candidateRecord.setEvaluatedAt(evaluatedAt != null ? evaluatedAt : Instant.now());

            candidateResultRepository.save(candidateRecord);

            // 2. Fetch all candidate records for the exam and update ExamAnalytics
            List<CandidateAnalyticsResult> allResults = candidateResultRepository.findByExamId(examId);
            return aggregateAndPersist(examId, allResults);
        }
    }

    /**
     * Exports analytics data in the specified format (csv or pdf).
     *
     * @param examId the exam identifier
     * @param format export format ('csv' or 'pdf')
     * @return raw export byte content
     */
    @Transactional(readOnly = true)
    public byte[] exportAnalytics(UUID examId, String format) {
        ExamAnalytics analytics = getAnalyticsForExam(examId);

        return switch (format.toLowerCase()) {
            case "csv" -> exportAsCsv(analytics);
            case "pdf" -> analyticsPdfService.generatePdfReport(analytics);
            default -> throw new IllegalArgumentException("Unsupported export format: " + format + ". Supported: csv, pdf");
        };
    }

    private ExamAnalytics aggregateAndPersist(UUID examId, List<CandidateAnalyticsResult> candidateResults) {
        long appearedCount = candidateResults.size();

        // Retrieve existing analytics to maintain totalRegistered if already set
        Optional<ExamAnalytics> existing = examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(examId);
        long registeredCount = existing.map(ExamAnalytics::getTotalRegistered).orElse(appearedCount);
        if (registeredCount < appearedCount) {
            registeredCount = appearedCount;
        }

        // 1. Extract raw scores
        List<Double> scores = new ArrayList<>(candidateResults.stream()
                .map(r -> r.getTotalRawScore().doubleValue())
                .sorted()
                .toList());

        // 2. Compute percentiles
        BigDecimal top10Percentile = computePercentileThreshold(scores, 90.0);
        BigDecimal bottom10Percentile = computePercentileThreshold(scores, 10.0);

        // 3. Compute score distribution
        Map<String, Long> scoreDistribution = computeScoreDistribution(scores);
        String scoreDistributionJson = serializeJson(scoreDistribution);

        // 4. Compute section averages
        Map<String, Double> sectionAverages = computeSectionAverages(candidateResults);
        String sectionAveragesJson = serializeJson(sectionAverages);

        // 5. Update existing record or create new
        ExamAnalytics analytics = existing.orElseGet(() ->
                ExamAnalytics.builder()
                        .id(UuidV7Generator.generate())
                        .examId(examId)
                        .build()
        );

        analytics.setTotalRegistered(registeredCount);
        analytics.setTotalAppeared(appearedCount);
        analytics.setScoreDistributionJson(scoreDistributionJson);
        analytics.setSectionAveragesJson(sectionAveragesJson);
        analytics.setTop10PercentileThreshold(top10Percentile);
        analytics.setBottom10PercentileThreshold(bottom10Percentile);
        analytics.setComputedAt(Instant.now());

        ExamAnalytics saved = examAnalyticsRepository.save(analytics);
        log.info("Analytics computed and saved for exam={}: appeared={}, P90={}, P10={}",
                examId, appearedCount, top10Percentile, bottom10Percentile);

        return saved;
    }

    /**
     * Computes the score threshold for the given percentile using linear interpolation.
     *
     * @param sortedScores sorted list of candidate scores
     * @param percentile   percentile value between 0 and 100 (e.g. 90.0 for Top 10%, 10.0 for Bottom 10%)
     * @return score threshold rounded to 4 decimal places
     */
    public BigDecimal computePercentileThreshold(List<Double> sortedScores, double percentile) {
        if (sortedScores == null || sortedScores.isEmpty()) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }

        int n = sortedScores.size();
        if (n == 1) {
            return BigDecimal.valueOf(sortedScores.get(0)).setScale(4, RoundingMode.HALF_UP);
        }

        double rank = (percentile / 100.0) * (n - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        double weight = rank - lowerIndex;

        double score = sortedScores.get(lowerIndex) + weight * (sortedScores.get(upperIndex) - sortedScores.get(lowerIndex));
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Buckets scores into score intervals ("0-10", "10-20", ..., "90-100").
     */
    public Map<String, Long> computeScoreDistribution(List<Double> scores) {
        Map<String, Long> distribution = new LinkedHashMap<>();
        distribution.put("0-10", 0L);
        distribution.put("10-20", 0L);
        distribution.put("20-30", 0L);
        distribution.put("30-40", 0L);
        distribution.put("40-50", 0L);
        distribution.put("50-60", 0L);
        distribution.put("60-70", 0L);
        distribution.put("70-80", 0L);
        distribution.put("80-90", 0L);
        distribution.put("90-100", 0L);

        if (scores == null || scores.isEmpty()) {
            return distribution;
        }

        for (Double score : scores) {
            if (score == null) continue;
            int bucket = Math.min(9, Math.max(0, (int) (score / 10.0)));
            String key = (bucket * 10) + "-" + ((bucket + 1) * 10);
            distribution.compute(key, (k, v) -> (v != null ? v : 0L) + 1L);
        }

        return distribution;
    }

    /**
     * Computes mean section scores across all candidate evaluation records.
     */
    public Map<String, Double> computeSectionAverages(List<CandidateAnalyticsResult> candidateResults) {
        Map<String, Double> sumMap = new HashMap<>();
        Map<String, Integer> countMap = new HashMap<>();

        for (CandidateAnalyticsResult result : candidateResults) {
            String json = result.getSectionScoresJson();
            if (json == null || json.isBlank()) continue;

            try {
                Map<String, Object> sectionMap = objectMapper.readValue(json, new TypeReference<>() {});
                for (Map.Entry<String, Object> entry : sectionMap.entrySet()) {
                    String section = entry.getKey();
                    double val = DataConversionUtils.toDouble(entry.getValue());
                    sumMap.put(section, sumMap.getOrDefault(section, 0.0) + val);
                    countMap.put(section, countMap.getOrDefault(section, 0) + 1);
                }
            } catch (Exception e) {
                log.warn("Could not parse section scores for candidate record: {}", result.getId());
            }
        }

        Map<String, Double> averages = new LinkedHashMap<>();
        sumMap.keySet().stream().sorted().forEach(section -> {
            double sum = sumMap.get(section);
            int count = countMap.get(section);
            double avg = count > 0 ? sum / count : 0.0;
            BigDecimal rounded = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
            averages.put(section, rounded.doubleValue());
        });

        return averages;
    }

    private byte[] exportAsCsv(ExamAnalytics analytics) {
        String csv = "exam_id,total_registered,total_appeared,top_10_percentile,bottom_10_percentile,computed_at\n" +
            String.format("%s,%d,%d,%s,%s,%s\n",
                analytics.getExamId(),
                analytics.getTotalRegistered(),
                analytics.getTotalAppeared(),
                analytics.getTop10PercentileThreshold(),
                analytics.getBottom10PercentileThreshold(),
                analytics.getComputedAt());
        return csv.getBytes();
    }

    private String serializeJson(Object obj) {
        if (obj == null) return "{}";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize object to JSON: {}", obj, e);
            return "{}";
        }
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
