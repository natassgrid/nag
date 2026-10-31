/**
 * NAG Candidate Mobile - Learn Feature Domain Models & Seed Data
 * 100% feature-parity mobile models ported from candidate-delivery learning system.
 */

// ============================================================================
// 1. Learner Profile & Onboarding Models
// ============================================================================

export type LearnerEducationLevel =
  | 'HIGH_SCHOOL'
  | 'UNDERGRADUATE'
  | 'GRADUATE'
  | 'POSTGRADUATE'
  | 'OTHER';

export type LearnerPriorExperience = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

export interface LearnerProfile {
  candidateId: string;
  displayName: string;
  educationLevel: LearnerEducationLevel;
  preferredLanguage: string;
  dailyStudyMinutes: number;
  preferredLessonTypes: LessonType[];
  priorExperience: LearnerPriorExperience;
  targetDate?: string | null;
  createdAt?: string;
  completedAt?: string | null;
}

export interface LearnerOnboardingPayload {
  displayName?: string;
  educationLevel: LearnerEducationLevel;
  preferredLanguage: string;
  dailyStudyMinutes: number;
  priorExperience?: LearnerPriorExperience;
  preferredLessonTypes?: LessonType[];
  targetExamId?: string;
  targetDate?: string;
}

export interface LearnerSyllabusSubtopic {
  subtopicId: number;
  subtopicName: string;
  weight?: number;
}

export interface LearnerSyllabusTopic {
  topicId: number;
  topicName: string;
  subtopics: LearnerSyllabusSubtopic[];
}

export interface LearnerSyllabusSubject {
  subjectId: number;
  subjectName: string;
  topics: LearnerSyllabusTopic[];
}

export interface LearnerExamSummary {
  id: string;
  code: string;
  title: string;
  name?: string;
  description?: string;
  conductingAuthority?: string;
  category?: string;
  totalQuestions?: number;
  totalMarks?: number;
  durationMinutes?: number;
  blueprintId?: string;
  sectionsCount?: number;
  syllabusNodesCount?: number;
  syllabusSummary?: {
    subjectCount?: number;
    topicCount?: number;
    subtopicCount?: number;
    subjects?: LearnerSyllabusSubject[];
  };
}

export type EnrollmentStatus = 'ACTIVE' | 'PAUSED' | 'COMPLETED';

export interface LearnerEnrollment {
  id: string;
  candidateId: string;
  examinationId: string;
  examName?: string;
  examinationTitle: string;
  examinationCode: string;
  targetDate?: string | null;
  status: EnrollmentStatus;
  diagnosticStatus?: string;
  diagnosticSessionId?: string;
  startedAt: string;
  diagnosticCompleted: boolean;
  readinessScore: number;
}

export interface CreateEnrollmentRequest {
  examinationId: string;
  targetDate?: string | null;
}

// ============================================================================
// 2. Diagnostic Assessment Models
// ============================================================================

export interface DiagnosticQuestion {
  id: string;
  questionId?: string;
  stem: string;
  content?: string;
  options: Array<{ id: string; text: string }>;
  subjectId: number;
  subjectName: string;
  topicId: number;
  topicName: string;
  subtopicId: number;
  subtopicName: string;
  difficulty: LessonDifficultyLevel | string;
  correctOptionId?: string;
  explanation?: string;
}

export interface DiagnosticSession {
  sessionId: string;
  enrollmentId: string;
  examinationId: string;
  examinationTitle: string;
  examinationCode?: string;
  status?: string;
  totalQuestions: number;
  estimatedMinutes: number;
  neutralPriorPercent?: number;
  questions: DiagnosticQuestion[];
}

export interface DiagnosticSubmitAnswer {
  questionId: string;
  selectedOptionId: string;
  timeSpentSeconds?: number;
}

export interface DiagnosticSubmitRequest {
  answers: DiagnosticSubmitAnswer[];
  isSkip?: boolean;
}

export interface DiagnosticSubmitResult {
  sessionId?: string;
  enrollmentId: string;
  status?: string;
  score: number;
  totalQuestions: number;
  correctCount?: number;
  correctAnswers?: number;
  accuracyPercent: number;
  seededSubtopicsCount: number;
  initialReadinessScore: number;
  strengths: string[];
  weaknesses: string[];
  baselineLabel?: string;
  neutralPriorPercent?: number;
  subtopicBreakdown?: Record<number, number>;
  completedAt?: string;
}

export type DiagnosticResult = DiagnosticSubmitResult;

// ============================================================================
// 3. Learner Progress, Mastery & Spaced Repetition Models
// ============================================================================

export type ActivityType = 'LESSON' | 'PRACTICE' | 'REVISION' | 'MOCK_SECTION';

export interface NextActivity {
  activityId: string;
  type: ActivityType;
  subtopicId: number;
  subtopicName: string;
  topicId: number;
  topicName: string;
  subjectId: number;
  subjectName: string;
  lessonId?: string | null;
  lessonType?: LessonType;
  difficultyLevel?: LessonDifficultyLevel;
  practiceSetId?: string | null;
  reason: string;
  urgency: 'HIGH' | 'MEDIUM' | 'LOW';
  estimatedMinutes: number;
}

export interface ActivityCompletionRequest {
  status: 'COMPLETED' | 'SKIPPED';
  score?: number;
  accuracyPercent?: number;
  helpfulRating?: number;
  timeSpentSeconds?: number;
  feedbackNotes?: string;
}

export type SubtopicMasteryStatus =
  | 'NOT_STARTED'
  | 'LEARNING'
  | 'PRACTISING'
  | 'MASTERED'
  | 'REVIEW_DUE';

export interface SubtopicMastery {
  subtopicId: number;
  subtopicName: string;
  topicId: number;
  topicName: string;
  subjectId: number;
  subjectName: string;
  masteryScore: number;
  confidence: number;
  attempts: number;
  correct: number;
  lastPracticedAt?: string | null;
  nextReviewAt?: string | null;
  status: SubtopicMasteryStatus;
  preferredLessonType?: LessonType | null;
}

export type TopicMasteryItem = SubtopicMastery;

export interface TopicMastery {
  topicId: number;
  topicName: string;
  masteryScore: number;
  subtopics: SubtopicMastery[];
}

export interface SubjectMastery {
  subjectId: number;
  subjectName: string;
  masteryScore: number;
  topics: TopicMastery[];
}

export interface LearnerProgress {
  enrollmentId: string;
  examinationId: string;
  examinationTitle: string;
  readinessScore: number;
  totalSubtopics: number;
  masteredCount: number;
  learningCount: number;
  reviewDueCount: number;
  notStartedCount: number;
  dueReviews: SubtopicMastery[];
  syllabus: SubjectMastery[];
}

// ============================================================================
// 4. Adaptive Recommendations & Study Plan Models
// ============================================================================

export interface WeakTopicRecommendation {
  topicName?: string;
  topic?: string;
  subject?: string;
  subjectName?: string;
  currentAccuracy?: number;
  subtopicsToRevise?: string[];
  subtopics?: string[];
  subtopicId?: number;
}

export type StudyRecommendation = WeakTopicRecommendation;

export interface StudyPlanItem {
  day: number;
  topicName: string;
  estimatedMinutes: number;
  tasks?: string[];
  focusTopic?: string;
  durationMinutes?: number;
  completed?: boolean;
}

export type StudyPlanDay = StudyPlanItem;

export interface Recommendation {
  id: string;
  candidateId: string;
  triggerSessionId: string | null;
  status: 'PENDING' | 'GENERATED' | 'DISMISSED';
  generatedAt: string | null;
  weakTopicRecommendations: WeakTopicRecommendation[];
  studyPlanItems: StudyPlanItem[];
  suggestedPracticeSetIds: string[];
  motivationalMessage: string | null;
}

export interface RecommendationLearnerProfile {
  candidateId: string;
  totalPracticeSessions: number;
  totalSessionsCompleted?: number;
  overallAccuracy: number;
  weakTopics: string[];
  strongTopics: string[];
  topicAccuracyMap: Record<string, { accuracy: number; attempts: number }>;
}

export type LegacyLearnerProfile = RecommendationLearnerProfile;

// ============================================================================
// 5. Dynamic Learning Path (DLP) Roadmap Models
// ============================================================================

export interface MilestoneItem {
  id?: string;
  milestoneId?: string;
  subtopicId?: number;
  subtopicName?: string;
  topicName?: string;
  subjectName?: string;
  completed?: boolean;
  status?: 'NOT_STARTED' | 'PENDING' | 'IN_PROGRESS' | 'COMPLETED';
  targetScore?: number;
  microLessonId?: string | null;
  completedAt?: string | null;
}

