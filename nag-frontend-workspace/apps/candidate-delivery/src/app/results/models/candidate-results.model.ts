export interface SubtopicScoreBreakdown {
  subtopicId: number;
  subtopicName: string;
  marksObtained: number;
  maxMarks: number;
  accuracyRate: number;
  questionsAttempted: number;
  questionsTotal: number;
}

export interface TopicScoreBreakdown {
  topicId: number;
  topicName: string;
  marksObtained: number;
  maxMarks: number;
  accuracyRate: number;
  questionsAttempted: number;
  questionsTotal: number;
  subtopics?: SubtopicScoreBreakdown[];
}

export interface SubjectScore {
  sectionId?: string | number;
  sectionName?: string;
  subjectId?: number;
  subject: string;
  topicId?: number;
  subtopicId?: number;
  topicName?: string;
  subtopicName?: string;
  marksObtained: number;
  maxMarks: number;
  accuracyRate: number;
  questionsAttempted: number;
  questionsTotal: number;
  cutoffMarks?: number;
  topics?: TopicScoreBreakdown[];
}

export interface ScorecardRecord {
  id: string;
  examId: string;
  examCode: string;
  examTitle: string;
  conductingAuthority: string;
  rollNumber: string;
  candidateName: string;
  category: string;
  declaredDate: string;
  totalScore: number;
  maxScore: number;
  percentile: number;
  nationalRank: number;
  categoryRank: number;
  totalAppeared: number;
  qualifyingStatus: 'QUALIFIED' | 'DISQUALIFIED';
  qualifyingCutoff: number;
  ledgerProofHash: string;
  merkleRoot: string;
  blockHeight: number;
  digiLockerPushed: boolean;
  subjectScores: SubjectScore[];
}

export interface DigiLockerPushResponse {
  docId: string;
  status: 'ISSUED' | 'PENDING';
  transactionId: string;
  pushedAt: string;
}
