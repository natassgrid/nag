/**
 * NAG Candidate Mobile - Learn Controller
 * State controller implementing Learner Onboarding, Adaptive Recommendations,
 * Dynamic Learning Path (DLP) Roadmap progression, and 5-Part Micro-Lesson Viewer
 * with AI Explanation IR engine.
 */

import {
  LearnerProfile,
  LearnerOnboardingPayload,
  LearnerExamSummary,
  LearnerEnrollment,
  DiagnosticSession,
  DiagnosticSubmitRequest,
  DiagnosticSubmitResult,
  LearnerProgress,
  NextActivity,
  Recommendation,
  WeakTopicRecommendation,
  StudyPlanItem,
  DynamicLearningPath,
  MilestonePhase,
  MilestoneItem,
  GatekeeperStatusResponse,
  StartAdaptiveQuizResponse,
  StartGatekeeperResponse,
  SubtopicMicroLesson,
  QuizSelectionState,
  ExplanationIr,
  SubtopicMastery,
  SEED_LEARNER_PROFILE,
  SEED_PUBLISHED_EXAMS,
  SEED_ACTIVE_ENROLLMENT,
  SEED_DIAGNOSTIC_SESSION,
  SEED_DIAGNOSTIC_RESULT,
  SEED_NEXT_ACTIVITY,
  SEED_LEARNER_PROGRESS,
  SEED_DYNAMIC_LEARNING_PATH,
  SEED_RECOMMENDATION,
  SEED_MICRO_LESSONS,
  SEED_EXPLANATION_IR,
} from './learn-models';
import { DOCKER_API_PATHS } from '@nag/candidate-core';
import { defaultDockerApiService } from '../../services/docker-api.service';

export interface LearnState {
  profile: LearnerProfile | null;
  activeEnrollment: LearnerEnrollment | null;
  publishedExams: LearnerExamSummary[];
  diagnosticSession: DiagnosticSession | null;
  diagnosticResult: DiagnosticSubmitResult | null;
  diagnosticSubmitting: boolean;
  progress: LearnerProgress | null;
  recommendation: Recommendation | null;
  learningPath: DynamicLearningPath | null;
  nextActivity: NextActivity | null;

  // 5-Part Micro-Lesson Viewer State
  currentLesson: SubtopicMicroLesson | null;
  currentSlideIndex: number;
  revealedStepCount: number;
  quizSelections: Record<number, QuizSelectionState>;
  lessonFeedbackRating: number | null;
  lessonCompleted: boolean;

  // AI Explanation IR State
  currentExplanation: ExplanationIr | null;
  activeExplanationStepIndex: number;
  isExplanationPlaying: boolean;
  explanationSpeed: number;

  // Orchestration & Navigation
  onboardingStep: 'profile' | 'exam-picker' | 'diagnostic';
  activeHubTab: 'recommendations' | 'roadmap' | 'lesson';
}

export class LearnController {
  private state: LearnState;
  private listeners: Set<(state: LearnState) => void> = new Set();
  private microLessonsRepo: Record<number, SubtopicMicroLesson> = { ...SEED_MICRO_LESSONS };
  private explanationsRepo: Record<string, ExplanationIr> = {
    [SEED_EXPLANATION_IR.questionId]: SEED_EXPLANATION_IR,
  };

  constructor(initialOverrides?: Partial<LearnState>) {
    this.state = {
      profile: SEED_LEARNER_PROFILE,
      activeEnrollment: SEED_ACTIVE_ENROLLMENT,
      publishedExams: SEED_PUBLISHED_EXAMS,
      diagnosticSession: SEED_DIAGNOSTIC_SESSION,
      diagnosticResult: SEED_DIAGNOSTIC_RESULT,
      diagnosticSubmitting: false,
      progress: JSON.parse(JSON.stringify(SEED_LEARNER_PROGRESS)),
      recommendation: JSON.parse(JSON.stringify(SEED_RECOMMENDATION)),
      learningPath: JSON.parse(JSON.stringify(SEED_DYNAMIC_LEARNING_PATH)),
      nextActivity: SEED_NEXT_ACTIVITY,

      currentLesson: SEED_MICRO_LESSONS[303] || null,
      currentSlideIndex: 0,
      revealedStepCount: 1,
      quizSelections: {},
      lessonFeedbackRating: null,
      lessonCompleted: false,

      currentExplanation: SEED_EXPLANATION_IR,
      activeExplanationStepIndex: 0,
      isExplanationPlaying: false,
      explanationSpeed: 1,

      onboardingStep: 'profile',
      activeHubTab: 'recommendations',
      ...initialOverrides,
    };

    if (typeof window !== 'undefined' && typeof (window as any).NagReactMount === 'function') {
      void this.syncFromDocker();
    }
  }