export type DlpMilestone = MilestoneItem;

export interface DlpSubtopicNode {
  subtopicId: number;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  status: 'LOCKED' | 'AVAILABLE' | 'IN_PROGRESS' | 'COMPLETED';
  masteryScore: number;
  prerequisiteIds?: number[];
}

export interface MilestonePhase {
  phaseNumber: number;
  name: string;
  focus: string;
  status: 'LOCKED' | 'ACTIVE' | 'IN_PROGRESS' | 'COMPLETED';
  durationWeeks: number;
  targetAccuracy: number;
  achievedAccuracy?: number | null;
  quizSessionId?: string | null;
  passThreshold: number;
  gatekeeperSessionId?: string | null;
  passed: boolean;
  items: MilestoneItem[];
}

export interface MilestoneGraph {
  phases: MilestonePhase[];
  currentPhase: number | string;
  currentPhaseNumber?: number;
  targetExam?: string;
  gatekeeperPassed?: boolean;
}

export interface DynamicLearningPath {
  id: string;
  candidateId: string;
  status: 'ACTIVE' | 'IN_PROGRESS' | 'COMPLETED' | 'SUPERSEDED' | 'ARCHIVED';
  targetExam?: string;
  examTarget?: string;
  createdAt: string;
  updatedAt?: string;
  milestoneGraph: MilestoneGraph;
  conceptualGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }> | string[];
  applicationGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }> | string[];
  gatekeeperPassed: boolean;
}

export type LearningPath = DynamicLearningPath;

export interface GatekeeperStatusResponse {
  allowed?: boolean;
  gatekeeperPassed?: boolean;
  gatekeeperRequired?: boolean;
  clearedForFullMock?: boolean;
  currentPhase?: string;
  gatekeeperSessionId?: string;
  remainingMilestonesCount?: number;
  remedialSubtopics?: string[];
  active?: boolean;
  hasGaps?: boolean;
  conceptualGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }> | string[];
  applicationGaps?: Array<{ subtopicId?: number; subtopicName: string; accuracyPercent?: number }> | string[];
  message?: string;
}

export interface SubjectMasterySummary {
  subjectName: string;
  totalSubtopics: number;
  conceptualGapsCount: number;
  applicationGapsCount: number;
  proficientCount: number;
  avgAccuracyPercent: number;
}

export interface LearningPathSummary {
  candidateId: string;
  activePathId?: string;
  examTarget?: string;
  status?: string;
  totalSubtopics: number;
  conceptualGapsCount: number;
  applicationGapsCount: number;
  proficientCount: number;
  masteryPercentage: number;
  subjectSummaries?: Record<string, SubjectMasterySummary>;
}

export interface StartAdaptiveQuizResponse {
  quizSessionId: string;
  practiceSetId?: string;
  practiceLauncherUrl?: string;
  targetAccuracy: number;
  questionCount: number;
  message?: string;
}

export interface StartGatekeeperResponse {
  gatekeeperSessionId: string;
  practiceSetId?: string;
  practiceLauncherUrl?: string;
  passThreshold: number;
  questionCount: number;
  message?: string;
}

// ============================================================================
// 6. 5-Part Micro-Lesson Models
// ============================================================================

export type LessonType =
  | 'CONCEPT'
  | 'WORKED_EXAMPLES'
  | 'TRAPS_AND_PITFALLS'
  | 'CHEAT_SHEET'
  | 'VISUAL'
  | 'QUICK_RECAP'
  | 'STORY_ANALOGY';

export type MicroLessonType = LessonType;
export type LessonDifficultyLevel = 'FOUNDATION' | 'CORE' | 'ADVANCED';
export type MicroLessonDifficulty = LessonDifficultyLevel;

export type MicroLessonPart =
  | 'CONCEPT_OVERVIEW'
  | 'CORE_FORMULAS'
  | 'WORKED_EXAMPLES'
  | 'QUICK_CHECK'
  | 'SUMMARY_TAKEAWAYS';

export interface FormulaEntry {
  name: string;
  latex: string;
  description: string;
  variables?: string[];
}

export type FormulaCard = FormulaEntry;

export interface MicroLessonStep {
  stepNumber: number;
  title: string;
  explanation: string;
  formula?: string;
  tip?: string;
}

export type StepItem = MicroLessonStep;
export type WorkedExample = MicroLessonStep;

export interface StepByStepBreakdown {
  problemStatement?: string;
  questionStem?: string;
  steps: StepItem[];
  finalAnswer?: string;
}

export interface TrapAlert {
  title: string;
  description: string;
  commonMistake?: string;
  correctApproach?: string;
}

export interface QuickCheckItem {
  question: string;
  options?: string[];
  correctAnswer?: string;
  answer: string;
  explanation?: string;
}

export type QuickCheckQuestion = QuickCheckItem;

export type LessonSlideType = 'INTRO' | 'CONCEPT' | 'FORMULA' | 'STEPS' | 'QUIZ';
export type TutorAvatarState = 'IDLE' | 'SPEAKING' | 'THINKING' | 'CELEBRATING';
export type LessonPlaybackMode = 'MANUAL' | 'AUTO';

export interface LessonTutorMediaHook {
  audioUrl?: string;
  videoUrl?: string;
  captionsUrl?: string;
  avatarMood?: TutorAvatarState;
  animationPreset?: 'slide' | 'zoom' | 'spotlight';
}

export interface LessonSlide {
  id: string;
  index: number;
  type: LessonSlideType;
  title: string;
  subtitle: string;
  icon: string;
  durationSeconds: number;
  narrationText: string;
  mediaHook?: LessonTutorMediaHook;
}

export interface QuizSelectionState {
  selectedOption: string;
  isCorrect: boolean;
}

export interface QuizSuccessStats {
  totalQuestions: number;
  answeredCount: number;
  correctCount: number;
  accuracyPercent: number;
  isAllAnswered: boolean;
  statusLabel: string;
}

export interface LessonVariantRef {
  id: string;
  lessonType: LessonType | string;
  difficultyLevel: LessonDifficultyLevel | string;
  sequenceNo?: number;
  title?: string;
  approachName?: string;
  estimatedMinutes?: number;
}

export interface MicroLessonContent {
  coreConcept: string;
  trapAlert: string | TrapAlert;
  structuredTrapAlert?: TrapAlert;
  formulaCheatSheet: string | FormulaEntry[];
  formulaEntries?: FormulaEntry[];
  stepByStepBreakdown?: StepByStepBreakdown;
  problemStatement?: string;
  questionStem?: string;
  finalAnswer?: string;
  keyTakeaways?: string[];
  quickCheckItems?: QuickCheckItem[];
}

export interface SubtopicMicroLesson {
  id: string;
  subtopicId: number;
  subtopicName: string;
  topicName: string;
  subjectName: string;
  targetExam?: string;
  title?: string;
  approachName?: string;
  sequenceNo?: number;
  estimatedMinutes?: number;
  lessonType?: LessonType | string;
  difficultyLevel?: LessonDifficultyLevel | string;
  availableVariants?: LessonVariantRef[];
  status: 'AI_GENERATED' | 'IN_REVIEW' | 'APPROVED' | 'PUBLISHED' | 'REJECTED' | 'DRAFT' | string;
  contentVersion: number;
  languageCode?: string;
  fallbackToEnglish?: boolean;
  mediaHook?: LessonTutorMediaHook;
  content: MicroLessonContent;
  createdAt?: string;
  updatedAt?: string;
}

export type MicroLesson = SubtopicMicroLesson;

export interface MicroTutorialApproach {
  id: string;
  sequenceNo: number;
  title: string;
  approachName: string;
  description: string;
  icon: string;
  badge: string;
  lessonType: LessonType;
  difficultyLevel: LessonDifficultyLevel;
  estimatedMinutes: number;
  lesson: SubtopicMicroLesson;
}

export interface MicroLessonLanguage {
  code: string;
  name: string;
  nativeName: string;
  script: string;
}

