// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.examplatform.practice.service.AdaptiveQuizGenerator;
import com.examplatform.practice.dto.PracticeSetDto;
import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
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
        if (SharedConstants.COMPLETED.equals(p1Status) && SharedConstants.IN_PROGRESS.equals(p2Status)) currentPhase = "PHASE_2_ADAPTIVE_QUIZ";
        if (SharedConstants.COMPLETED.equals(p2Status) && SharedConstants.IN_PROGRESS.equals(p3Status)) currentPhase = "PHASE_3_GATEKEEPER";
        if (SharedConstants.COMPLETED.equals(p3Status)) currentPhase = SharedConstants.COMPLETED;

        MilestoneGraphDto graph = new MilestoneGraphDto(
                currentPhase, SharedConstants.JEE_MAIN, SharedConstants.COMPLETED.equals(p3Status),
                List.of(phase1, phase2, phase3), List.of(gap), List.of(), List.of(), 1, 0.0
        );

        return DynamicLearningPath.builder()
                .candidateId(CANDIDATE_ID)
                .examTarget(SharedConstants.JEE_MAIN)
                .status("ACTIVE")
                .milestoneGraphJson(objectMapper.writeValueAsString(graph))
                .build();
    }

    @Test
    @DisplayName("Completing all Phase 1 milestones promotes DLP to Phase 2 Adaptive Quizzing")
    void testCompleteMilestoneUnlocksPhase2() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.IN_PROGRESS, "LOCKED", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));
        when(generatorService.toResponse(any())).thenReturn(null);

        service.completeMilestone(CANDIDATE_ID, "P1-1");

        verify(dlpRepository).save(dlp);
        MilestoneGraphDto updatedGraph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        assertThat(updatedGraph.currentPhase()).isEqualTo("PHASE_2_ADAPTIVE_QUIZ");
        assertThat(updatedGraph.phases().get(0).status()).isEqualTo(SharedConstants.COMPLETED);
        assertThat(updatedGraph.phases().get(1).status()).isEqualTo(SharedConstants.IN_PROGRESS);
    }

    @Test
    @DisplayName("Starting Adaptive Quiz before Phase 1 completion throws 400 Bad Request")
    void testStartAdaptiveQuizBeforePhase1CompleteThrows() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.IN_PROGRESS, "LOCKED", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        assertThatThrownBy(() -> service.startAdaptiveQuiz(CANDIDATE_ID))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Phase 1 Remediation must be completed");
    }

    @Test
    @DisplayName("Submitting Adaptive Quiz with >= 70% accuracy promotes candidate to Phase 3 Gatekeeper")
    void testSubmitAdaptiveQuizPassPromotesToPhase3() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.COMPLETED, SharedConstants.IN_PROGRESS, "LOCKED", true);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));
        when(generatorService.toResponse(any())).thenReturn(null);

        service.submitAdaptiveQuiz(CANDIDATE_ID, new SubmitQuizScoreRequest(UUID.randomUUID(), 75.0));

        verify(dlpRepository).save(dlp);
        MilestoneGraphDto updatedGraph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        assertThat(updatedGraph.currentPhase()).isEqualTo("PHASE_3_GATEKEEPER");
        assertThat(updatedGraph.phases().get(1).status()).isEqualTo(SharedConstants.COMPLETED);
        assertThat(updatedGraph.phases().get(2).status()).isEqualTo(SharedConstants.IN_PROGRESS);
    }

    @Test
    @DisplayName("Submitting Gatekeeper with >= 75% score unlocks full mock exam permissions")
    void testSubmitGatekeeperPassRestoresMockAccess() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.COMPLETED, SharedConstants.COMPLETED,
            SharedConstants.IN_PROGRESS, true);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));
        when(generatorService.toResponse(any())).thenReturn(null);

        service.submitGatekeeperTest(CANDIDATE_ID, new SubmitGatekeeperScoreRequest(UUID.randomUUID(), 80.0));

        verify(dlpRepository).save(dlp);
        MilestoneGraphDto updatedGraph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        assertThat(updatedGraph.gatekeeperPassed()).isTrue();
        assertThat(updatedGraph.currentPhase()).isEqualTo(SharedConstants.COMPLETED);
        assertThat(updatedGraph.phases().get(2).status()).isEqualTo(SharedConstants.COMPLETED);
        assertThat(updatedGraph.phases().get(2).passed()).isTrue();
    }

    @Test
    @DisplayName("Gatekeeper status blocks full mocks until Phase 3 test is passed")
    void testGatekeeperStatusCheck() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.IN_PROGRESS, "LOCKED", "LOCKED", false);
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
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.COMPLETED, SharedConstants.IN_PROGRESS, "LOCKED", false);
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

    @Test
    @DisplayName("startGatekeeperTest invokes AdaptiveQuizGenerator with isGatekeeper=true and sets practiceSetId")
    void testStartGatekeeperTestWithGenerator() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.COMPLETED, SharedConstants.COMPLETED,
            SharedConstants.IN_PROGRESS, true);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        AdaptiveQuizGenerator mockGenerator = org.mockito.Mockito.mock(AdaptiveQuizGenerator.class);
        UUID expectedSetId = UUID.randomUUID();
        PracticeSetDto mockDto = new PracticeSetDto(
                expectedSetId, "Gatekeeper Set", "Desc", "GATEKEEPER_TEST", 45, "physics", true, 20
        );
        when(mockGenerator.generateAdaptiveSet(any(), any(), any(), eq(20), eq(true)))
                .thenReturn(mockDto);

        @SuppressWarnings("unchecked")
        org.springframework.beans.factory.ObjectProvider<AdaptiveQuizGenerator> mockProvider =
                org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        when(mockProvider.getIfAvailable()).thenReturn(mockGenerator);

        MilestoneProgressionService serviceWithGen = new MilestoneProgressionService(
                dlpRepository, generatorService, objectMapper, mockProvider);

        StartGatekeeperResponse response = serviceWithGen.startGatekeeperTest(CANDIDATE_ID);
        assertThat(response.practiceSetId()).isEqualTo(expectedSetId);
        assertThat(response.practiceLauncherUrl()).contains(expectedSetId.toString());
    }

    @Test
    @DisplayName("getGatekeeperStatus returns cleared when currentPhase is DIAGNOSTIC_REQUIRED")
    void testGatekeeperStatusDiagnosticRequired() throws Exception {
        MilestoneGraphDto diagGraph = new MilestoneGraphDto(
                "DIAGNOSTIC_REQUIRED",
            SharedConstants.JEE_MAIN,
                false,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0.0
        );
        DynamicLearningPath dlp = DynamicLearningPath.builder()
                .candidateId(CANDIDATE_ID)
                .status("ACTIVE")
                .milestoneGraphJson(objectMapper.writeValueAsString(diagGraph))
                .build();

        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        GatekeeperStatusResponse status = service.getGatekeeperStatus(CANDIDATE_ID);
        assertThat(status.gatekeeperRequired()).isFalse();
        assertThat(status.clearedForFullMock()).isTrue();
        assertThat(status.currentPhase()).isEqualTo("DIAGNOSTIC_REQUIRED");
    }

    @Test
    @DisplayName("getGatekeeperStatus populates conceptualGaps and applicationGaps")
    void testGatekeeperStatusPopulatesGaps() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.IN_PROGRESS, "LOCKED", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        GatekeeperStatusResponse status = service.getGatekeeperStatus(CANDIDATE_ID);
        assertThat(status.gatekeeperRequired()).isTrue();
        assertThat(status.clearedForFullMock()).isFalse();
        assertThat(status.conceptualGaps()).contains("Moment of Inertia");
        assertThat(status.applicationGaps()).contains("Work Energy Theorem");
        assertThat(status.remedialSubtopics()).contains("Moment of Inertia");
    }

    @Test
    @DisplayName("handleCompletedSessionIfApplicable completes Phase 2 quiz when in PHASE_2_ADAPTIVE_QUIZ")
    void testHandleCompletedSessionPhase2() throws Exception {
        UUID quizSessionId = UUID.randomUUID();
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.COMPLETED, SharedConstants.IN_PROGRESS, "LOCKED", true);

        // Update phase2 with quizSessionId
        MilestoneGraphDto graph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        List<MilestonePhaseDto> phases = new ArrayList<>();
        for (MilestonePhaseDto p : graph.phases()) {
            if (p.phaseNumber() == 2) {
                phases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        p.status(), p.targetAccuracy(), p.achievedAccuracy(), quizSessionId,
                        p.passThreshold(), p.gatekeeperSessionId(), p.passed(), p.items()
                ));
            } else {
                phases.add(p);
            }
        }
        MilestoneGraphDto updatedGraph = new MilestoneGraphDto(
                "PHASE_2_ADAPTIVE_QUIZ", graph.targetExam(), false, phases,
                graph.conceptualGaps(), graph.applicationGaps(), graph.proficientTopics(),
                graph.totalSubtopicsEvaluated(), graph.overallReadinessScore()
        );
        dlp.setMilestoneGraphJson(objectMapper.writeValueAsString(updatedGraph));

        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));
        when(dlpRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean handled = service.handleCompletedSessionIfApplicable(CANDIDATE_ID, quizSessionId, 85.0);
        assertThat(handled).isTrue();

        MilestoneGraphDto savedGraph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        assertThat(savedGraph.getPhase2().status()).isEqualTo(SharedConstants.COMPLETED);
        assertThat(savedGraph.currentPhase()).isEqualTo("PHASE_3_GATEKEEPER");
    }

    @Test
    @DisplayName("handleCompletedSessionIfApplicable returns false when in Phase 1")
    void testHandleCompletedSessionPhase1ReturnsFalse() throws Exception {
        DynamicLearningPath dlp = createSampleDlp(SharedConstants.IN_PROGRESS, "LOCKED", "LOCKED", false);
        when(dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(CANDIDATE_ID, "ACTIVE"))
                .thenReturn(Optional.of(dlp));

        boolean handled = service.handleCompletedSessionIfApplicable(CANDIDATE_ID, UUID.randomUUID(), 85.0);
        assertThat(handled).isFalse();
    }
}
