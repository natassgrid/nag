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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Unit Tests")
class AnalyticsServiceTest {

    @Mock
    private ExamAnalyticsRepository examAnalyticsRepository;

    @Mock
    private CandidateAnalyticsResultRepository candidateResultRepository;

    @Mock
    private AnalyticsPdfService analyticsPdfService;

    private ObjectMapper objectMapper;
    private AnalyticsService analyticsService;

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final UUID CANDIDATE_ID = UUID.randomUUID();
    private static final UUID SESSION_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        analyticsService = new AnalyticsService(
                examAnalyticsRepository,
                candidateResultRepository,
                analyticsPdfService,
                objectMapper
        );
    }

    @Nested
    @DisplayName("Percentile Calculations")
    class PercentileCalculations {

        @Test
        @DisplayName("Returns 0.0000 for empty scores")
        void emptyScoresReturnZero() {
            BigDecimal result = analyticsService.computePercentileThreshold(List.of(), 90.0);
            assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Returns exact single score for 1 element")
        void singleScoreReturnsExact() {
            BigDecimal result = analyticsService.computePercentileThreshold(List.of(75.5), 90.0);
            assertThat(result).isEqualByComparingTo(BigDecimal.valueOf(75.5));
        }

        @Test
        @DisplayName("Computes interpolated 10th and 90th percentile correctly")
        void multipleScoresInterpolated() {
            // Scores: 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 (10 scores)
            List<Double> scores = Arrays.asList(10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0);

            BigDecimal p10 = analyticsService.computePercentileThreshold(scores, 10.0);
            BigDecimal p90 = analyticsService.computePercentileThreshold(scores, 90.0);

            // 10th percentile index = 0.1 * 9 = 0.9 -> 10 + 0.9*(20-10) = 19.0
            assertThat(p10).isEqualByComparingTo(BigDecimal.valueOf(19.0));
            // 90th percentile index = 0.9 * 9 = 8.1 -> 90 + 0.1*(100-90) = 91.0
            assertThat(p90).isEqualByComparingTo(BigDecimal.valueOf(91.0));
        }
    }

    @Nested
    @DisplayName("Score Distribution Histogram Bucketing")
    class ScoreDistributionBucketing {

        @Test
        @DisplayName("Buckets scores across 10 standard ranges")
        void bucketsScoresCorrectly() {
            List<Double> scores = List.of(5.0, 15.0, 18.0, 25.0, 55.0, 92.0, 99.0, 100.0);
            Map<String, Long> dist = analyticsService.computeScoreDistribution(scores);

            assertThat(dist.get("0-10")).isEqualTo(1L);
            assertThat(dist.get("10-20")).isEqualTo(2L);
            assertThat(dist.get("20-30")).isEqualTo(1L);
            assertThat(dist.get("30-40")).isEqualTo(0L);
            assertThat(dist.get("40-50")).isEqualTo(0L);
            assertThat(dist.get("50-60")).isEqualTo(1L);
            assertThat(dist.get("60-70")).isEqualTo(0L);
            assertThat(dist.get("70-80")).isEqualTo(0L);
            assertThat(dist.get("80-90")).isEqualTo(0L);
            assertThat(dist.get("90-100")).isEqualTo(3L);
        }

        @Test
        @DisplayName("Empty scores produce all zeros in standard buckets")
        void emptyScoresProduceZeros() {
            Map<String, Long> dist = analyticsService.computeScoreDistribution(List.of());
            assertThat(dist).hasSize(10);
            assertThat(dist.values()).allMatch(v -> v == 0L);
        }
    }

    @Nested
    @DisplayName("Section Averages Computation")
    class SectionAveragesComputation {

        @Test
        @DisplayName("Computes mean score per section across multiple candidates")
        void computesSectionMeans() {
            CandidateAnalyticsResult c1 = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .sectionScoresJson("{\"Physics\": 30.0, \"Chemistry\": 40.0}")
                    .build();
            CandidateAnalyticsResult c2 = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .sectionScoresJson("{\"Physics\": 50.0, \"Chemistry\": 20.0}")
                    .build();

            Map<String, Double> averages = analyticsService.computeSectionAverages(List.of(c1, c2));

            assertThat(averages.get("Physics")).isEqualTo(40.0);
            assertThat(averages.get("Chemistry")).isEqualTo(30.0);
        }

        @Test
        @DisplayName("Handles missing or empty section JSON gracefully")
        void handlesEmptySectionJson() {
            CandidateAnalyticsResult c1 = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .sectionScoresJson(null)
                    .build();
            CandidateAnalyticsResult c2 = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .sectionScoresJson("")
                    .build();

            Map<String, Double> averages = analyticsService.computeSectionAverages(List.of(c1, c2));
            assertThat(averages).isEmpty();
        }
    }

    @Nested
    @DisplayName("Process Evaluation Completed Event Ingestion")
    class ProcessEvaluationCompleted {

        @Test
        @DisplayName("Saves candidate record and creates new ExamAnalytics")
        void ingestsEventAndAggregates() {
            when(candidateResultRepository.findByCandidateIdAndExamId(CANDIDATE_ID, EXAM_ID))
                    .thenReturn(Optional.empty());

            CandidateAnalyticsResult persistedResult = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .candidateId(CANDIDATE_ID)
                    .sessionId(SESSION_ID)
                    .totalRawScore(BigDecimal.valueOf(85.0))
                    .sectionScoresJson("{\"Math\": 85.0}")
                    .tenantId("tenant-1")
                    .evaluatedAt(Instant.now())
                    .createdAt(Instant.now())
                    .build();

            when(candidateResultRepository.findByExamId(EXAM_ID))
                    .thenReturn(List.of(persistedResult));
            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID))
                    .thenReturn(Optional.empty());
            when(examAnalyticsRepository.save(any(ExamAnalytics.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ExamAnalytics result = analyticsService.processEvaluationCompleted(
                    EXAM_ID,
                    CANDIDATE_ID,
                    SESSION_ID,
                    85.0,
                    Map.of("Math", 85.0),
                    List.of(),
                    "tenant-1",
                    Instant.now()
            );

            assertThat(result).isNotNull();
            assertThat(result.getExamId()).isEqualTo(EXAM_ID);
            assertThat(result.getTotalAppeared()).isEqualTo(1L);
            assertThat(result.getTop10PercentileThreshold()).isEqualByComparingTo(BigDecimal.valueOf(85.0));

            verify(candidateResultRepository).save(any(CandidateAnalyticsResult.class));
            verify(examAnalyticsRepository).save(any(ExamAnalytics.class));
        }

        @Test
        @DisplayName("Idempotent update when candidate evaluation record already exists")
        void idempotentWhenRecordExists() {
            CandidateAnalyticsResult existing = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .candidateId(CANDIDATE_ID)
                    .totalRawScore(BigDecimal.valueOf(70.0))
                    .build();

            when(candidateResultRepository.findByCandidateIdAndExamId(CANDIDATE_ID, EXAM_ID))
                    .thenReturn(Optional.of(existing));
            when(candidateResultRepository.findByExamId(EXAM_ID))
                    .thenReturn(List.of(existing));
            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID))
                    .thenReturn(Optional.empty());
            when(examAnalyticsRepository.save(any(ExamAnalytics.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            analyticsService.processEvaluationCompleted(
                    EXAM_ID,
                    CANDIDATE_ID,
                    SESSION_ID,
                    90.0,
                    Map.of("Math", 90.0),
                    List.of(),
                    "tenant-1",
                    Instant.now()
            );

            ArgumentCaptor<CandidateAnalyticsResult> captor = ArgumentCaptor.forClass(CandidateAnalyticsResult.class);
            verify(candidateResultRepository).save(captor.capture());
            assertThat(captor.getValue().getTotalRawScore()).isEqualByComparingTo(BigDecimal.valueOf(90.00));
        }
    }

    @Nested
    @DisplayName("Batch Computation Trigger")
    class BatchComputation {

        @Test
        @DisplayName("Computes analytics when candidate results exist")
        void recomputesSuccessfully() {
            CandidateAnalyticsResult r1 = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .candidateId(UUID.randomUUID())
                    .totalRawScore(BigDecimal.valueOf(40.0))
                    .sectionScoresJson("{\"GK\": 40.0}")
                    .build();
            CandidateAnalyticsResult r2 = CandidateAnalyticsResult.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .candidateId(UUID.randomUUID())
                    .totalRawScore(BigDecimal.valueOf(80.0))
                    .sectionScoresJson("{\"GK\": 80.0}")
                    .build();

            when(candidateResultRepository.findByExamId(EXAM_ID)).thenReturn(List.of(r1, r2));
            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID)).thenReturn(Optional.empty());
            when(examAnalyticsRepository.save(any(ExamAnalytics.class))).thenAnswer(inv -> inv.getArgument(0));

            ExamAnalytics computed = analyticsService.computeAnalyticsForExam(EXAM_ID);

            assertThat(computed.getTotalAppeared()).isEqualTo(2L);
            assertThat(computed.getTop10PercentileThreshold()).isNotNull();
            assertThat(computed.getBottom10PercentileThreshold()).isNotNull();
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when no candidate records exist")
        void throwsWhenNoCandidateRecords() {
            when(candidateResultRepository.findByExamId(EXAM_ID)).thenReturn(List.of());

            assertThatThrownBy(() -> analyticsService.computeAnalyticsForExam(EXAM_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No candidate evaluation records found");
        }
    }

    @Nested
    @DisplayName("Get Analytics and Mock Fallback Removal")
    class GetAnalytics {

        @Test
        @DisplayName("Returns persisted ExamAnalytics when present")
        void returnsExistingAnalytics() {
            ExamAnalytics expected = ExamAnalytics.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .totalAppeared(100L)
                    .build();

            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID))
                    .thenReturn(Optional.of(expected));

            ExamAnalytics actual = analyticsService.getAnalyticsForExam(EXAM_ID);
            assertThat(actual).isEqualTo(expected);
        }

        @Test
        @DisplayName("Throws IllegalArgumentException when analytics not found (no mock fallback)")
        void throwsNotFoundWhenAbsent() {
            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> analyticsService.getAnalyticsForExam(EXAM_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Analytics not found for exam");
        }
    }

    @Nested
    @DisplayName("Analytics Export")
    class ExportAnalytics {

        @Test
        @DisplayName("Exports CSV format")
        void exportsCsv() {
            ExamAnalytics analytics = ExamAnalytics.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .totalRegistered(100L)
                    .totalAppeared(95L)
                    .top10PercentileThreshold(BigDecimal.valueOf(90.0))
                    .bottom10PercentileThreshold(BigDecimal.valueOf(20.0))
                    .computedAt(Instant.now())
                    .build();

            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID))
                    .thenReturn(Optional.of(analytics));

            byte[] csv = analyticsService.exportAnalytics(EXAM_ID, "csv");
            String content = new String(csv);

            assertThat(content).contains("exam_id,total_registered,total_appeared");
            assertThat(content).contains(EXAM_ID.toString());
        }

        @Test
        @DisplayName("Exports PDF format via AnalyticsPdfService")
        void exportsPdf() {
            ExamAnalytics analytics = ExamAnalytics.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .build();

            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID))
                    .thenReturn(Optional.of(analytics));
            when(analyticsPdfService.generatePdfReport(analytics)).thenReturn(new byte[]{1, 2, 3});

            byte[] pdf = analyticsService.exportAnalytics(EXAM_ID, "pdf");
            assertThat(pdf).isEqualTo(new byte[]{1, 2, 3});
            verify(analyticsPdfService).generatePdfReport(analytics);
        }

        @Test
        @DisplayName("Throws on unsupported format")
        void throwsOnUnsupportedFormat() {
            ExamAnalytics analytics = ExamAnalytics.builder()
                    .id(UUID.randomUUID())
                    .examId(EXAM_ID)
                    .build();

            when(examAnalyticsRepository.findTopByExamIdOrderByComputedAtDesc(EXAM_ID))
                    .thenReturn(Optional.of(analytics));

            assertThatThrownBy(() -> analyticsService.exportAnalytics(EXAM_ID, "xlsx"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unsupported export format");
        }
    }
}