export const SUPPORTED_MICROLESSON_LANGUAGES: MicroLessonLanguage[] = [
  { code: 'en', name: 'English', nativeName: 'English', script: 'Latin' },
  { code: 'hi', name: 'Hindi', nativeName: 'हिन्दी', script: 'Devanagari' },
  { code: 'bn', name: 'Bengali', nativeName: 'বাংলা', script: 'Bengali' },
  { code: 'te', name: 'Telugu', nativeName: 'తెలుగు', script: 'Telugu' },
  { code: 'mr', name: 'Marathi', nativeName: 'मराठी', script: 'Devanagari' },
  { code: 'ta', name: 'Tamil', nativeName: 'தமிழ்', script: 'Tamil' },
  { code: 'gu', name: 'Gujarati', nativeName: 'ગુજરાતી', script: 'Gujarati' },
  { code: 'kn', name: 'Kannada', nativeName: 'ಕನ್ನಡ', script: 'Kannada' },
  { code: 'ml', name: 'Malayalam', nativeName: 'മലയാളം', script: 'Malayalam' },
  { code: 'or', name: 'Odia', nativeName: 'ଓଡ଼ିଆ', script: 'Odia' },
  { code: 'pa', name: 'Punjabi', nativeName: 'ਪੰਜਾਬੀ', script: 'Gurmukhi' },
  { code: 'as', name: 'Assamese', nativeName: 'অসমীয়া', script: 'Bengali' },
  { code: 'ur', name: 'Urdu', nativeName: 'اردو', script: 'Perso-Arabic' },
];

// ============================================================================
// 7. AI Explanation Intermediate Representation (IR) Models
// ============================================================================

export type ExplanationStatus = 'DRAFT' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';

export type StepType =
  | 'GIVEN'
  | 'APPLY_RULE'
  | 'TRANSFORM'
  | 'CONCLUDE'
  | 'CALCULATE'
  | 'SUBSTITUTE';

export interface StepOperation {
  name: string;
  target?: string;
  value?: unknown;
  params?: Record<string, unknown>;
}

export interface StepJustification {
  rule?: string;
  formula?: string;
  rationale?: string;
  why?: string;
}

export interface VerificationResult {
  passed: boolean;
  confidence?: number;
  checks?: string[];
  details?: string;
}

export interface ExplanationHint {
  id: string;
  level: number;
  text: string;
  mathExpression?: string;
}

export interface ExplanationMisconception {
  id: string;
  incorrectReasoning: string;
  whyWrong: string;
  correctedReasoning: string;
}

export interface MathExpression {
  latex?: string;
  ascii?: string;
  mathMl?: string;
  canonical?: string;
}

export interface ExplanationStep {
  id: string;
  sequence: number;
  type: StepType;
  inputs?: string[];
  operation?: StepOperation;
  before?: string;
  after?: string;
  outputs?: string[];
  justification?: StepJustification;
  representations?: string[];
  math?: MathExpression;
  verification?: VerificationResult;
  misconception?: ExplanationMisconception;
}

export interface Problem {
  statement: string;
  context?: string;
  authoritativeAnswer?: string;
}

export interface ReasoningGraph {
  nodes?: Array<{ id: string; label?: string; type?: string }>;
  edges?: Array<{ source: string; target: string; relation?: string }>;
}

export interface ExplanationDiagram {
  id?: string;
  type: string;
  title?: string;
  svgContent?: string;
  data?: Record<string, unknown>;
}

export interface ExplanationAnimation {
  type?: string;
  durationMs?: number;
  keyframes?: unknown[];
}

export interface ExplanationIr {
  schemaVersion?: string;
  explanationId: string;
  questionId: string;
  status: ExplanationStatus;
  problem?: Problem;
  concepts?: string[];
  facts?: string[];
  assumptions?: string[];
  reasoningGraph?: ReasoningGraph;
  steps: ExplanationStep[];
  verification?: VerificationResult;
  hints?: ExplanationHint[];
  misconceptions?: ExplanationMisconception[];
  diagrams?: ExplanationDiagram[];
  animation?: ExplanationAnimation;
  metadata?: Record<string, unknown>;
}

export type ExplanationIR = ExplanationIr;

// ============================================================================
// 8. Rich Seed Data Ported from learner-seed.data.ts & candidate-delivery
// ============================================================================

export const SEED_LEARNER_PROFILE: LearnerProfile = {
  candidateId: 'cand-001',
  displayName: 'Aryan Sharma',
  educationLevel: 'UNDERGRADUATE',
  preferredLanguage: 'en',
  dailyStudyMinutes: 60,
  preferredLessonTypes: ['CONCEPT', 'WORKED_EXAMPLES', 'TRAPS_AND_PITFALLS'],
  priorExperience: 'INTERMEDIATE',
  targetDate: '2026-11-15',
  createdAt: '2026-10-01T08:00:00Z',
  completedAt: '2026-10-01T08:30:00Z',
};

export const SEED_PUBLISHED_EXAMS: LearnerExamSummary[] = [
  {
    id: 'exam-cgl-2026',
    code: 'SSC-CGL-2026',
    title: 'SSC Combined Graduate Level (CGL) 2026',
    description: 'Tier-1 and Tier-2 competitive examination for central government group B and C appointments.',
    conductingAuthority: 'Staff Selection Commission (SSC)',
    category: 'Government Services',
    totalQuestions: 100,
    totalMarks: 200,
    durationMinutes: 60,
    blueprintId: 'bp-cgl-2026',
    syllabusSummary: {
      subjectCount: 4,
      topicCount: 16,
      subtopicCount: 48,
      subjects: [
        {
          subjectId: 101,
          subjectName: 'Quantitative Aptitude',
          topics: [
            {
              topicId: 201,
              topicName: 'Arithmetic & Number Systems',
              subtopics: [
                { subtopicId: 301, subtopicName: 'Percentages & Ratio-Proportion', weight: 0.12 },
                { subtopicId: 302, subtopicName: 'Profit, Loss & Discount', weight: 0.1 },
                { subtopicId: 303, subtopicName: 'Time, Speed & Distance', weight: 0.14 },
              ],
            },
            {
              topicId: 202,
              topicName: 'Algebra & Geometry',
              subtopics: [
                { subtopicId: 304, subtopicName: 'Linear & Quadratic Equations', weight: 0.08 },
                { subtopicId: 305, subtopicName: 'Circles & Triangles Theorems', weight: 0.11 },
              ],
            },
          ],
        },
        {
          subjectId: 102,
          subjectName: 'General Intelligence & Reasoning',
          topics: [
            {
              topicId: 203,
              topicName: 'Verbal & Analytical Reasoning',
              subtopics: [
                { subtopicId: 306, subtopicName: 'Syllogisms & Logical Deduction', weight: 0.12 },
                { subtopicId: 307, subtopicName: 'Coding-Decoding & Analogy', weight: 0.1 },
              ],
            },
          ],
        },
        {
          subjectId: 103,
          subjectName: 'English Comprehension',
          topics: [
            {
              topicId: 204,
              topicName: 'Grammar & Vocabulary',
              subtopics: [
                { subtopicId: 308, subtopicName: 'Error Spotting & Sentence Correction', weight: 0.11 },
                { subtopicId: 309, subtopicName: 'Reading Comprehension & Cloze Test', weight: 0.12 },
              ],
            },
          ],
        },
      ],
    },
  },
  {
    id: 'exam-jee-2026',
    code: 'JEE-MAIN-2026',
    title: 'Joint Entrance Examination (Main) 2026',
    description: 'Premier engineering admissions assessment covering Physics, Chemistry, and Mathematics.',
    conductingAuthority: 'National Testing Agency (NTA)',
    category: 'Engineering Entrance',
    totalQuestions: 90,
    totalMarks: 300,
    durationMinutes: 180,
    blueprintId: 'bp-jee-2026',
    syllabusSummary: {
      subjectCount: 3,
      topicCount: 15,
      subtopicCount: 42,
      subjects: [
        {
          subjectId: 104,
          subjectName: 'Physics',
          topics: [
            {
              topicId: 205,
              topicName: 'Mechanics',
              subtopics: [
                { subtopicId: 310, subtopicName: 'Rotational Dynamics & Inertia', weight: 0.15 },
                { subtopicId: 311, subtopicName: 'Newtonian Laws & Work-Energy', weight: 0.12 },
              ],
            },
          ],
        },
        {
          subjectId: 105,
          subjectName: 'Mathematics',
          topics: [
            {
              topicId: 206,
              topicName: 'Calculus',
              subtopics: [
                { subtopicId: 312, subtopicName: 'Definite Integrals & Areas', weight: 0.16 },
                { subtopicId: 313, subtopicName: 'Differential Equations', weight: 0.13 },
              ],
            },
          ],
        },
      ],
    },
  },
];

