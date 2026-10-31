/**
 * NAG Candidate Mobile - Practice Controller
 * State controller implementing Practice Hub catalog, custom drill generation,
 * active drill execution & result analysis, per-question review with deep link triggers,
 * and practice history analytics.
 */

import {
  PracticeSet,
  PracticeSession,
  PracticeResult,
  PracticeHistoryItem,
  QuestionResult,
  CustomPracticeConfig,
  SubjectAccuracyMetric,
  PracticeSourceFilter,
  PracticeSessionMode,
  SEED_PRACTICE_SETS,
  SEED_PRACTICE_RESULT,
  SEED_PRACTICE_HISTORY,
  SEED_PRACTICE_QUESTIONS_TSD,
} from './practice-models';

export type QuestionReviewFilter = 'ALL' | 'CORRECT' | 'INCORRECT' | 'SKIPPED' | 'FLAGGED';

export interface PracticeState {
  practiceSets: PracticeSet[];
  searchQuery: string;
  selectedSource: PracticeSourceFilter;
  selectedSubject: string;

  activeSession: PracticeSession | null;
  activeQuestions: QuestionResult[];
  activeQuestionIndex: number;
  userAnswers: Record<string, string | null>;
  questionTimes: Record<string, number>;
  flaggedQuestions: Set<string>;
  bookmarkedQuestions: Set<string>;

  currentResult: PracticeResult | null;
  reviewFilter: QuestionReviewFilter;

  history: PracticeHistoryItem[];
  historySearchQuery: string;

  activeTab: 'hub' | 'active_drill' | 'result' | 'history';
}

export class PracticeController {
  private state: PracticeState;
  private listeners: Set<(state: PracticeState) => void> = new Set();

  constructor(initialOverrides?: Partial<PracticeState>) {
    this.state = {
      practiceSets: [...SEED_PRACTICE_SETS],
      searchQuery: '',
      selectedSource: 'ALL',
      selectedSubject: 'ALL',

      activeSession: null,
      activeQuestions: [...SEED_PRACTICE_QUESTIONS_TSD],
      activeQuestionIndex: 0,
      userAnswers: {},
      questionTimes: {},
      flaggedQuestions: new Set(),
      bookmarkedQuestions: new Set(),

      currentResult: SEED_PRACTICE_RESULT,
      reviewFilter: 'ALL',

      history: [...SEED_PRACTICE_HISTORY],
      historySearchQuery: '',

      activeTab: 'hub',
      ...initialOverrides,
    };
  }

  public getState(): PracticeState {
    return { ...this.state };
  }

  public subscribe(listener: (state: PracticeState) => void): () => void {
    this.listeners.add(listener);
    listener(this.getState());
    return () => {
      this.listeners.delete(listener);
    };
  }

  private emit(): void {
    const s = this.getState();
    this.listeners.forEach((l) => l(s));
  }

  // ==========================================================================
  // 1. Practice Hub Catalog & Filtering
  // ==========================================================================

  public setSearchQuery(q: string): void {
    this.state.searchQuery = q;
    this.emit();
  }

  public setSourceFilter(source: PracticeSourceFilter): void {
    this.state.selectedSource = source;
    this.emit();
  }

  public setSubjectFilter(subject: string): void {
    this.state.selectedSubject = subject;
    this.emit();
  }

  public setActiveTab(tab: 'hub' | 'active_drill' | 'result' | 'history'): void {
    this.state.activeTab = tab;
    this.emit();
  }

  public getSubjects(): string[] {
    const raw = this.state.practiceSets.map((s) => s.subjectSlug).filter(Boolean);
    return Array.from(new Set(raw)).sort();
  }

