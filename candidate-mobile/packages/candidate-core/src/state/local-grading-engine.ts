/**
 * NAG Offline Local Grading & Performance Analytics Engine
 * Evaluates candidate CBT responses locally using standard NTA marking schemes:
 * - Both ANSWERED and ANSWERED_AND_MARKED_FOR_REVIEW are evaluated.
 * - Negative marking penalty applied for incorrect attempts.
 * - Unattempted (NOT_VISITED, NOT_ANSWERED, MARKED_FOR_REVIEW without selection) receive zero marks without penalty.
 */

import { sha256Hex } from '../crypto/qr-verifier';
import {
  ExamItem,
  ExamSubmissionReceipt,
  LocalGradingBreakdown,
  QuestionStateEntry,
  SectionPerformanceSummary,
  TopicMasterySummary,
} from '../models';

export interface LocalGradingSessionMeta {
  sessionId: string;
  examId: string;
  candidateId: string;
  evaluatedAt?: number;
}

/**
 * Finds the correct answer identifier for an ExamItem.
 * Checks item.correctAnswerId first, then falls back to an option with isCorrect: true.
 */
export function getCorrectAnswerId(item: ExamItem): string | undefined {
  if (item.correctAnswerId) {
    return item.correctAnswerId;
  }
  const correctOption = item.options.find(opt => opt.isCorrect === true);
  return correctOption ? correctOption.id : undefined;
}

/**
 * Determines whether a question state counts as an attempted answer in NTA evaluation.
 * In NTA standard CBT:
 * - ANSWERED: Yes
 * - ANSWERED_AND_MARKED_FOR_REVIEW: Yes (Purple with checkmark)
 * - MARKED_FOR_REVIEW: No (Amber without answer selected)
 * - NOT_ANSWERED: No
 * - NOT_VISITED: No
 */
export function isAttemptedAnswer(state?: QuestionStateEntry): boolean {
  if (!state) return false;
  if (state.status === 'ANSWERED' || state.status === 'ANSWERED_AND_MARKED_FOR_REVIEW') {
    return Boolean(state.selectedOptionId);
  }
  return false;
}

/**
 * Grades an examination session locally and generates section/topic performance summaries
 * along with a cryptographically tamper-evident submission receipt.
 */