export const SEED_ACTIVE_ENROLLMENT: LearnerEnrollment = {
  id: 'enr-cgl-101',
  candidateId: 'cand-001',
  examinationId: 'exam-cgl-2026',
  examinationTitle: 'SSC Combined Graduate Level (CGL) 2026',
  examinationCode: 'SSC-CGL-2026',
  targetDate: '2026-11-15',
  status: 'ACTIVE',
  startedAt: '2026-10-02T09:00:00Z',
  diagnosticCompleted: true,
  readinessScore: 72,
};

export const SEED_DIAGNOSTIC_SESSION: DiagnosticSession = {
  sessionId: 'diag-session-001',
  enrollmentId: 'enr-cgl-101',
  examinationId: 'exam-cgl-2026',
  examinationTitle: 'SSC Combined Graduate Level (CGL) 2026',
  totalQuestions: 6,
  estimatedMinutes: 12,
  questions: [
    {
      id: 'dq-1',
      stem: 'If $$A$$ is $$25\\%$$ more than $$B$$, by what percentage is $$B$$ less than $$A$$?',
      options: [
        { id: 'opt-a', text: '$$20\\%$$' },
        { id: 'opt-b', text: '$$25\\%$$' },
        { id: 'opt-c', text: '$$16.67\\%$$' },
        { id: 'opt-d', text: '$$33.33\\%$$' },
      ],
      subjectId: 101,
      subjectName: 'Quantitative Aptitude',
      topicId: 201,
      topicName: 'Arithmetic & Number Systems',
      subtopicId: 301,
      subtopicName: 'Percentages & Ratio-Proportion',
      difficulty: 'CORE',
      correctOptionId: 'opt-a',
      explanation: 'Let $$B = 100$$, then $$A = 125$$. The difference is $$25$$. Percentage less = $$\\frac{25}{125} \\times 100 = 20\\%$$.',
    },
    {
      id: 'dq-2',
      stem: 'A trader marks an article $$40\\%$$ above cost and gives a discount of $$20\\%$$. What is the net profit percentage?',
      options: [
        { id: 'opt-a', text: '$$12\\%$$' },
        { id: 'opt-b', text: '$$15\\%$$' },
        { id: 'opt-c', text: '$$20\\%$$' },
        { id: 'opt-d', text: '$$8\\%$$' },
      ],
      subjectId: 101,
      subjectName: 'Quantitative Aptitude',
      topicId: 201,
      topicName: 'Arithmetic & Number Systems',
      subtopicId: 302,
      subtopicName: 'Profit, Loss & Discount',
      difficulty: 'CORE',
      correctOptionId: 'opt-a',
      explanation: '$$1.40 \\times 0.80 = 1.12$$, giving a net profit of $$12\\%$$.',
    },
    {
      id: 'dq-3',
      stem: 'Two trains running in opposite directions at $$60\\text{ km/h}$$ and $$90\\text{ km/h}$$ cross each other in $$12\\text{ s}$$. If the length of the first train is $$240\\text{ m}$$, find the length of the second train.',
      options: [
        { id: 'opt-a', text: '$$260\\text{ m}$$' },
        { id: 'opt-b', text: '$$280\\text{ m}$$' },
        { id: 'opt-c', text: '$$300\\text{ m}$$' },
        { id: 'opt-d', text: '$$250\\text{ m}$$' },
      ],
      subjectId: 101,
      subjectName: 'Quantitative Aptitude',
      topicId: 201,
      topicName: 'Arithmetic & Number Systems',
      subtopicId: 303,
      subtopicName: 'Time, Speed & Distance',
      difficulty: 'ADVANCED',
      correctOptionId: 'opt-a',
      explanation: 'Relative speed = $$150\\text{ km/h} = 150 \\times \\frac{5}{18} = \\frac{125}{3}\\text{ m/s}$$. Total distance = $$\\frac{125}{3} \\times 12 = 500\\text{ m}$$. Length of second train = $$500 - 240 = 260\\text{ m}$$.',
    },
    {
      id: 'dq-4',
      stem: '**Statements:**\n1. All squares are rectangles.\n2. All rectangles are polygons.\n\n**Conclusions:**\nI. Some polygons are squares.\nII. Every square is a polygon.',
      options: [
        { id: 'opt-a', text: 'Only Conclusion I follows' },
        { id: 'opt-b', text: 'Only Conclusion II follows' },
        { id: 'opt-c', text: 'Both Conclusions I and II follow' },
        { id: 'opt-d', text: 'Neither follows' },
      ],
      subjectId: 102,
      subjectName: 'General Intelligence & Reasoning',
      topicId: 203,
      topicName: 'Verbal & Analytical Reasoning',
      subtopicId: 306,
      subtopicName: 'Syllogisms & Logical Deduction',
      difficulty: 'CORE',
      correctOptionId: 'opt-c',
      explanation: 'Since Squares $$\\subseteq$$ Rectangles $$\\subseteq$$ Polygons, both conclusions logically follow.',
    },
    {
      id: 'dq-5',
      stem: 'Find the tangent length drawn from an external point $$P$$ at distance $$13\\text{ cm}$$ from the centre of a circle of radius $$5\\text{ cm}$$.',
      options: [
        { id: 'opt-a', text: '$$12\\text{ cm}$$' },
        { id: 'opt-b', text: '$$10\\text{ cm}$$' },
        { id: 'opt-c', text: '$$8\\text{ cm}$$' },
        { id: 'opt-d', text: '$$14\\text{ cm}$$' },
      ],
      subjectId: 101,
      subjectName: 'Quantitative Aptitude',
      topicId: 202,
      topicName: 'Algebra & Geometry',
      subtopicId: 305,
      subtopicName: 'Circles & Triangles Theorems',
      difficulty: 'CORE',
      correctOptionId: 'opt-a',
      explanation: 'By Pythagoras theorem: $$L = \\sqrt{13^2 - 5^2} = \\sqrt{144} = 12\\text{ cm}$$.',
    },
    {
      id: 'dq-6',
      stem: 'Identify the error in the sentence: *"Neither of the two candidates have submitted their attested documents before the deadline."*',
      options: [
        { id: 'opt-a', text: 'Replace "have" with "has"' },
        { id: 'opt-b', text: 'Replace "Neither" with "None"' },
        { id: 'opt-c', text: 'Replace "their" with "them"' },
        { id: 'opt-d', text: 'No error' },
      ],
      subjectId: 103,
      subjectName: 'English Comprehension',
      topicId: 204,
      topicName: 'Grammar & Vocabulary',
      subtopicId: 308,
      subtopicName: 'Error Spotting & Sentence Correction',
      difficulty: 'FOUNDATION',
      correctOptionId: 'opt-a',
      explanation: 'Subject "Neither" is singular, so it takes the singular verb "has".',
    },
  ],
};

export const SEED_DIAGNOSTIC_RESULT: DiagnosticSubmitResult = {
  enrollmentId: 'enr-cgl-101',
  score: 5,
  totalQuestions: 6,
  accuracyPercent: 83.3,
  seededSubtopicsCount: 6,
  initialReadinessScore: 68,
  strengths: [
    'Percentages & Ratio-Proportion (92% mastery)',
    'Syllogisms & Logical Deduction (88% mastery)',
    'Circles & Triangles Theorems (85% mastery)',
  ],
  weaknesses: [
    'Time, Speed & Distance (Relativity & Trains)',
    'Profit, Loss & Discount (Successive Discounts & Marked Price)',
  ],
};

export const SEED_NEXT_ACTIVITY: NextActivity = {
  activityId: 'act-top-001',
  type: 'LESSON',
  subtopicId: 303,
  subtopicName: 'Time, Speed & Distance',
  topicId: 201,
  topicName: 'Arithmetic & Number Systems',
  subjectId: 101,
  subjectName: 'Quantitative Aptitude',
  lessonId: 'lesson-tsd-303-worked',
  lessonType: 'WORKED_EXAMPLES',
  difficultyLevel: 'CORE',
  reason: 'High blueprint weight (14%) & diagnostic identified multi-train relativity gap',
  urgency: 'HIGH',
  estimatedMinutes: 8,
};

