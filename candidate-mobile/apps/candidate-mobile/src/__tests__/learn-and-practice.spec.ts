/**
 * Comprehensive Unit Tests for Candidate Mobile Learn and Practice Features
 * Tests Learner Onboarding, Adaptive Recommendations, DLP Roadmap Progression,
 * 5-Part Micro-Lesson Viewer & AI Explanation IR Engine, Practice Hub Filtering,
 * Custom Drill Generator, Result Panel Analytics, and Practice History Trends.
 */

import {
  LearnController,
  LearnState,
} from '../features/learn/learn-controller';
import {
  LearnerProfile,
  SEED_LEARNER_PROFILE,
  SEED_PUBLISHED_EXAMS,
  SEED_DIAGNOSTIC_SESSION,
  SEED_MICRO_LESSONS,
  SEED_EXPLANATION_IR,
} from '../features/learn/learn-models';

import {
  PracticeController,
  PracticeState,
} from '../features/practice/practice-controller';
import {
  PracticeSet,
  SEED_PRACTICE_SETS,
  SEED_PRACTICE_RESULT,
  SEED_PRACTICE_HISTORY,
} from '../features/practice/practice-models';

describe('1. Learner Onboarding Flow & Guard Verification', () => {
  let controller: LearnController;

  beforeEach(() => {
    controller = new LearnController({
      profile: {
        candidateId: 'cand-new-001',
        displayName: 'New Candidate',
        educationLevel: 'HIGH_SCHOOL',
        preferredLanguage: 'en',
        dailyStudyMinutes: 45,
        preferredLessonTypes: ['CONCEPT'],
        priorExperience: 'BEGINNER',
        completedAt: null,
      },
      activeEnrollment: null,
      onboardingStep: 'profile',
    });
  });

  it('correctly reports onboarding incomplete when profile is not completed', () => {
    expect(controller.hasCompletedOnboarding()).toBe(false);
  });

  it('updates candidate profile and transitions onboarding step to exam-picker', () => {
    const updated = controller.updateProfile({
      educationLevel: 'GRADUATE',
      preferredLanguage: 'hi',
      dailyStudyMinutes: 60,
      priorExperience: 'INTERMEDIATE',
    });

    expect(updated.educationLevel).toBe('GRADUATE');
    expect(updated.preferredLanguage).toBe('hi');
    expect(updated.dailyStudyMinutes).toBe(60);
    expect(updated.completedAt).toBeTruthy();
    expect(controller.getState().onboardingStep).toBe('exam-picker');
    // Still false because no active enrollment
    expect(controller.hasCompletedOnboarding()).toBe(false);
  });

  it('allows updating daily study minutes and preferred language independently', () => {
    controller.updateDailyStudyMinutes(90);
    expect(controller.getState().profile?.dailyStudyMinutes).toBe(90);

    // Minimum boundary check (at least 15 mins)
    controller.updateDailyStudyMinutes(5);
    expect(controller.getState().profile?.dailyStudyMinutes).toBe(15);

    controller.setPreferredLanguage('te');
    expect(controller.getState().profile?.preferredLanguage).toBe('te');
  });

  it('selects target exam and initializes diagnostic assessment session', () => {
    controller.updateProfile({
      educationLevel: 'GRADUATE',
      preferredLanguage: 'en',
      dailyStudyMinutes: 60,
    });

    const enrollment = controller.selectExam('exam-cgl-2026', '2026-12-01');
    expect(enrollment.examinationId).toBe('exam-cgl-2026');
    expect(enrollment.examinationCode).toBe('SSC-CGL-2026');
    expect(enrollment.diagnosticCompleted).toBe(false);

    const state = controller.getState();
    expect(state.activeEnrollment?.id).toBe(enrollment.id);
    expect(state.diagnosticSession).toBeTruthy();
    expect(state.diagnosticSession?.questions.length).toBe(6);
    expect(state.onboardingStep).toBe('diagnostic');
  });

  it('executes and scores diagnostic test with strengths, weaknesses, and readiness score', () => {
    controller.updateProfile({
      educationLevel: 'GRADUATE',
      preferredLanguage: 'en',
      dailyStudyMinutes: 60,
    });
    const enrollment = controller.selectExam('exam-cgl-2026');

    // Answer 5 correctly, 1 incorrectly
    const answers = [
      { questionId: 'dq-1', selectedOptionId: 'opt-a' }, // Correct
      { questionId: 'dq-2', selectedOptionId: 'opt-a' }, // Correct
      { questionId: 'dq-3', selectedOptionId: 'opt-a' }, // Correct
      { questionId: 'dq-4', selectedOptionId: 'opt-c' }, // Correct
      { questionId: 'dq-5', selectedOptionId: 'opt-a' }, // Correct
      { questionId: 'dq-6', selectedOptionId: 'opt-d' }, // Incorrect (correct is opt-a)
    ];

    const result = controller.submitDiagnostic(enrollment.id, { answers, isSkip: false });
    expect(result.score).toBe(5);
    expect(result.totalQuestions).toBe(6);
    expect(result.accuracyPercent).toBe(83);
    expect(result.initialReadinessScore).toBe(83);
    expect(result.strengths.length).toBeGreaterThan(0);
    expect(result.weaknesses.length).toBeGreaterThan(0);

    const state = controller.getState();
    expect(state.activeEnrollment?.diagnosticCompleted).toBe(true);
    expect(state.activeEnrollment?.readinessScore).toBe(83);
    expect(state.progress?.readinessScore).toBe(83);

    // Onboarding guard is now fully satisfied!
    expect(controller.hasCompletedOnboarding()).toBe(true);
  });

  it('supports skipping diagnostic test with neutral 50% baseline prior', () => {
    controller.updateProfile({
      educationLevel: 'GRADUATE',
      preferredLanguage: 'en',
      dailyStudyMinutes: 60,
    });
    const enrollment = controller.selectExam('exam-cgl-2026');

    const result = controller.skipDiagnostic(enrollment.id);
    expect(result.status).toBe('SKIPPED');
    expect(result.initialReadinessScore).toBe(50);
    expect(result.neutralPriorPercent).toBe(50);

    expect(controller.getState().activeEnrollment?.diagnosticCompleted).toBe(true);
    expect(controller.getState().activeEnrollment?.readinessScore).toBe(50);
    expect(controller.hasCompletedOnboarding()).toBe(true);
  });
});