  public async syncFromDocker(): Promise<void> {
    try {
      const [dlpRes, recRes] = await Promise.all([
        defaultDockerApiService
          .requestDocker<any>(DOCKER_API_PATHS.LEARN.DLP_CURRENT, { method: 'GET' })
          .catch(() => null),
        defaultDockerApiService
          .requestDocker<any>(DOCKER_API_PATHS.LEARN.RECOMMENDATIONS_LATEST, { method: 'GET' })
          .catch(() => null),
      ]);

      const dlpData = dlpRes?.data || dlpRes;
      if (dlpData && (dlpData.id || dlpData.milestoneGraph)) {
        this.state.learningPath = dlpData;
      }

      const recData = recRes?.data || recRes;
      if (recData && (recData.id || recData.weakTopicRecommendations)) {
        this.state.recommendation = recData;
      }

      if (dlpData || recData) {
        this.emit();
      }
    } catch {
      // Non-blocking fallback
    }
  }

  public getState(): LearnState {
    return { ...this.state };
  }

  public subscribe(listener: (state: LearnState) => void): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private emit(): void {
    const s = this.getState();
    this.listeners.forEach((listener) => listener(s));
  }

  // ==========================================================================
  // 1. Learner Onboarding Flow & Guard Check
  // ==========================================================================

  /**
   * Guard Check: Verifies whether candidate has completed onboarding.
   * Requires profile completion and active enrollment with diagnostic completion or active status.
   */
  public hasCompletedOnboarding(): boolean {
    const p = this.state.profile;
    const e = this.state.activeEnrollment;
    const profileDone = !!(p && p.completedAt && p.educationLevel && p.preferredLanguage);
    const enrollmentDone = !!(e && (e.diagnosticCompleted || e.status === 'ACTIVE'));
    return profileDone && enrollmentDone;
  }

  public setOnboardingStep(step: 'profile' | 'exam-picker' | 'diagnostic'): void {
    this.state.onboardingStep = step;
    this.emit();
  }

  public setActiveHubTab(tab: 'recommendations' | 'roadmap' | 'lesson'): void {
    this.state.activeHubTab = tab;
    this.emit();
  }

  public updateProfile(payload: Partial<LearnerProfile> | LearnerOnboardingPayload): LearnerProfile {
    const current = this.state.profile || SEED_LEARNER_PROFILE;
    const updated: LearnerProfile = {
      ...current,
      ...payload,
      completedAt: new Date().toISOString(),
    };
    this.state.profile = updated;

    if (payload.dailyStudyMinutes) {
      this.state.profile.dailyStudyMinutes = payload.dailyStudyMinutes;
    }
    if (payload.preferredLanguage) {
      this.state.profile.preferredLanguage = payload.preferredLanguage;
    }

    this.state.onboardingStep = 'exam-picker';
    this.emit();
    return updated;
  }

  public updateDailyStudyMinutes(minutes: number): void {
    if (this.state.profile) {
      this.state.profile = {
        ...this.state.profile,
        dailyStudyMinutes: Math.max(15, minutes),
      };
      this.emit();
    }
  }

  public setPreferredLanguage(lang: string): void {
    if (this.state.profile) {
      this.state.profile = {
        ...this.state.profile,
        preferredLanguage: lang,
      };
      this.emit();
    }
  }

