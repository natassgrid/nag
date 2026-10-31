/**
 * NAG Candidate Mobile - Practice Feature Domain Models & Seed Data
 * Ported from candidate-delivery practice modules with full mobile feature parity.
 */

export type PracticeSessionMode = 'TIMED' | 'UNTIMED' | 'SECTION_WISE';
export type PracticeSessionStatus = 'CREATED' | 'IN_PROGRESS' | 'SUBMITTED' | 'ABANDONED';
export type PracticeSourceFilter = 'ALL' | 'EXAM_CLONE' | 'MANUAL';
export type PracticeDifficulty = 'FOUNDATION' | 'CORE' | 'ADVANCED';

export interface PracticeSet {
  id: string;
  name: string;
  description: string;
  source: 'EXAM_CLONE' | 'MANUAL' | string;
  durationMinutes: number;
  subjectSlug: string;
  published: boolean;
  totalQuestions: number;
  difficulty?: PracticeDifficulty;
  totalMarks?: number;
  isOfficialMock?: boolean;
}

export type PracticeSetSummary = PracticeSet;

export interface PracticeSession {
  id: string;
  practiceSetId: string;
  mode: PracticeSessionMode;
  status: PracticeSessionStatus;
  startedAt: string;
  totalQuestions: number;
  durationMinutes: number;
  preferredLanguage?: string;
}

export interface QuestionTranslation {
  languageCode: string;
  content: string;
  options?: Array<{ id: string; text: string }>;
  explanation?: string;
}

export interface QuestionResult {
  questionId: string;
  candidateAnswer: string | null;
  correctAnswer: string | null;
  correct: boolean;
  marksAwarded: number;
  timeSpentMs: number;
  markedForReview: boolean;
  content?: string | null;
  optionsJson?: string | null;
  explanation?: string | null;
  topic?: string | null;
  subject?: string | null;
  subtopicId?: number | null;
  questionType?: string | null;
  primaryLanguage?: string | null;
  fallbackToEnglish?: boolean;
  primaryTranslation?: QuestionTranslation | null;
  translations?: Record<string, QuestionTranslation> | null;
  bookmarked?: boolean;
}

export type PracticeQuestionResult = QuestionResult;

export interface PracticeResult {
  sessionId: string;
  correctCount: number;
  incorrectCount: number;
  skippedCount: number;
  obtainedMarks: number;
  totalMarks: number;
  accuracyPercent: number;
  topicWiseBreakdown: Record<string, Record<string, number>> | null;
  difficultyBreakdown: Record<string, number[]> | null;
  timingBreakdown: Record<string, number> | null;
  questionResults: QuestionResult[];
  practiceSetName?: string | null;
  mode?: string | null;
  flaggedCount?: number;
  timeSpentSeconds?: number;
}

export interface PracticeHistoryItem {
  sessionId: string;
  practiceSetId: string;
  practiceSetName: string | null;
  submittedAt: string | null;
  obtainedMarks: number;
  totalMarks: number;
  accuracyPercent: number;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
  subject?: string;
  mode?: PracticeSessionMode;
}

export type PracticeAttemptRecord = PracticeHistoryItem;

export interface CustomPracticeConfig {
  subject: string;
  topic?: string;
  subtopicId?: number;
  difficulty: PracticeDifficulty;
  questionCount: number;
  mode: PracticeSessionMode;
  durationMinutes?: number;
}

export interface SubjectAccuracyMetric {
  subject: string;
  attemptsCount: number;
  averageAccuracy: number;
  totalQuestionsSolved: number;
}

export interface PracticeHistoryFilter {
  searchQuery?: string;
  subject?: string;
  mode?: PracticeSessionMode;
  startDate?: string;
  endDate?: string;
}

export interface StartSessionRequest {
  practiceSetId: string;
  mode: PracticeSessionMode;
  preferredLanguage?: string;
}

