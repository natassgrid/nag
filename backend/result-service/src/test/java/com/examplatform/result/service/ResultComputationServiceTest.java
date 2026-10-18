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
import com.examplatform.shared.dto.SubtopicPerformanceDto;
import com.examplatform.shared.messaging.EventPublisher;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ResultComputationService}.
 * Validates: Requirements 13.1, 13.2
 */
@ExtendWith(MockitoExtension.class)
class ResultComputationServiceTest {

    @Mock
    private ResultRepository resultRepository;

    @Mock
    private EventPublisher eventPublisher;

    private ResultComputationService service;

    private static final UUID EXAM_ID = UUID.randomUUID();
    private static final String TENANT_ID = "tenant-1";

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        service = new ResultComputationService(resultRepository, objectMapper, eventPublisher);
    }

    @Nested
    @DisplayName("Score, Rank, and Percentile Computation")
    class ScoreRankPercentile {

        @Test
        @DisplayName("Computes correct totalScore, rank, and percentile for 3 candidates")
        void computeResults_threeCandidate_correctRankAndPercentile() {
            // Given: 3 candidates with scores 80, 60, 90
            UUID c1 = UUID.randomUUID();
            UUID c2 = UUID.randomUUID();
            UUID c3 = UUID.randomUUID();

            List<CandidateScoreInput> inputs = List.of(
                    CandidateScoreInput.builder()
                            .candidateId(c1).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(80.0)
                            .sectionScores(Map.of("Math", 40.0, "Science", 40.0))
                            .build(),
                    CandidateScoreInput.builder()
                            .candidateId(c2).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(60.0)
                            .sectionScores(Map.of("Math", 30.0, "Science", 30.0))
                            .build(),
                    CandidateScoreInput.builder()
                            .candidateId(c3).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(90.0)
                            .sectionScores(Map.of("Math", 50.0, "Science", 40.0))
                            .build()
            );

            when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<Result> results = service.computeResults(EXAM_ID, inputs, false, TENANT_ID);

            // Then: sorted by score desc → c3(90), c1(80), c2(60)
            assertThat(results).hasSize(3);

            Result rank1 = results.get(0);
            assertThat(rank1.getCandidateId()).isEqualTo(c3);
            assertThat(rank1.getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(90.00));
            assertThat(rank1.getOverallRank()).isEqualTo(1);
            // 2 candidates below score 90 → percentile = (2/3)*100 = 66.667
            assertThat(rank1.getOverallPercentile().doubleValue()).isCloseTo(66.667, within(0.001));

            Result rank2 = results.get(1);
            assertThat(rank2.getCandidateId()).isEqualTo(c1);
            assertThat(rank2.getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
            assertThat(rank2.getOverallRank()).isEqualTo(2);
            // 1 candidate below score 80 → percentile = (1/3)*100 = 33.333
            assertThat(rank2.getOverallPercentile().doubleValue()).isCloseTo(33.333, within(0.001));

            Result rank3 = results.get(2);
            assertThat(rank3.getCandidateId()).isEqualTo(c2);
            assertThat(rank3.getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(60.00));
            assertThat(rank3.getOverallRank()).isEqualTo(3);
            // 0 candidates below score 60 → percentile = 0
            assertThat(rank3.getOverallPercentile().doubleValue()).isCloseTo(0.0, within(0.001));
        }
    }

    @Nested
    @DisplayName("Tie Handling")
    class TieHandling {

        @Test
        @DisplayName("Handles ties in ranking — same score gets same rank")
        void computeResults_ties_sameRank() {
            // Given: 4 candidates, two with the same score of 75
            UUID c1 = UUID.randomUUID();
            UUID c2 = UUID.randomUUID();
            UUID c3 = UUID.randomUUID();
            UUID c4 = UUID.randomUUID();

            List<CandidateScoreInput> inputs = List.of(
                    CandidateScoreInput.builder()
                            .candidateId(c1).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(90.0)
                            .sectionScores(Map.of("Physics", 90.0))
                            .build(),
                    CandidateScoreInput.builder()
                            .candidateId(c2).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(75.0)
                            .sectionScores(Map.of("Physics", 75.0))
                            .build(),
                    CandidateScoreInput.builder()
                            .candidateId(c3).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(75.0)
                            .sectionScores(Map.of("Physics", 75.0))
                            .build(),
                    CandidateScoreInput.builder()
                            .candidateId(c4).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(50.0)
                            .sectionScores(Map.of("Physics", 50.0))
                            .build()
            );

            when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<Result> results = service.computeResults(EXAM_ID, inputs, false, TENANT_ID);

            // Then: rank 1=90, rank 2=75 (tied), rank 2=75 (tied), rank 4=50
            assertThat(results).hasSize(4);
            assertThat(results.get(0).getOverallRank()).isEqualTo(1);
            assertThat(results.get(1).getOverallRank()).isEqualTo(2);
            assertThat(results.get(2).getOverallRank()).isEqualTo(2);
            // After tie at rank 2, next rank is 4 (position-based)
            assertThat(results.get(3).getOverallRank()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Shift Normalization")
    class ShiftNormalization {

        @Test
        @DisplayName("Applies shift normalization when enabled")
        void computeResults_withNormalization_appliesFormula() {
            // Given: candidate with rawScore=70, shiftMean=60, shiftStdDev=10
            // Formula: (70-60)/10 * 10 + 50 = 60
            UUID c1 = UUID.randomUUID();

            List<CandidateScoreInput> inputs = List.of(
                    CandidateScoreInput.builder()
                            .candidateId(c1).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(70.0)
                            .shiftMean(60.0)
                            .shiftStdDev(10.0)
                            .sectionScores(Map.of("Math", 70.0))
                            .build()
            );

            when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<Result> results = service.computeResults(EXAM_ID, inputs, true, TENANT_ID);

            // Then: normalized score = (70-60)/10 * 10 + 50 = 60.00
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(60.00));
        }

        @Test
        @DisplayName("Without normalization — uses raw scores directly")
        void computeResults_withoutNormalization_usesRawScores() {
            // Given
            UUID c1 = UUID.randomUUID();

            List<CandidateScoreInput> inputs = List.of(
                    CandidateScoreInput.builder()
                            .candidateId(c1).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(85.5)
                            .shiftMean(60.0)
                            .shiftStdDev(10.0)
                            .sectionScores(Map.of("Math", 45.5, "English", 40.0))
                            .build()
            );

            when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<Result> results = service.computeResults(EXAM_ID, inputs, false, TENANT_ID);

            // Then: raw score preserved
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getTotalScore()).isEqualByComparingTo(BigDecimal.valueOf(85.50));
        }
    }

    @Nested
    @DisplayName("Section Scores Serialization")
    class SectionScoresSerialization {

        @Test
        @DisplayName("sectionScores JSON is correctly serialized")
        void computeResults_sectionScoresJsonSerialized() {
            // Given
            UUID c1 = UUID.randomUUID();
            Map<String, Double> sections = Map.of("Math", 45.0, "Science", 35.0);

            List<CandidateScoreInput> inputs = List.of(
                    CandidateScoreInput.builder()
                            .candidateId(c1).examId(EXAM_ID).shiftId("shift-1")
                            .totalRawScore(80.0)
                            .sectionScores(sections)
                            .build()
            );

            when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            // When
            List<Result> results = service.computeResults(EXAM_ID, inputs, false, TENANT_ID);

            // Then: section scores are serialized as valid JSON containing both sections
            assertThat(results).hasSize(1);
            String json = results.get(0).getSectionScoresJson();
            assertThat(json).isNotNull();
            assertThat(json).contains("Math");
            assertThat(json).contains("45.0");
            assertThat(json).contains("Science");
            assertThat(json).contains("35.0");
        }
    }

    @Nested
    @DisplayName("Get Result")
    class GetResult {

        @Test
        @DisplayName("getResult returns result when found")
        void getResult_found_returnsResult() {
            // Given
            UUID candidateId = UUID.randomUUID();
            Result expected = Result.builder()
                    .candidateId(candidateId)
                    .examId(EXAM_ID)
                    .totalScore(BigDecimal.valueOf(85.0))
                    .overallRank(1)
                    .overallPercentile(BigDecimal.valueOf(100.0))
                    .digiLockerPushed(false)
                    .build();

            when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, EXAM_ID, TENANT_ID))
                    .thenReturn(Optional.of(expected));

            // When
            Result result = service.getResult(candidateId, EXAM_ID, TENANT_ID);

            // Then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("getResult throws EntityNotFoundException when not found")
        void getResult_notFound_throwsException() {
            // Given
            UUID candidateId = UUID.randomUUID();

            when(resultRepository.findByCandidateIdAndExamIdAndTenantId(candidateId, EXAM_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> service.getResult(candidateId, EXAM_ID, TENANT_ID))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Result not found");
        }
    }

    @Nested
    @DisplayName("Section and Taxonomy Breakdowns")
    class SectionAndTaxonomyBreakdowns {

        @Test
        @DisplayName("Serializes explicit section and taxonomy breakdowns into sectionalStatusJson and topicBreakdownJson")
        void computeResults_withExplicitBreakdowns_serializesCorrectly() throws Exception {
            UUID c1 = UUID.randomUUID();
            UUID secId = UUID.randomUUID();
            ObjectMapper mapper = new ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            SectionBreakdownDto secDto = SectionBreakdownDto.builder()
                    .sectionId(secId)
                    .sectionName("Physics Section A")
                    .subjectId(1001L)
                    .subject("Physics")
                    .score(40.0)
                    .maxMarks(50.0)
                    .accuracyRate(80.0)
                    .questionCount(10)
                    .attemptedCount(10)
                    .correctCount(8)
                    .build();

            TaxonomyBreakdownDto taxDto = TaxonomyBreakdownDto.builder()
                    .subjectId(1001L)
                    .subject("Physics")
                    .topicId(2001L)
                    .topic("Thermodynamics")
                    .subtopicId(3001L)
                    .subtopic("Carnot Cycle")
                    .score(16.0)
                    .maxMarks(20.0)
                    .accuracyRate(80.0)
                    .questionCount(5)
                    .attemptedCount(5)
                    .correctCount(4)
                    .totalTimeSpentMs(150000L)
                    .build();

            CandidateScoreInput input = CandidateScoreInput.builder()
                    .candidateId(c1)
                    .examId(EXAM_ID)
                    .totalRawScore(40.0)
                    .sectionScores(Map.of("Physics Section A", 40.0))
                    .sectionBreakdowns(List.of(secDto))
                    .subtopicBreakdowns(List.of(taxDto))
                    .accuracyRate(80.0)
                    .build();

            when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<Result> results = service.computeResults(EXAM_ID, List.of(input), false, TENANT_ID);

            assertThat(results).hasSize(1);
            Result res = results.get(0);

            assertThat(res.getAccuracyRate()).isEqualByComparingTo(BigDecimal.valueOf(80.00));
            assertThat(res.getSectionalStatusJson()).isNotNull();
            assertThat(res.getTopicBreakdownJson()).isNotNull();

            List<SectionBreakdownDto> parsedSections = mapper.readValue(
                    res.getSectionalStatusJson(), new TypeReference<>() {});
            assertThat(parsedSections).hasSize(1);
            assertThat(parsedSections.get(0).getSectionId()).isEqualTo(secId);
            assertThat(parsedSections.get(0).getSubjectId()).isEqualTo(1001L);
            assertThat(parsedSections.get(0).getAccuracyRate()).isEqualTo(80.0);

            List<TaxonomyBreakdownDto> parsedTax = mapper.readValue(
                    res.getTopicBreakdownJson(), new TypeReference<>() {});
            assertThat(parsedTax).hasSize(1);
            assertThat(parsedTax.get(0).getSubtopicId()).isEqualTo(3001L);
            assertThat(parsedTax.get(0).getSubjectId()).isEqualTo(1001L);

            // Interoperability with SubtopicPerformanceDto for learner mastery updates
            List<SubtopicPerformanceDto> learnerPerfs = mapper.readValue(
                    res.getTopicBreakdownJson(), new TypeReference<>() {});
            assertThat(learnerPerfs).hasSize(1);
            assertThat(learnerPerfs.get(0).subtopicId()).isEqualTo(3001L);
            assertThat(learnerPerfs.get(0).subtopicName()).isEqualTo("Carnot Cycle");
            assertThat(learnerPerfs.get(0).accuracyPercent()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("Derives section and taxonomy breakdowns from questionLevelScores when breakdowns are absent")
        void computeResults_derivesBreakdownsFromQuestionLevelScores() throws Exception {
            UUID c1 = UUID.randomUUID();
            UUID secId = UUID.randomUUID();
            ObjectMapper mapper = new ObjectMapper()
                    .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            List<Map<String, Object>> questions = List.of(
                    Map.ofEntries(
                            Map.entry("sectionId", secId.toString()),
                            Map.entry("sectionName", "Math Section"),
                            Map.entry("subjectId", 1002L),
                            Map.entry("subject", "Mathematics"),
                            Map.entry("topicId", 2002L),
                            Map.entry("topic", "Calculus"),
                            Map.entry("subtopicId", 3002L),
                            Map.entry("subtopic", "Definite Integrals"),
                            Map.entry("score", 4.0),
                            Map.entry("maxMarks", 4.0),
                            Map.entry("status", "CORRECT"),
                            Map.entry("timeSpentMs", 60000L)
                    ),
                    Map.ofEntries(
                            Map.entry("sectionId", secId.toString()),
                            Map.entry("sectionName", "Math Section"),
                            Map.entry("subjectId", 1002L),
                            Map.entry("subject", "Mathematics"),
                            Map.entry("topicId", 2002L),
                            Map.entry("topic", "Calculus"),
                            Map.entry("subtopicId", 3002L),
                            Map.entry("subtopic", "Definite Integrals"),
                            Map.entry("score", -1.0),
                            Map.entry("maxMarks", 4.0),
                            Map.entry("status", "INCORRECT"),
                            Map.entry("timeSpentMs", 75000L)
                    )
            );

            CandidateScoreInput input = CandidateScoreInput.builder()
                    .candidateId(c1)
                    .examId(EXAM_ID)
                    .totalRawScore(3.0)
                    .sectionScores(Map.of("Math Section", 3.0))
                    .questionLevelScores(questions)
                    .build();

            when(resultRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<Result> results = service.computeResults(EXAM_ID, List.of(input), false, TENANT_ID);

            assertThat(results).hasSize(1);
            Result res = results.get(0);

            // 1 correct out of 2 attempted = 50.0%
            assertThat(res.getAccuracyRate()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
            assertThat(res.getSectionalStatusJson()).contains("Math Section");
            assertThat(res.getTopicBreakdownJson()).contains("Definite Integrals");

            List<SubtopicPerformanceDto> perfDtos = mapper.readValue(
                    res.getTopicBreakdownJson(), new TypeReference<>() {});
            assertThat(perfDtos).hasSize(1);
            assertThat(perfDtos.get(0).subtopicId()).isEqualTo(3002L);
            assertThat(perfDtos.get(0).correctCount()).isEqualTo(1);
            assertThat(perfDtos.get(0).attemptedCount()).isEqualTo(2);
            assertThat(perfDtos.get(0).accuracyPercent()).isEqualTo(50.0);
        }
    }
}