  public selectExam(examId: string, targetDate?: string): LearnerEnrollment {
    const exam = this.state.publishedExams.find((e) => e.id === examId) || this.state.publishedExams[0];
    const newEnrollment: LearnerEnrollment = {
      id: `enr-${Date.now()}`,
      candidateId: this.state.profile?.candidateId || 'cand-001',
      examinationId: examId,
      examName: exam?.title,
      examinationTitle: exam ? exam.title : 'Competitive Examination',
      examinationCode: exam ? exam.code : 'EXAM-GEN',
      targetDate: targetDate || this.state.profile?.targetDate || '2026-11-15',
      status: 'ACTIVE',
      diagnosticCompleted: false,
      readinessScore: 0,
      startedAt: new Date().toISOString(),
    };

    this.state.activeEnrollment = newEnrollment;
    this.state.diagnosticSession = {
      ...SEED_DIAGNOSTIC_SESSION,
      enrollmentId: newEnrollment.id,
      examinationId: examId,
      examinationTitle: newEnrollment.examinationTitle,
      examinationCode: newEnrollment.examinationCode,
    };
    this.state.onboardingStep = 'diagnostic';
    this.emit();
    return newEnrollment;
  }

  public startDiagnostic(enrollmentId?: string): DiagnosticSession {
    const enrId = enrollmentId || this.state.activeEnrollment?.id || `enr-${Date.now()}`;
    const session: DiagnosticSession = {
      ...SEED_DIAGNOSTIC_SESSION,
      enrollmentId: enrId,
      examinationId: this.state.activeEnrollment?.examinationId || 'exam-cgl-2026',
      examinationTitle: this.state.activeEnrollment?.examinationTitle || 'SSC Combined Graduate Level (CGL) 2026',
      examinationCode: this.state.activeEnrollment?.examinationCode || 'SSC-CGL-2026',
    };
    this.state.diagnosticSession = session;
    this.state.diagnosticResult = null;
    this.state.onboardingStep = 'diagnostic';
    this.emit();
    return session;
  }

  public submitDiagnostic(
    enrollmentId: string,
    req: DiagnosticSubmitRequest
  ): DiagnosticSubmitResult {
    this.state.diagnosticSubmitting = true;
    this.emit();

    const session = this.state.diagnosticSession || SEED_DIAGNOSTIC_SESSION;
    let correctCount = 0;
    const breakdown: Record<number, number> = {};
    const strengths: string[] = [];
    const weaknesses: string[] = [];

    session.questions.forEach((q) => {
      const userAns = req.answers.find((a) => a.questionId === q.id);
      const isCorrect = userAns && q.correctOptionId ? userAns.selectedOptionId === q.correctOptionId : false;
      if (isCorrect) {
        correctCount++;
        breakdown[q.subtopicId] = 90;
        strengths.push(`${q.subtopicName} (90% mastery)`);
      } else {
        breakdown[q.subtopicId] = 40;
        weaknesses.push(`${q.subtopicName} (Requires remediation)`);
      }
    });

    const totalQuestions = session.questions.length;
    const accuracyPercent = totalQuestions > 0 ? Math.round((correctCount * 100) / totalQuestions) : 0;
    const readinessScore = accuracyPercent;

    const result: DiagnosticSubmitResult = {
      sessionId: session.sessionId,
      enrollmentId,
      status: 'COMPLETED',
      score: correctCount,
      totalQuestions,
      correctCount,
      correctAnswers: correctCount,
      accuracyPercent,
      seededSubtopicsCount: Object.keys(breakdown).length,
      initialReadinessScore: readinessScore,
      strengths: strengths.slice(0, 3),
      weaknesses: weaknesses.slice(0, 3),
      baselineLabel: 'Calibrated Diagnostic Baseline',
      subtopicBreakdown: breakdown,
      completedAt: new Date().toISOString(),
    };

    this.state.diagnosticSubmitting = false;
    this.state.diagnosticResult = result;
    if (this.state.activeEnrollment) {
      this.state.activeEnrollment.diagnosticCompleted = true;
      this.state.activeEnrollment.readinessScore = readinessScore;
    }
    if (this.state.progress) {
      this.state.progress.readinessScore = readinessScore;
    }

    this.emit();
    return result;
  }

