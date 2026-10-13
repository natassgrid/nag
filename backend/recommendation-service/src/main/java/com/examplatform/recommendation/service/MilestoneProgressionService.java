// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MilestoneProgressionService {

    private final DynamicLearningPathRepository dlpRepository;
    private final LearningPathGeneratorService generatorService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DynamicLearningPathResponse completeMilestone(UUID candidateId, String milestoneId) {
        DynamicLearningPath dlp = getActiveDlp(candidateId);
        MilestoneGraphDto graph = parseGraph(dlp);

        boolean itemFound = false;
        List<MilestonePhaseDto> updatedPhases = new ArrayList<>();

        for (MilestonePhaseDto phase : graph.phases()) {
            List<MilestoneItemDto> updatedItems = new ArrayList<>();
            boolean phaseModified = false;

            if (phase.items() != null) {
                for (MilestoneItemDto item : phase.items()) {
                    if (item.id().equalsIgnoreCase(milestoneId)) {
                        updatedItems.add(new MilestoneItemDto(
                                item.id(),
                                item.subtopicId(),
                                item.subtopicName(),
                                item.topicName(),
                                item.subjectName(),
                                item.bracket(),
                                item.actionType(),
                                item.title(),
                                item.description(),
                                item.estimatedMinutes(),
                                true,
                                Instant.now(),
                                item.microLessonId()
                        ));
                        itemFound = true;
                        phaseModified = true;
                    } else {
                        updatedItems.add(item);
                    }
                }
            }

            if (phaseModified) {
                updatedPhases.add(new MilestonePhaseDto(
                        phase.phaseNumber(),
                        phase.phaseName(),
                        phase.targetDescription(),
                        phase.durationWeeks(),
                        phase.status(),
                        phase.targetAccuracy(),
                        phase.achievedAccuracy(),
                        phase.quizSessionId(),
                        phase.passThreshold(),
                        phase.gatekeeperSessionId(),
                        phase.passed(),
                        updatedItems
                ));
            } else {
                updatedPhases.add(phase);
            }
        }

        if (!itemFound) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Milestone item not found: " + milestoneId);
        }

        // Check if Phase 1 is now fully completed
        List<MilestonePhaseDto> resolvedPhases = new ArrayList<>();
        String newCurrentPhase = graph.currentPhase();

        MilestonePhaseDto phase1 = findPhase(updatedPhases, 1);
        boolean phase1Complete = phase1 != null && phase1.items() != null && phase1.items().stream().allMatch(MilestoneItemDto::completed);

        for (MilestonePhaseDto p : updatedPhases) {
            if (p.phaseNumber() == 1) {
                String p1Status = phase1Complete ? "COMPLETED" : "IN_PROGRESS";
                resolvedPhases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        p1Status, p.targetAccuracy(), p.achievedAccuracy(), p.quizSessionId(),
                        p.passThreshold(), p.gatekeeperSessionId(), p.passed(), p.items()
                ));
            } else if (p.phaseNumber() == 2) {
                String p2Status = p.status();
                if (phase1Complete && !"COMPLETED".equalsIgnoreCase(p2Status)) {
                    p2Status = "IN_PROGRESS";
                    newCurrentPhase = "PHASE_2_ADAPTIVE_QUIZ";
                }
                resolvedPhases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        p2Status, p.targetAccuracy(), p.achievedAccuracy(), p.quizSessionId(),
                        p.passThreshold(), p.gatekeeperSessionId(), p.passed(), p.items()
                ));
            } else {
                resolvedPhases.add(p);
            }
        }

        MilestoneGraphDto updatedGraph = new MilestoneGraphDto(
                newCurrentPhase,
                graph.targetExam(),
                graph.gatekeeperPassed(),
                resolvedPhases,
                graph.conceptualGaps(),
                graph.applicationGaps(),
                graph.proficientTopics(),
                graph.totalSubtopicsEvaluated(),
                graph.overallReadinessScore()
        );

        saveGraph(dlp, updatedGraph);
        return generatorService.toResponse(dlp);
    }

    @Transactional
    public StartAdaptiveQuizResponse startAdaptiveQuiz(UUID candidateId) {
        DynamicLearningPath dlp = getActiveDlp(candidateId);
        MilestoneGraphDto graph = parseGraph(dlp);

        MilestonePhaseDto phase1 = findPhase(graph.phases(), 1);
        if (phase1 != null && !"COMPLETED".equalsIgnoreCase(phase1.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phase 1 Remediation must be completed before starting adaptive quiz");
        }

        MilestonePhaseDto phase2 = findPhase(graph.phases(), 2);
        UUID quizSessionId = (phase2 != null && phase2.quizSessionId() != null)
                ? phase2.quizSessionId()
                : UUID.randomUUID();

        List<MilestonePhaseDto> updatedPhases = new ArrayList<>();
        for (MilestonePhaseDto p : graph.phases()) {
            if (p.phaseNumber() == 2) {
                updatedPhases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        "IN_PROGRESS", p.targetAccuracy(), p.achievedAccuracy(), quizSessionId,
                        p.passThreshold(), p.gatekeeperSessionId(), p.passed(), p.items()
                ));
            } else {
                updatedPhases.add(p);
            }
        }

        List<String> targetSubtopics = new ArrayList<>();
        if (phase2 != null && phase2.items() != null) {
            for (MilestoneItemDto it : phase2.items()) {
                targetSubtopics.add(it.subtopicName());
            }
        }
        if (targetSubtopics.isEmpty() && graph.applicationGaps() != null) {
            for (SubtopicClassificationResult r : graph.applicationGaps()) {
                targetSubtopics.add(r.subtopicName());
            }
        }

        MilestoneGraphDto updatedGraph = new MilestoneGraphDto(
                "PHASE_2_ADAPTIVE_QUIZ",
                graph.targetExam(),
                graph.gatekeeperPassed(),
                updatedPhases,
                graph.conceptualGaps(),
                graph.applicationGaps(),
                graph.proficientTopics(),
                graph.totalSubtopicsEvaluated(),
                graph.overallReadinessScore()
        );

        saveGraph(dlp, updatedGraph);

        return new StartAdaptiveQuizResponse(
                quizSessionId,
                "PHASE_2_ADAPTIVE_QUIZ",
                phase2 != null && phase2.targetAccuracy() != null ? phase2.targetAccuracy() : 70.0,
                15,
                targetSubtopics,
                "Adaptive quiz session successfully initialized with 70% Easy/Medium and 30% Hard ratio."
        );
    }

    @Transactional
    public DynamicLearningPathResponse submitAdaptiveQuiz(UUID candidateId, SubmitQuizScoreRequest request) {
        DynamicLearningPath dlp = getActiveDlp(candidateId);
        MilestoneGraphDto graph = parseGraph(dlp);

        MilestonePhaseDto phase2 = findPhase(graph.phases(), 2);
        if (phase2 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phase 2 Adaptive Quizzing not found in DLP");
        }

        double targetAcc = phase2.targetAccuracy() != null ? phase2.targetAccuracy() : 70.0;
        boolean passed = request.scorePercent() >= targetAcc;

        List<MilestonePhaseDto> updatedPhases = new ArrayList<>();
        String nextCurrentPhase = graph.currentPhase();

        for (MilestonePhaseDto p : graph.phases()) {
            if (p.phaseNumber() == 2) {
                String p2Status = passed ? "COMPLETED" : "IN_PROGRESS";
                updatedPhases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        p2Status, p.targetAccuracy(), request.scorePercent(),
                        request.sessionId() != null ? request.sessionId() : p.quizSessionId(),
                        p.passThreshold(), p.gatekeeperSessionId(), p.passed(), p.items()
                ));
            } else if (p.phaseNumber() == 3) {
                String p3Status = p.status();
                if (passed && !"COMPLETED".equalsIgnoreCase(p3Status)) {
                    p3Status = "IN_PROGRESS";
                    nextCurrentPhase = "PHASE_3_GATEKEEPER";
                }
                updatedPhases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        p3Status, p.targetAccuracy(), p.achievedAccuracy(), p.quizSessionId(),
                        p.passThreshold(), p.gatekeeperSessionId(), p.passed(), p.items()
                ));
            } else {
                updatedPhases.add(p);
            }
        }

        MilestoneGraphDto updatedGraph = new MilestoneGraphDto(
                nextCurrentPhase,
                graph.targetExam(),
                graph.gatekeeperPassed(),
                updatedPhases,
                graph.conceptualGaps(),
                graph.applicationGaps(),
                graph.proficientTopics(),
                graph.totalSubtopicsEvaluated(),
                graph.overallReadinessScore()
        );

        saveGraph(dlp, updatedGraph);
        return generatorService.toResponse(dlp);
    }

    @Transactional
    public StartGatekeeperResponse startGatekeeperTest(UUID candidateId) {
        DynamicLearningPath dlp = getActiveDlp(candidateId);
        MilestoneGraphDto graph = parseGraph(dlp);

        MilestonePhaseDto phase2 = findPhase(graph.phases(), 2);
        if (phase2 != null && !"COMPLETED".equalsIgnoreCase(phase2.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phase 2 Adaptive Quizzing must be completed before starting Gatekeeper test");
        }

        MilestonePhaseDto phase3 = findPhase(graph.phases(), 3);
        UUID gatekeeperSessionId = (phase3 != null && phase3.gatekeeperSessionId() != null)
                ? phase3.gatekeeperSessionId()
                : UUID.randomUUID();

        List<MilestonePhaseDto> updatedPhases = new ArrayList<>();
        for (MilestonePhaseDto p : graph.phases()) {
            if (p.phaseNumber() == 3) {
                updatedPhases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        "IN_PROGRESS", p.targetAccuracy(), p.achievedAccuracy(), p.quizSessionId(),
                        p.passThreshold(), gatekeeperSessionId, false, p.items()
                ));
            } else {
                updatedPhases.add(p);
            }
        }

        List<String> failedSubtopics = new ArrayList<>();
        if (graph.conceptualGaps() != null) {
            for (SubtopicClassificationResult r : graph.conceptualGaps()) {
                failedSubtopics.add(r.subtopicName());
            }
        }

        MilestoneGraphDto updatedGraph = new MilestoneGraphDto(
                "PHASE_3_GATEKEEPER",
                graph.targetExam(),
                false,
                updatedPhases,
                graph.conceptualGaps(),
                graph.applicationGaps(),
                graph.proficientTopics(),
                graph.totalSubtopicsEvaluated(),
                graph.overallReadinessScore()
        );

        saveGraph(dlp, updatedGraph);

        return new StartGatekeeperResponse(
                gatekeeperSessionId,
                "PHASE_3_GATEKEEPER",
                phase3 != null && phase3.passThreshold() != null ? phase3.passThreshold() : 75.0,
                20,
                failedSubtopics,
                "Gatekeeper test successfully initialized. Achieving >= 75% unlocks full mock examination access."
        );
    }

    @Transactional
    public DynamicLearningPathResponse submitGatekeeperTest(UUID candidateId, SubmitGatekeeperScoreRequest request) {
        DynamicLearningPath dlp = getActiveDlp(candidateId);
        MilestoneGraphDto graph = parseGraph(dlp);

        MilestonePhaseDto phase3 = findPhase(graph.phases(), 3);
        if (phase3 == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phase 3 Gatekeeper Test not found in DLP");
        }

        double threshold = phase3.passThreshold() != null ? phase3.passThreshold() : 75.0;
        boolean passed = request.scorePercent() >= threshold;

        List<MilestonePhaseDto> updatedPhases = new ArrayList<>();
        for (MilestonePhaseDto p : graph.phases()) {
            if (p.phaseNumber() == 3) {
                String p3Status = passed ? "COMPLETED" : "IN_PROGRESS";
                updatedPhases.add(new MilestonePhaseDto(
                        p.phaseNumber(), p.phaseName(), p.targetDescription(), p.durationWeeks(),
                        p3Status, p.targetAccuracy(), p.achievedAccuracy(), p.quizSessionId(),
                        p.passThreshold(),
                        request.sessionId() != null ? request.sessionId() : p.gatekeeperSessionId(),
                        passed, p.items()
                ));
            } else {
                updatedPhases.add(p);
            }
        }

        String nextCurrentPhase = passed ? "COMPLETED" : "PHASE_3_GATEKEEPER";

        MilestoneGraphDto updatedGraph = new MilestoneGraphDto(
                nextCurrentPhase,
                graph.targetExam(),
                passed,
                updatedPhases,
                graph.conceptualGaps(),
                graph.applicationGaps(),
                graph.proficientTopics(),
                graph.totalSubtopicsEvaluated(),
                graph.overallReadinessScore()
        );

        saveGraph(dlp, updatedGraph);
        return generatorService.toResponse(dlp);
    }

    @Transactional(readOnly = true)
    public GatekeeperStatusResponse getGatekeeperStatus(UUID candidateId) {
        DynamicLearningPath dlp = dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(candidateId, "ACTIVE")
                .orElse(null);

        if (dlp == null) {
            return new GatekeeperStatusResponse(false, true, "COMPLETED", null, 0, Collections.emptyList());
        }

        MilestoneGraphDto graph = parseGraph(dlp);
        boolean hasGaps = (graph.conceptualGaps() != null && !graph.conceptualGaps().isEmpty())
                || (graph.applicationGaps() != null && !graph.applicationGaps().isEmpty());

        if (!hasGaps || graph.gatekeeperPassed()) {
            return new GatekeeperStatusResponse(false, true, "COMPLETED", null, 0, Collections.emptyList());
        }

        MilestonePhaseDto phase3 = findPhase(graph.phases(), 3);
        UUID gatekeeperSessionId = phase3 != null ? phase3.gatekeeperSessionId() : null;

        int remainingMilestones = 0;
        if (graph.phases() != null) {
            for (MilestonePhaseDto p : graph.phases()) {
                if (p.items() != null) {
                    for (MilestoneItemDto it : p.items()) {
                        if (!it.completed()) remainingMilestones++;
                    }
                }
            }
        }

        List<String> remedialSubtopics = new ArrayList<>();
        if (graph.conceptualGaps() != null) {
            for (SubtopicClassificationResult r : graph.conceptualGaps()) {
                remedialSubtopics.add(r.subtopicName());
            }
        }

        return new GatekeeperStatusResponse(
                true,
                false,
                graph.currentPhase(),
                gatekeeperSessionId,
                remainingMilestones,
                remedialSubtopics
        );
    }

    private DynamicLearningPath getActiveDlp(UUID candidateId) {
        return dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(candidateId, "ACTIVE")
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No active Dynamic Learning Path found for candidate"));
    }

    private MilestoneGraphDto parseGraph(DynamicLearningPath dlp) {
        if (dlp.getMilestoneGraphJson() == null || dlp.getMilestoneGraphJson().isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Corrupted milestone graph");
        }
        try {
            return objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse milestone graph", e);
        }
    }

    private void saveGraph(DynamicLearningPath dlp, MilestoneGraphDto graph) {
        try {
            dlp.setMilestoneGraphJson(objectMapper.writeValueAsString(graph));
            dlpRepository.save(dlp);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize milestone graph", e);
        }
    }

    private MilestonePhaseDto findPhase(List<MilestonePhaseDto> phases, int number) {
        if (phases == null) return null;
        return phases.stream().filter(p -> p.phaseNumber() == number).findFirst().orElse(null);
    }
}
