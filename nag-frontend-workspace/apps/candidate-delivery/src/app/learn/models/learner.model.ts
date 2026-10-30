import type { LessonType, LessonDifficultyLevel } from './micro-lesson.model';

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