export const SEED_LEARNER_PROGRESS: LearnerProgress = {
  enrollmentId: 'enr-cgl-101',
  examinationId: 'exam-cgl-2026',
  examinationTitle: 'SSC Combined Graduate Level (CGL) 2026',
  readinessScore: 74,
  totalSubtopics: 9,
  masteredCount: 4,
  learningCount: 2,
  reviewDueCount: 2,
  notStartedCount: 1,
  dueReviews: [
    {
      subtopicId: 301,
      subtopicName: 'Percentages & Ratio-Proportion',
      topicId: 201,
      topicName: 'Arithmetic & Number Systems',
      subjectId: 101,
      subjectName: 'Quantitative Aptitude',
      masteryScore: 88,
      confidence: 0.85,
      attempts: 24,
      correct: 21,
      lastPracticedAt: '2026-10-10T11:00:00Z',
      nextReviewAt: '2026-10-18T00:00:00Z',
      status: 'REVIEW_DUE',
      preferredLessonType: 'CHEAT_SHEET',
    },
    {
      subtopicId: 306,
      subtopicName: 'Syllogisms & Logical Deduction',
      topicId: 203,
      topicName: 'Verbal & Analytical Reasoning',
      subjectId: 102,
      subjectName: 'General Intelligence & Reasoning',
      masteryScore: 82,
      confidence: 0.8,
      attempts: 18,
      correct: 15,
      lastPracticedAt: '2026-10-12T14:30:00Z',
      nextReviewAt: '2026-10-18T00:00:00Z',
      status: 'REVIEW_DUE',
      preferredLessonType: 'TRAPS_AND_PITFALLS',
    },
  ],
  syllabus: [
    {
      subjectId: 101,
      subjectName: 'Quantitative Aptitude',
      masteryScore: 71,
      topics: [
        {
          topicId: 201,
          topicName: 'Arithmetic & Number Systems',
          masteryScore: 73,
          subtopics: [
            {
              subtopicId: 301,
              subtopicName: 'Percentages & Ratio-Proportion',
              topicId: 201,
              topicName: 'Arithmetic & Number Systems',
              subjectId: 101,
              subjectName: 'Quantitative Aptitude',
              masteryScore: 88,
              confidence: 0.85,
              attempts: 24,
              correct: 21,
              lastPracticedAt: '2026-10-10T11:00:00Z',
              nextReviewAt: '2026-10-18T00:00:00Z',
              status: 'REVIEW_DUE',
              preferredLessonType: 'CHEAT_SHEET',
            },
            {
              subtopicId: 302,
              subtopicName: 'Profit, Loss & Discount',
              topicId: 201,
              topicName: 'Arithmetic & Number Systems',
              subjectId: 101,
              subjectName: 'Quantitative Aptitude',
              masteryScore: 54,
              confidence: 0.6,
              attempts: 12,
              correct: 6,
              lastPracticedAt: '2026-10-16T09:00:00Z',
              nextReviewAt: '2026-10-21T00:00:00Z',
              status: 'LEARNING',
              preferredLessonType: 'WORKED_EXAMPLES',
            },
            {
              subtopicId: 303,
              subtopicName: 'Time, Speed & Distance',
              topicId: 201,
              topicName: 'Arithmetic & Number Systems',
              subjectId: 101,
              subjectName: 'Quantitative Aptitude',
              masteryScore: 42,
              confidence: 0.5,
              attempts: 8,
              correct: 3,
              lastPracticedAt: '2026-10-17T15:00:00Z',
              nextReviewAt: '2026-10-19T00:00:00Z',
              status: 'PRACTISING',
              preferredLessonType: 'CONCEPT',
            },
          ],
        },
        {
          topicId: 202,
          topicName: 'Algebra & Geometry',
          masteryScore: 68,
          subtopics: [
            {
              subtopicId: 304,
              subtopicName: 'Linear & Quadratic Equations',
              topicId: 202,
              topicName: 'Algebra & Geometry',
              subjectId: 101,
              subjectName: 'Quantitative Aptitude',
              masteryScore: 30,
              confidence: 0.35,
              attempts: 3,
              correct: 1,
              lastPracticedAt: null,
              nextReviewAt: null,
              status: 'NOT_STARTED',
              preferredLessonType: 'CONCEPT',
            },
            {
              subtopicId: 305,
              subtopicName: 'Circles & Triangles Theorems',
              topicId: 202,
              topicName: 'Algebra & Geometry',
              subjectId: 101,
              subjectName: 'Quantitative Aptitude',
              masteryScore: 91,
              confidence: 0.92,
              attempts: 30,
              correct: 28,
              lastPracticedAt: '2026-10-15T18:00:00Z',
              nextReviewAt: '2026-10-25T00:00:00Z',
              status: 'MASTERED',
              preferredLessonType: 'VISUAL',
            },
          ],
        },
      ],
    },
    {
      subjectId: 102,
      subjectName: 'General Intelligence & Reasoning',
      masteryScore: 84,
      topics: [
        {
          topicId: 203,
          topicName: 'Verbal & Analytical Reasoning',
          masteryScore: 84,
          subtopics: [
            {
              subtopicId: 306,
              subtopicName: 'Syllogisms & Logical Deduction',
              topicId: 203,
              topicName: 'Verbal & Analytical Reasoning',
              subjectId: 102,
              subjectName: 'General Intelligence & Reasoning',
              masteryScore: 82,
              confidence: 0.8,
              attempts: 18,
              correct: 15,
              lastPracticedAt: '2026-10-12T14:30:00Z',
              nextReviewAt: '2026-10-18T00:00:00Z',
              status: 'REVIEW_DUE',
              preferredLessonType: 'TRAPS_AND_PITFALLS',
            },
            {
              subtopicId: 307,
              subtopicName: 'Coding-Decoding & Analogy',
              topicId: 203,
              topicName: 'Verbal & Analytical Reasoning',
              subjectId: 102,
              subjectName: 'General Intelligence & Reasoning',
              masteryScore: 94,
              confidence: 0.95,
              attempts: 20,
              correct: 19,
              lastPracticedAt: '2026-10-14T10:00:00Z',
              nextReviewAt: '2026-10-26T00:00:00Z',
              status: 'MASTERED',
              preferredLessonType: 'QUICK_RECAP',
            },
          ],
        },
      ],
    },
    {
      subjectId: 103,
      subjectName: 'English Comprehension',
      masteryScore: 82,
      topics: [
        {
          topicId: 204,
          topicName: 'Grammar & Vocabulary',
          masteryScore: 82,
          subtopics: [
            {
              subtopicId: 308,
              subtopicName: 'Error Spotting & Sentence Correction',
              topicId: 204,
              topicName: 'Grammar & Vocabulary',
              subjectId: 103,
              subjectName: 'English Comprehension',
              masteryScore: 80,
              confidence: 0.78,
              attempts: 15,
              correct: 12,
              lastPracticedAt: '2026-10-16T17:00:00Z',
              nextReviewAt: '2026-10-22T00:00:00Z',
              status: 'MASTERED',
              preferredLessonType: 'TRAPS_AND_PITFALLS',
            },
            {
              subtopicId: 309,
              subtopicName: 'Reading Comprehension & Cloze Test',
              topicId: 204,
              topicName: 'Grammar & Vocabulary',
              subjectId: 103,
              subjectName: 'English Comprehension',
              masteryScore: 85,
              confidence: 0.82,
              attempts: 10,
              correct: 8,
              lastPracticedAt: '2026-10-15T12:00:00Z',
              nextReviewAt: '2026-10-24T00:00:00Z',
              status: 'MASTERED',
              preferredLessonType: 'WORKED_EXAMPLES',
            },
          ],
        },
      ],
    },
  ],
};