describe('2. Adaptive Recommendations Engine & Mastery Computation', () => {
  let controller: LearnController;

  beforeEach(() => {
    controller = new LearnController();
  });

  it('retrieves prioritized weak subtopic recommendations', () => {
    const weakTopics = controller.getWeakTopics();
    expect(weakTopics.length).toBeGreaterThan(0);
    expect(weakTopics[0].subtopicId).toBe(303);
    expect(weakTopics[0].currentAccuracy).toBe(42);
  });

  it('calculates overall mastery accurately from the syllabus tree', () => {
    const overall = controller.calculateOverallMastery();
    expect(overall).toBeGreaterThan(60);
    expect(overall).toBeLessThan(90);
  });

  it('recalculates subtopic, topic, and subject mastery dynamically upon correct attempt', () => {
    // Initial mastery for subtopic 303 (Time, Speed & Distance) is 42
    const prevOverall = controller.calculateOverallMastery();
    const newSubScore = controller.recalculateMastery(303, true, 4);

    expect(newSubScore).toBeGreaterThan(42);
    const newOverall = controller.calculateOverallMastery();
    expect(newOverall).toBeGreaterThanOrEqual(prevOverall);

    // Verify subtopic in progress state
    const sub = controller
      .getState()
      .progress?.syllabus[0].topics[0].subtopics.find((s) => s.subtopicId === 303);
    expect(sub?.attempts).toBe(12); // 8 + 4
    expect(sub?.correct).toBe(7); // 3 + 4
    expect(sub?.masteryScore).toBe(newSubScore);
    expect(sub?.lastPracticedAt).toBeTruthy();
  });

  it('re-generates adaptive recommendations for a target exam', () => {
    const rec = controller.generateRecommendations('SSC-CGL-2026');
    expect(rec.status).toBe('GENERATED');
    expect(rec.weakTopicRecommendations.length).toBeGreaterThan(0);
    expect(rec.suggestedPracticeSetIds.length).toBeGreaterThan(0);
    expect(rec.motivationalMessage).toContain('SSC-CGL-2026');
  });

  it('toggles completion status of study plan days', () => {
    const planDay1 = controller.getState().recommendation?.studyPlanItems.find((i) => i.day === 1);
    expect(planDay1?.completed).toBe(true);

    controller.toggleStudyPlanDay(1);
    const toggled = controller.getState().recommendation?.studyPlanItems.find((i) => i.day === 1);
    expect(toggled?.completed).toBe(false);

    controller.toggleStudyPlanDay(1, true);
    expect(controller.getState().recommendation?.studyPlanItems.find((i) => i.day === 1)?.completed).toBe(true);
  });

  it('dismisses a recommendation by ID', () => {
    const recId = controller.getState().recommendation?.id || 'rec-001';
    controller.dismissRecommendation(recId);
    expect(controller.getState().recommendation?.status).toBe('DISMISSED');
  });

  it('completes next recommended activity and updates next best action', () => {
    const initialActivity = controller.getState().nextActivity;
    expect(initialActivity).toBeTruthy();

    controller.completeNextActivity(initialActivity!, 90);
    const updatedActivity = controller.getState().nextActivity;
    expect(updatedActivity?.activityId).not.toBe(initialActivity?.activityId);
    expect(updatedActivity?.subtopicId).toBe(302);
  });
});