  public getFilteredSets(): PracticeSet[] {
    let list = this.state.practiceSets;
    const q = this.state.searchQuery.trim().toLowerCase();
    const source = this.state.selectedSource;
    const subject = this.state.selectedSubject;

    if (source !== 'ALL') {
      list = list.filter((s) => s.source === source);
    }
    if (subject !== 'ALL') {
      list = list.filter((s) => s.subjectSlug.toLowerCase() === subject.toLowerCase());
    }
    if (q) {
      list = list.filter(
        (s) =>
          s.name.toLowerCase().includes(q) ||
          s.description.toLowerCase().includes(q) ||
          s.subjectSlug.toLowerCase().includes(q)
      );
    }
    return list;
  }

  public getKpiStats(): {
    totalSets: number;
    officialCount: number;
    curatedCount: number;
    totalQuestions: number;
    avgDuration: number;
  } {
    const sets = this.state.practiceSets;
    const totalSets = sets.length;
    const officialCount = sets.filter((s) => s.source === 'EXAM_CLONE').length;
    const curatedCount = sets.filter((s) => s.source === 'MANUAL').length;
    const totalQuestions = sets.reduce((sum, s) => sum + (s.totalQuestions || 0), 0);
    const avgDuration =
      totalSets > 0 ? Math.round(sets.reduce((sum, s) => sum + (s.durationMinutes || 0), 0) / totalSets) : 0;

    return {
      totalSets,
      officialCount,
      curatedCount,
      totalQuestions,
      avgDuration,
    };
  }

  // ==========================================================================
  // 2. Custom Practice Session Generator
  // ==========================================================================

  public generateCustomPractice(config: CustomPracticeConfig): PracticeSet {
    const id = `custom-drill-${Date.now()}`;
    const newSet: PracticeSet = {
      id,
      name: `${config.subject}: ${config.difficulty} Custom Practice Drill`,
      description: `Targeted personalized drill with ${config.questionCount} questions in ${config.mode.toLowerCase()} mode.`,
      source: 'MANUAL',
      durationMinutes: config.durationMinutes || Math.max(10, Math.round(config.questionCount * 1.5)),
      subjectSlug: config.subject,
      published: true,
      totalQuestions: config.questionCount,
      difficulty: config.difficulty,
      totalMarks: config.questionCount * 2,
      isOfficialMock: false,
    };

    this.state.practiceSets.unshift(newSet);
    this.emit();
    return newSet;
  }

  // ==========================================================================
  // 3. Active Practice Drill Execution
  // ==========================================================================

  public startSession(
    practiceSetId: string,
    mode: PracticeSessionMode = 'TIMED',
    preferredLanguage = 'en'
  ): PracticeSession {
    const pSet = this.state.practiceSets.find((s) => s.id === practiceSetId) || this.state.practiceSets[0];
    const session: PracticeSession = {
      id: `sess-${Date.now()}`,
      practiceSetId: pSet.id,
      mode,
      status: 'IN_PROGRESS',
      startedAt: new Date().toISOString(),
      totalQuestions: pSet.totalQuestions || 10,
      durationMinutes: pSet.durationMinutes || 20,
      preferredLanguage,
    };

    this.state.activeSession = session;
    this.state.activeQuestions = [...SEED_PRACTICE_QUESTIONS_TSD];
    this.state.activeQuestionIndex = 0;
    this.state.userAnswers = {};
    this.state.questionTimes = {};
    this.state.flaggedQuestions = new Set();
    this.state.activeTab = 'active_drill';
    this.emit();
    return session;
  }

  public setActiveQuestionIndex(idx: number): void {
    const total = this.state.activeQuestions.length;
    this.state.activeQuestionIndex = Math.max(0, Math.min(total - 1, idx));
    this.emit();
  }

  public saveAnswer(
    questionId: string,
    selectedOption: string | null,
    timeSpentMs: number = 30000,
    markedForReview: boolean = false
  ): void {
    this.state.userAnswers[questionId] = selectedOption;
    this.state.questionTimes[questionId] = (this.state.questionTimes[questionId] || 0) + timeSpentMs;

    if (markedForReview) {
      this.state.flaggedQuestions.add(questionId);
    } else {
      this.state.flaggedQuestions.delete(questionId);
    }
    this.emit();
  }