export const SEED_DYNAMIC_LEARNING_PATH: DynamicLearningPath = {
  id: 'dlp-cgl-001',
  candidateId: 'cand-001',
  status: 'ACTIVE',
  targetExam: 'SSC-CGL-2026',
  examTarget: 'SSC-CGL-2026',
  createdAt: '2026-10-02T09:10:00Z',
  gatekeeperPassed: false,
  milestoneGraph: {
    currentPhase: 1,
    currentPhaseNumber: 1,
    targetExam: 'SSC-CGL-2026',
    gatekeeperPassed: false,
    phases: [
      {
        phaseNumber: 1,
        name: 'Phase 1: Remediation & Foundational Gaps',
        focus: 'Targeted micro-lessons on diagnostic weak subtopics',
        status: 'ACTIVE',
        durationWeeks: 2,
        targetAccuracy: 75,
        achievedAccuracy: 58,
        passThreshold: 70,
        passed: false,
        items: [
          {
            id: 'm-101',
            milestoneId: 'm-101',
            subtopicId: 303,
            subtopicName: 'Time, Speed & Distance',
            topicName: 'Arithmetic & Number Systems',
            subjectName: 'Quantitative Aptitude',
            completed: false,
            status: 'IN_PROGRESS',
            targetScore: 80,
            microLessonId: 'lesson-tsd-303-worked',
          },
          {
            id: 'm-102',
            milestoneId: 'm-102',
            subtopicId: 302,
            subtopicName: 'Profit, Loss & Discount',
            topicName: 'Arithmetic & Number Systems',
            subjectName: 'Quantitative Aptitude',
            completed: false,
            status: 'PENDING',
            targetScore: 75,
            microLessonId: 'lesson-pld-302-worked',
          },
          {
            id: 'm-103',
            milestoneId: 'm-103',
            subtopicId: 304,
            subtopicName: 'Linear & Quadratic Equations',
            topicName: 'Algebra & Geometry',
            subjectName: 'Quantitative Aptitude',
            completed: false,
            status: 'PENDING',
            targetScore: 70,
            microLessonId: 'lesson-alg-304-concept',
          },
        ],
      },
      {
        phaseNumber: 2,
        name: 'Phase 2: Adaptive Practice Drills',
        focus: 'Timed Sectional Practice and Mixed Problem Sets',
        status: 'LOCKED',
        durationWeeks: 2,
        targetAccuracy: 80,
        achievedAccuracy: null,
        passThreshold: 75,
        passed: false,
        items: [
          {
            id: 'm-201',
            milestoneId: 'm-201',
            subtopicId: 301,
            subtopicName: 'Percentages & Ratio-Proportion',
            topicName: 'Arithmetic & Number Systems',
            subjectName: 'Quantitative Aptitude',
            completed: false,
            status: 'NOT_STARTED',
            targetScore: 85,
          },
          {
            id: 'm-202',
            milestoneId: 'm-202',
            subtopicId: 306,
            subtopicName: 'Syllogisms & Logical Deduction',
            topicName: 'Verbal & Analytical Reasoning',
            subjectName: 'General Intelligence & Reasoning',
            completed: false,
            status: 'NOT_STARTED',
            targetScore: 85,
          },
        ],
      },
      {
        phaseNumber: 3,
        name: 'Phase 3: High-Fidelity Mock & Gatekeeper Evaluation',
        focus: 'Comprehensive 100-Question Simulated CBT Exam',
        status: 'LOCKED',
        durationWeeks: 1,
        targetAccuracy: 85,
        achievedAccuracy: null,
        passThreshold: 80,
        passed: false,
        items: [
          {
            id: 'm-301',
            milestoneId: 'm-301',
            subtopicId: 305,
            subtopicName: 'Full Mock Readiness Gatekeeper',
            topicName: 'Comprehensive Mock Examination',
            subjectName: 'All Sections',
            completed: false,
            status: 'NOT_STARTED',
            targetScore: 80,
          },
        ],
      },
    ],
  },
  conceptualGaps: [
    { subtopicId: 303, subtopicName: 'Time, Speed & Distance', accuracyPercent: 42 },
    { subtopicId: 302, subtopicName: 'Profit, Loss & Discount', accuracyPercent: 54 },
  ],
  applicationGaps: [
    { subtopicId: 304, subtopicName: 'Linear & Quadratic Equations', accuracyPercent: 30 },
  ],
};

export const SEED_RECOMMENDATION: Recommendation = {
  id: 'rec-001',
  candidateId: 'cand-001',
  triggerSessionId: 'diag-session-001',
  status: 'GENERATED',
  generatedAt: '2026-10-02T09:15:00Z',
  motivationalMessage: 'Focus on relative speed conversions and successive discount chains to unlock a 15+ mark boost.',
  weakTopicRecommendations: [
    {
      subtopicId: 303,
      topicName: 'Arithmetic & Number Systems',
      subjectName: 'Quantitative Aptitude',
      currentAccuracy: 42,
      subtopicsToRevise: ['Time, Speed & Distance (Relative Speed of Trains)'],
    },
    {
      subtopicId: 302,
      topicName: 'Arithmetic & Number Systems',
      subjectName: 'Quantitative Aptitude',
      currentAccuracy: 54,
      subtopicsToRevise: ['Profit, Loss & Discount (Successive Discounts Formula)'],
    },
    {
      subtopicId: 304,
      topicName: 'Algebra & Geometry',
      subjectName: 'Quantitative Aptitude',
      currentAccuracy: 30,
      subtopicsToRevise: ['Linear & Quadratic Equations (Roots & Discriminant)'],
    },
  ],
  studyPlanItems: [
    {
      day: 1,
      topicName: 'Time, Speed & Distance: Train Relativity',
      estimatedMinutes: 45,
      tasks: ['Complete Micro-Lesson #303', 'Attempt 10-Question Drill on Opposing Trains'],
      completed: true,
    },
    {
      day: 2,
      topicName: 'Profit & Loss: Successive Discounts & Markup',
      estimatedMinutes: 40,
      tasks: ['Review Formula Cheat Sheet', 'Solve 8 Practice Questions'],
      completed: false,
    },
    {
      day: 3,
      topicName: 'Algebra: Quadratic Roots & Discriminant',
      estimatedMinutes: 45,
      tasks: ['Study Micro-Lesson #304', 'Graph Parabola Vertex'],
      completed: false,
    },
    {
      day: 4,
      topicName: 'Spaced Review: Percentages & Ratio Spaced Repetition',
      estimatedMinutes: 30,
      tasks: ['Quick-Check Recap on Percentages', 'Spaced Flashcard Review'],
      completed: false,
    },
    {
      day: 5,
      topicName: 'Verbal Reasoning: Syllogism Venn Representations',
      estimatedMinutes: 45,
      tasks: ['Deductive Reasoning Drill', 'Euler Diagram Validation'],
      completed: false,
    },
    {
      day: 6,
      topicName: 'Grammar: Subject-Verb Agreement & Error Spotting',
      estimatedMinutes: 35,
      tasks: ['Pronoun-Antecedent Rules', '15 Spotting Error Drills'],
      completed: false,
    },
    {
      day: 7,
      topicName: 'Sectional Mixed Practice & Phase 1 Milestone Test',
      estimatedMinutes: 60,
      tasks: ['Attempt Phase 1 Quiz', 'Review Diagnostic Analytics'],
      completed: false,
    },
  ],
  suggestedPracticeSetIds: ['set-cgl-quant-drill-01', 'set-cgl-reasoning-02'],
};