export interface SaveResponseRequest {
  questionId: string;
  selectedOptionIds: string | null;
  enteredValue: string | null;
  timeSpentMs: number;
  markedForReview: boolean;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

// ============================================================================
// Seed Data for Practice Feature
// ============================================================================

export const SEED_PRACTICE_SETS: PracticeSet[] = [
  {
    id: 'set-cgl-mock-01',
    name: 'SSC CGL 2026 Tier-1 Official Simulation Mock #1',
    description: 'Full-length 100-question computer based simulation replicating actual Staff Selection Commission exam pattern.',
    source: 'EXAM_CLONE',
    durationMinutes: 60,
    subjectSlug: 'All Subjects',
    published: true,
    totalQuestions: 100,
    difficulty: 'CORE',
    totalMarks: 200,
    isOfficialMock: true,
  },
  {
    id: 'set-quant-tsd-01',
    name: 'Time, Speed & Distance: Train Relativity Speed Drill',
    description: 'Targeted drill focusing on relative velocity of opposing and parallel trains with conversion traps.',
    source: 'MANUAL',
    durationMinutes: 20,
    subjectSlug: 'Quantitative Aptitude',
    published: true,
    totalQuestions: 15,
    difficulty: 'CORE',
    totalMarks: 30,
    isOfficialMock: false,
  },
  {
    id: 'set-quant-pld-02',
    name: 'Profit, Loss & Successive Discounts Master Drill',
    description: 'Focused practice on marked price ratios, successive discount chains, and false weight calculations.',
    source: 'MANUAL',
    durationMinutes: 15,
    subjectSlug: 'Quantitative Aptitude',
    published: true,
    totalQuestions: 10,
    difficulty: 'CORE',
    totalMarks: 20,
    isOfficialMock: false,
  },
  {
    id: 'set-reasoning-syl-01',
    name: 'Syllogisms & Categorical Deduction Drill',
    description: 'Multi-premise statements and conclusions testing Venn boundary intersections and possibility cases.',
    source: 'MANUAL',
    durationMinutes: 15,
    subjectSlug: 'General Intelligence & Reasoning',
    published: true,
    totalQuestions: 12,
    difficulty: 'CORE',
    totalMarks: 24,
    isOfficialMock: false,
  },
  {
    id: 'set-jee-phy-rot-01',
    name: 'JEE Main Mechanics: Rotational Dynamics & Inertia',
    description: 'Advanced problems on moment of inertia, parallel/perpendicular axis theorems, and angular momentum.',
    source: 'EXAM_CLONE',
    durationMinutes: 45,
    subjectSlug: 'Physics',
    published: true,
    totalQuestions: 25,
    difficulty: 'ADVANCED',
    totalMarks: 100,
    isOfficialMock: true,
  },
];

export const SEED_PRACTICE_QUESTIONS_TSD: QuestionResult[] = [
  {
    questionId: 'q-tsd-1',
    candidateAnswer: 'opt-a',
    correctAnswer: 'opt-a',
    correct: true,
    marksAwarded: 2,
    timeSpentMs: 38000,
    markedForReview: false,
    content:
      'Two trains running in opposite directions at $$60\\text{ km/h}$$ and $$90\\text{ km/h}$$ cross each other in $$12\\text{ seconds}$$. If the length of the first train is $$240\\text{ m}$$, find the length of the second train.',
    optionsJson: JSON.stringify([
      { id: 'opt-a', text: '$$260\\text{ m}$$' },
      { id: 'opt-b', text: '$$280\\text{ m}$$' },
      { id: 'opt-c', text: '$$300\\text{ m}$$' },
      { id: 'opt-d', text: '$$250\\text{ m}$$' },
    ]),
    explanation:
      'Relative speed = $$150\\text{ km/h} = 150 \\times \\frac{5}{18} = \\frac{125}{3}\\text{ m/s}$$. Distance = $$\\frac{125}{3} \\times 12 = 500\\text{ m}$$. Length of second train = $$500 - 240 = 260\\text{ m}$$.',
    topic: 'Arithmetic & Number Systems',
    subject: 'Quantitative Aptitude',
    subtopicId: 303,
    questionType: 'SINGLE_CHOICE',
    bookmarked: false,
  },
  {
    questionId: 'q-tsd-2',
    candidateAnswer: 'opt-b',
    correctAnswer: 'opt-c',
    correct: false,
    marksAwarded: -0.5,
    timeSpentMs: 52000,
    markedForReview: true,
    content:
      'A train $$150\\text{ m}$$ long passes a platform $$300\\text{ m}$$ long in $$18\\text{ seconds}$$. Find the speed of the train in km/h.',
    optionsJson: JSON.stringify([
      { id: 'opt-a', text: '$$80\\text{ km/h}$$' },
      { id: 'opt-b', text: '$$85\\text{ km/h}$$' },
      { id: 'opt-c', text: '$$90\\text{ km/h}$$' },
      { id: 'opt-d', text: '$$75\\text{ km/h}$$' },
    ]),
    explanation:
      'Total distance = $$150 + 300 = 450\\text{ m}$$. Speed = $$\\frac{450}{18} = 25\\text{ m/s}$$. Convert to km/h: $$25 \\times \\frac{18}{5} = 90\\text{ km/h}$$.',
    topic: 'Arithmetic & Number Systems',
    subject: 'Quantitative Aptitude',
    subtopicId: 303,
    questionType: 'SINGLE_CHOICE',
    bookmarked: true,
  },
  {
    questionId: 'q-tsd-3',
    candidateAnswer: null,
    correctAnswer: 'opt-b',
    correct: false,
    marksAwarded: 0,
    timeSpentMs: 14000,
    markedForReview: false,
    content:
      'Two persons start walking at $$4\\text{ km/h}$$ and $$5\\text{ km/h}$$ respectively in the same direction. What will be the distance between them after $$3.5\\text{ hours}$$?',
    optionsJson: JSON.stringify([
      { id: 'opt-a', text: '$$3.0\\text{ km}$$' },
      { id: 'opt-b', text: '$$3.5\\text{ km}$$' },
      { id: 'opt-c', text: '$$4.0\\text{ km}$$' },
      { id: 'opt-d', text: '$$2.5\\text{ km}$$' },
    ]),
    explanation:
      'Relative speed = $$5 - 4 = 1\\text{ km/h}$$. Distance separation = $$1\\text{ km/h} \\times 3.5\\text{ h} = 3.5\\text{ km}$$.',
    topic: 'Arithmetic & Number Systems',
    subject: 'Quantitative Aptitude',
    subtopicId: 303,
    questionType: 'SINGLE_CHOICE',
    bookmarked: false,
  },
];

export const SEED_PRACTICE_RESULT: PracticeResult = {
  sessionId: 'sess-tsd-001',
  correctCount: 1,
  incorrectCount: 1,
  skippedCount: 1,
  obtainedMarks: 1.5,
  totalMarks: 6,
  accuracyPercent: 33.3,
  practiceSetName: 'Time, Speed & Distance: Train Relativity Speed Drill',
  mode: 'TIMED',
  flaggedCount: 1,
  timeSpentSeconds: 104,
  topicWiseBreakdown: {
    'Arithmetic & Number Systems': {
      correct: 1,
      incorrect: 1,
      skipped: 1,
      accuracy: 33.3,
    },
  },
  difficultyBreakdown: {
    CORE: [1, 1, 1],
  },
  timingBreakdown: {
    avgTimePerQuestionSeconds: 34.7,
    fastestAnswerSeconds: 14,
    slowestAnswerSeconds: 52,
  },
  questionResults: SEED_PRACTICE_QUESTIONS_TSD,
};

export const SEED_PRACTICE_HISTORY: PracticeHistoryItem[] = [
  {
    sessionId: 'sess-tsd-001',
    practiceSetId: 'set-quant-tsd-01',
    practiceSetName: 'Time, Speed & Distance: Train Relativity Speed Drill',
    submittedAt: '2026-10-18T14:30:00Z',
    obtainedMarks: 1.5,
    totalMarks: 6,
    accuracyPercent: 33.3,
    totalQuestions: 3,
    correctCount: 1,
    incorrectCount: 1,
    subject: 'Quantitative Aptitude',
    mode: 'TIMED',
  },
  {
    sessionId: 'sess-syl-002',
    practiceSetId: 'set-reasoning-syl-01',
    practiceSetName: 'Syllogisms & Categorical Deduction Drill',
    submittedAt: '2026-10-17T11:20:00Z',
    obtainedMarks: 20,
    totalMarks: 24,
    accuracyPercent: 83.3,
    totalQuestions: 12,
    correctCount: 10,
    incorrectCount: 2,
    subject: 'General Intelligence & Reasoning',
    mode: 'TIMED',
  },
  {
    sessionId: 'sess-pld-003',
    practiceSetId: 'set-quant-pld-02',
    practiceSetName: 'Profit, Loss & Successive Discounts Master Drill',
    submittedAt: '2026-10-15T16:45:00Z',
    obtainedMarks: 14,
    totalMarks: 20,
    accuracyPercent: 70,
    totalQuestions: 10,
    correctCount: 7,
    incorrectCount: 3,
    subject: 'Quantitative Aptitude',
    mode: 'UNTIMED',
  },
  {
    sessionId: 'sess-mock-004',
    practiceSetId: 'set-cgl-mock-01',
    practiceSetName: 'SSC CGL 2026 Tier-1 Official Simulation Mock #1',
    submittedAt: '2026-10-12T09:00:00Z',
    obtainedMarks: 142,
    totalMarks: 200,
    accuracyPercent: 74.5,
    totalQuestions: 100,
    correctCount: 76,
    incorrectCount: 18,
    subject: 'All Subjects',
    mode: 'TIMED',
  },
];
