// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class LearningPathGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathGeneratorService.class);
    public static final String DEFAULT_EXAM_TARGET = "GENERAL_COMPETITIVE";

    private final SubtopicClassifierService classifierService;
    private final LearnerSubtopicPerformanceRepository subtopicRepository;
    private final DynamicLearningPathRepository dlpRepository;
    private final ObjectMapper objectMapper;

    private long deriveSubtopicId(String subtopicName) {
        if (subtopicName == null || subtopicName.isBlank()) return 1L;
        long hash = Math.abs((long) subtopicName.toLowerCase().trim().hashCode());
        return hash > 0 ? hash : 1L;
    }

    /**
     * Deterministically generates or recalculates a Dynamic Learning Path (DLP)
     * based on candidate's granular subtopic performance.
     */
    @Transactional
    public DynamicLearningPath generateOrRecalculate(UUID candidateId, UUID sourceSessionId, String examTarget) {
        String resolvedTarget = (examTarget != null && !examTarget.isBlank())
                ? examTarget
                : dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(candidateId, "ACTIVE")
                        .map(DynamicLearningPath::getExamTarget)
                        .orElse(DEFAULT_EXAM_TARGET);

        // Inspect existing ACTIVE paths before superseding to preserve completed milestones
        List<DynamicLearningPath> existingActive = dlpRepository.findByCandidateIdAndStatus(candidateId, "ACTIVE");
        Set<Long> completedP1Ids = new HashSet<>();
        Set<String> completedP1Names = new HashSet<>();
        Map<String, Instant> completedP1Timestamps = new HashMap<>();
        boolean prevPhase2Completed = false;
        boolean prevPhase3Passed = false;

        for (DynamicLearningPath oldPath : existingActive) {
            if (oldPath.getMilestoneGraphJson() != null && !oldPath.getMilestoneGraphJson().isBlank()) {
                try {
                    MilestoneGraphDto oldGraph = objectMapper.readValue(oldPath.getMilestoneGraphJson(), MilestoneGraphDto.class);
                    if (oldGraph.phases() != null) {
                        for (MilestonePhaseDto p : oldGraph.phases()) {
                            if (p.phaseNumber() == 1 && p.items() != null) {
                                for (MilestoneItemDto it : p.items()) {
                                    if (it.completed()) {
                                        if (it.subtopicId() != null) {
                                            completedP1Ids.add(it.subtopicId());
                                        }
                                        if (it.subtopicName() != null) {
                                            String norm = it.subtopicName().toLowerCase().trim();
                                            completedP1Names.add(norm);
                                            if (it.completedAt() != null) {
                                                completedP1Timestamps.put(norm, it.completedAt());
                                            }
                                        }
                                    }
                                }
                            }
                            if (p.phaseNumber() == 2 && ("COMPLETED".equalsIgnoreCase(p.status())
                                    || (p.achievedAccuracy() != null && p.targetAccuracy() != null && p.achievedAccuracy() >= p.targetAccuracy()))) {
                                prevPhase2Completed = true;
                            }
                            if (p.phaseNumber() == 3 && ("COMPLETED".equalsIgnoreCase(p.status())
                                    || Boolean.TRUE.equals(p.passed()))) {
                                prevPhase3Passed = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to inspect previous active DLP graph for candidate {}: {}", candidateId, e.getMessage());
                }
            }
        }

        List<LearnerSubtopicPerformance> performances = subtopicRepository.findByCandidateId(candidateId);
        boolean isColdStart = performances.isEmpty();

        List<SubtopicClassificationResult> conceptualGaps = new ArrayList<>();
        List<SubtopicClassificationResult> applicationGaps = new ArrayList<>();
        List<SubtopicClassificationResult> proficientTopics = new ArrayList<>();

        for (LearnerSubtopicPerformance perf : performances) {
            if (perf.getSubtopicId() == null && perf.getSubtopicName() != null) {
                perf.setSubtopicId(deriveSubtopicId(perf.getSubtopicName()));
            }
            SubtopicClassificationResult result = classifierService.classify(perf);
            if (result.bracket() == PerformanceBracket.CONCEPTUAL_GAP) {
                conceptualGaps.add(result);
            } else if (result.bracket() == PerformanceBracket.APPLICATION_GAP) {
                applicationGaps.add(result);
            } else {
                proficientTopics.add(result);
            }
        }

        int totalEvaluated = performances.size();
        double readinessScore = 0.0;
        if (totalEvaluated > 0) {
            double weightedPoints = (proficientTopics.size() * 1.0) + (applicationGaps.size() * 0.5);
            readinessScore = Math.round((weightedPoints / totalEvaluated) * 1000.0) / 10.0;
        }

        // Assemble phased milestone schedule
        List<MilestonePhaseDto> phases = assembleMilestonePhases(
                isColdStart,
                conceptualGaps,
                applicationGaps,
                proficientTopics,
                completedP1Ids,
                completedP1Names,
                completedP1Timestamps,
                prevPhase2Completed,
                prevPhase3Passed
        );

        String currentPhase;
        boolean gatekeeperPassed;
        String dlpStatus;

        if (isColdStart) {
            currentPhase = "DIAGNOSTIC_REQUIRED";
            gatekeeperPassed = false;
            dlpStatus = "ACTIVE";
        } else if (conceptualGaps.isEmpty() && applicationGaps.isEmpty()) {
            currentPhase = "COMPLETED";
            gatekeeperPassed = true;
            dlpStatus = "COMPLETED";
        } else {
            MilestonePhaseDto p1 = phases.stream().filter(p -> p.phaseNumber() == 1).findFirst().orElse(null);
            MilestonePhaseDto p2 = phases.stream().filter(p -> p.phaseNumber() == 2).findFirst().orElse(null);
            MilestonePhaseDto p3 = phases.stream().filter(p -> p.phaseNumber() == 3).findFirst().orElse(null);

            if (p1 != null && !"COMPLETED".equalsIgnoreCase(p1.status())) {
                currentPhase = "PHASE_1_REMEDIATION";
                gatekeeperPassed = false;
                dlpStatus = "ACTIVE";
            } else if (p2 != null && !"COMPLETED".equalsIgnoreCase(p2.status())) {
                currentPhase = "PHASE_2_ADAPTIVE_QUIZ";
                gatekeeperPassed = false;
                dlpStatus = "ACTIVE";
            } else if (p3 != null && !"COMPLETED".equalsIgnoreCase(p3.status())) {
                currentPhase = "PHASE_3_GATEKEEPER";
                gatekeeperPassed = false;
                dlpStatus = "ACTIVE";
            } else {
                currentPhase = "COMPLETED";
                gatekeeperPassed = true;
                dlpStatus = "COMPLETED";
            }
        }

        MilestoneGraphDto milestoneGraph = new MilestoneGraphDto(
                currentPhase,
                resolvedTarget,
                gatekeeperPassed,
                phases,
                conceptualGaps,
                applicationGaps,
                proficientTopics,
                totalEvaluated,
                readinessScore
        );

        String milestoneGraphJson;
        try {
            milestoneGraphJson = objectMapper.writeValueAsString(milestoneGraph);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize milestone graph for candidate {}", candidateId, e);
            milestoneGraphJson = "{}";
        }

        // Mark previously active paths as SUPERSEDED
        for (DynamicLearningPath oldPath : existingActive) {
            oldPath.setStatus("SUPERSEDED");
            dlpRepository.save(oldPath);
        }

        // Create new active DLP
        DynamicLearningPath dlp = DynamicLearningPath.builder()
                .candidateId(candidateId)
                .sourceSessionId(sourceSessionId)
                .examTarget(resolvedTarget)
                .status(dlpStatus)
                .conceptualGapsCount(conceptualGaps.size())
                .applicationGapsCount(applicationGaps.size())
                .proficientCount(proficientTopics.size())
                .milestoneGraphJson(milestoneGraphJson)
                .build();

        return dlpRepository.save(dlp);
    }

    private List<MilestonePhaseDto> assembleMilestonePhases(
            boolean isColdStart,
            List<SubtopicClassificationResult> conceptualGaps,
            List<SubtopicClassificationResult> applicationGaps,
            List<SubtopicClassificationResult> proficientTopics,
            Set<Long> completedP1Ids,
            Set<String> completedP1Names,
            Map<String, Instant> completedP1Timestamps,
            boolean prevPhase2Completed,
            boolean prevPhase3Passed) {

        List<MilestonePhaseDto> phases = new ArrayList<>();

        if (isColdStart) {
            phases.add(new MilestonePhaseDto(
                    1,
                    "Phase 1: Remediation (Micro-Lessons)",
                    "Review core concept derivations, formula cheat sheets, and trap warnings for conceptual gaps.",
                    2,
                    "PENDING",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Collections.emptyList()
            ));
            phases.add(new MilestonePhaseDto(
                    2,
                    "Phase 2: Adaptive Practice (Worksheets)",
                    "Practice application sets with varied edge cases, elimination shortcuts, and time management.",
                    1,
                    "LOCKED",
                    70.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Collections.emptyList()
            ));
            phases.add(new MilestonePhaseDto(
                    3,
                    "Phase 3: Speed Mastery & Gatekeeper",
                    "Execute timed sprint drills to accelerate pacing and pass Gatekeeper test before mock unlock.",
                    1,
                    "LOCKED",
                    null,
                    null,
                    null,
                    75.0,
                    null,
                    false,
                    Collections.emptyList()
            ));
            return phases;
        }

        // Phase 1: Remediation (Study Micro-Lessons)
        List<MilestoneItemDto> phase1Items = new ArrayList<>();
        int itemIdx = 1;
        for (SubtopicClassificationResult item : conceptualGaps) {
            Long subId = item.subtopicId() != null ? item.subtopicId() : deriveSubtopicId(item.subtopicName());
            String normName = item.subtopicName() != null ? item.subtopicName().toLowerCase().trim() : "";
            boolean wasCompleted = (subId != null && completedP1Ids.contains(subId)) || completedP1Names.contains(normName);
            Instant completedAt = wasCompleted ? completedP1Timestamps.getOrDefault(normName, Instant.now()) : null;

            phase1Items.add(new MilestoneItemDto(
                    "P1-" + (itemIdx++),
                    subId,
                    item.subtopicName(),
                    item.topicName(),
                    item.subjectName(),
                    PerformanceBracket.CONCEPTUAL_GAP,
                    "MICRO_LESSON",
                    "Remedial Micro-Lesson: " + item.subtopicName(),
                    item.recommendationNote(),
                    20,
                    wasCompleted,
                    completedAt,
                    null
            ));
        }

        boolean allP1Completed = !phase1Items.isEmpty() && phase1Items.stream().allMatch(MilestoneItemDto::completed);
        String p1Status = (conceptualGaps.isEmpty() || allP1Completed) ? "COMPLETED" : "IN_PROGRESS";
        phases.add(new MilestonePhaseDto(
                1,
                "Phase 1: Remediation (Micro-Lessons)",
                "Review core concept derivations, formula cheat sheets, and trap warnings for conceptual gaps.",
                2,
                p1Status,
                null,
                null,
                null,
                null,
                null,
                null,
                phase1Items
        ));

        // Phase 2: Adaptive Practice (Worksheets & Mini-Quiz)
        List<MilestoneItemDto> phase2Items = new ArrayList<>();
        itemIdx = 1;
        for (SubtopicClassificationResult item : applicationGaps) {
            Long subId = item.subtopicId() != null ? item.subtopicId() : deriveSubtopicId(item.subtopicName());
            phase2Items.add(new MilestoneItemDto(
                    "P2-" + (itemIdx++),
                    subId,
                    item.subtopicName(),
                    item.topicName(),
                    item.subjectName(),
                    PerformanceBracket.APPLICATION_GAP,
                    "PRACTICE_MEDIUM",
                    "Targeted Medium Practice: " + item.subtopicName(),
                    item.recommendationNote(),
                    30,
                    false,
                    null,
                    null
            ));
        }

        boolean trueMastery = conceptualGaps.isEmpty() && applicationGaps.isEmpty();

        String p2Status;
        if (trueMastery || prevPhase2Completed) {
            p2Status = "COMPLETED";
        } else if (!"COMPLETED".equals(p1Status)) {
            p2Status = "LOCKED";
        } else {
            p2Status = "IN_PROGRESS";
        }
        phases.add(new MilestonePhaseDto(
                2,
                "Phase 2: Adaptive Practice (Worksheets)",
                "Practice application sets with varied edge cases, elimination shortcuts, and time management.",
                1,
                p2Status,
                70.0,
                null,
                null,
                null,
                null,
                null,
                phase2Items
        ));

        // Phase 3: Speed Mastery & Gatekeeper (Sprints / Gatekeeper Test)
        List<MilestoneItemDto> phase3Items = new ArrayList<>();
        itemIdx = 1;
        for (SubtopicClassificationResult item : proficientTopics) {
            Long subId = item.subtopicId() != null ? item.subtopicId() : deriveSubtopicId(item.subtopicName());
            String actionType = item.slowPace() ? "TIMED_SPRINT" : "ADVANCED_DRILL";
            String title = (item.slowPace() ? "Timed Sprint: " : "Advanced Tier: ") + item.subtopicName();
            phase3Items.add(new MilestoneItemDto(
                    "P3-" + (itemIdx++),
                    subId,
                    item.subtopicName(),
                    item.topicName(),
                    item.subjectName(),
                    PerformanceBracket.PROFICIENT,
                    actionType,
                    title,
                    item.recommendationNote(),
                    25,
                    false,
                    null,
                    null
            ));
        }

        String p3Status;
        boolean p3Passed;
        if (trueMastery || prevPhase3Passed) {
            p3Status = "COMPLETED";
            p3Passed = true;
        } else if (!"COMPLETED".equals(p2Status)) {
            p3Status = "LOCKED";
            p3Passed = false;
        } else {
            p3Status = "IN_PROGRESS";
            p3Passed = false;
        }
        phases.add(new MilestonePhaseDto(
                3,
                "Phase 3: Speed Mastery & Gatekeeper",
                "Execute timed sprint drills to accelerate pacing and pass Gatekeeper test before mock unlock.",
                1,
                p3Status,
                null,
                null,
                null,
                75.0,
                null,
                p3Passed,
                phase3Items
        ));

        return phases;
    }

    @Transactional(readOnly = true)
    public Optional<DynamicLearningPathResponse> getCurrentActivePath(UUID candidateId) {
        return dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(candidateId, "ACTIVE")
                .or(() -> dlpRepository.findTopByCandidateIdOrderByCreatedAtDesc(candidateId))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LearningPathSummaryResponse getSummary(UUID candidateId) {
        Optional<DynamicLearningPath> activeOpt = dlpRepository.findFirstByCandidateIdAndStatusOrderByCreatedAtDesc(candidateId, "ACTIVE")
                .or(() -> dlpRepository.findTopByCandidateIdOrderByCreatedAtDesc(candidateId));

        List<LearnerSubtopicPerformance> performances = subtopicRepository.findByCandidateId(candidateId);
        int total = performances.size();
        int conceptual = 0;
        int application = 0;
        int proficient = 0;

        Map<String, List<LearnerSubtopicPerformance>> subjectGroups = new HashMap<>();
        for (LearnerSubtopicPerformance p : performances) {
            String subject = (p.getSubjectName() != null && !p.getSubjectName().isBlank())
                    ? p.getSubjectName() : "General";
            subjectGroups.computeIfAbsent(subject, k -> new ArrayList<>()).add(p);

            SubtopicClassificationResult cr = classifierService.classify(p);
            if (cr.bracket() == PerformanceBracket.CONCEPTUAL_GAP) conceptual++;
            else if (cr.bracket() == PerformanceBracket.APPLICATION_GAP) application++;
            else proficient++;
        }

        Map<String, SubjectMasterySummaryDto> subjectSummaries = new HashMap<>();
        for (Map.Entry<String, List<LearnerSubtopicPerformance>> entry : subjectGroups.entrySet()) {
            String subject = entry.getKey();
            List<LearnerSubtopicPerformance> subList = entry.getValue();
            int subConcept = 0;
            int subApp = 0;
            int subProf = 0;
            double totalAcc = 0.0;
            for (LearnerSubtopicPerformance sp : subList) {
                totalAcc += sp.getAvgAccuracyPercent();
                SubtopicClassificationResult scr = classifierService.classify(sp);
                if (scr.bracket() == PerformanceBracket.CONCEPTUAL_GAP) subConcept++;
                else if (scr.bracket() == PerformanceBracket.APPLICATION_GAP) subApp++;
                else subProf++;
            }
            double avgAcc = subList.isEmpty() ? 0.0 : Math.round((totalAcc / subList.size()) * 10.0) / 10.0;
            subjectSummaries.put(subject, new SubjectMasterySummaryDto(
                    subject,
                    subList.size(),
                    subConcept,
                    subApp,
                    subProf,
                    avgAcc
            ));
        }

        double mastery = 0.0;
        if (total > 0) {
            mastery = Math.round(((proficient * 1.0 + application * 0.5) / total) * 1000.0) / 10.0;
        }

        return new LearningPathSummaryResponse(
                candidateId,
                activeOpt.map(DynamicLearningPath::getId).orElse(null),
                activeOpt.map(DynamicLearningPath::getExamTarget).orElse(DEFAULT_EXAM_TARGET),
                activeOpt.map(DynamicLearningPath::getStatus).orElse("NONE"),
                total,
                conceptual,
                application,
                proficient,
                mastery,
                subjectSummaries
        );
    }

    public DynamicLearningPathResponse toResponse(DynamicLearningPath entity) {
        MilestoneGraphDto graph = null;
        if (entity.getMilestoneGraphJson() != null && !entity.getMilestoneGraphJson().isBlank()) {
            try {
                graph = objectMapper.readValue(entity.getMilestoneGraphJson(), MilestoneGraphDto.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse milestoneGraphJson for DLP {}: {}", entity.getId(), e.getMessage());
            }
        }

        if (graph == null) {
            graph = new MilestoneGraphDto(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    0.0
            );
        }

        return new DynamicLearningPathResponse(
                entity.getId(),
                entity.getCandidateId(),
                entity.getSourceSessionId(),
                entity.getExamTarget(),
                entity.getStatus(),
                entity.getConceptualGapsCount(),
                entity.getApplicationGapsCount(),
                entity.getProficientCount(),
                graph,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