  public skipDiagnostic(enrollmentId: string): DiagnosticSubmitResult {
    const skippedResult: DiagnosticSubmitResult = {
      enrollmentId,
      status: 'SKIPPED',
      score: 0,
      totalQuestions: 6,
      accuracyPercent: 50,
      seededSubtopicsCount: 6,
      initialReadinessScore: 50,
      neutralPriorPercent: 50,
      baselineLabel: 'Neutral Baseline (50% Prior)',
      strengths: [],
      weaknesses: ['Diagnostic skipped: subtopic mastery initialized with neutral prior.'],
      completedAt: new Date().toISOString(),
    };

    this.state.diagnosticResult = skippedResult;
    if (this.state.activeEnrollment) {
      this.state.activeEnrollment.diagnosticCompleted = true;
      this.state.activeEnrollment.readinessScore = 50;
    }
    if (this.state.progress) {
      this.state.progress.readinessScore = 50;
    }

    this.emit();
    return skippedResult;
  }

  // ==========================================================================
  // 2. Adaptive Recommendations Engine
  // ==========================================================================

  public getWeakTopics(): WeakTopicRecommendation[] {
    return this.state.recommendation?.weakTopicRecommendations || [];
  }

  public calculateOverallMastery(): number {
    const progress = this.state.progress;
    if (!progress || !progress.syllabus || progress.syllabus.length === 0) {
      return this.state.activeEnrollment?.readinessScore || 0;
    }
    let totalScore = 0;
    let subtopicCount = 0;
    progress.syllabus.forEach((subject) => {
      subject.topics.forEach((topic) => {
        topic.subtopics.forEach((sub) => {
          totalScore += sub.masteryScore;
          subtopicCount++;
        });
      });
    });
    return subtopicCount > 0 ? Math.round(totalScore / subtopicCount) : 0;
  }

  public recalculateMastery(subtopicId: number, isCorrect: boolean, attemptsInc: number = 1): number {
    let newScore = 50;
    if (!this.state.progress) return newScore;

    this.state.progress.syllabus.forEach((subj) => {
      subj.topics.forEach((top) => {
        const sub = top.subtopics.find((s) => s.subtopicId === subtopicId);
        if (sub) {
          sub.attempts += attemptsInc;
          if (isCorrect) sub.correct += attemptsInc;
          const ratio = sub.attempts > 0 ? sub.correct / sub.attempts : 0.5;
          sub.confidence = Math.min(0.99, Math.max(0.1, ratio));
          sub.masteryScore = Math.round(ratio * 100);
          sub.lastPracticedAt = new Date().toISOString();

          if (sub.masteryScore >= 80) {
            sub.status = 'MASTERED';
          } else if (sub.masteryScore >= 50) {
            sub.status = 'PRACTISING';
          } else {
            sub.status = 'LEARNING';
          }
          newScore = sub.masteryScore;
        }
        // Recalculate topic average
        const topicSum = top.subtopics.reduce((acc, cur) => acc + cur.masteryScore, 0);
        top.masteryScore = top.subtopics.length > 0 ? Math.round(topicSum / top.subtopics.length) : 0;
      });
      // Recalculate subject average
      const subjSum = subj.topics.reduce((acc, cur) => acc + cur.masteryScore, 0);
      subj.masteryScore = subj.topics.length > 0 ? Math.round(subjSum / subj.topics.length) : 0;
    });

    const overall = this.calculateOverallMastery();
    this.state.progress.readinessScore = overall;
    if (this.state.activeEnrollment) {
      this.state.activeEnrollment.readinessScore = overall;
    }

    this.emit();
    return newScore;
  }

