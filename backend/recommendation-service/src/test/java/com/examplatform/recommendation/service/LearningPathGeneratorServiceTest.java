// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.DynamicLearningPathResponse;
import com.examplatform.recommendation.dto.LearningPathSummaryResponse;
import com.examplatform.recommendation.dto.MilestonePhaseDto;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LearningPathGeneratorService Hybrid Engine Tests")
class LearningPathGeneratorServiceTest {

    @Mock
    private LearnerSubtopicPerformanceRepository subtopicRepository;

    @Mock
    private DynamicLearningPathRepository dlpRepository;

    private SubtopicClassifierService classifierService;
    private ObjectMapper objectMapper;
    private LearningPathGeneratorService generatorService;

    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SESSION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        classifierService = new SubtopicClassifierService();
        objectMapper = new ObjectMapper();
        generatorService = new LearningPathGeneratorService(classifierService, subtopicRepository, dlpRepository, objectMapper);
    }

    @Test
    @DisplayName("Generates 3-phase milestone graph and categorizes subtopics into brackets correctly")
    void testGenerateOrRecalculate() {
        LearnerSubtopicPerformance p1 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .subjectName("Physics")
                .topicName("Mechanics")
                .subtopicName("Newton Laws")
                .avgAccuracyPercent(40.0) // Conceptual Gap
                .avgPaceRatio(1.0)
                .avgSkipRatePercent(10.0)
                .build();

        LearnerSubtopicPerformance p2 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .subjectName("Physics")
                .topicName("Mechanics")
                .subtopicName("Work Energy")
                .avgAccuracyPercent(65.0) // Application Gap
                .avgPaceRatio(1.1)
                .avgSkipRatePercent(5.0)
                .build();

        LearnerSubtopicPerformance p3 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .subjectName("Mathematics")
                .topicName("Calculus")
                .subtopicName("Limits")
                .avgAccuracyPercent(90.0) // Proficient (Slow)
                .avgPaceRatio(1.35)
                .avgSkipRatePercent(0.0)
                .build();

        LearnerSubtopicPerformance p4 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .subjectName("Chemistry")
                .topicName("Physical")
                .subtopicName("Thermodynamics")
                .avgAccuracyPercent(80.0) // Conceptual Gap via skip rate override
                .avgPaceRatio(0.9)
                .avgSkipRatePercent(45.0)
                .build();

        when(subtopicRepository.findByCandidateId(CANDIDATE_ID)).thenReturn(List.of(p1, p2, p3, p4));

        // Existing active path to be superseded
        DynamicLearningPath oldActive = DynamicLearningPath.builder()
                .candidateId(CANDIDATE_ID)
                .examTarget("JEE_MAIN")
                .status("ACTIVE")
                .build();
        when(dlpRepository.findByCandidateIdAndStatus(CANDIDATE_ID, "ACTIVE")).thenReturn(List.of(oldActive));
        when(dlpRepository.save(any(DynamicLearningPath.class))).thenAnswer(inv -> inv.getArgument(0));

        DynamicLearningPath result = generatorService.generateOrRecalculate(CANDIDATE_ID, SESSION_ID, "JEE_MAIN");

        assertThat(result).isNotNull();
        assertThat(result.getCandidateId()).isEqualTo(CANDIDATE_ID);
        assertThat(result.getSourceSessionId()).isEqualTo(SESSION_ID);
        assertThat(result.getExamTarget()).isEqualTo("JEE_MAIN");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");

        // p1 and p4 are conceptual gaps (p4 due to 45% skips)
        assertThat(result.getConceptualGapsCount()).isEqualTo(2);
        // p2 is application gap
        assertThat(result.getApplicationGapsCount()).isEqualTo(1);
        // p3 is proficient
        assertThat(result.getProficientCount()).isEqualTo(1);

        // Verify old path was superseded
        assertThat(oldActive.getStatus()).isEqualTo("SUPERSEDED");

        // Verify milestone graph serialization
        DynamicLearningPathResponse resp = generatorService.toResponse(result);
        assertThat(resp.milestoneGraph()).isNotNull();
        assertThat(resp.milestoneGraph().phases()).hasSize(3);

        MilestonePhaseDto phase1 = resp.milestoneGraph().phases().get(0);
        assertThat(phase1.phaseName()).contains("Phase 1: Remediation");
        assertThat(phase1.status()).isEqualTo("IN_PROGRESS");
        assertThat(phase1.items()).hasSize(2);

        MilestonePhaseDto phase2 = resp.milestoneGraph().phases().get(1);
        assertThat(phase2.phaseName()).contains("Phase 2: Adaptive Practice");
        assertThat(phase2.status()).isEqualTo("LOCKED"); // Locked because Phase 1 has gaps
        assertThat(phase2.items()).hasSize(1);

        MilestonePhaseDto phase3 = resp.milestoneGraph().phases().get(2);
        assertThat(phase3.phaseName()).contains("Phase 3: Speed Mastery & Gatekeeper");
        assertThat(phase3.status()).isEqualTo("LOCKED");
        assertThat(phase3.items()).hasSize(1);
        assertThat(phase3.items().get(0).actionType()).isEqualTo("TIMED_SPRINT");
    }

    @Test
    @DisplayName("Summary aggregation computes subject-wise and overall mastery stats")
    void testGetSummary() {
        LearnerSubtopicPerformance p1 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .subjectName("Physics")
                .subtopicName("Kinematics")
                .avgAccuracyPercent(80.0)
                .avgPaceRatio(1.0)
                .avgSkipRatePercent(0.0)
                .build();

        LearnerSubtopicPerformance p2 = LearnerSubtopicPerformance.builder()
                .candidateId(CANDIDATE_ID)
                .subjectName("Physics")
                .subtopicName("Dynamics")
                .avgAccuracyPercent(40.0)
                .avgPaceRatio(1.0)
                .avgSkipRatePercent(0.0)
                .build();

        when(subtopicRepository.findByCandidateId(CANDIDATE_ID)).thenReturn(List.of(p1, p2));
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(DynamicLearningPath.builder()
                        .candidateId(CANDIDATE_ID)
                        .examTarget("NEET_UG")
                        .status("ACTIVE")
                        .build()));

        LearningPathSummaryResponse summary = generatorService.getSummary(CANDIDATE_ID);

        assertThat(summary.candidateId()).isEqualTo(CANDIDATE_ID);
        assertThat(summary.examTarget()).isEqualTo("NEET_UG");
        assertThat(summary.totalSubtopics()).isEqualTo(2);
        assertThat(summary.conceptualGapsCount()).isEqualTo(1);
        assertThat(summary.proficientCount()).isEqualTo(1);
        assertThat(summary.subjectSummaries()).containsKey("Physics");
        assertThat(summary.subjectSummaries().get("Physics").avgAccuracyPercent()).isEqualTo(60.0);
    }
}