  public toggleFlagQuestion(questionId: string): boolean {
    let flagged = false;
    if (this.state.flaggedQuestions.has(questionId)) {
      this.state.flaggedQuestions.delete(questionId);
    } else {
      this.state.flaggedQuestions.add(questionId);
      flagged = true;
    }
    this.emit();
    return flagged;
  }

  public toggleBookmark(questionId: string): boolean {
    let bookmarked = false;
    if (this.state.bookmarkedQuestions.has(questionId)) {
      this.state.bookmarkedQuestions.delete(questionId);
    } else {
      this.state.bookmarkedQuestions.add(questionId);
      bookmarked = true;
    }

    // Also update in active questions / result
    if (this.state.currentResult) {
      const q = this.state.currentResult.questionResults.find((i) => i.questionId === questionId);
      if (q) q.bookmarked = bookmarked;
    }
    this.emit();
    return bookmarked;
  }

  public submitSession(sessionId?: string): PracticeResult {
    const session = this.state.activeSession;
    const questions = this.state.activeQuestions;
    const sId = sessionId || session?.id || `sess-${Date.now()}`;

    let correctCount = 0;
    let incorrectCount = 0;
    let skippedCount = 0;
    let totalMarksAwarded = 0;
    let totalTimeMs = 0;

    const evaluatedQuestions: QuestionResult[] = questions.map((q) => {
      const userAns = this.state.userAnswers[q.questionId] ?? null;
      const isCorrect = userAns !== null && q.correctAnswer ? userAns === q.correctAnswer : false;
      const isSkipped = userAns === null;
      const timeMs = this.state.questionTimes[q.questionId] || q.timeSpentMs || 25000;
      totalTimeMs += timeMs;

      let marks = 0;
      if (isCorrect) {
        correctCount++;
        marks = 2;
      } else if (!isSkipped) {
        incorrectCount++;
        marks = -0.5;
      } else {
        skippedCount++;
        marks = 0;
      }
      totalMarksAwarded += marks;

      return {
        ...q,
        candidateAnswer: userAns,
        correct: isCorrect,
        marksAwarded: marks,
        timeSpentMs: timeMs,
        markedForReview: this.state.flaggedQuestions.has(q.questionId),
        bookmarked: this.state.bookmarkedQuestions.has(q.questionId),
      };
    });

    const totalQuestions = questions.length;
    const accuracyPercent = totalQuestions > 0 ? Math.round((correctCount * 100) / totalQuestions) : 0;
    const totalPossibleMarks = totalQuestions * 2;
    const pSet = this.state.practiceSets.find((s) => s.id === session?.practiceSetId);

    const result: PracticeResult = {
      sessionId: sId,
      correctCount,
      incorrectCount,
      skippedCount,
      obtainedMarks: Math.max(0, totalMarksAwarded),
      totalMarks: totalPossibleMarks,
      accuracyPercent,
      practiceSetName: pSet?.name || 'Practice Drill',
      mode: session?.mode || 'TIMED',
      flaggedCount: this.state.flaggedQuestions.size,
      timeSpentSeconds: Math.round(totalTimeMs / 1000),
      topicWiseBreakdown: {
        'Quantitative Aptitude': {
          correct: correctCount,
          incorrect: incorrectCount,
          skipped: skippedCount,
          accuracy: accuracyPercent,
        },
      },
      difficultyBreakdown: {
        CORE: [correctCount, incorrectCount, skippedCount],
      },
      timingBreakdown: {
        avgTimePerQuestionSeconds: totalQuestions > 0 ? Math.round(totalTimeMs / totalQuestions / 1000) : 0,
        totalTimeSeconds: Math.round(totalTimeMs / 1000),
      },
      questionResults: evaluatedQuestions,
    };

    // Record into history
    const historyItem: PracticeHistoryItem = {
      sessionId: sId,
      practiceSetId: pSet?.id || 'set-custom',
      practiceSetName: pSet?.name || 'Custom Practice Drill',
      submittedAt: new Date().toISOString(),
      obtainedMarks: result.obtainedMarks,
      totalMarks: totalPossibleMarks,
      accuracyPercent,
      totalQuestions,
      correctCount,
      incorrectCount,
      subject: pSet?.subjectSlug || 'Quantitative Aptitude',
      mode: session?.mode || 'TIMED',
    };

    this.state.history.unshift(historyItem);
    this.state.currentResult = result;
    if (this.state.activeSession) {
      this.state.activeSession.status = 'SUBMITTED';
    }
    this.state.activeTab = 'result';
    this.emit();
    return result;
  }

