// src/types/api.ts
// TypeScript interfaces matching the backend Spring Boot DTOs

// ─── Shared ─────────────────────────────────────────────────────────────────

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;   // current page (0-indexed)
  size: number;
  first: boolean;
  last: boolean;
}

// ─── Identity Service DTOs ────────────────────────────────────────────────────

export interface RegistrationRequest {
  fullName: string;
  email: string;
  mobile: string;
  password: string;
  identityDocType: 'AADHAAR' | 'PAN' | 'PASSPORT' | 'VOTER_ID' | 'DL';
  identityDocNumber: string;
}

export interface RegistrationResponse {
  userId: string;     // UUID
  message: string;
  otpSentTo: {
    email: string;    // masked, e.g. c*****@gmail.com
    mobile: string;   // masked, e.g. ******3210
  };
}

export interface OtpVerifyRequest {
  userId?: string;
  mobile?: string;
  otp: string;
}

export interface AuthTokenRequest {
  username: string;             // email or mobile
  password: string;
  otp?: string;                 // optional MFA
  deviceFingerprint?: string;
}

export interface AuthTokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number;            // seconds
  tokenType: string;            // "Bearer"
  userId: string;               // UUID from JWT subject
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  userId: string;
  otp: string;
  newPassword: string;
}

export interface OtpResendRequest {
  userId: string;
}

// ─── Candidate Service DTOs ──────────────────────────────────────────────────

export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';
export type Category = 'GENERAL' | 'OBC' | 'SC' | 'ST' | 'EWS';
export type Qualification =
  | 'BELOW_10TH'
  | '10TH'
  | '12TH'
  | 'DIPLOMA'
  | 'GRADUATE'
  | 'POST_GRADUATE'
  | 'PHD';

export interface Address {
  street: string;
  city: string;
  district: string;
  state: string;
  pincode: string;
  country: string;
}

export interface EducationDetail {
  qualification: Qualification;
  boardOrUniversity: string;
  passingYear: number;
  percentage: number;
  specialization?: string;
}

export interface CreateCandidateProfileRequest {
  userId: string;
  firstName: string;
  lastName: string;
  dateOfBirth: string;          // ISO date: "YYYY-MM-DD"
  gender: Gender;
  category: Category;
  address: Address;
  education: EducationDetail;
}

export interface UpdateCandidateProfileRequest {
  firstName?: string;
  lastName?: string;
  dateOfBirth?: string;
  gender?: Gender;
  category?: Category;
  address?: Address;
  education?: EducationDetail;
}

export interface CandidateProfileResponse {
  id: string;                   // UUID
  userId: string;               // UUID - linked identity account
  firstName: string;
  lastName: string;
  fullName: string;
  dateOfBirth: string;
  gender: Gender;
  category: Category;
  address: Address;
  education: EducationDetail;
  photoAssetId?: string;        // UUID pointing to asset-service
  signatureAssetId?: string;
  idProofAssetId?: string;
  kycVerified: boolean;
  consentGiven: boolean;
  completionPercentage: number; // 0-100
  createdAt: string;
  updatedAt: string;
}

export interface ConsentRequest {
  consentGiven: boolean;
  consentVersion: string;
}

// ─── Examination Service DTOs ────────────────────────────────────────────────

export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type ExamMode = 'ONLINE' | 'OFFLINE' | 'HYBRID';

export interface ExaminationResponse {
  id: string;                   // UUID
  title: string;
  description: string;
  status: ExamStatus;
  mode: ExamMode;
  durationMinutes: number;
  totalMarks: number;
  passingMarks: number;
  applicationStartDate: string;
  applicationEndDate: string;
  examDate: string;
  eligibilityCriteria: string;
  syllabus?: string;
  applicationFee: number;
  tenantId: string;
  createdAt: string;
}

export interface ExamApplicationRequest {
  examId: string;
}

export interface ExamApplicationResponse {
  applicationId: string;        // UUID
  examId: string;
  candidateId: string;
  status: 'APPLIED' | 'CONFIRMED' | 'REJECTED';
  applicationDate: string;
  hallTicketNumber?: string;
}

