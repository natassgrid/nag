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
import com.examplatform.recommendation.dto.ActivityCompletionResponse;
import com.examplatform.recommendation.dto.CompleteActivityRequest;
import com.examplatform.recommendation.dto.LearnerProgressResponse;
import com.examplatform.recommendation.dto.NextActivityResponse;
import com.examplatform.recommendation.mastery.BktLiteMasteryModel;
import com.examplatform.recommendation.mastery.MasteryModel;
import com.examplatform.recommendation.repository.DynamicLearningPathRepository;
import com.examplatform.recommendation.repository.LearnerLessonProgressRepository;
import com.examplatform.recommendation.repository.LearnerSubtopicStateRepository;
import com.examplatform.shared.constants.SharedConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NextBestActivityEngine Adaptive Learning Loop Tests")
class NextBestActivityEngineTest {

    @Mock
    private LearnerSubtopicStateRepository subtopicStateRepository;

    @Mock
    private LearnerLessonProgressRepository lessonProgressRepository;

    @Mock
    private DynamicLearningPathRepository dlpRepository;

    @Mock
    private ExamSyllabusClient syllabusClient;

    @Mock
    private QuestionBankContentClient questionBankContentClient;

    private MasteryModel masteryModel;
    private ObjectMapper objectMapper;
    private NextBestActivityEngine engine;

    private final UUID enrollmentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final UUID candidateId = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private final UUID examId = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @BeforeEach
    void setUp() {
        masteryModel = new BktLiteMasteryModel();
        objectMapper = new ObjectMapper();
        engine = new NextBestActivityEngine(
                subtopicStateRepository,
                lessonProgressRepository,
                dlpRepository,
                syllabusClient,
                questionBankContentClient,
                masteryModel,
                objectMapper
        );
    }

    @Test
    @DisplayName("Spaced repetition review takes priority when next_review_at is in the past (REVISION)")
    void shouldRecommendRevisionWhenSpacedReviewIsDue() {
        Instant past = Instant.now().minus(Duration.ofDays(1));
        LearnerSubtopicState reviewDue = LearnerSubtopicState.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(examId)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(101L)
                .masteryScore(0.70)
                .confidence(0.6)
                .attempts(10)
                .nextReviewAt(past)
                .status("REVIEW_DUE")
                .build();

        when(subtopicStateRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(reviewDue));
        when(syllabusClient.getBlueprintWeights(examId)).thenReturn(Map.of(101L, 0.40));
        when(lessonProgressRepository.findByEnrollmentId(enrollmentId)).thenReturn(Collections.emptyList());

        NextActivityResponse response = engine.determineNextActivity(enrollmentId);

        assertThat(response.activityType()).isEqualTo("REVISION");
        assertThat(response.subtopicId()).isEqualTo(101L);
        assertThat(response.reason()).contains("spaced repetition");
    }

    @Test
    @DisplayName("Unseen subtopic with no completed lessons triggers foundational LESSON")
    void shouldRecommendLessonWhenNoLessonSeenYet() {
        LearnerSubtopicState fresh = LearnerSubtopicState.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(examId)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(101L)
                .masteryScore(0.0)
                .confidence(0.1)
                .attempts(0)
                .status("NOT_STARTED")
                .build();

        when(subtopicStateRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(fresh));
        when(syllabusClient.getBlueprintWeights(examId)).thenReturn(Map.of(101L, 0.50));
        when(lessonProgressRepository.findByEnrollmentId(enrollmentId)).thenReturn(Collections.emptyList());

        UUID lessonId = UUID.randomUUID();
        MicroLessonDto conceptLesson = new MicroLessonDto(
                lessonId, 101L, "CONCEPT", "FOUNDATION", 1, "Core Concept Primer", 5
        );
        when(questionBankContentClient.getLessonsForSubtopic(101L)).thenReturn(List.of(conceptLesson));

        NextActivityResponse response = engine.determineNextActivity(enrollmentId);

        assertThat(response.activityType()).isEqualTo("LESSON");
        assertThat(response.subtopicId()).isEqualTo(101L);
        assertThat(response.lessonId()).isEqualTo(lessonId);
        assertThat(response.lessonType()).isEqualTo("CONCEPT");
    }

