// SPDX-License-Identifier: AGPL-3.0-only
package com.examplatform.recommendation.service;

import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.domain.LearnerSubtopicPerformance;
import com.examplatform.recommendation.domain.PerformanceBracket;
import com.examplatform.recommendation.dto.DynamicLearningPathResponse;
import com.examplatform.recommendation.dto.LearningPathSummaryResponse;
import com.examplatform.recommendation.dto.MilestoneGraphDto;
import com.examplatform.recommendation.dto.MilestoneItemDto;
import com.examplatform.recommendation.dto.MilestonePhaseDto;
import com.examplatform.recommendation.dto.SubjectMasterySummaryDto;
import com.examplatform.recommendation.dto.SubtopicClassificationResult;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicPerformanceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningPathGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(LearningPathGeneratorService.class);
    public static final String DEFAULT_EXAM_TARGET = "GENERAL_COMPETITIVE";

    private final SubtopicClassifierService classifierService;
    private final LearnerSubtopicPerformanceRepository subtopicRepository;
    private final DynamicLearningPathRepository dlpRepository;
    private final ObjectMapper objectMapper;

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

        List<LearnerSubtopicPerformance> performances = subtopicRepository.findByCandidateId(candidateId);

        List<SubtopicClassificationResult> conceptualGaps = new ArrayList<>();
        List<SubtopicClassificationResult> applicationGaps = new ArrayList<>();
        List<SubtopicClassificationResult> proficientTopics = new ArrayList<>();

        for (LearnerSubtopicPerformance perf : performances) {
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
        List<MilestonePhaseDto> phases = assembleMilestonePhases(conceptualGaps, applicationGaps, proficientTopics);

        MilestoneGraphDto milestoneGraph = new MilestoneGraphDto(
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
        List<DynamicLearningPath> existingActive = dlpRepository.findByCandidateIdAndStatus(candidateId, "ACTIVE");
        for (DynamicLearningPath oldPath : existingActive) {
            oldPath.setStatus("SUPERSEDED");
            dlpRepository.save(oldPath);
        }

        // Create new active DLP
        DynamicLearningPath dlp = DynamicLearningPath.builder()
                .candidateId(candidateId)
                .sourceSessionId(sourceSessionId)
                .examTarget(resolvedTarget)
                .status("ACTIVE")
                .conceptualGapsCount(conceptualGaps.size())
                .applicationGapsCount(applicationGaps.size())
                .proficientCount(proficientTopics.size())
                .milestoneGraphJson(milestoneGraphJson)
                .build();

        return dlpRepository.save(dlp);
    }

    private List<MilestonePhaseDto> assembleMilestonePhases(
            List<SubtopicClassificationResult> conceptualGaps,
            List<SubtopicClassificationResult> applicationGaps,
            List<SubtopicClassificationResult> proficientTopics) {

        List<MilestonePhaseDto> phases = new ArrayList<>();

        // Phase 1: Remediation (Study Micro-Lessons)
        List<MilestoneItemDto> phase1Items = new ArrayList<>();
        int itemIdx = 1;
        for (SubtopicClassificationResult item : conceptualGaps) {
            phase1Items.add(new MilestoneItemDto(
                    "P1-" + (itemIdx++),
                    item.subtopicName(),
                    item.topicName(),
                    item.subjectName(),
                    PerformanceBracket.CONCEPTUAL_GAP,
                    "MICRO_LESSON",
                    "Remedial Micro-Lesson: " + item.subtopicName(),
                    item.recommendationNote(),
                    20,
                    false
            ));
        }
        String p1Status = conceptualGaps.isEmpty() ? "COMPLETED" : "IN_PROGRESS";
        phases.add(new MilestonePhaseDto(
                1,
                "Phase 1: Remediation (Micro-Lessons)",
                "Review core concept derivations, formula cheat sheets, and trap warnings for conceptual gaps.",
                p1Status,
                phase1Items
        ));

        // Phase 2: Adaptive Practice (Medium Practice Sets)
        List<MilestoneItemDto> phase2Items = new ArrayList<>();
        itemIdx = 1;
        for (SubtopicClassificationResult item : applicationGaps) {
            phase2Items.add(new MilestoneItemDto(
                    "P2-" + (itemIdx++),
                    item.subtopicName(),
                    item.topicName(),
                    item.subjectName(),
                    PerformanceBracket.APPLICATION_GAP,
                    "PRACTICE_MEDIUM",
                    "Targeted Medium Practice: " + item.subtopicName(),
                    item.recommendationNote(),
                    30,
                    false
            ));
        }
        String p2Status;
        if (!conceptualGaps.isEmpty()) {
            p2Status = "LOCKED";
        } else if (applicationGaps.isEmpty()) {
            p2Status = "COMPLETED";
        } else {
            p2Status = "IN_PROGRESS";
        }
        phases.add(new MilestonePhaseDto(
                2,
                "Phase 2: Adaptive Practice (Worksheets)",
                "Practice application sets with varied edge cases, elimination shortcuts, and time management.",
                p2Status,
                phase2Items
        ));

        // Phase 3: Speed Mastery & Gatekeeper (Sprints / Advanced Multi-Correct)
        List<MilestoneItemDto> phase3Items = new ArrayList<>();
        itemIdx = 1;
        for (SubtopicClassificationResult item : proficientTopics) {
            String actionType = item.slowPace() ? "TIMED_SPRINT" : "ADVANCED_DRILL";
            String title = (item.slowPace() ? "Timed Sprint: " : "Advanced Tier: ") + item.subtopicName();
            phase3Items.add(new MilestoneItemDto(
                    "P3-" + (itemIdx++),
                    item.subtopicName(),
                    item.topicName(),
                    item.subjectName(),
                    PerformanceBracket.PROFICIENT,
                    actionType,
                    title,
                    item.recommendationNote(),
                    25,
                    false
            ));
        }
        String p3Status;
        if (!conceptualGaps.isEmpty() || !applicationGaps.isEmpty()) {
            p3Status = "LOCKED";
        } else if (proficientTopics.isEmpty()) {
            p3Status = "COMPLETED";
        } else {
            p3Status = "IN_PROGRESS";
        }
        phases.add(new MilestonePhaseDto(
                3,
                "Phase 3: Speed Mastery & Gatekeeper",
                "Execute timed sprint drills to accelerate pacing and tackle multi-assertion advanced questions.",
                p3Status,
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

    public DynamicLearningPathResponse toResponse(DynamicLearningPath dlp) {
        MilestoneGraphDto graph = null;
        if (dlp.getMilestoneGraphJson() != null && !dlp.getMilestoneGraphJson().isBlank()) {
            try {
                graph = objectMapper.readValue(dlp.getMilestoneGraphJson(), MilestoneGraphDto.class);
            } catch (JsonProcessingException e) {
                log.warn("Failed to deserialize milestone graph for DLP {}: {}", dlp.getId(), e.getMessage());
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
                dlp.getId(),
                dlp.getCandidateId(),
                dlp.getSourceSessionId(),
                dlp.getExamTarget(),
                dlp.getStatus(),
                dlp.getConceptualGapsCount(),
                dlp.getApplicationGapsCount(),
                dlp.getProficientCount(),
                graph,
                dlp.getCreatedAt(),
                dlp.getUpdatedAt()
        );
    }
}