export function gradeExamLocally(
  items: ExamItem[],
  states: Record<string, QuestionStateEntry>,
  sessionMeta: LocalGradingSessionMeta
): LocalGradingBreakdown {
  const evaluatedAt = sessionMeta.evaluatedAt ?? Date.now();

  let totalAttempted = 0;
  let totalCorrect = 0;
  let totalIncorrect = 0;
  let totalUnattempted = 0;
  let rawScore = 0;
  let maxMarks = 0;

  // Intermediate section aggregates
  const sectionMap = new Map<
    string,
    {
      sectionId: string;
      title: string;
      totalQuestions: number;
      attemptedQuestions: number;
      correctQuestions: number;
      incorrectQuestions: number;
      unattemptedQuestions: number;
      scoreObtained: number;
      maxScore: number;
      totalTimeSpentMs: number;
    }
  >();

  // Intermediate topic aggregates
  const topicMap = new Map<
    string,
    {
      topic: string;
      totalQuestions: number;
      correctCount: number;
      incorrectCount: number;
    }
  >();

  for (const item of items) {
    const marks = item.marks ?? 4;
    const negativeMarks = Math.abs(item.negativeMarks ?? 1);
    maxMarks += marks;

    const correctId = getCorrectAnswerId(item);
    const state = states[item.id];
    const attempted = isAttemptedAnswer(state);
    const timeSpentMs = state?.timeSpentMs ?? 0;

    // Initialize section bucket if not present
    if (!sectionMap.has(item.sectionId)) {
      sectionMap.set(item.sectionId, {
        sectionId: item.sectionId,
        title: `Section ${item.sectionId}`,
        totalQuestions: 0,
        attemptedQuestions: 0,
        correctQuestions: 0,
        incorrectQuestions: 0,
        unattemptedQuestions: 0,
        scoreObtained: 0,
        maxScore: 0,
        totalTimeSpentMs: 0,
      });
    }
    const sec = sectionMap.get(item.sectionId)!;
    sec.totalQuestions++;
    sec.maxScore += marks;
    sec.totalTimeSpentMs += timeSpentMs;

    // Initialize topic bucket if not present
    const topicKey = item.topic?.trim() || 'General';
    if (!topicMap.has(topicKey)) {
      topicMap.set(topicKey, {
        topic: topicKey,
        totalQuestions: 0,
        correctCount: 0,
        incorrectCount: 0,
      });
    }
    const top = topicMap.get(topicKey)!;
    top.totalQuestions++;

    if (!attempted) {
      totalUnattempted++;
      sec.unattemptedQuestions++;
    } else {
      totalAttempted++;
      sec.attemptedQuestions++;

      const candidateAnswer = state?.selectedOptionId;
      const isCorrect = correctId !== undefined && candidateAnswer === correctId;

      if (isCorrect) {
        totalCorrect++;
        rawScore += marks;
        sec.correctQuestions++;
        sec.scoreObtained += marks;
        top.correctCount++;
      } else {
        totalIncorrect++;
        rawScore -= negativeMarks;
        sec.incorrectQuestions++;
        sec.scoreObtained -= negativeMarks;
        top.incorrectCount++;
      }
    }
  }

  // Format section performance summaries
  const sectionBreakdowns: SectionPerformanceSummary[] = Array.from(
    sectionMap.values()
  ).map(sec => {
    const accuracy =
      sec.attemptedQuestions > 0
        ? Math.round((sec.correctQuestions / sec.attemptedQuestions) * 10000) / 100
        : 0;

    return {
      sectionId: sec.sectionId,
      sectionTitle: sec.title,
      totalQuestions: sec.totalQuestions,
      attemptedQuestions: sec.attemptedQuestions,
      correctQuestions: sec.correctQuestions,
      incorrectQuestions: sec.incorrectQuestions,
      unattemptedQuestions: sec.unattemptedQuestions,
      scoreObtained: Math.round(sec.scoreObtained * 100) / 100,
      maxScore: sec.maxScore,
      accuracyPercentage: accuracy,
      timeSpentSeconds: Math.round(sec.totalTimeSpentMs / 1000),
    };
  });

  // Format topic mastery summaries
  const topicBreakdowns: TopicMasterySummary[] = Array.from(topicMap.values()).map(top => {
    const mastery =
      top.totalQuestions > 0
        ? Math.round((top.correctCount / top.totalQuestions) * 10000) / 100
        : 0;

    return {
      topic: top.topic,
      totalQuestions: top.totalQuestions,
      correctCount: top.correctCount,
      incorrectCount: top.incorrectCount,
      masteryPercentage: mastery,
    };
  });

  // Calculate overall percentages
  const roundedRawScore = Math.round(rawScore * 100) / 100;
  const percentage =
    maxMarks > 0
      ? Math.round((roundedRawScore / maxMarks) * 10000) / 100
      : 0;
  const accuracyPercentage =
    totalAttempted > 0
      ? Math.round((totalCorrect / totalAttempted) * 10000) / 100
      : 0;

  // Generate deterministic submission receipt
  const receiptData = `${sessionMeta.sessionId}:${sessionMeta.examId}:${sessionMeta.candidateId}:${totalAttempted}:${roundedRawScore}:${evaluatedAt}`;
  const receiptChecksum = sha256Hex(receiptData);
  const receiptId = `REC-${sessionMeta.sessionId}-${receiptChecksum.substring(0, 12).toUpperCase()}`;

  const receipt: ExamSubmissionReceipt = {
    receiptId,
    sessionId: sessionMeta.sessionId,
    examId: sessionMeta.examId,
    candidateId: sessionMeta.candidateId,
    submittedAt: new Date(evaluatedAt).toISOString(),
    totalQuestionsAnswered: totalAttempted,
    checksum: receiptChecksum,
    localGradingCompleted: true,
  };

  return {
    sessionId: sessionMeta.sessionId,
    examId: sessionMeta.examId,
    candidateId: sessionMeta.candidateId,
    evaluatedAt,
    totalQuestions: items.length,
    totalAttempted,
    totalCorrect,
    totalIncorrect,
    totalUnattempted,
    rawScore: roundedRawScore,
    maxMarks,
    percentage,
    accuracyPercentage,
    sectionBreakdowns,
    topicBreakdowns,
    receipt,
  };
}
