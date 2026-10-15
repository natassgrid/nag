// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.examplatform.practice.service.AdaptiveQuizGenerator;
import com.examplatform.practice.dto.PracticeSetDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MilestoneProgressionService Phased STAT Plan Unit Tests")
class MilestoneProgressionServiceTest {

    @Mock
    private DynamicLearningPathRepository dlpRepository;

    @Mock
    private LearningPathGeneratorService generatorService;

    private ObjectMapper objectMapper;
    private MilestoneProgressionService service;

    private static final UUID CANDIDATE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new MilestoneProgressionService(dlpRepository, generatorService, objectMapper);
    }

    private DynamicLearningPath createSampleDlp(String p1Status, String p2Status, String p3Status, boolean item1Completed) throws Exception {
        MilestoneItemDto item1 = new MilestoneItemDto(
                "P1-1",
                100L,
                "Moment of Inertia",
                "Rotational Mechanics",
                "Physics",
                PerformanceBracket.CONCEPTUAL_GAP,
                "MICRO_LESSON",
                "Remedial Micro-Lesson: Moment of Inertia",
                "Conceptual gap",
                20,
                item1Completed,
                null,
                null
        );

        MilestonePhaseDto phase1 = new MilestonePhaseDto(
                1, "Phase 1: Remediation", "Study micro-lessons", 2, p1Status, null, null, null, null, null, null, List.of(item1)
        );

        MilestoneItemDto item2 = new MilestoneItemDto(
                "P2-1",
                200L,
                "Work Energy Theorem",
                "Work and Energy",
                "Physics",
                PerformanceBracket.APPLICATION_GAP,
                "PRACTICE_MEDIUM",
                "Application practice",
                "Application gap",
                30,
                false,
                null,
                null
        );

        MilestonePhaseDto phase2 = new MilestonePhaseDto(
                2, "Phase 2: Adaptive Practice", "Worksheets", 1, p2Status, 70.0, null, null, null, null, null, List.of(item2)
        );

        MilestonePhaseDto phase3 = new MilestonePhaseDto(
                3, "Phase 3: Speed Mastery & Gatekeeper", "Gatekeeper", 1, p3Status, null, null, null, 75.0, null, false, List.of()
        );

        SubtopicClassificationResult gap = new SubtopicClassificationResult(
                1L, "Physics", 10L, "Mechanics", 100L, "Moment of Inertia",
                PerformanceBracket.CONCEPTUAL_GAP, 40.0, 1.2, 20.0, false, "STUDY", "Gap"
        );

        String currentPhase = "PHASE_1_REMEDIATION";
        if ("COMPLETED".equals(p1Status) && "IN_PROGRESS".equals(p2Status)) currentPhase = "PHASE_2_ADAPTIVE_QUIZ";
        if ("COMPLETED".equals(p2Status) && "IN_PROGRESS".equals(p3Status)) currentPhase = "PHASE_3_GATEKEEPER";
        if ("COMPLETED".equals(p3Status)) currentPhase = "COMPLETED";

        MilestoneGraphDto graph = new MilestoneGraphDto(
                currentPhase, "JEE_MAIN", "COMPLETED".equals(p3Status),
                List.of(phase1, phase2, phase3), List.of(gap), List.of(), List.of(), 1, 0.0
        );

        return DynamicLearningPath.builder()
                .candidateId(CANDIDATE_ID)
                .examTarget("JEE_MAIN")
                .status("ACTIVE")
                .milestoneGraphJson(objectMapper.writeValueAsString(graph))
                .build();
    }

    @Test
    @DisplayName("Completing all Phase 1 milestones promotes DLP to Phase 2 Adaptive Quizzing")
    void testCompleteMilestoneUnlocksPhase2() throws Exception {
        DynamicLearningPath dlp = createSampleDlp("IN_PROGRESS", "LOCKED", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));
        when(generatorService.toResponse(any())).thenReturn(null);

        service.completeMilestone(CANDIDATE_ID, "P1-1");

        verify(dlpRepository).save(dlp);
        MilestoneGraphDto updatedGraph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        assertThat(updatedGraph.currentPhase()).isEqualTo("PHASE_2_ADAPTIVE_QUIZ");
        assertThat(updatedGraph.phases().get(0).status()).isEqualTo("COMPLETED");
        assertThat(updatedGraph.phases().get(1).status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Starting Adaptive Quiz before Phase 1 completion throws 400 Bad Request")
    void testStartAdaptiveQuizBeforePhase1CompleteThrows() throws Exception {
        DynamicLearningPath dlp = createSampleDlp("IN_PROGRESS", "LOCKED", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        assertThatThrownBy(() -> service.startAdaptiveQuiz(CANDIDATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Phase 1 Remediation must be completed");
    }

    @Test
    @DisplayName("Submitting Adaptive Quiz with >= 70% accuracy promotes candidate to Phase 3 Gatekeeper")
    void testSubmitAdaptiveQuizPassPromotesToPhase3() throws Exception {
        DynamicLearningPath dlp = createSampleDlp("COMPLETED", "IN_PROGRESS", "LOCKED", true);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));
        when(generatorService.toResponse(any())).thenReturn(null);

        service.submitAdaptiveQuiz(CANDIDATE_ID, new SubmitQuizScoreRequest(UUID.randomUUID(), 75.0));

        verify(dlpRepository).save(dlp);
        MilestoneGraphDto updatedGraph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        assertThat(updatedGraph.currentPhase()).isEqualTo("PHASE_3_GATEKEEPER");
        assertThat(updatedGraph.phases().get(1).status()).isEqualTo("COMPLETED");
        assertThat(updatedGraph.phases().get(2).status()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("Submitting Gatekeeper with >= 75% score unlocks full mock exam permissions")
    void testSubmitGatekeeperPassRestoresMockAccess() throws Exception {
        DynamicLearningPath dlp = createSampleDlp("COMPLETED", "COMPLETED", "IN_PROGRESS", true);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));
        when(generatorService.toResponse(any())).thenReturn(null);

        service.submitGatekeeperTest(CANDIDATE_ID, new SubmitGatekeeperScoreRequest(UUID.randomUUID(), 80.0));

        verify(dlpRepository).save(dlp);
        MilestoneGraphDto updatedGraph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        assertThat(updatedGraph.gatekeeperPassed()).isTrue();
        assertThat(updatedGraph.currentPhase()).isEqualTo("COMPLETED");
        assertThat(updatedGraph.phases().get(2).status()).isEqualTo("COMPLETED");
        assertThat(updatedGraph.phases().get(2).passed()).isTrue();
    }

    @Test
    @DisplayName("Gatekeeper status blocks full mocks until Phase 3 test is passed")
    void testGatekeeperStatusCheck() throws Exception {
        DynamicLearningPath dlp = createSampleDlp("IN_PROGRESS", "LOCKED", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        GatekeeperStatusResponse status = service.getGatekeeperStatus(CANDIDATE_ID);
        assertThat(status.gatekeeperRequired()).isTrue();
        assertThat(status.clearedForFullMock()).isFalse();
        assertThat(status.remainingMilestonesCount()).isGreaterThan(0);
        assertThat(status.remedialSubtopics()).contains("Moment of Inertia");
    }

    @Test
    @DisplayName("startAdaptiveQuiz invokes AdaptiveQuizGenerator when present and sets practiceSetId")
    void testStartAdaptiveQuizWithGenerator() throws Exception {
        DynamicLearningPath dlp = createSampleDlp("COMPLETED", "IN_PROGRESS", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        AdaptiveQuizGenerator mockGenerator = org.mockito.Mockito.mock(AdaptiveQuizGenerator.class);
        UUID expectedSetId = UUID.randomUUID();
        PracticeSetDto mockDto = new PracticeSetDto(
                expectedSetId, "Test Set", "Desc", "ADAPTIVE_DLP", 30, "physics", true, 15
        );
        when(mockGenerator.generateAdaptiveSet(any(), any(), any(), eq(15), eq(false)))
                .thenReturn(mockDto);

        @SuppressWarnings("unchecked")
        org.springframework.beans.factory.ObjectProvider<AdaptiveQuizGenerator> mockProvider =
                org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        when(mockProvider.getIfAvailable()).thenReturn(mockGenerator);

        MilestoneProgressionService serviceWithGen = new MilestoneProgressionService(
                dlpRepository, generatorService, objectMapper, mockProvider);

        StartAdaptiveQuizResponse response = serviceWithGen.startAdaptiveQuiz(CANDIDATE_ID);
        assertThat(response.practiceSetId()).isEqualTo(expectedSetId);
        assertThat(response.practiceLauncherUrl()).contains(expectedSetId.toString());
    }
}