  public generateRecommendations(examTarget?: string): Recommendation {
    const target = examTarget || this.state.activeEnrollment?.examinationCode || 'SSC-CGL-2026';
    const weakSubs: SubtopicMastery[] = [];

    this.state.progress?.syllabus.forEach((s) => {
      s.topics.forEach((t) => {
        t.subtopics.forEach((sub) => {
          if (sub.masteryScore < 60) weakSubs.push(sub);
        });
      });
    });

    const weakRecs: WeakTopicRecommendation[] = weakSubs.map((w) => ({
      subtopicId: w.subtopicId,
      topicName: w.topicName,
      subjectName: w.subjectName,
      currentAccuracy: w.masteryScore,
      subtopicsToRevise: [`${w.subtopicName} (Mastery: ${w.masteryScore}%)`],
    }));

    const updatedRecommendation: Recommendation = {
      id: `rec-${Date.now()}`,
      candidateId: this.state.profile?.candidateId || 'cand-001',
      triggerSessionId: this.state.diagnosticSession?.sessionId || null,
      status: 'GENERATED',
      generatedAt: new Date().toISOString(),
      motivationalMessage: `Adaptive schedule generated for ${target}. Prioritize your weakest ${weakRecs.length} areas to maximize score gains.`,
      weakTopicRecommendations: weakRecs.length > 0 ? weakRecs : this.state.recommendation?.weakTopicRecommendations || [],
      studyPlanItems: this.state.recommendation?.studyPlanItems || [],
      suggestedPracticeSetIds: ['set-cgl-quant-drill-01', 'set-cgl-reasoning-02'],
    };

    this.state.recommendation = updatedRecommendation;
    this.emit();
    return updatedRecommendation;
  }

  public dismissRecommendation(id: string): void {
    if (this.state.recommendation && this.state.recommendation.id === id) {
      this.state.recommendation.status = 'DISMISSED';
      this.emit();
    }
  }

  public toggleStudyPlanDay(day: number, completed?: boolean): StudyPlanItem[] {
    if (!this.state.recommendation) return [];
    this.state.recommendation.studyPlanItems = this.state.recommendation.studyPlanItems.map((item) => {
      if (item.day === day) {
        return {
          ...item,
          completed: completed !== undefined ? completed : !item.completed,
        };
      }
      return item;
    });
    this.emit();
    return this.state.recommendation.studyPlanItems;
  }

  public completeNextActivity(activity: NextActivity, score?: number): void {
    if (activity.subtopicId) {
      this.recalculateMastery(activity.subtopicId, (score ?? 80) >= 60);
    }
    this.state.nextActivity = {
      ...SEED_NEXT_ACTIVITY,
      activityId: `act-${Date.now()}`,
      subtopicId: 302,
      subtopicName: 'Profit, Loss & Discount',
      reason: 'Next recommended high-yield subtopic based on spaced repetition review',
      urgency: 'MEDIUM',
    };
    this.emit();
  }

  // ==========================================================================
  // 3. Dynamic Learning Path (DLP) Roadmap Progression
  // ==========================================================================

  public getOverallProgressPercent(): number {
    const lp = this.state.learningPath;
    if (!lp) return 0;
    if (lp.status === 'COMPLETED' || lp.gatekeeperPassed) return 100;

    const allItems: MilestoneItem[] = [];
    lp.milestoneGraph.phases.forEach((p) => {
      allItems.push(...p.items);
    });

    if (allItems.length === 0) return 0;
    const completedCount = allItems.filter((i) => i.completed || i.status === 'COMPLETED').length;
    return Math.round((completedCount * 100) / allItems.length);
  }

  public getPhaseProgress(phaseNumber: number): number {
    const phase = this.state.learningPath?.milestoneGraph.phases.find((p) => p.phaseNumber === phaseNumber);
    if (!phase || phase.items.length === 0) return 0;
    const done = phase.items.filter((i) => i.completed || i.status === 'COMPLETED').length;
    return Math.round((done * 100) / phase.items.length);
  }