describe('3. Dynamic Learning Path (DLP) Roadmap Progression', () => {
  let controller: LearnController;

  beforeEach(() => {
    controller = new LearnController();
  });

  it('tracks overall DLP roadmap completion percentage', () => {
    const progress = controller.getOverallProgressPercent();
    expect(progress).toBe(0); // 0 out of 6 items initially completed in seed
  });

  it('completes milestone sequentially and unlocks subsequent phase when prerequisite phase finishes', () => {
    // Complete milestone 101 in Phase 1
    controller.completeMilestone('m-101');
    expect(controller.getOverallProgressPercent()).toBeGreaterThan(0);
    expect(controller.getPhaseProgress(1)).toBe(33); // 1 of 3 items

    // Complete milestone 102
    controller.completeMilestone('m-102');
    expect(controller.getPhaseProgress(1)).toBe(67);

    // Complete milestone 103 (last item in Phase 1)
    const updatedLp = controller.completeMilestone('m-103');
    expect(controller.getPhaseProgress(1)).toBe(100);

    const phase1 = updatedLp.milestoneGraph.phases.find((p) => p.phaseNumber === 1);
    expect(phase1?.status).toBe('COMPLETED');
    expect(phase1?.passed).toBe(true);

    // Phase 2 must now be unlocked!
    const phase2 = updatedLp.milestoneGraph.phases.find((p) => p.phaseNumber === 2);
    expect(phase2?.status).toBe('ACTIVE');
    expect(updatedLp.milestoneGraph.currentPhase).toBe(2);
  });

  it('starts and submits Phase 2 adaptive quiz to unlock Phase 3 gatekeeper', () => {
    const quizResponse = controller.startAdaptiveQuiz();
    expect(quizResponse.quizSessionId).toBeTruthy();
    expect(quizResponse.targetAccuracy).toBe(75);

    // Submit with passing score (85%)
    const lp = controller.submitAdaptiveQuiz(quizResponse.quizSessionId, 85);
    const phase2 = lp.milestoneGraph.phases.find((p) => p.phaseNumber === 2);
    expect(phase2?.passed).toBe(true);
    expect(phase2?.status).toBe('COMPLETED');

    const phase3 = lp.milestoneGraph.phases.find((p) => p.phaseNumber === 3);
    expect(phase3?.status).toBe('ACTIVE');
    expect(lp.milestoneGraph.currentPhase).toBe(3);
  });

  it('starts and submits Phase 3 gatekeeper to grant clearance for full mock tests', () => {
    const gkResponse = controller.startGatekeeper();
    expect(gkResponse.gatekeeperSessionId).toBeTruthy();
    expect(gkResponse.passThreshold).toBe(80);

    const lp = controller.submitGatekeeper(gkResponse.gatekeeperSessionId, 88);
    expect(lp.gatekeeperPassed).toBe(true);
    expect(lp.status).toBe('COMPLETED');

    const status = controller.getGatekeeperStatus();
    expect(status.gatekeeperPassed).toBe(true);
    expect(status.clearedForFullMock).toBe(true);
    expect(status.remainingMilestonesCount).toBeGreaterThanOrEqual(0);
  });
});

