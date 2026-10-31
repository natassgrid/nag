/**
 * NAG Official NTA 5-Status Computer Based Test (CBT) State Machine
 * Implements national testing standards for question navigation, palette counters,
 * and offline monotonic response packet sequencing.
 */

import { sha256Hex } from '../crypto/qr-verifier';
import {
  ExamItem,
  NtaPaletteSummary,
  NtaQuestionStatus,
  OfflineResponsePacket,
  QuestionStateEntry,
  SectionPaletteBreakdown,
} from '../models';

export interface CbtSessionMeta {
  sessionId: string;
  examId: string;
  candidateId: string;
}

export class NtaCbtStateMachine {
  private readonly items: ExamItem[];
  private readonly itemsById: Map<string, ExamItem> = new Map();
  private readonly sessionMeta: CbtSessionMeta;

  private states: Record<string, QuestionStateEntry> = {};
  private activeQuestionId: string | null = null;
  private activeQuestionStartTime: number = Date.now();
  private monotonicRevision = 0;

  constructor(
    items: ExamItem[],
    sessionMeta: CbtSessionMeta,
    initialStates?: Record<string, QuestionStateEntry>,
    initialRevision = 0
  ) {
    this.items = [...items];
    this.sessionMeta = { ...sessionMeta };
    this.monotonicRevision = initialRevision;

    for (const item of this.items) {
      this.itemsById.set(item.id, item);
    }

    if (initialStates && Object.keys(initialStates).length > 0) {
      this.hydrateStates(initialStates);
    } else {
      this.initializeDefaultStates();
    }

    // Set first question as active if available
    if (this.items.length > 0) {
      this.selectQuestion(this.items[0].id);
    }
  }

  // ============================================================================
  // Initialization & Hydration
  // ============================================================================

  private initializeDefaultStates(): void {
    this.states = {};
    for (const item of this.items) {
      this.states[item.id] = {
        questionId: item.id,
        status: 'NOT_VISITED',
        selectedOptionId: null,
        timeSpentMs: 0,
        reviewFlag: false,
      };
    }
  }

  public hydrateStates(states: Record<string, QuestionStateEntry>): void {
    this.states = {};
    for (const item of this.items) {
      if (states[item.id]) {
        this.states[item.id] = { ...states[item.id] };
      } else {
        this.states[item.id] = {
          questionId: item.id,
          status: 'NOT_VISITED',
          selectedOptionId: null,
          timeSpentMs: 0,
          reviewFlag: false,
        };
      }
    }
  }

  // ============================================================================
  // Navigation & Time Tracking
  // ============================================================================

  /**
   * Commits elapsed time on the currently active question.
   */
  public commitActiveQuestionTime(): void {
    if (!this.activeQuestionId) return;

    const currentEntry = this.states[this.activeQuestionId];
    if (currentEntry) {
      const now = Date.now();
      const elapsed = Math.max(0, now - this.activeQuestionStartTime);
      currentEntry.timeSpentMs += elapsed;
      currentEntry.lastModifiedAt = now;
      this.activeQuestionStartTime = now;
    }
  }

  /**
   * Explicitly records time spent on a specific question.
   */
  public recordTimeSpent(questionId: string, durationMs: number): void {
    const entry = this.states[questionId];
    if (entry && durationMs > 0) {
      entry.timeSpentMs += durationMs;
      entry.lastModifiedAt = Date.now();
    }
  }

  /**
   * Selects and visits a question by ID.
   * Visiting transitions NOT_VISITED -> NOT_ANSWERED (Red).
   */
  public selectQuestion(questionId: string): QuestionStateEntry | undefined {
    if (!this.itemsById.has(questionId)) {
      return undefined;
    }

    // Commit time on current question before switching
    this.commitActiveQuestionTime();

    this.activeQuestionId = questionId;
    this.activeQuestionStartTime = Date.now();

    const entry = this.states[questionId];
    if (entry) {
      if (entry.status === 'NOT_VISITED') {
        entry.status = 'NOT_ANSWERED';
        entry.visitedAt = this.activeQuestionStartTime;
      }
      return { ...entry };
    }

    return undefined;
  }

  public getActiveQuestionId(): string | null {
    return this.activeQuestionId;
  }