  public completeMilestone(milestoneId: string): DynamicLearningPath {
    const lp = this.state.learningPath;
    if (!lp) return SEED_DYNAMIC_LEARNING_PATH;

    let targetSubtopicId: number | undefined;

    lp.milestoneGraph.phases.forEach((phase) => {
      phase.items.forEach((item) => {
        if (item.milestoneId === milestoneId || item.id === milestoneId) {
          item.completed = true;
          item.status = 'COMPLETED';
          item.completedAt = new Date().toISOString();
          targetSubtopicId = item.subtopicId;
        }
      });

      // Check if all items in Phase are done
      const allDone = phase.items.every((i) => i.completed || i.status === 'COMPLETED');
      if (allDone && phase.status !== 'COMPLETED') {
        phase.status = 'COMPLETED';
        phase.passed = true;
        phase.achievedAccuracy = 85;

        // Unlock next phase if exists
        const nextPhase = lp.milestoneGraph.phases.find((p) => p.phaseNumber === phase.phaseNumber + 1);
        if (nextPhase && nextPhase.status === 'LOCKED') {
          nextPhase.status = 'ACTIVE';
          lp.milestoneGraph.currentPhase = nextPhase.phaseNumber;
          lp.milestoneGraph.currentPhaseNumber = nextPhase.phaseNumber;
        }
      }
    });

    if (targetSubtopicId) {
      this.recalculateMastery(targetSubtopicId, true);
    }

    const overall = this.getOverallProgressPercent();
    if (overall >= 100) {
      lp.status = 'COMPLETED';
      lp.gatekeeperPassed = true;
      lp.milestoneGraph.gatekeeperPassed = true;
    }

    this.state.learningPath = { ...lp };
    this.emit();
    return this.state.learningPath;
  }

  public startAdaptiveQuiz(): StartAdaptiveQuizResponse {
    return {
      quizSessionId: `quiz-${Date.now()}`,
      practiceSetId: 'set-cgl-quant-drill-01',
      targetAccuracy: 75,
      questionCount: 15,
      message: 'Adaptive Quiz started for Phase 2 readiness check.',
    };
  }

  public submitAdaptiveQuiz(quizSessionId: string, scorePercent: number): DynamicLearningPath {
    const lp = this.state.learningPath;
    if (lp) {
      const phase2 = lp.milestoneGraph.phases.find((p) => p.phaseNumber === 2);
      if (phase2) {
        phase2.achievedAccuracy = scorePercent;
        if (scorePercent >= phase2.passThreshold) {
          phase2.passed = true;
          phase2.status = 'COMPLETED';
          const phase3 = lp.milestoneGraph.phases.find((p) => p.phaseNumber === 3);
          if (phase3) {
            phase3.status = 'ACTIVE';
            lp.milestoneGraph.currentPhase = 3;
            lp.milestoneGraph.currentPhaseNumber = 3;
          }
        }
      }
      this.state.learningPath = { ...lp };
      this.emit();
    }
    return this.state.learningPath || SEED_DYNAMIC_LEARNING_PATH;
  }

  public startGatekeeper(): StartGatekeeperResponse {
    return {
      gatekeeperSessionId: `gk-${Date.now()}`,
      practiceSetId: 'set-cgl-full-mock-01',
      passThreshold: 80,
      questionCount: 25,
      message: 'Gatekeeper Assessment initialized.',
    };
  }

  public submitGatekeeper(gatekeeperSessionId: string, scorePercent: number): DynamicLearningPath {
    const lp = this.state.learningPath;
    if (lp) {
      const phase3 = lp.milestoneGraph.phases.find((p) => p.phaseNumber === 3);
      if (phase3) {
        phase3.achievedAccuracy = scorePercent;
        if (scorePercent >= phase3.passThreshold) {
          phase3.passed = true;
          phase3.status = 'COMPLETED';
          lp.gatekeeperPassed = true;
          lp.milestoneGraph.gatekeeperPassed = true;
          lp.status = 'COMPLETED';
        }
      }
      this.state.learningPath = { ...lp };
      this.emit();
    }
    return this.state.learningPath || SEED_DYNAMIC_LEARNING_PATH;
  }

  public getGatekeeperStatus(): GatekeeperStatusResponse {
    const lp = this.state.learningPath;
    const passed = !!lp?.gatekeeperPassed;
    return {
      allowed: !passed,
      gatekeeperPassed: passed,
      gatekeeperRequired: !passed,
      clearedForFullMock: passed,
      currentPhase: String(lp?.milestoneGraph.currentPhase || 1),
      remainingMilestonesCount: lp ? lp.milestoneGraph.phases.flatMap((p) => p.items).filter((i) => !i.completed).length : 0,
      message: passed ? 'Candidate cleared for unrestricted full mock tests' : 'Gatekeeper evaluation pending',
    };
  }

