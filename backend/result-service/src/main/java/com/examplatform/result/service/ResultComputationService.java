package com.examplatform.result.service;

import com.examplatform.result.domain.Result;
import com.examplatform.result.dto.CandidateScoreInput;
import com.examplatform.result.repository.ResultRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

        Result result = Result.builder()
                .candidateId(sc.input().getCandidateId())
                .examId(examId)
                .totalScore(BigDecimal.valueOf(sc.finalScore()).setScale(2, RoundingMode.HALF_UP))
                .sectionScoresJson(sectionScoresJson)
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