    @Test
    @DisplayName("Struggling learner after failed practice receives an ALTERNATE lesson type (not seen before)")
    void shouldRecommendAlternativeLessonTypeAfterFailure() {
        LearnerSubtopicState struggling = LearnerSubtopicState.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(examId)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(101L)
                .masteryScore(0.35) // low mastery
                .confidence(0.3)
                .attempts(10) // already attempted questions and struggled
                .status("LEARNING")
                .build();

        when(subtopicStateRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(struggling));
        when(syllabusClient.getBlueprintWeights(examId)).thenReturn(Map.of(101L, 0.50));

        UUID conceptLessonId = UUID.randomUUID();
        LearnerLessonProgress seenConcept = LearnerLessonProgress.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .lessonId(conceptLessonId)
                .subtopicId(101L)
                .lessonType("CONCEPT")
                .status(SharedConstants.COMPLETED)
                .build();
        when(lessonProgressRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(seenConcept));

        UUID workedLessonId = UUID.randomUUID();
        MicroLessonDto seenDto = new MicroLessonDto(conceptLessonId, 101L, "CONCEPT", "FOUNDATION", 1, "Concept", 5);
        MicroLessonDto alternateDto = new MicroLessonDto(workedLessonId, 101L, "WORKED_EXAMPLES", "CORE", 2, "Worked Examples", 7);
        when(questionBankContentClient.getLessonsForSubtopic(101L)).thenReturn(List.of(seenDto, alternateDto));

        NextActivityResponse response = engine.determineNextActivity(enrollmentId);

        assertThat(response.activityType()).isEqualTo("LESSON");
        assertThat(response.lessonType()).isEqualTo("WORKED_EXAMPLES");
        assertThat(response.lessonId()).isEqualTo(workedLessonId);
        assertThat(response.reason()).contains("friction");
    }

    @Test
    @DisplayName("Learner with completed concept lesson and ready mastery receives PRACTICE drill")
    void shouldRecommendPracticeWhenLessonCompletedAndReady() {
        LearnerSubtopicState ready = LearnerSubtopicState.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(examId)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(101L)
                .masteryScore(0.55) // solid foundation
                .confidence(0.4)
                .attempts(2)
                .status("PRACTISING")
                .build();

        when(subtopicStateRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(ready));
        when(syllabusClient.getBlueprintWeights(examId)).thenReturn(Map.of(101L, 0.40));

        UUID conceptLessonId = UUID.randomUUID();
        LearnerLessonProgress seenConcept = LearnerLessonProgress.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .lessonId(conceptLessonId)
                .subtopicId(101L)
                .lessonType("CONCEPT")
                .status(SharedConstants.COMPLETED)
                .build();
        when(lessonProgressRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(seenConcept));

        NextActivityResponse response = engine.determineNextActivity(enrollmentId);

        assertThat(response.activityType()).isEqualTo("PRACTICE");
        assertThat(response.subtopicId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("Completing practice activity updates subtopic mastery and enrollment readiness")
    void shouldCompletePracticeActivityAndUpdateMastery() {
        LearnerSubtopicState state = LearnerSubtopicState.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(examId)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(101L)
                .masteryScore(0.40)
                .confidence(0.2)
                .attempts(5)
                .correct(2)
                .status("LEARNING")
                .build();

        when(subtopicStateRepository.findByEnrollmentIdAndSubtopicId(enrollmentId, 101L))
                .thenReturn(Optional.of(state));
        when(subtopicStateRepository.save(any(LearnerSubtopicState.class))).thenAnswer(i -> i.getArgument(0));
        when(subtopicStateRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(state));
        when(syllabusClient.getBlueprintWeights(examId)).thenReturn(Map.of(101L, 1.0));

        CompleteActivityRequest req = new CompleteActivityRequest(
                "PRACTICE", 101L, null, null, null, 10, 9, 120000L
        );

        UUID activityId = UUID.randomUUID();
        ActivityCompletionResponse response = engine.completeActivity(enrollmentId, activityId, req);

        assertThat(response.activityId()).isEqualTo(activityId);
        assertThat(response.newMasteryScore()).isGreaterThan(0.40);
        assertThat(response.newConfidence()).isGreaterThan(0.20);
        assertThat(response.updatedReadinessScore()).isGreaterThan(0.40);
    }

    @Test
    @DisplayName("getProgress compiles comprehensive syllabus mastery distribution")
    void shouldGetProgressDistribution() {
        LearnerSubtopicState s1 = LearnerSubtopicState.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(examId)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(101L)
                .masteryScore(0.85)
                .confidence(0.8)
                .attempts(25)
                .status("MASTERED")
                .build();

        LearnerSubtopicState s2 = LearnerSubtopicState.builder()
                .enrollmentId(enrollmentId)
                .candidateId(candidateId)
                .examinationId(examId)
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(102L)
                .masteryScore(0.45)
                .confidence(0.3)
                .attempts(5)
                .status("LEARNING")
                .build();

        when(subtopicStateRepository.findByEnrollmentId(enrollmentId)).thenReturn(List.of(s1, s2));
        when(syllabusClient.getBlueprintWeights(examId)).thenReturn(Map.of(101L, 0.60, 102L, 0.40));

        LearnerProgressResponse progress = engine.getProgress(enrollmentId);

        assertThat(progress.totalSubtopics()).isEqualTo(2);
        assertThat(progress.masteredSubtopics()).isEqualTo(1);
        assertThat(progress.inProgressSubtopics()).isEqualTo(1);
        // readiness = 0.60 * 0.85 + 0.40 * 0.45 = 0.51 + 0.18 = 0.69
        assertThat(progress.readinessScore()).isBetween(0.68, 0.70);
    }
}