  // ==========================================================================
  // 4. 5-Part Micro-Lesson Viewer Engine
  // ==========================================================================

  public loadMicroLesson(subtopicId: number): SubtopicMicroLesson | null {
    const lesson = this.microLessonsRepo[subtopicId] || SEED_MICRO_LESSONS[303];
    this.state.currentLesson = lesson;
    this.state.currentSlideIndex = 0;
    this.state.revealedStepCount = 1;
    this.state.quizSelections = {};
    this.state.lessonFeedbackRating = null;
    this.state.lessonCompleted = false;
    this.state.activeHubTab = 'lesson';
    this.emit();
    return lesson;
  }

  public setSlideIndex(index: number): void {
    const bounded = Math.max(0, Math.min(4, index));
    this.state.currentSlideIndex = bounded;
    this.emit();
  }

  public nextSlide(): void {
    if (this.state.currentSlideIndex < 4) {
      this.setSlideIndex(this.state.currentSlideIndex + 1);
    }
  }

  public prevSlide(): void {
    if (this.state.currentSlideIndex > 0) {
      this.setSlideIndex(this.state.currentSlideIndex - 1);
    }
  }

  public revealNextStep(): void {
    const totalSteps = this.state.currentLesson?.content.stepByStepBreakdown?.steps.length || 4;
    if (this.state.revealedStepCount < totalSteps) {
      this.state.revealedStepCount++;
      this.emit();
    }
  }

  public revealAllSteps(): void {
    const totalSteps = this.state.currentLesson?.content.stepByStepBreakdown?.steps.length || 4;
    this.state.revealedStepCount = totalSteps;
    this.emit();
  }

  public recordQuizAnswer(questionIndex: number, selectedOption: string): boolean {
    const question = this.state.currentLesson?.content.quickCheckItems?.[questionIndex];
    if (!question) return false;

    const isCorrect = question.correctAnswer
      ? question.correctAnswer.trim().toLowerCase() === selectedOption.trim().toLowerCase()
      : question.answer.trim().toLowerCase() === selectedOption.trim().toLowerCase();

    this.state.quizSelections = {
      ...this.state.quizSelections,
      [questionIndex]: {
        selectedOption,
        isCorrect,
      },
    };

    this.emit();
    return isCorrect;
  }

  public submitLessonFeedback(rating: number, feedbackNotes?: string): void {
    this.state.lessonFeedbackRating = rating;
    this.emit();
  }

  public completeMicroLesson(subtopicId?: number, score: number = 100): void {
    const sId = subtopicId || this.state.currentLesson?.subtopicId || 303;
    this.state.lessonCompleted = true;

    // Recalculate mastery
    this.recalculateMastery(sId, score >= 60);

    // If an associated milestone exists in DLP, complete it!
    const lp = this.state.learningPath;
    if (lp) {
      lp.milestoneGraph.phases.forEach((p) => {
        p.items.forEach((item) => {
          if (item.subtopicId === sId && !item.completed) {
            this.completeMilestone(item.milestoneId || item.id || '');
          }
        });
      });
    }

    this.emit();
  }

  // ==========================================================================
  // 5. AI Explanation IR Engine
  // ==========================================================================

  public loadExplanation(questionId: string): ExplanationIr | null {
    const ir = this.explanationsRepo[questionId] || SEED_EXPLANATION_IR;
    this.state.currentExplanation = ir;
    this.state.activeExplanationStepIndex = 0;
    this.state.isExplanationPlaying = false;
    this.emit();
    return ir;
  }

  public selectExplanationStep(stepIndex: number): void {
    const total = this.state.currentExplanation?.steps.length || 1;
    this.state.activeExplanationStepIndex = Math.max(0, Math.min(total - 1, stepIndex));
    this.emit();
  }

  public toggleExplanationPlay(play?: boolean): void {
    const nextVal = play !== undefined ? play : !this.state.isExplanationPlaying;
    this.state.isExplanationPlaying = nextVal;
    this.emit();
  }

  public setExplanationSpeed(speed: number): void {
    this.state.explanationSpeed = Math.max(0.5, Math.min(3, speed));
    this.emit();
  }
}
