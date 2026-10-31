/**
 * NAG Candidate Domain Core Models
 * Unified data contracts for authentication, admit cards, catalog, CBT delivery, and security.
 */

// ============================================================================
// 1. Authentication & Identity Models
// ============================================================================

export interface CandidateRegistrationPayload {
  fullName: string;
  mobileNumber: string; // 10-digit Indian mobile number: ^[6-9]\d{9}$
  email: string;
  password: string;
  dpdpConsentAccepted: boolean;
  dateOfBirth?: string; // YYYY-MM-DD
  category?: string; // GEN, OBC, SC, ST, EWS
  aadhaarRef?: string;
}

export interface MobileOtpRequest {
  mobileNumber: string;
  purpose?: 'REGISTRATION' | 'LOGIN' | 'PASSWORD_RESET';
}

export interface MobileOtpVerifyPayload {
  mobileNumber: string;
  otp: string; // 6-digit numeric OTP: ^\d{6}$
  purpose?: 'REGISTRATION' | 'LOGIN' | 'PASSWORD_RESET';
}

export interface TotpVerifyPayload {
  totpCode: string; // 6-digit numeric TOTP token: ^\d{6}$
  sessionToken?: string;
  candidateId?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string; // default: 'Bearer'
  expiresIn: number; // in seconds
  refreshExpiresIn?: number;
}

