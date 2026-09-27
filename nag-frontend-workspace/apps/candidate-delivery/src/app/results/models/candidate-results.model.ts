export interface SubjectScore {
  subject: string;
  marksObtained: number;
  maxMarks: number;
  accuracyRate: number;
  questionsAttempted: number;
  questionsTotal: number;
  cutoffMarks?: number;
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