  public getActiveQuestion(): ExamItem | undefined {
    if (!this.activeQuestionId) return undefined;
    return this.itemsById.get(this.activeQuestionId);
  }

  public getQuestionState(questionId: string): QuestionStateEntry | undefined {
    const entry = this.states[questionId];
    return entry ? { ...entry } : undefined;
  }

  public getAllQuestionStates(): Record<string, QuestionStateEntry> {
    const copy: Record<string, QuestionStateEntry> = {};
    for (const [key, val] of Object.entries(this.states)) {
      copy[key] = { ...val };
    }
    return copy;
  }

  // ============================================================================
  // Response Actions & Transitions
  // ============================================================================

  /**
   * Selects an option on the active question without finalizing transition.
   */
  public selectOption(optionId: string | null): void {
    if (!this.activeQuestionId) return;

    const entry = this.states[this.activeQuestionId];
    if (entry) {
      entry.selectedOptionId = optionId;
      entry.lastModifiedAt = Date.now();
    }
  }

  /**
   * Save & Next:
   * - If an option is selected: transitions to ANSWERED (Green)
   * - If no option is selected: transitions/remains NOT_ANSWERED (Red)
   * - Moves to the next question.
   */
  public saveAndNext(): { current: QuestionStateEntry | undefined; next?: ExamItem } {
    if (!this.activeQuestionId) {
      return { current: undefined };
    }

    this.commitActiveQuestionTime();
    const currentEntry = this.states[this.activeQuestionId];

    if (currentEntry) {
      currentEntry.reviewFlag = false;
      if (currentEntry.selectedOptionId !== null && currentEntry.selectedOptionId !== '') {
        currentEntry.status = 'ANSWERED';
      } else {
        currentEntry.status = 'NOT_ANSWERED';
      }
      currentEntry.lastModifiedAt = Date.now();
    }

    const nextItem = this.advanceToNextQuestion();
    return {
      current: currentEntry ? { ...currentEntry } : undefined,
      next: nextItem,
    };
  }

  /**
   * Mark for Review & Next:
   * - If NO option selected: transitions to MARKED_FOR_REVIEW (Amber)
   * - If an option IS selected: transitions to ANSWERED_AND_MARKED_FOR_REVIEW (Purple)
   *   (According to NTA standards, Purple is evaluated during final scoring!)
   * - Moves to the next question.
   */
  public markForReviewAndNext(): { current: QuestionStateEntry | undefined; next?: ExamItem } {
    if (!this.activeQuestionId) {
      return { current: undefined };
    }

    this.commitActiveQuestionTime();
    const currentEntry = this.states[this.activeQuestionId];

    if (currentEntry) {
      currentEntry.reviewFlag = true;
      if (currentEntry.selectedOptionId !== null && currentEntry.selectedOptionId !== '') {
        currentEntry.status = 'ANSWERED_AND_MARKED_FOR_REVIEW';
      } else {
        currentEntry.status = 'MARKED_FOR_REVIEW';
      }
      currentEntry.lastModifiedAt = Date.now();
    }

    const nextItem = this.advanceToNextQuestion();
    return {
      current: currentEntry ? { ...currentEntry } : undefined,
      next: nextItem,
    };
  }

  /**
   * Clear Response:
   * - Clears selectedOptionId
   * - Resets reviewFlag to false
   * - Transitions to NOT_ANSWERED (Red)
   */
  public clearResponse(): QuestionStateEntry | undefined {
    if (!this.activeQuestionId) return undefined;

    const currentEntry = this.states[this.activeQuestionId];
    if (currentEntry) {
      currentEntry.selectedOptionId = null;
      currentEntry.reviewFlag = false;
      currentEntry.status = 'NOT_ANSWERED';
      currentEntry.lastModifiedAt = Date.now();
      return { ...currentEntry };
    }

    return undefined;
  }

  /**
   * Navigates to the next question index.
   */
  public advanceToNextQuestion(): ExamItem | undefined {
    if (!this.activeQuestionId || this.items.length === 0) return undefined;

    const currentIndex = this.items.findIndex(it => it.id === this.activeQuestionId);
    if (currentIndex >= 0 && currentIndex < this.items.length - 1) {
      const nextId = this.items[currentIndex + 1].id;
      this.selectQuestion(nextId);
      return this.itemsById.get(nextId);
    }

    // At the last question, stay on it
    return this.itemsById.get(this.activeQuestionId);
  }

