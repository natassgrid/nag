/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * NextGen Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2026 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 */

package com.examplatform.recommendation.service;

import com.examplatform.recommendation.client.ExamSyllabusClient;
import com.examplatform.recommendation.client.ExamSyllabusNodeDto;
import com.examplatform.recommendation.client.MicroLessonDto;
import com.examplatform.recommendation.client.QuestionBankContentClient;
import com.examplatform.recommendation.domain.DynamicLearningPath;
import com.examplatform.recommendation.domain.LearnerLessonProgress;
import com.examplatform.recommendation.domain.LearnerSubtopicState;
import com.examplatform.recommendation.dto.*;
import com.examplatform.recommendation.mastery.MasteryModel;
import com.examplatform.recommendation.mastery.MasteryUpdateResult;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.examplatform.recommendation.repository.LearnerLessonProgressRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicStateRepository;
import com.examplatform.shared.constants.SharedConstants;
import com.examplatform.shared.error.ResourceNotFoundException;
import com.examplatform.shared.taxonomy.TaxonomyClient;
import com.examplatform.shared.taxonomy.TaxonomyNodeType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class NextBestActivityEngine {

    private final LearnerSubtopicStateRepository subtopicStateRepository;
    private final LearnerLessonProgressRepository lessonProgressRepository;
    private final DynamicLearningPathRepository dlpRepository;
    private final ExamSyllabusClient syllabusClient;
    private final QuestionBankContentClient questionBankContentClient;
    private final MasteryModel masteryModel;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private TaxonomyClient taxonomyClient;

    /**
     * Computes the next-best-activity for a learner enrollment based on:
     * - Spaced repetition review due triggers (REVISION)
     * - Unseen conceptual lessons for low mastery subtopics (LESSON)
     * - Alternating lesson styles upon repeated practice failure (LESSON)
     * - Procedure drills for subtopics with foundational knowledge (PRACTICE)
     * - Section mocks when overall readiness is high (MOCK_SECTION)
     */
    @Transactional
    public NextActivityResponse determineNextActivity(UUID enrollmentId) {
        List<LearnerSubtopicState> states = subtopicStateRepository.findByEnrollmentId(enrollmentId);

        // Cold-start calibration: if no subtopic states exist yet for this enrollment, seed them from exam syllabus
        if (states.isEmpty()) {
            states = initializeSubtopicStates(enrollmentId, null);
        }

        UUID examId = states.getFirst().getExaminationId();
        Map<Long, Double> weights = syllabusClient.getBlueprintWeights(examId);

        Instant now = Instant.now();
        List<LearnerLessonProgress> progressList = lessonProgressRepository.findByEnrollmentId(enrollmentId);
        Set<UUID> completedLessonIds = new HashSet<>();
        Map<Long, Set<String>> completedLessonTypesBySubtopic = new HashMap<>();

        for (LearnerLessonProgress p : progressList) {
            completedLessonIds.add(p.getLessonId());
            if (p.getLessonType() != null) {
                completedLessonTypesBySubtopic
                        .computeIfAbsent(p.getSubtopicId(), k -> new HashSet<>())
                        .add(p.getLessonType().toUpperCase());
            }
        }

        // 1. Spaced Repetition Priority Check (REVISION activity)
        Optional<LearnerSubtopicState> reviewDueSubtopic = states.stream()
                .filter(s -> s.getNextReviewAt() != null && now.isAfter(s.getNextReviewAt()) && s.getMasteryScore() >= 0.40)
                .max(Comparator.comparingDouble(s -> weights.getOrDefault(s.getSubtopicId(), 0.10) * (1.0 - s.getMasteryScore())));

        if (reviewDueSubtopic.isPresent()) {
            LearnerSubtopicState target = reviewDueSubtopic.get();
            double priority = weights.getOrDefault(target.getSubtopicId(), 0.10) * (1.0 - target.getMasteryScore()) * 1.5;
            String subtopicName = resolveSubtopicName(target.getSubtopicId());
            String topicName = resolveTopicName(target.getTopicId());
            String subjectName = resolveSubjectName(target.getSubjectId());

            return new NextActivityResponse(
                    UUID.randomUUID(),
                    "REVISION",
                    target.getSubjectId(),
                    subjectName,
                    target.getTopicId(),
                    topicName,
                    target.getSubtopicId(),
                    subtopicName,
                    null,
                    null,
                    "Spaced Repetition Review Drill",
                    null,
                    Math.round(priority * 1000.0) / 1000.0,
                    "Scheduled spaced repetition review is due to solidify retention."
            );
        }

        // 2. Rank syllabus subtopics by: priority = blueprint weight x (1 - mastery)
        List<SubtopicPriority> ranked = new ArrayList<>();
        for (LearnerSubtopicState s : states) {
            double w = weights.getOrDefault(s.getSubtopicId(), 0.10);
            double priority = w * (1.0 - s.getMasteryScore());
            ranked.add(new SubtopicPriority(s, priority));
        }
        ranked.sort(Comparator.comparingDouble(SubtopicPriority::priority).reversed());

        double overallReadiness = computeReadiness(states, weights);
        updateDlpReadiness(enrollmentId, states.getFirst().getCandidateId(), overallReadiness);

        // If overall readiness is advanced (>= 0.75) and all high-weight subtopics have mastery >= 0.70, suggest section mock
        if (overallReadiness >= 0.75 && ranked.getFirst().priority() < 0.10) {
            LearnerSubtopicState top = ranked.getFirst().state();
            return new NextActivityResponse(
                    UUID.randomUUID(),
                    "MOCK_SECTION",
                    top.getSubjectId(),
                    resolveSubjectName(top.getSubjectId()),
                    top.getTopicId(),
                    resolveTopicName(top.getTopicId()),
                    top.getSubtopicId(),
                    resolveSubtopicName(top.getSubtopicId()),
                    null,
                    null,
                    "High-Readiness Section Simulation",
                    null,
                    0.95,
                    "Comprehensive syllabus mastery achieved; section simulation assesses timing and stamina."
            );
        }

        // 3. For the highest priority deficit subtopic, determine whether to prescribe LESSON or PRACTICE
        SubtopicPriority highestDeficit = ranked.getFirst();
        LearnerSubtopicState target = highestDeficit.state();
        Long subtopicId = target.getSubtopicId();
        Set<String> seenTypes = completedLessonTypesBySubtopic.getOrDefault(subtopicId, Collections.emptySet());

        List<MicroLessonDto> availableLessons = questionBankContentClient.getLessonsForSubtopic(subtopicId);

        // If learner has never seen a lesson for this subtopic: serve foundational CONCEPT lesson
        if (seenTypes.isEmpty()) {
            MicroLessonDto nextLesson = availableLessons.stream()
                    .filter(l -> !completedLessonIds.contains(l.id()))
                    .filter(l -> "CONCEPT".equalsIgnoreCase(l.lessonType()) || "FOUNDATION".equalsIgnoreCase(l.difficultyLevel()))
                    .findFirst()
                    .orElseGet(() -> availableLessons.getFirst());

            return buildLessonResponse(target, nextLesson, highestDeficit.priority(),
                    "Initial conceptual foundation required before attempting questions.");
        }

        // If learner attempted practice but struggled (attempts > 0 and mastery < 0.50):
        // "choose different lesson type after failure; skip completed lessons"
        if (target.getAttempts() > 0 && target.getMasteryScore() < 0.50) {
            Optional<MicroLessonDto> alternateLesson = availableLessons.stream()
                    .filter(l -> !completedLessonIds.contains(l.id()))
                    .filter(l -> !seenTypes.contains(l.lessonType().toUpperCase()))
                    .findFirst();

            if (alternateLesson.isPresent()) {
                MicroLessonDto alt = alternateLesson.get();
                return buildLessonResponse(target, alt, highestDeficit.priority() * 1.2,
                        "Recent practice indicated friction; serving alternative " + alt.lessonType() + " pedagogical approach.");
            }
        }

        // If foundational lessons completed and mastery is in learning/practice phase: serve adaptive PRACTICE
        return new NextActivityResponse(
                UUID.randomUUID(),
                "PRACTICE",
                target.getSubjectId(),
                resolveSubjectName(target.getSubjectId()),
                target.getTopicId(),
                resolveTopicName(target.getTopicId()),
                target.getSubtopicId(),
                resolveSubtopicName(target.getSubtopicId()),
                null,
                null,
                null,
                null,
                Math.round(highestDeficit.priority() * 1000.0) / 1000.0,
                "Focused adaptive drill to build speed, accuracy, and confidence on this high-yield subtopic."
        );
    }

    /**
     * Processes activity completion and updates mastery state and readiness score.
     */
    @Transactional
    public ActivityCompletionResponse completeActivity(
            UUID enrollmentId, UUID activityId, CompleteActivityRequest req) {

        LearnerSubtopicState state = subtopicStateRepository.findByEnrollmentIdAndSubtopicId(enrollmentId, req.subtopicId())
                .orElseGet(() -> {
                    // Initialize if missing
                    LearnerSubtopicState s = LearnerSubtopicState.builder()
                            .enrollmentId(enrollmentId)
                            .candidateId(UUID.randomUUID())
                            .subjectId(1L)
                            .topicId(10L)
                            .subtopicId(req.subtopicId())
                            .status("NOT_STARTED")
                            .build();
                    s.setTenantId(SharedConstants.DEFAULT_TENANT);
                    return subtopicStateRepository.save(s);
                });

        Instant now = Instant.now();
        String actType = req.activityType() != null ? req.activityType().toUpperCase() : "PRACTICE";

        if ("LESSON".equalsIgnoreCase(actType)) {
            UUID lessonId = (req.lessonId() != null) ? req.lessonId() : UUID.randomUUID();
            LearnerLessonProgress progress = LearnerLessonProgress.builder()
                    .enrollmentId(enrollmentId)
                    .candidateId(state.getCandidateId())
                    .lessonId(lessonId)
                    .subtopicId(req.subtopicId())
                    .lessonType(req.lessonType() != null ? req.lessonType() : "CONCEPT")
                    .status("COMPLETED")
                    .helpfulRating(req.helpfulRating())
                    .completedAt(now)
                    .build();
            progress.setTenantId(state.getTenantId());
            lessonProgressRepository.save(progress);

            MasteryUpdateResult result = masteryModel.onLessonCompleted(
                    state.getMasteryScore(), state.getConfidence(), state.getAttempts(), state.getCorrect(), now
            );

            state.setMasteryScore(result.newMasteryScore());
            state.setConfidence(result.newConfidence());
            state.setStatus(result.status());
            state.setNextReviewAt(result.nextReviewAt());
            if (Boolean.TRUE.equals(req.helpfulRating()) && req.lessonType() != null) {
                state.setPreferredLessonType(req.lessonType());
            }
        } else {
            // PRACTICE | REVISION | MOCK_SECTION
            int questions = req.totalQuestions() != null ? req.totalQuestions() : 10;
            int correct = req.correctCount() != null ? req.correctCount() : 7;

            MasteryUpdateResult result = masteryModel.onPracticeCompleted(
                    state.getMasteryScore(), state.getConfidence(), state.getAttempts(), state.getCorrect(),
                    questions, correct, now
            );

            state.setMasteryScore(result.newMasteryScore());
            state.setConfidence(result.newConfidence());
            state.setAttempts(result.totalAttempts());
            state.setCorrect(result.totalCorrect());
            state.setStatus(result.status());
            state.setNextReviewAt(result.nextReviewAt());
            state.setLastPracticedAt(now);
        }

        LearnerSubtopicState saved = subtopicStateRepository.save(state);

        // Recalculate enrollment-wide readiness score
        List<LearnerSubtopicState> allStates = subtopicStateRepository.findByEnrollmentId(enrollmentId);
        Map<Long, Double> weights = syllabusClient.getBlueprintWeights(saved.getExaminationId());
        double updatedReadiness = computeReadiness(allStates, weights);
        updateDlpReadiness(enrollmentId, saved.getCandidateId(), updatedReadiness);

        log.info("Activity {} completed for enrollment {}, subtopic {}: mastery={}, status={}, readiness={}",
                activityId, enrollmentId, req.subtopicId(), saved.getMasteryScore(), saved.getStatus(), updatedReadiness);

        return new ActivityCompletionResponse(
                activityId,
                actType,
                saved.getSubtopicId(),
                saved.getMasteryScore(),
                saved.getConfidence(),
                saved.getStatus(),
                saved.getNextReviewAt(),
                updatedReadiness
        );
    }

    /**
     * Summarizes candidate learning progress, readiness score, and syllabus mastery distribution.
     */
    @Transactional(readOnly = true)
    public LearnerProgressResponse getProgress(UUID enrollmentId) {
        List<LearnerSubtopicState> states = subtopicStateRepository.findByEnrollmentId(enrollmentId);
        if (states.isEmpty()) {
            return new LearnerProgressResponse(
                    enrollmentId, null, null, 0.0, 0, 0, 0, 0, Collections.emptyList()
            );
        }

        UUID candidateId = states.getFirst().getCandidateId();
        UUID examId = states.getFirst().getExaminationId();
        Map<Long, Double> weights = syllabusClient.getBlueprintWeights(examId);

        Instant now = Instant.now();
        int totalSubtopics = states.size();
        int masteredCount = 0;
        int inProgressCount = 0;
        int dueRevisionsCount = 0;

        List<SubtopicProgressDto> subtopicDtos = new ArrayList<>();
        for (LearnerSubtopicState s : states) {
            boolean isReviewDue = s.getNextReviewAt() != null && now.isAfter(s.getNextReviewAt()) && s.getMasteryScore() >= 0.40;
            if (isReviewDue) dueRevisionsCount++;

            if (s.getMasteryScore() >= 0.80 || "MASTERED".equalsIgnoreCase(s.getStatus())) {
                masteredCount++;
            } else if ("LEARNING".equalsIgnoreCase(s.getStatus()) || "PRACTISING".equalsIgnoreCase(s.getStatus()) || s.getAttempts() > 0) {
                inProgressCount++;
            }

            subtopicDtos.add(new SubtopicProgressDto(
                    s.getSubjectId(),
                    resolveSubjectName(s.getSubjectId()),
                    s.getTopicId(),
                    resolveTopicName(s.getTopicId()),
                    s.getSubtopicId(),
                    resolveSubtopicName(s.getSubtopicId()),
                    s.getMasteryScore(),
                    s.getConfidence(),
                    s.getAttempts(),
                    s.getCorrect(),
                    isReviewDue ? "REVIEW_DUE" : s.getStatus(),
                    s.getNextReviewAt(),
                    isReviewDue,
                    Math.round(weights.getOrDefault(s.getSubtopicId(), 0.10) * 1000.0) / 1000.0
            ));
        }

        double readiness = computeReadiness(states, weights);

        return new LearnerProgressResponse(
                enrollmentId,
                candidateId,
                examId,
                readiness,
                totalSubtopics,
                masteredCount,
                inProgressCount,
                dueRevisionsCount,
                subtopicDtos
        );
    }

    // ── Internal Helpers ─────────────────────────────────────────────────

    private double computeReadiness(List<LearnerSubtopicState> states, Map<Long, Double> weights) {
        if (states.isEmpty()) return 0.0;
        double score = 0.0;
        for (LearnerSubtopicState s : states) {
            double w = weights.getOrDefault(s.getSubtopicId(), 0.10);
            score += w * s.getMasteryScore();
        }
        return Math.round(Math.clamp(score, 0.0, 1.0) * 1000.0) / 1000.0;
    }

    private void updateDlpReadiness(UUID enrollmentId, UUID candidateId, double readiness) {
        Optional<DynamicLearningPath> dlpOpt = dlpRepository.findByEnrollmentId(enrollmentId);
        if (dlpOpt.isPresent()) {
            DynamicLearningPath dlp = dlpOpt.get();
            dlp.setReadinessScore(readiness);
            dlpRepository.save(dlp);
        } else if (candidateId != null) {
            DynamicLearningPath newDlp = DynamicLearningPath.builder()
                    .candidateId(candidateId)
                    .enrollmentId(enrollmentId)
                    .readinessScore(readiness)
                    .status("ACTIVE")
                    .milestoneGraphJson("{}")
                    .build();
            newDlp.setTenantId(SharedConstants.DEFAULT_TENANT);
            dlpRepository.save(newDlp);
        }
    }

    private List<LearnerSubtopicState> initializeSubtopicStates(UUID enrollmentId, UUID candidateId) {
        UUID effectiveCandidateId = candidateId != null ? candidateId : UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID defaultExamId = UUID.fromString("11111111-2222-3333-4444-555555555551");

        List<ExamSyllabusNodeDto> syllabus = syllabusClient.getSyllabusNodes(defaultExamId);
        List<LearnerSubtopicState> created = new ArrayList<>();

        for (ExamSyllabusNodeDto node : syllabus) {
            LearnerSubtopicState state = LearnerSubtopicState.builder()
                    .enrollmentId(enrollmentId)
                    .candidateId(effectiveCandidateId)
                    .examinationId(defaultExamId)
                    .subjectId(node.subjectId())
                    .topicId(node.topicId())
                    .subtopicId(node.subtopicId())
                    .masteryScore(0.0)
                    .confidence(0.1)
                    .attempts(0)
                    .correct(0)
                    .status("NOT_STARTED")
                    .build();
            state.setTenantId(SharedConstants.DEFAULT_TENANT);
            created.add(subtopicStateRepository.save(state));
        }

        return created;
    }

    private NextActivityResponse buildLessonResponse(
            LearnerSubtopicState target, MicroLessonDto lesson, double priority, String reason) {
        return new NextActivityResponse(
                UUID.randomUUID(),
                "LESSON",
                target.getSubjectId(),
                resolveSubjectName(target.getSubjectId()),
                target.getTopicId(),
                resolveTopicName(target.getTopicId()),
                target.getSubtopicId(),
                resolveSubtopicName(target.getSubtopicId()),
                lesson.id(),
                lesson.lessonType(),
                lesson.title(),
                null,
                Math.round(priority * 1000.0) / 1000.0,
                reason
        );
    }

    private String resolveSubtopicName(Long id) {
        if (taxonomyClient != null && id != null) {
            return taxonomyClient.resolveName(TaxonomyNodeType.SUBTOPIC, id).orElse("Subtopic " + id);
        }
        return "Subtopic " + id;
    }

    private String resolveTopicName(Long id) {
        if (taxonomyClient != null && id != null) {
            return taxonomyClient.resolveName(TaxonomyNodeType.TOPIC, id).orElse("Topic " + id);
        }
        return "Topic " + id;
    }

    private String resolveSubjectName(Long id) {
        if (taxonomyClient != null && id != null) {
            return taxonomyClient.resolveName(TaxonomyNodeType.SUBJECT, id).orElse("Subject " + id);
        }
        return "Subject " + id;
    }

    private record SubtopicPriority(LearnerSubtopicState state, double priority) {}
}