describe('4. 5-Part Micro-Lesson Viewer & AI Explanation IR Engine', () => {
  let controller: LearnController;

  beforeEach(() => {
    controller = new LearnController();
    controller.loadMicroLesson(303);
  });

  it('loads micro-lesson for a subtopic with all 5 parts intact', () => {
    const state = controller.getState();
    const lesson = state.currentLesson;
    expect(lesson).toBeTruthy();
    expect(lesson?.subtopicId).toBe(303);
    expect(lesson?.content.coreConcept).toContain('relative speed');
    expect(lesson?.content.trapAlert).toBeTruthy();

    // Verify formulas contain $$...$$ LaTeX syntax
    const formulas = Array.isArray(lesson?.content.formulaCheatSheet)
      ? lesson?.content.formulaCheatSheet
      : [];
    expect(formulas.length).toBeGreaterThan(0);
    expect(formulas[0].latex).toContain('$$');

    // Verify step breakdown
    expect(lesson?.content.stepByStepBreakdown?.steps.length).toBe(4);
    expect(lesson?.content.stepByStepBreakdown?.finalAnswer).toContain('260');

    // Verify quick check items
    expect(lesson?.content.quickCheckItems?.length).toBe(2);
    expect(lesson?.content.keyTakeaways?.length).toBe(3);
  });

  it('navigates through slides 0 to 4 with boundary constraints', () => {
    expect(controller.getState().currentSlideIndex).toBe(0);

    controller.nextSlide();
    expect(controller.getState().currentSlideIndex).toBe(1);

    controller.nextSlide();
    controller.nextSlide();
    controller.nextSlide();
    expect(controller.getState().currentSlideIndex).toBe(4);

    // Does not exceed upper bound 4
    controller.nextSlide();
    expect(controller.getState().currentSlideIndex).toBe(4);

    controller.prevSlide();
    expect(controller.getState().currentSlideIndex).toBe(3);

    // Direct jump
    controller.setSlideIndex(2);
    expect(controller.getState().currentSlideIndex).toBe(2);
  });

  it('reveals worked example steps sequentially and in bulk', () => {
    expect(controller.getState().revealedStepCount).toBe(1);

    controller.revealNextStep();
    expect(controller.getState().revealedStepCount).toBe(2);

    controller.revealAllSteps();
    expect(controller.getState().revealedStepCount).toBe(4);
  });

  it('evaluates interactive quick-check questions with instant correctness feedback', () => {
    // Question 0: correct answer is "$$10\\text{ m/s}$$"
    const isCorrect1 = controller.recordQuizAnswer(0, '$$10\\text{ m/s}$$');
    expect(isCorrect1).toBe(true);
    expect(controller.getState().quizSelections[0].isCorrect).toBe(true);

    // Question 1: incorrect answer
    const isCorrect2 = controller.recordQuizAnswer(1, '$$60\\text{ km/h}$$');
    expect(isCorrect2).toBe(false);
    expect(controller.getState().quizSelections[1].isCorrect).toBe(false);
  });

  it('submits lesson feedback rating and completes micro-lesson', () => {
    controller.submitLessonFeedback(5);
    expect(controller.getState().lessonFeedbackRating).toBe(5);

    // Complete lesson and verify subtopic mastery and milestone completion
    controller.completeMicroLesson(303, 100);
    expect(controller.getState().lessonCompleted).toBe(true);

    const milestone = controller
      .getState()
      .learningPath?.milestoneGraph.phases[0].items.find((i) => i.subtopicId === 303);
    expect(milestone?.completed).toBe(true);
  });

  it('navigates and controls AI Explanation IR engine', () => {
    const ir = controller.loadExplanation('dq-3');
    expect(ir).toBeTruthy();
    expect(ir?.questionId).toBe('dq-3');
    expect(ir?.steps.length).toBe(4);

    expect(controller.getState().activeExplanationStepIndex).toBe(0);
    controller.selectExplanationStep(2);
    expect(controller.getState().activeExplanationStepIndex).toBe(2);

    // Playback and speed controls
    controller.toggleExplanationPlay(true);
    expect(controller.getState().isExplanationPlaying).toBe(true);

    controller.setExplanationSpeed(1.5);
    expect(controller.getState().explanationSpeed).toBe(1.5);

    controller.toggleExplanationPlay(false);
    expect(controller.getState().isExplanationPlaying).toBe(false);
  });
});