  /**
   * Navigates to the previous question index.
   */
  public previousQuestion(): ExamItem | undefined {
    if (!this.activeQuestionId || this.items.length === 0) return undefined;

    const currentIndex = this.items.findIndex(it => it.id === this.activeQuestionId);
    if (currentIndex > 0) {
      const prevId = this.items[currentIndex - 1].id;
      this.selectQuestion(prevId);
      return this.itemsById.get(prevId);
    }

    return this.itemsById.get(this.activeQuestionId);
  }

  // ============================================================================
  // Section Filtering & Palette Metrics
  // ============================================================================

  public getQuestionsForSection(sectionId: string): ExamItem[] {
    return this.items.filter(item => item.sectionId === sectionId);
  }

  /**
   * Computes the official 5-color NTA palette summary for the entire exam or a given section.
   */
  public getPaletteSummary(sectionId?: string): NtaPaletteSummary {
    const targetItems = sectionId
      ? this.items.filter(item => item.sectionId === sectionId)
      : this.items;

    let notVisitedCount = 0;
    let notAnsweredCount = 0;
    let answeredCount = 0;
    let markedForReviewCount = 0;
    let answeredAndMarkedForReviewCount = 0;

    for (const item of targetItems) {
      const state = this.states[item.id];
      const status: NtaQuestionStatus = state ? state.status : 'NOT_VISITED';

      switch (status) {
        case 'NOT_VISITED':
          notVisitedCount++;
          break;
        case 'NOT_ANSWERED':
          notAnsweredCount++;
          break;
        case 'ANSWERED':
          answeredCount++;
          break;
        case 'MARKED_FOR_REVIEW':
          markedForReviewCount++;
          break;
        case 'ANSWERED_AND_MARKED_FOR_REVIEW':
          answeredAndMarkedForReviewCount++;
          break;
      }
    }

    // Compute per-section breakdown
    const sectionSummaries: Record<string, SectionPaletteBreakdown> = {};
    for (const item of this.items) {
      if (!sectionSummaries[item.sectionId]) {
        sectionSummaries[item.sectionId] = {
          total: 0,
          notVisited: 0,
          notAnswered: 0,
          answered: 0,
          markedForReview: 0,
          answeredAndMarkedForReview: 0,
        };
      }

      const sec = sectionSummaries[item.sectionId];
      sec.total++;

      const state = this.states[item.id];
      const status: NtaQuestionStatus = state ? state.status : 'NOT_VISITED';

      if (status === 'NOT_VISITED') sec.notVisited++;
      else if (status === 'NOT_ANSWERED') sec.notAnswered++;
      else if (status === 'ANSWERED') sec.answered++;
      else if (status === 'MARKED_FOR_REVIEW') sec.markedForReview++;
      else if (status === 'ANSWERED_AND_MARKED_FOR_REVIEW') sec.answeredAndMarkedForReview++;
    }

    return {
      totalQuestions: targetItems.length,
      notVisitedCount,
      notAnsweredCount,
      answeredCount,
      markedForReviewCount,
      answeredAndMarkedForReviewCount,
      sectionSummaries,
    };
  }

  // ============================================================================
  // Monotonic Revision & Offline Sync Packet Generation
  // ============================================================================

  /**
   * Generates a tamper-evident, monotonically incrementing offline response packet for sync.
   */
  public createOfflinePacket(): OfflineResponsePacket {
    this.commitActiveQuestionTime();
    this.monotonicRevision++;

    const statesSnapshot = this.getAllQuestionStates();
    const packetPayload = JSON.stringify({
      sessionId: this.sessionMeta.sessionId,
      examId: this.sessionMeta.examId,
      candidateId: this.sessionMeta.candidateId,
      revision: this.monotonicRevision,
      states: statesSnapshot,
    });

    const clientChecksum = sha256Hex(packetPayload);

    return {
      sessionId: this.sessionMeta.sessionId,
      examId: this.sessionMeta.examId,
      candidateId: this.sessionMeta.candidateId,
      revision: this.monotonicRevision,
      timestamp: Date.now(),
      questionStates: statesSnapshot,
      clientChecksum,
    };
  }

  public getRevision(): number {
    return this.monotonicRevision;
  }
}