export interface CandidateProfile {
  id: string;
  fullName: string;
  email: string;
  mobileNumber: string;
  dateOfBirth?: string;
  category?: string;
  photoUrl?: string;
  signatureUrl?: string;
  aadhaarVerified: boolean;
  digiLockerLinked: boolean;
  pwdStatus?: boolean;
  pwdCategory?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DigiLockerBinding {
  linked: boolean;
  aadhaarNumberMasked?: string;
  digiLockerId?: string;
  verifiedAt?: string;
  issuer?: string;
}

// ============================================================================
// 2. Hall Ticket / Digital Admit Card Models
// ============================================================================

export interface DigitalAdmitCard {
  hallTicketNumber: string; // ^HT-\d{4}-\d{6}$
  candidateId: string;
  candidateName: string;
  examId: string;
  examTitle: string;
  examCode: string;
  shiftId: string;
  shiftName: string;
  examDate: string; // YYYY-MM-DD
  reportingTime: string; // HH:mm
  gateClosingTime: string; // HH:mm
  examStartTime: string; // HH:mm
  examEndTime: string; // HH:mm
  centreId: string;
  centreName: string;
  centreAddress: string;
  centreCity: string;
  centreState: string;
  centrePincode: string;
  centreGpsCoordinates?: {
    latitude: number;
    longitude: number;
  };
  instructions: string[];
  photoUrl?: string;
  signatureUrl?: string;
  barcodePayload?: string;
  issuedAt: string | number;
  expiresAt: string | number;
}

export interface SignedQrPayload {
  version: string; // e.g. "1.0"
  keyId: string;
  algorithm: 'ECDSA_P256' | 'ED25519';
  hallTicketNumber: string;
  candidateId: string;
  candidateName: string;
  examId: string;
  shiftId: string;
  centreId: string;
  issuedAt: number;
  expiresAt: number;
  digest: string; // SHA-256 hex digest of canonical admit card representation
  signature: string; // Cryptographic digital signature (hex or base64)
}

export type QrVerificationCode =
  | 'VALID'
  | 'EXPIRED'
  | 'INVALID_SIGNATURE'
  | 'INVALID_DIGEST'
  | 'MALFORMED'
  | 'UNTRUSTED_KEY'
  | 'FORMAT_MISMATCH';

export interface QrVerificationResult {
  valid: boolean;
  code: QrVerificationCode;
  message: string;
  payload?: SignedQrPayload;
  checkedAt: number;
}

export interface OfflineWalletMetadata {
  lastSyncedAt: string;
  cachedAdmitCardsCount: number;
  storageEncryption: string;
  integrityChecked: boolean;
}

// ============================================================================
// 3. Exam Catalog & Application Models
// ============================================================================

export interface ExamSection {
  id: string;
  title: string;
  totalQuestions: number;
  durationMinutes?: number;
  mandatory: boolean;
  marksPerQuestion?: number;
  negativeMarksPerQuestion?: number;
  instructions?: string;
}

export interface ExamShift {
  id: string;
  name: string; // e.g. "Shift 1 (Morning)"
  startTime: string; // e.g. "09:00"
  endTime: string; // e.g. "12:00"
  reportingTime: string; // e.g. "07:30"
  gateClosingTime: string; // e.g. "08:30"
}

export type ExamStatus =
  | 'UPCOMING'
  | 'REGISTRATION_OPEN'
  | 'REGISTRATION_CLOSED'
  | 'ADMIT_CARD_OUT'
  | 'EXAM_CONCLUDED'
  | 'RESULTS_DECLARED';

export interface CatalogExam {
  id: string;
  code: string;
  title: string;
  conductingBody: string;
  category: string;
  description: string;
  registrationStartDate: string;
  registrationEndDate: string;
  examDate: string;
  feeAmount: number;
  durationMinutes: number;
  totalMarks: number;
  sections: ExamSection[];
  availableShifts: ExamShift[];
  eligibilityCriteria?: {
    minAge?: number;
    maxAge?: number;
    minQualification?: string;
    allowedCategories?: string[];
  };
  status: ExamStatus;
}

export interface PublicCentre {
  id: string;
  name: string;
  code: string;
  city: string;
  state: string;
  address: string;
  pincode: string;
  capacity?: number;
  availableSeats?: number;
}

export interface ApplyExamPayload {
  examId: string;
  firstChoiceCentreId: string;
  secondChoiceCentreId: string;
  thirdChoiceCentreId: string;
  preferredShiftId: string;
  instructionsAccepted: boolean;
  pwdRequired: boolean;
  scribeRequired?: boolean;
  pwdCategory?: string;
  scribeQualification?: string;
}

export interface ApplicationReceipt {
  applicationNumber: string;
  examId: string;
  candidateId: string;
  submissionTimestamp: string;
  status: 'SUBMITTED' | 'UNDER_REVIEW' | 'ACCEPTED' | 'REJECTED';
  centrePreferences: [string, string, string];
  preferredShiftId: string;
  feeStatus: 'PAID' | 'EXEMPTED' | 'PENDING';
  hallTicketNumber?: string;
}

export interface CatalogFilterState {
  searchQuery?: string;
  conductingBody?: string;
  category?: string;
  status?: string;
  eligibleOnly?: boolean;
}

export interface CandidateEligibilityProfile {
  age: number;
  qualification: string;
  category: string;
  pwd: boolean;
}

// ============================================================================
// 4. CBT & Grading Models
// ============================================================================

export type NtaQuestionStatus =
  | 'NOT_VISITED'
  | 'NOT_ANSWERED'
  | 'ANSWERED'
  | 'MARKED_FOR_REVIEW'
  | 'ANSWERED_AND_MARKED_FOR_REVIEW';

export interface ExamOption {
  id: string; // 'A' | 'B' | 'C' | 'D'
  text: string; // Supports $$...$$ LaTeX and mhchem
  isCorrect?: boolean;
}

export interface ExamTranslation {
  language: string; // 'en' | 'hi'
  content: string;
  options: ExamOption[];
  explanation?: string;
}

export interface ExamItem {
  id: string;
  sectionId: string;
  questionNumber: number;
  topic: string;
  content: string; // Question stem with LaTeX $$...$$
  options: ExamOption[];
  translations?: Record<string, ExamTranslation>;
  marks: number;
  negativeMarks: number;
  explanation?: string;
  correctAnswerId?: string; // 'A' | 'B' | 'C' | 'D'
}

export interface QuestionStateEntry {
  questionId: string;
  status: NtaQuestionStatus;
  selectedOptionId: string | null;
  timeSpentMs: number;
  visitedAt?: number;
  lastModifiedAt?: number;
  reviewFlag: boolean;
}

export interface SectionPaletteBreakdown {
  total: number;
  notVisited: number;
  notAnswered: number;
  answered: number;
  markedForReview: number;
  answeredAndMarkedForReview: number;
}

export interface NtaPaletteSummary {
  totalQuestions: number;
  notVisitedCount: number;
  notAnsweredCount: number;
  answeredCount: number;
  markedForReviewCount: number;
  answeredAndMarkedForReviewCount: number;
  sectionSummaries?: Record<string, SectionPaletteBreakdown>;
}

export interface OfflineResponsePacket {
  sessionId: string;
  examId: string;
  candidateId: string;
  revision: number; // Monotonically increasing revision counter
  timestamp: number;
  questionStates: Record<string, QuestionStateEntry>;
  clientChecksum: string;
}

export interface SectionPerformanceSummary {
  sectionId: string;
  sectionTitle: string;
  totalQuestions: number;
  attemptedQuestions: number;
  correctQuestions: number;
  incorrectQuestions: number;
  unattemptedQuestions: number;
  scoreObtained: number;
  maxScore: number;
  accuracyPercentage: number;
  timeSpentSeconds: number;
}

export interface TopicMasterySummary {
  topic: string;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
  masteryPercentage: number;
}

export interface ExamSubmissionReceipt {
  receiptId: string;
  sessionId: string;
  examId: string;
  candidateId: string;
  submittedAt: string;
  totalQuestionsAnswered: number;
  checksum: string;
  localGradingCompleted: boolean;
}

export interface LocalGradingBreakdown {
  sessionId: string;
  examId: string;
  candidateId: string;
  evaluatedAt: number;
  totalQuestions: number;
  totalAttempted: number;
  totalCorrect: number;
  totalIncorrect: number;
  totalUnattempted: number;
  rawScore: number;
  maxMarks: number;
  percentage: number;
  accuracyPercentage: number;
  sectionBreakdowns: SectionPerformanceSummary[];
  topicBreakdowns: TopicMasterySummary[];
  receipt: ExamSubmissionReceipt;
}

// ============================================================================
// 5. Security & Proctoring Models
// ============================================================================

export type KioskLockMode = 'NONE' | 'PINNED' | 'LOCK_TASK_PERMITTED' | 'DEVICE_OWNER';

export interface LockTaskStatus {
  isLocked: boolean;
  mode: KioskLockMode;
  lastChangedAt?: number;
}

export interface ScreenSecurityStatus {
  isSecure: boolean;
  screenCaptureDetected: boolean;
  displayCount: number;
  isMirroring: boolean;
}

export interface HardwareSignatureResult {
  keyAlias: string;
  algorithm: string; // e.g. 'SHA256withECDSA'
  isStrongBoxBacked: boolean;
  signatureHex: string;
  signatureBase64: string;
  timestamp: number;
}

export interface ProctoringSensorSample {
  timestamp: number;
  ambientNoiseDb: number;
  devicePitch: number;
  deviceRoll: number;
  lightLevelLux?: number;
  faceDetected?: boolean;
  faceCount?: number;
}

export type ProctoringAnomalyType =
  | 'NOISE_SPIKE'
  | 'ORIENTATION_CHANGE'
  | 'APP_SWITCH'
  | 'SCREEN_RECORDING_ATTEMPT'
  | 'MULTIPLE_FACES'
  | 'NO_FACE';

export type AnomalySeverity = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface ProctoringAnomaly {
  id: string;
  timestamp: number;
  type: ProctoringAnomalyType;
  severity: AnomalySeverity;
  description: string;
  sensorData?: Record<string, unknown>;
}