describe('5. Practice Hub Filtering & Custom Drill Generator', () => {
  let controller: PracticeController;

  beforeEach(() => {
    controller = new PracticeController();
  });

  it('retrieves practice sets catalog and computes accurate KPI stats', () => {
    const kpis = controller.getKpiStats();
    expect(kpis.totalSets).toBe(5);
    expect(kpis.officialCount).toBe(2);
    expect(kpis.curatedCount).toBe(3);
    expect(kpis.totalQuestions).toBeGreaterThan(100);
    expect(kpis.avgDuration).toBeGreaterThan(0);
  });

  it('filters practice sets by search query keyword', () => {
    controller.setSearchQuery('Relativity');
    const filtered = controller.getFilteredSets();
    expect(filtered.length).toBe(1);
    expect(filtered[0].id).toBe('set-quant-tsd-01');

    controller.setSearchQuery('');
    expect(controller.getFilteredSets().length).toBe(5);
  });

  it('filters practice sets by source (EXAM_CLONE vs MANUAL)', () => {
    controller.setSourceFilter('EXAM_CLONE');
    const officialMocks = controller.getFilteredSets();
    expect(officialMocks.every((s) => s.source === 'EXAM_CLONE')).toBe(true);
    expect(officialMocks.length).toBe(2);

    controller.setSourceFilter('MANUAL');
    const curatedDrills = controller.getFilteredSets();
    expect(curatedDrills.every((s) => s.source === 'MANUAL')).toBe(true);
    expect(curatedDrills.length).toBe(3);
  });

  it('filters practice sets by subject category', () => {
    controller.setSubjectFilter('Quantitative Aptitude');
    const quantSets = controller.getFilteredSets();
    expect(quantSets.length).toBe(2);
    expect(quantSets.every((s) => s.subjectSlug === 'Quantitative Aptitude')).toBe(true);
  });

  it('generates and launches a custom practice drill with custom configuration', () => {
    const customSet = controller.generateCustomPractice({
      subject: 'Physics',
      difficulty: 'ADVANCED',
      questionCount: 15,
      mode: 'TIMED',
      durationMinutes: 25,
    });

    expect(customSet.id).toContain('custom-drill');
    expect(customSet.totalQuestions).toBe(15);
    expect(customSet.difficulty).toBe('ADVANCED');
    expect(customSet.durationMinutes).toBe(25);

    // Verify added to catalog
    expect(controller.getState().practiceSets[0].id).toBe(customSet.id);
  });
});