export const SEED_MICRO_LESSONS: Record<number, SubtopicMicroLesson> = {
  303: {
    id: 'lesson-tsd-303-worked',
    subtopicId: 303,
    subtopicName: 'Time, Speed & Distance',
    topicName: 'Arithmetic & Number Systems',
    subjectName: 'Quantitative Aptitude',
    targetExam: 'SSC-CGL-2026',
    title: 'Relative Speed, Trains & Crossing Dynamics',
    approachName: 'Worked Examples & Relativity Dynamics',
    lessonType: 'WORKED_EXAMPLES',
    difficultyLevel: 'CORE',
    estimatedMinutes: 8,
    status: 'PUBLISHED',
    contentVersion: 2,
    languageCode: 'en',
    content: {
      coreConcept:
        'When two objects move towards each other at speeds $$S_1$$ and $$S_2$$, their relative speed is $$S_{rel} = S_1 + S_2$$. When moving in the same direction, relative speed is $$S_{rel} = |S_1 - S_2|$$. Total crossing distance equals the sum of both lengths: $$D = L_1 + L_2$$. Remember to convert km/h to m/s multiplying by $$\\frac{5}{18}$$.',
      trapAlert: {
        title: 'Unit Mismatch & Length Trap',
        description:
          'Forgetting to multiply km/h by $$\\frac{5}{18}$$ before equating with seconds is the #1 reason for incorrect answers in competitive exams. Furthermore, if a train crosses a pole or man, distance is solely $$L_1$$, but crossing a platform or another train requires $$L_1 + L_2$$.',
        commonMistake: 'Using $$150\\text{ km/h} \\times 12\\text{ s}$$ without converting km/h to m/s.',
        correctApproach: 'Always convert units first: $$S\\text{ (m/s)} = S\\text{ (km/h)} \\times \\frac{5}{18}$$.',
      },
      formulaCheatSheet: [
        {
          name: 'Unit Conversion Factor',
          latex: '$$1\\text{ km/h} = \\frac{5}{18}\\text{ m/s}$$',
          description: 'Fundamental speed unit normalization factor.',
          variables: ['S (km/h)', 'S (m/s)'],
        },
        {
          name: 'Opposite Direction Relativity',
          latex: '$$S_{rel} = S_1 + S_2$$',
          description: 'Effective closing velocity for opposite directions.',
          variables: ['S_1', 'S_2'],
        },
        {
          name: 'Same Direction Relativity',
          latex: '$$S_{rel} = |S_1 - S_2|$$',
          description: 'Effective relative velocity for same direction pursuit.',
          variables: ['S_1', 'S_2'],
        },
        {
          name: 'Crossing Time Equation',
          latex: '$$t = \\frac{L_1 + L_2}{S_{rel}}$$',
          description: 'Time taken for two extended bodies to completely pass each other.',
          variables: ['t', 'L_1', 'L_2', 'S_{rel}'],
        },
      ],
      stepByStepBreakdown: {
        problemStatement:
          'Two trains running in opposite directions at $$60\\text{ km/h}$$ and $$90\\text{ km/h}$$ cross each other in $$12\\text{ seconds}$$. If the length of the first train is $$240\\text{ m}$$, find the length of the second train $$L_2$$.',
        steps: [
          {
            stepNumber: 1,
            title: 'Compute Relative Speed in km/h',
            explanation: 'Since the trains move in opposite directions, speeds add up.',
            formula: '$$S_{rel} = 60 + 90 = 150\\text{ km/h}$$',
            tip: 'Opposite directions mean the closing speed is the arithmetic sum.',
          },
          {
            stepNumber: 2,
            title: 'Convert Relative Speed to m/s',
            explanation: 'Multiply relative speed by the conversion fraction $$\\frac{5}{18}$$.',
            formula: '$$S_{rel} = 150 \\times \\frac{5}{18} = \\frac{125}{3}\\text{ m/s}$$',
            tip: 'Keep as a fraction $$\\frac{125}{3}$$ to avoid round-off inaccuracies.',
          },
          {
            stepNumber: 3,
            title: 'Calculate Total Distance Traversed',
            explanation: 'Distance is relative speed multiplied by crossing time ($$12\\text{ s}$$).',
            formula: '$$D = \\frac{125}{3} \\times 12 = 125 \\times 4 = 500\\text{ m}$$',
            tip: '$$3$$ divides $$12$$ cleanly into $$4$$.',
          },
          {
            stepNumber: 4,
            title: 'Deduct Length of First Train',
            explanation: 'Total distance equals $$L_1 + L_2$$. Therefore, $$L_2 = D - L_1$$.',
            formula: '$$L_2 = 500\\text{ m} - 240\\text{ m} = 260\\text{ m}$$',
            tip: 'Final Answer: $$260\\text{ m}$$.',
          },
        ],
        finalAnswer: '$$260\\text{ m}$$',
      },
      keyTakeaways: [
        'Relative speed adds up for opposite direction motion: $$S_{rel} = S_1 + S_2$$.',
        'Always convert speed to m/s using $$\\frac{5}{18}$$ before multiplying with time in seconds.',
        'Total distance is always $$L_1 + L_2$$ when two trains pass each other.',
      ],
      quickCheckItems: [
        {
          question: 'If two trains run in the same direction at $$80\\text{ km/h}$$ and $$44\\text{ km/h}$$, what is their relative speed in m/s?',
          options: ['$$10\\text{ m/s}$$', '$$20\\text{ m/s}$$', '$$15\\text{ m/s}$$', '$$12\\text{ m/s}$$'],
          correctAnswer: '$$10\\text{ m/s}$$',
          answer: '$$10\\text{ m/s}$$',
          explanation: 'Relative speed = $$80 - 44 = 36\\text{ km/h}$$. In m/s: $$36 \\times \\frac{5}{18} = 10\\text{ m/s}$$.',
        },
        {
          question: 'A train of length $$180\\text{ m}$$ passes a telegraph pole in $$9\\text{ seconds}$$. What is its speed in km/h?',
          options: ['$$72\\text{ km/h}$$', '$$60\\text{ km/h}$$', '$$80\\text{ km/h}$$', '$$54\\text{ km/h}$$'],
          correctAnswer: '$$72\\text{ km/h}$$',
          answer: '$$72\\text{ km/h}$$',
          explanation: 'Speed = $$\\frac{180}{9} = 20\\text{ m/s}$$. Convert to km/h: $$20 \\times \\frac{18}{5} = 72\\text{ km/h}$$.',
        },
      ],
    },
  },
  301: {
    id: 'lesson-pct-301-concept',
    subtopicId: 301,
    subtopicName: 'Percentages & Ratio-Proportion',
    topicName: 'Arithmetic & Number Systems',
    subjectName: 'Quantitative Aptitude',
    targetExam: 'SSC-CGL-2026',
    title: 'Percentage Changes & Inverse Proportions',
    approachName: 'Conceptual Foundation',
    lessonType: 'CONCEPT',
    difficultyLevel: 'CORE',
    estimatedMinutes: 6,
    status: 'PUBLISHED',
    contentVersion: 1,
    languageCode: 'en',
    content: {
      coreConcept:
        'If value $$A$$ is $$x\\%$$ greater than $$B$$, then $$B$$ is smaller than $$A$$ by $$\\frac{x}{100 + x} \\times 100\\%$$. This symmetry inversion governs expenditure, speed-time reciprocal variations, and profit fractions.',
      trapAlert: {
        title: 'Symmetry Misconception',
        description: 'Assuming that if $$A$$ is $$25\\%$$ more than $$B$$, $$B$$ must be $$25\\%$$ less than $$A$$. The base changes from $$B$$ to $$A$$.',
        commonMistake: 'Selecting 25% directly without recalculating the ratio on the new base.',
        correctApproach: 'Use the reciprocal formula $$\\frac{x}{100 + x} \\times 100\\%$$.',
      },
      formulaCheatSheet: [
        {
          name: 'Inverse Base Reduction',
          latex: '$$\\Delta\\%_{down} = \\frac{x}{100 + x} \\times 100\\%$$',
          description: 'Percentage reduction on the increased base.',
          variables: ['x'],
        },
        {
          name: 'Successive Percentage Change',
          latex: '$$\\text{Net }\\% = a + b + \\frac{ab}{100}$$',
          description: 'Combined impact of two successive changes.',
          variables: ['a', 'b'],
        },
      ],
      stepByStepBreakdown: {
        problemStatement: 'If $$A$$ is $$25\\%$$ more than $$B$$, find by what percentage $$B$$ is less than $$A$$.',
        steps: [
          {
            stepNumber: 1,
            title: 'Assume Base Value for B',
            explanation: 'Let $$B = 100$$.',
            formula: '$$B = 100$$',
          },
          {
            stepNumber: 2,
            title: 'Express A with respect to B',
            explanation: 'Since $$A$$ is $$25\\%$$ more, $$A = 100 + 25 = 125$$.',
            formula: '$$A = 125$$',
          },
          {
            stepNumber: 3,
            title: 'Compute Difference and New Base',
            explanation: 'The difference is $$25$$, measured over base $$A = 125$$.',
            formula: '$$\\text{Percentage} = \\frac{25}{125} \\times 100 = 20\\%$$',
            tip: 'Final Answer: $$20\\%$$.',
          },
        ],
        finalAnswer: '$$20\\%$$',
      },
      keyTakeaways: [
        'Base shift changes denominator: $$\\frac{1}{4} \\to \\frac{1}{5} = 20\\%$$.',
        'General rule: Increase of $$\\frac{1}{n} \\implies$$ decrease of $$\\frac{1}{n+1}$$.',
      ],
      quickCheckItems: [
        {
          question: 'If the price of sugar rises by $$50\\%$$, by what percentage must consumption drop to keep expenditure unchanged?',
          options: ['$$33.33\\%$$', '$$50\\%$$', '$$25\\%$$', '$$40\\%$$'],
          correctAnswer: '$$33.33\\%$$',
          answer: '$$33.33\\%$$',
          explanation: 'Drop = $$\\frac{50}{100+50} \\times 100 = \\frac{50}{150} \\times 100 = 33.33\\%$$.',
        },
      ],
    },
  },
  310: {
    id: 'lesson-rot-310-advanced',
    subtopicId: 310,
    subtopicName: 'Rotational Dynamics & Inertia',
    topicName: 'Mechanics',
    subjectName: 'Physics',
    targetExam: 'JEE-MAIN-2026',
    title: 'Moment of Inertia & Parallel Axis Theorem',
    approachName: 'Rigorous Physical Principles',
    lessonType: 'CONCEPT',
    difficultyLevel: 'ADVANCED',
    estimatedMinutes: 10,
    status: 'PUBLISHED',
    contentVersion: 1,
    languageCode: 'en',
    content: {
      coreConcept:
        'Moment of inertia describes rotational sluggishness: $$I = \\int r^2 dm$$. By the Parallel Axis Theorem, the moment of inertia about any axis parallel to a centroidal axis is $$I = I_{cm} + M d^2$$, where $$d$$ is the perpendicular distance between axes. In chemical thermodynamics, molecular rotation also depends on bond lengths: $$\\ce{H2 + 1/2 O2 -> H2O}$$.',
      trapAlert: {
        title: 'Axis Origin Trap',
        description: 'Applying $$I = I_0 + M d^2$$ where $$I_0$$ is NOT passing through the center of mass. The theorem only holds when one axis is strictly the center of mass axis.',
        commonMistake: 'Using an arbitrary known axis instead of the centroidal axis.',
        correctApproach: 'Always reference the parallel distance $$d$$ to $$I_{cm}$$.',
      },
      formulaCheatSheet: [
        {
          name: 'Parallel Axis Theorem',
          latex: '$$I = I_{cm} + M d^2$$',
          description: 'Moment of inertia about parallel axis displaced by d.',
          variables: ['I', 'I_{cm}', 'M', 'd'],
        },
        {
          name: 'Kinetic Energy of Pure Rolling',
          latex: '$$K = \\frac{1}{2} M v^2 + \\frac{1}{2} I_{cm} \\omega^2$$',
          description: 'Total kinetic energy partitioned between translational and rotational motion.',
          variables: ['K', 'M', 'v', 'I_{cm}', '\\omega'],
        },
      ],
      stepByStepBreakdown: {
        problemStatement: 'Find the moment of inertia of a uniform thin rod of mass $$M$$ and length $$L$$ about an axis perpendicular to its length passing through one end.',
        steps: [
          {
            stepNumber: 1,
            title: 'Identify Centroidal Moment of Inertia',
            explanation: 'The moment of inertia of a uniform rod about its center of mass is $$I_{cm} = \\frac{1}{12} M L^2$$.',
            formula: '$$I_{cm} = \\frac{1}{12} M L^2$$',
          },
          {
            stepNumber: 2,
            title: 'Determine Distance to End Axis',
            explanation: 'The distance from center to one end is $$d = \\frac{L}{2}$$.',
            formula: '$$d = \\frac{L}{2}$$',
          },
          {
            stepNumber: 3,
            title: 'Apply Parallel Axis Theorem',
            explanation: '$$I_{end} = I_{cm} + M d^2 = \\frac{1}{12} M L^2 + M \\left(\\frac{L}{2}\\right)^2$$.',
            formula: '$$I_{end} = \\frac{1}{12} M L^2 + \\frac{1}{4} M L^2 = \\frac{1 + 3}{12} M L^2 = \\frac{1}{3} M L^2$$',
            tip: 'Final Answer: $$\\frac{1}{3} M L^2$$.',
          },
        ],
        finalAnswer: '$$\\frac{1}{3} M L^2$$',
      },
      keyTakeaways: [
        'Parallel Axis Theorem requires centroidal axis $$I_{cm}$$.',
        'End of rod: $$I_{end} = \\frac{1}{3} M L^2$$.',
      ],
      quickCheckItems: [
        {
          question: 'What is the moment of inertia of a solid cylinder of mass $$M$$ and radius $$R$$ about its central longitudinal axis?',
          options: ['$$\\frac{1}{2} M R^2$$', '$$M R^2$$', '$$\\frac{2}{5} M R^2$$', '$$\\frac{1}{4} M R^2$$'],
          correctAnswer: '$$\\frac{1}{2} M R^2$$',
          answer: '$$\\frac{1}{2} M R^2$$',
          explanation: 'Solid cylinder is equivalent to stacked circular discs: $$I = \\frac{1}{2} M R^2$$.',
        },
      ],
    },
  },
};