// ─── Delivery Service DTOs ───────────────────────────────────────────────────

export interface SessionStartRequest {
  examId: string;               // UUID
  shiftId: string;              // UUID
}

export type NavigationMode = 'SEQUENTIAL' | 'FLEXIBLE' | 'RESTRICTED';

export interface QuestionOption {
  index: number;
  text: string;
  imageUrl?: string;
}

export interface QuestionDto {
  id: string;                   // UUID - never expose correctOptionIndex!
  text: string;
  imageUrl?: string;
  options: QuestionOption[];
  marks: number;
  negativeMarks: number;
  sectionId: string;
  sectionName: string;
}

export interface SessionStartResponse {
  sessionId: string;            // UUID
  examId: string;
  candidateId: string;
  durationSeconds: number;
  totalQuestions: number;
  navigationMode: NavigationMode;
  questions: QuestionDto[];     // all questions delivered at session start
  serverTime: string;           // ISO timestamp for clock sync
  expiresAt: string;            // ISO timestamp for session expiry
}

export interface NavigationRequest {
  sessionId: string;
  targetQuestionIndex: number;
  targetSectionIndex?: number;
}

export interface NavigationResponse {
  currentQuestionIndex: number;
  currentSectionIndex: number;
  allowedActions: string[];     // ['NEXT', 'PREVIOUS', 'JUMP', 'MARK_REVIEW']
}

// ─── Response Service DTOs ───────────────────────────────────────────────────

export type ResponseType = 'MCQ' | 'INTEGER' | 'DESCRIPTIVE';

export interface SaveResponseRequest {
  questionId: string;           // UUID
  responseType: ResponseType;
  selectedOptionIndex?: number; // for MCQ
  integerAnswer?: number;       // for INTEGER
  markedForReview: boolean;
  timeTakenSeconds: number;
  revisionSequence: number;     // increment on each change to same question
}

export interface SaveResponseResponse {
  responseId: string;           // UUID
  questionId: string;
  savedAt: string;
  revisionSequence: number;
}

export interface BulkSaveRequest {
  responses: SaveResponseRequest[];
}

// ─── Result Service DTOs ─────────────────────────────────────────────────────

export type ResultStatus = 'PENDING' | 'COMPUTED' | 'PUBLISHED' | 'WITHHELD';

export interface SectionResult {
  sectionId: string;
  sectionName: string;
  attempted: number;
  correct: number;
  incorrect: number;
  marks: number;
  totalMarks: number;
}

export interface ResultDto {
  id: string;                   // UUID
  candidateId: string;
  examId: string;
  examTitle: string;
  rawScore: number;
  normalizedScore?: number;
  percentile?: number;
  rank?: number;
  status: ResultStatus;
  sectionResults: SectionResult[];
  totalAttempted: number;
  totalCorrect: number;
  totalIncorrect: number;
  totalMarks: number;
  qualifyingScore: number;
  qualified: boolean;
  scorecardPdfRef?: string;
  computedAt?: string;
  publishedAt?: string;
}

// ─── Asset Service DTOs ──────────────────────────────────────────────────────

export type AssetType = 'IMAGE' | 'DOCUMENT' | 'VIDEO' | 'AUDIO';

export interface AssetUploadResponse {
  id: string;                   // UUID
  originalFilename: string;
  contentType: string;
  fileSize: number;
  assetType: AssetType;
  downloadUrl: string;
  createdAt: string;
}

// ─── Notification DTOs ───────────────────────────────────────────────────────

export type NotificationType =
  | 'EXAM_APPLIED'
  | 'EXAM_RESULT_PUBLISHED'
  | 'ADMIT_CARD_READY'
  | 'PROFILE_INCOMPLETE'
  | 'SYSTEM'
  | 'ANNOUNCEMENT';

export interface NotificationDto {
  id: string;                   // UUID
  userId: string;
  title: string;
  body: string;
  type: NotificationType;
  isRead: boolean;
  createdAt: string;
  actionUrl?: string;
}
