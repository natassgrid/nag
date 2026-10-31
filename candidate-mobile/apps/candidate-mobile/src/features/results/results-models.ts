/**
 * Candidate Mobile Results & Cryptographic Digital Scorecard Models
 * Ported from nag-frontend-workspace candidate-delivery results module.
 */

export interface NormalizedSectionScore {
  sectionId: string;
  sectionName: string;
  subject: string;
  rawScore: number;
  maxMarks: number;
  normalizedScore: number;
  percentileScore: number;
  accuracyRate: number;
  questionsAttempted: number;
  questionsTotal: number;
  cutoffMarks: number;
  status: 'CLEARED' | 'BELOW_CUTOFF';
}

export interface CategoryCutoffEntry {
  category: 'GENERAL' | 'OBC_NCL' | 'SC' | 'ST' | 'EWS' | 'PWD';
  categoryLabel: string;
  qualifyingCutoffMarks: number;
  qualifyingPercentile: number;
  isCandidateCategory: boolean;
  qualified: boolean;
}

export interface ScorecardVerificationMetadata {
  ledgerProofHash: string;
  merkleRoot: string;
  blockHeight: number;
  smartContractAddress: string;
  issuerPublicKeyFingerprint: string;
  verifiedTimestamp: string;
  qrPayload: string;
  signatureAlgorithm: 'ECDSA_P256' | 'ED25519';
}

export interface CandidateExamResult {
  id: string;
  examId: string;
  examCode: string;
  examTitle: string;
  conductingAuthority: string;
  rollNumber: string;
  candidateName: string;
  candidateCategory: string;
  declaredDate: string;
  shiftName: string;
  isMultiShiftNormalized: boolean;
  rawTotalMarks: number;
  normalizedTotalMarks: number;
  maxScore: number;
  percentile: number;
  zScore: number;
  allIndiaRank: number; // AIR
  categoryRank: number;
  totalCandidatesAppeared: number;
  qualifyingStatus: 'QUALIFIED' | 'NOT_QUALIFIED';
  categoryCutoffs: CategoryCutoffEntry[];
  sectionScores: NormalizedSectionScore[];
  verification: ScorecardVerificationMetadata;
  digiLockerPushed: boolean;
  savedToOfflineWallet: boolean;
}

export interface DigiLockerDepositReceipt {
  docId: string;
  txnId: string;
  pushedAt: string;
  status: 'ISSUED' | 'PENDING';
}

export interface ResultsState {
  resultsList: CandidateExamResult[];
  selectedResult: CandidateExamResult | null;
  verificationStatus: 'VERIFIED' | 'UNVERIFIED' | 'FAILED';
  isSavedToWallet: boolean;
  pushingDigiLocker: boolean;
  digiLockerReceipt: DigiLockerDepositReceipt | null;
  loading: boolean;
  error: string | null;
}