describe('6. Active Practice Drill Execution & Result Panel Analytics', () => {
  let controller: PracticeController;

  beforeEach(() => {
    controller = new PracticeController();
    controller.startSession('set-quant-tsd-01', 'TIMED', 'en');
  });

  it('initializes active drill session with question list and navigation', () => {
    const state = controller.getState();
    expect(state.activeSession?.status).toBe('IN_PROGRESS');
    expect(state.activeQuestions.length).toBe(3);
    expect(state.activeQuestionIndex).toBe(0);

    controller.setActiveQuestionIndex(2);
    expect(controller.getState().activeQuestionIndex).toBe(2);
  });

  it('saves candidate responses with timing and review flags', () => {
    controller.saveAnswer('q-tsd-1', 'opt-a', 35000, false);
    controller.saveAnswer('q-tsd-2', 'opt-b', 45000, true);

    const state = controller.getState();
    expect(state.userAnswers['q-tsd-1']).toBe('opt-a');
    expect(state.userAnswers['q-tsd-2']).toBe('opt-b');
    expect(state.flaggedQuestions.has('q-tsd-2')).toBe(true);
    expect(state.flaggedQuestions.has('q-tsd-1')).toBe(false);
  });

  it('toggles bookmarking on question', () => {
    expect(controller.toggleBookmark('q-tsd-1')).toBe(true);
    expect(controller.getState().bookmarkedQuestions.has('q-tsd-1')).toBe(true);

    expect(controller.toggleBookmark('q-tsd-1')).toBe(false);
    expect(controller.getState().bookmarkedQuestions.has('q-tsd-1')).toBe(false);
  });

  it('submits drill session, scores responses with negative marking, and computes analytics', () => {
    // q-tsd-1: correct ('opt-a') -> +2
    // q-tsd-2: incorrect ('opt-b', correct is 'opt-c') -> -0.5
    // q-tsd-3: skipped (null) -> 0
    controller.saveAnswer('q-tsd-1', 'opt-a', 30000, false);
    controller.saveAnswer('q-tsd-2', 'opt-b', 40000, true);

    const result = controller.submitSession();
    expect(result.correctCount).toBe(1);
    expect(result.incorrectCount).toBe(1);
    expect(result.skippedCount).toBe(1);
    expect(result.obtainedMarks).toBe(1.5); // 2 - 0.5
    expect(result.totalMarks).toBe(6);
    expect(result.accuracyPercent).toBe(33);
    expect(result.flaggedCount).toBe(1);

    expect(controller.getState().activeTab).toBe('result');
    expect(controller.getState().history[0].sessionId).toBe(result.sessionId);
  });

  it('filters question results in Result Review panel', () => {
    controller.saveAnswer('q-tsd-1', 'opt-a', 30000, false);
    controller.saveAnswer('q-tsd-2', 'opt-b', 40000, true);
    controller.submitSession();

    controller.setReviewFilter('CORRECT');
    expect(controller.getFilteredResults().length).toBe(1);
    expect(controller.getFilteredResults()[0].questionId).toBe('q-tsd-1');

    controller.setReviewFilter('INCORRECT');
    expect(controller.getFilteredResults().length).toBe(1);
    expect(controller.getFilteredResults()[0].questionId).toBe('q-tsd-2');

    controller.setReviewFilter('SKIPPED');
    expect(controller.getFilteredResults().length).toBe(1);
    expect(controller.getFilteredResults()[0].questionId).toBe('q-tsd-3');

    controller.setReviewFilter('FLAGGED');
    expect(controller.getFilteredResults().length).toBe(1);
    expect(controller.getFilteredResults()[0].questionId).toBe('q-tsd-2');
  });

  it('retrieves subtopic ID for deep linking to micro-lesson from missed question', () => {
    controller.saveAnswer('q-tsd-2', 'opt-b', 40000, false);
    controller.submitSession();

    const subtopicId = controller.getStudyLessonSubtopicId('q-tsd-2');
    expect(subtopicId).toBe(303);
  });
});

describe('7. Practice History & Long-Term Analytics', () => {
  let controller: PracticeController;

  beforeEach(() => {
    controller = new PracticeController();
  });

  it('computes average accuracy trend, best score, and total questions solved', () => {
    const avgAcc = controller.getAverageAccuracy();
    expect(avgAcc).toBeGreaterThan(60);

    const highestScore = controller.getHighestScore();
    expect(highestScore).toBe(142); // from mock paper in seed

    const totalSolved = controller.getTotalQuestionsSolved();
    expect(totalSolved).toBe(125);
  });

  it('computes subject-wise accuracy and attempts breakdown metrics', () => {
    const metrics = controller.getSubjectWiseMetrics();
    expect(metrics.length).toBeGreaterThan(0);

    const quant = metrics.find((m) => m.subject === 'Quantitative Aptitude');
    expect(quant).toBeTruthy();
    expect(quant?.attemptsCount).toBe(2);
    expect(quant?.averageAccuracy).toBeGreaterThan(0);
  });

  it('filters history attempts by search query keyword', () => {
    controller.setHistorySearchQuery('Syllogisms');
    const filtered = controller.getFilteredHistory();
    expect(filtered.length).toBe(1);
    expect(filtered[0].practiceSetName).toContain('Syllogisms');

    controller.setHistorySearchQuery('non-existent');
    expect(controller.getFilteredHistory().length).toBe(0);
  });

  it('supports re-attempting a session from history', () => {
    const session = controller.reattemptSession('set-quant-tsd-01');
    expect(session.practiceSetId).toBe('set-quant-tsd-01');
    expect(session.status).toBe('IN_PROGRESS');
    expect(controller.getState().activeTab).toBe('active_drill');
  });
});
