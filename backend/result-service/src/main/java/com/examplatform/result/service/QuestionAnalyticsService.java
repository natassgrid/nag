package com.examplatform.result.service;

import com.examplatform.result.dto.QuestionAnalyticsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for computing per-question analytics for an exam.
 * Calculates difficulty index, discrimination index, and response distribution.
 *
 * Validates: Requirements 26.1, 26.5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionAnalyticsService {

    /**
     * Computes analytics for all questions in an exam.
     *
     * For each question:
     * - Difficulty index = correct responses / total responses
     * - Discrimination index = (top 27% correct rate) - (bottom 27% correct rate)
     * - Response distribution = count per option selected
     *
     * Currently uses mock data as actual computation requires cross-service data
     * from the delivery and question-bank services.
     *
     * @param examId   the exam identifier
     * @param tenantId the tenant identifier
     * @return list of per-question analytics results
     */
    public List<QuestionAnalyticsResult> computeAnalytics(UUID examId, String tenantId) {
        log.info("Computing question analytics for exam={}, tenant={}", examId, tenantId);

        // Stub: uses hard-coded mock data for now
        // In production, this would:
        // 1. Fetch all responses for the exam from delivery-service
        // 2. Fetch correct answers from question-bank-service
        // 3. Compute indices per question
        List<QuestionAnalyticsResult> results = computeFromMockData(examId);

        log.info("Computed analytics for {} questions in exam={}", results.size(), examId);
        return results;
    }

    /**
     * Computes analytics from mock data representing a typical exam scenario.
     * This simulates 5 questions with varying difficulty and discrimination.
     */
    List<QuestionAnalyticsResult> computeFromMockData(UUID examId) {
        List<QuestionAnalyticsResult> results = new ArrayList<>();

        // Mock data representing candidate responses for 5 questions
        // Each entry: [totalResponses, correctResponses, top27CorrectRate, bottom27CorrectRate]
        double[][] mockData = {
                {100, 75, 0.92, 0.55},  // Easy question, moderate discrimination
                {100, 40, 0.70, 0.15},  // Medium question, good discrimination
                {100, 20, 0.45, 0.05},  // Hard question, good discrimination
                {100, 90, 0.95, 0.85},  // Very easy, poor discrimination
                {100, 55, 0.80, 0.30},  // Medium, excellent discrimination
        };

        Map<String, Integer>[] distributions = new Map[]{
                Map.of("A", 75, "B", 10, "C", 8, "D", 7),
                Map.of("A", 20, "B", 40, "C", 25, "D", 15),
                Map.of("A", 30, "B", 25, "C", 20, "D", 25),
                Map.of("A", 5, "B", 2, "C", 90, "D", 3),
                Map.of("A", 15, "B", 55, "C", 20, "D", 10),
        };

        for (int i = 0; i < mockData.length; i++) {
            double totalResponses = mockData[i][0];
            double correctResponses = mockData[i][1];
            double top27CorrectRate = mockData[i][2];
            double bottom27CorrectRate = mockData[i][3];

            double difficultyIndex = correctResponses / totalResponses;
            double discriminationIndex = top27CorrectRate - bottom27CorrectRate;

            @SuppressWarnings("unchecked")
            Map<String, Integer> distribution = (Map<String, Integer>) distributions[i];

            results.add(QuestionAnalyticsResult.builder()
                    .questionId(UUID.nameUUIDFromBytes(("q" + (i + 1) + "-" + examId).getBytes()))
                    .difficultyIndex(difficultyIndex)
                    .discriminationIndex(discriminationIndex)
                    .responseDistribution(distribution)
                    .build());
        }

        return results;
    }
}