export const SEED_EXPLANATION_IR: ExplanationIr = {
  explanationId: 'exp-dq-3',
  questionId: 'dq-3',
  status: 'PUBLISHED',
  problem: {
    statement:
      'Two trains running in opposite directions at $$60\\text{ km/h}$$ and $$90\\text{ km/h}$$ cross each other in $$12\\text{ seconds}$$. If the length of the first train is $$240\\text{ m}$$, find the length of the second train.',
    authoritativeAnswer: '$$260\\text{ m}$$',
  },
  concepts: ['Relative Velocity in 1D', 'Unit Normalization (km/h to m/s)', 'Distance Traversed by Extended Rigid Bodies'],
  steps: [
    {
      id: 'step-1',
      sequence: 1,
      type: 'GIVEN',
      inputs: ['S_1 = 60\\text{ km/h}', 'S_2 = 90\\text{ km/h}', 't = 12\\text{ s}', 'L_1 = 240\\text{ m}'],
      after: 'Opposite direction closing velocity',
      justification: {
        rule: 'Vector Addition in 1D Opposite Motion',
        formula: '$$S_{rel} = S_1 + S_2$$',
        rationale: 'In opposing directions, the distance between the two bodies closes at the sum of their individual speeds.',
      },
      math: { latex: '$$S_{rel} = 60 + 90 = 150\\text{ km/h}$$' },
    },
    {
      id: 'step-2',
      sequence: 2,
      type: 'TRANSFORM',
      inputs: ['S_{rel} = 150\\text{ km/h}'],
      after: 'Speed in coherent SI base units (m/s)',
      justification: {
        rule: 'Dimensional Unit Normalization',
        formula: '$$1\\text{ km/h} = \\frac{1000\\text{ m}}{3600\\text{ s}} = \\frac{5}{18}\\text{ m/s}$$',
        rationale: 'Crossing time is specified in seconds and length in meters, so speed must be in m/s.',
      },
      math: { latex: '$$S_{rel} = 150 \\times \\frac{5}{18} = \\frac{125}{3}\\text{ m/s}$$' },
    },
    {
      id: 'step-3',
      sequence: 3,
      type: 'CALCULATE',
      inputs: ['S_{rel} = \\frac{125}{3}\\text{ m/s}', 't = 12\\text{ s}'],
      after: 'Combined crossing distance',
      justification: {
        rule: 'Kinematic Equation for Uniform Motion',
        formula: '$$D = S_{rel} \\times t$$',
        rationale: 'Total distance traversed during crossing equals total relative speed times time.',
      },
      math: { latex: '$$D = \\frac{125}{3} \\times 12 = 500\\text{ m}$$' },
    },
    {
      id: 'step-4',
      sequence: 4,
      type: 'CONCLUDE',
      inputs: ['D = 500\\text{ m}', 'L_1 = 240\\text{ m}'],
      after: 'Length of the second train',
      justification: {
        rule: 'Conservation of Total Overlap Distance',
        formula: '$$D = L_1 + L_2 \\implies L_2 = D - L_1$$',
        rationale: 'For complete passage from engine head contact to caboose separation, distance is $$L_1 + L_2$$.',
      },
      math: { latex: '$$L_2 = 500 - 240 = 260\\text{ m}$$' },
      verification: {
        passed: true,
        confidence: 0.99,
        checks: ['Units match meters', 'Result is positive physical dimension', 'Consistent with velocity boundary'],
      },
    },
  ],
  hints: [
    {
      id: 'hint-1',
      level: 1,
      text: 'Because the trains move towards each other, their relative speed is the sum of their individual speeds.',
    },
    {
      id: 'hint-2',
      level: 2,
      text: 'Remember to multiply by 5/18 to turn km/h into m/s before multiplying by 12 seconds.',
      mathExpression: '$$S = 150 \\times \\frac{5}{18}$$',
    },
  ],
  misconceptions: [
    {
      id: 'misc-1',
      incorrectReasoning: 'Subtracting speeds ($$90 - 60 = 30\\text{ km/h}$$) as if running in the same direction.',
      whyWrong: 'Opposing motions add relative closure speeds, they do not subtract.',
      correctedReasoning: 'Use $$S_1 + S_2$$ for opposite directions.',
    },
    {
      id: 'misc-2',
      incorrectReasoning: 'Assuming distance is only the length of the second train ($$L_2$$).',
      whyWrong: 'Both trains have finite lengths and must completely traverse each other.',
      correctedReasoning: 'Total distance traversed is the sum $$L_1 + L_2$$.',
    },
  ],
};