  // ==========================================================================
  // 4. Result Review & Deep Link Triggers
  // ==========================================================================

  public setReviewFilter(filter: QuestionReviewFilter): void {
    this.state.reviewFilter = filter;
    this.emit();
  }

  public getFilteredResults(): QuestionResult[] {
    const list = this.state.currentResult?.questionResults || [];
    const filter = this.state.reviewFilter;
    switch (filter) {
      case 'CORRECT':
        return list.filter((q) => q.correct);
      case 'INCORRECT':
        return list.filter((q) => !q.correct && q.candidateAnswer !== null);
      case 'SKIPPED':
        return list.filter((q) => q.candidateAnswer === null);
      case 'FLAGGED':
        return list.filter((q) => q.markedForReview);
      case 'ALL':
      default:
        return list;
    }
  }

  public getStudyLessonSubtopicId(questionId: string): number | null {
    const q = this.state.currentResult?.questionResults.find((item) => item.questionId === questionId);
    return q?.subtopicId ?? 303;
  }

  // ==========================================================================
  // 5. Practice History & Analytics
  // ==========================================================================

  public setHistorySearchQuery(q: string): void {
    this.state.historySearchQuery = q;
    this.emit();
  }

  public getFilteredHistory(): PracticeHistoryItem[] {
    const q = this.state.historySearchQuery.trim().toLowerCase();
    if (!q) return this.state.history;
    return this.state.history.filter(
      (item) =>
        (item.practiceSetName && item.practiceSetName.toLowerCase().includes(q)) ||
        item.sessionId.toLowerCase().includes(q) ||
        (item.subject && item.subject.toLowerCase().includes(q))
    );
  }

  public getAverageAccuracy(): number {
    const list = this.state.history;
    if (!list.length) return 0;
    const sum = list.reduce((acc, item) => acc + (item.accuracyPercent || 0), 0);
    return Math.round((sum / list.length) * 10) / 10;
  }

  public getHighestScore(): number {
    const list = this.state.history;
    if (!list.length) return 0;
    return Math.max(...list.map((i) => i.obtainedMarks || 0));
  }

  public getTotalQuestionsSolved(): number {
    return this.state.history.reduce((acc, item) => acc + (item.totalQuestions || 0), 0);
  }

  public getSubjectWiseMetrics(): SubjectAccuracyMetric[] {
    const subjectMap: Record<string, { totalAcc: number; count: number; totalQuestions: number }> = {};

    this.state.history.forEach((h) => {
      const subj = h.subject || 'General';
      if (!subjectMap[subj]) {
        subjectMap[subj] = { totalAcc: 0, count: 0, totalQuestions: 0 };
      }
      subjectMap[subj].totalAcc += h.accuracyPercent || 0;
      subjectMap[subj].count += 1;
      subjectMap[subj].totalQuestions += h.totalQuestions || 0;
    });

    return Object.entries(subjectMap).map(([subject, data]) => ({
      subject,
      attemptsCount: data.count,
      averageAccuracy: data.count > 0 ? Math.round((data.totalAcc / data.count) * 10) / 10 : 0,
      totalQuestionsSolved: data.totalQuestions,
    }));
  }

  public reattemptSession(practiceSetId: string): PracticeSession {
    return this.startSession(practiceSetId);
  }
}
