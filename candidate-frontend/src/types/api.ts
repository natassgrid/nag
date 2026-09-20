// src/types/api.ts
// TypeScript interfaces matching the backend Spring Boot DTOs

// ─── Shared ──────────────────────────────────────────────────

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

// ─── Identity Service DTOs ───────────────────────────────────

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
  otpSentTo?: {
    email: string;    // masked, e.g. c*****@gmail.com
    mobile: string;   // masked, e.g. ******3210
  };
}

export interface VerificationStatusResponse {
  userId: string;
  emailVerified: boolean;
  mobileVerified: boolean;
  accountStatus: string;
  smsRemainingThisWeek: number;
  nextSmsAvailableAt?: string | null;
  fullyVerified: boolean;
}

export interface EmailVerifyRequest {
  userId: string;
  otp: string;
}

export interface MobileVerifyRequest {
  userId: string;
  otp: string;
}

export interface OtpVerifyRequest {
  userId?: string;
  mobile?: string;
  otp: string;
}

export interface AuthTokenRequest {
  username: string;             // email or mobile
  password: string;
  otp?: string;                 // optional MFA / TOTP
  otpCode?: string;
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

// ─── Admin Invitation & 2FA DTOs ─────────────────────────────

export interface AdminInviteRequest {
  email: string;
  fullName: string;
  roles: string[];
  specialization?: string;
}

export interface AdminInviteResponse {
  invitationId: string;
  email: string;
  fullName: string;
  roles: string[];
  status: string;
  expiresAt: string;
  message: string;
}

export interface ValidateInviteResponse {
  valid: boolean;
  email: string;
  fullName: string;
  roles: string[];
  tenantId: string;
  expiresAt: string;
  message: string;
}

export interface AcceptInviteRequest {
  token: string;
  password: string;
  totpSecret: string;
  totpCode: string;
  backupCodes?: string[];
}

export interface TotpSetupResponse {
  secret: string;
  otpauthUri: string;
  issuer: string;
  username: string;
  backupCodes: string[];
}

export interface TotpVerifySetupRequest {
  userId?: string;
  secret: string;
  code: string;
  backupCodes?: string[];
}

// ─── Candidate Service DTOs ──────────────────────────────────

export type Gender = 'MALE' | 'FEMALE' | 'OTHER' | 'PREFER_NOT_TO_SAY';
export type Category = 'GENERAL' | 'OBC' | 'SC' | 'ST' | 'EWS';
export type Qualification =
  | 'BELOW_10TH'
  | '10TH'
  | '12TH'
  | 'DIPLOMA'
  | 'GRADUATE'
  | 'POST_GRADUATE'
  | 'PHD'
  | 'OTHER';

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

export interface CandidateEducation {
  id?: string;
  userId?: string;
  qualification: Qualification | string;
  courseName?: string;
  boardOrUniversity: string;
  institutionName?: string;
  passingYear: number;
  percentageOrCgpa?: number | string;
  gradeOrDivision?: string;
  specialization?: string;
  rollNumber?: string;
  certificateAssetId?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type CandidateEducationRequest = {
  qualification: Qualification | string;
  courseName?: string;
  boardOrUniversity: string;
  institutionName?: string;
  passingYear: number;
  percentageOrCgpa?: number | string;
  gradeOrDivision?: string;
  specialization?: string;
  rollNumber?: string;
  certificateAssetId?: string;
};

export interface CreateCandidateProfileRequest {
  userId: string;
  fullName: string;
  dateOfBirth: string;          // ISO date: "YYYY-MM-DD"
  gender: string;
  nationality: string;
  category?: string;
  mobile: string;
  email: string;
  address?: string;
  reservationCategory?: string;
  identityDocNumber: string;
}

export interface UpdateCandidateProfileRequest {
  fullName?: string;
  dateOfBirth?: string;
  gender?: string;
  nationality?: string;
  category?: string;
  mobile?: string;
  email?: string;
  address?: string;
  reservationCategory?: string;
  identityDocNumber?: string;
  photoAssetId?: string;
  signatureAssetId?: string;
  idProofAssetId?: string;
}

export interface CandidateProfileResponse {
  userId: string;               // UUID - linked identity account
  fullName?: string | null;
  dateOfBirth?: string | null;
  gender?: string | null;
  nationality?: string | null;
  category?: string | null;
  mobile?: string | null;       // masked: ****1234
  email?: string | null;        // masked
  address?: string | null;
  reservationCategory?: string | null;
  identityDocNumber?: string | null;
  digiLockerVerified?: string | null;
  faceVerificationStatus?: string | null;
  consentRecorded?: boolean;
  photoAssetId?: string | null;
  signatureAssetId?: string | null;
  idProofAssetId?: string | null;
  completionPercentage?: number; // 0-100 client calculated
  // Client backward compatibility fields
  firstName?: string;
  lastName?: string;
  education?: EducationDetail;
}

export interface ConsentRequest {
  consentGiven: boolean;
  consentVersion?: string;
}

// ─── Examination Service DTOs ────────────────────────────────

export type ExamStatus = 'DRAFT' | 'PUBLISHED' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';
export type ExamMode = 'ONLINE' | 'OFFLINE' | 'HYBRID' | 'CBT' | 'OMR';

export interface ExamSection {
  name: string;
  subject?: string;
  questionCount?: number;
  marksPerQuestion?: number;
  negativeMarksPerQuestion?: number;
}

export interface ExaminationResponse {
  id: string;                   // UUID
  name?: string;
  title?: string;
  description: string;
  status: ExamStatus;
  mode: ExamMode;
  durationMinutes: number;
  totalMarks: number;
  passingMarks?: number;
  applicationStartDate?: string;
  applicationEndDate?: string;
  examDate?: string;
  eligibilityCriteria?: string;
  syllabus?: string;
  applicationFee?: number;
  tenantId: string;
  createdAt: string;
  conductingAuthority?: string;
  code?: string;
  category?: string;
  examinationType?: string;
  academicYear?: string;
  examinationMode?: string;
  negativeMarkingEnabled?: boolean;
  negativeMarkingValue?: number;
  navigationPolicy?: string;
  calculatorPolicy?: string;
  reviewFlagEnabled?: boolean;
  sections?: ExamSection[];
}

export interface PublicCentreResponse {
  id: string;
  centreName: string;
  region?: string;
  state: string;
  district?: string;
  city: string;
  building?: string;
  totalCapacity?: number;
}

export interface ExamApplicationRequest {
  examId: string;
  firstChoiceCentreId?: string;
  secondChoiceCentreId?: string;
  thirdChoiceCentreId?: string;
  preferredShiftId?: string;
  pwdRequired?: boolean;
  scribeRequired?: boolean;
}

export interface ExamApplicationResponse {
  applicationId: string;        // UUID
  examId: string;
  candidateId: string;
  status: 'APPLIED' | 'CONFIRMED' | 'REJECTED';
  applicationDate: string;
  hallTicketNumber?: string;
  examName?: string;
  examCode?: string;
  conductingAuthority?: string;
  durationMinutes?: number;
  totalMarks?: number;
  allocatedCentreId?: string;
  centreName?: string;
  city?: string;
  state?: string;
  examDate?: string;
  shiftName?: string;
  shiftId?: string;
  allocatedShiftId?: string;
  preferredShiftId?: string;
}

export interface AdmitCardResponse {
  applicationId: string;
  hallTicketNumber: string;
  candidateId: string;
  candidateName: string;
  examId: string;
  examName: string;
  examCode?: string;
  conductingAuthority?: string;
  examinationMode?: string;
  durationMinutes: number;
  totalMarks: number;
  examDate: string;
  shiftId?: string;
  shiftName: string;
  shiftNumber: number;
  reportingTime: string;
  gateClosingTime: string;
  loginStartTime: string;
  examStartTime: string;
  examEndTime: string;
  centreId?: string;
  centreName: string;
  building?: string;
  floor?: string;
  city: string;
  state: string;
  laboratoryIdentifier?: string;
  qrData: string;
  verificationHash?: string;
  pwdRequired?: boolean;
  scribeRequired?: boolean;
  instructions: string[];
}

// ─── Delivery Service DTOs ───────────────────────────────────

export type NavigationMode = 'SEQUENTIAL' | 'FLEXIBLE' | 'RESTRICTED';

export type LanguageCode = string;

export interface ExamLanguage {
  code: LanguageCode;
  name: string;
  nativeName: string;
  rtl?: boolean;
}

export interface QuestionOptionTranslation {
  id?: string;
  index?: number;
  text?: string;
  content?: string;
  imageUrl?: string;
  imageAltText?: string;
}

export interface QuestionTranslation {
  id?: string;
  languageCode?: string;
  text?: string;
  content?: string;
  imageUrl?: string;
  imageAltText?: string;
  options?: QuestionOptionTranslation[];
  explanation?: string;
}

export interface QuestionOption {
  id?: string;
  index: number;
  text?: string;
  content?: string;
  imageUrl?: string;
  imageAltText?: string;
  isCorrect?: boolean;
}

export interface QuestionDto {
  id: string;
  text?: string;
  content?: string;
  imageUrl?: string;
  imageAltText?: string;
  hasImages?: boolean;
  options: QuestionOption[];
  marks: number;
  negativeMarks: number;
  sectionId?: string;
  sectionName?: string;
  topic?: string;
  questionType?: string;
  passageId?: string;
  passageContent?: string;
  passageOrderIndex?: number;
  sequenceNumber?: number;
  explanation?: string;
  correctOptionIndex?: number;
  translations?: Record<LanguageCode, QuestionTranslation>;
}

export interface SessionStartRequest {
  examId: string;
  shiftId?: string;
  candidateId?: string;
  languageCode?: LanguageCode;
  forceNewSession?: boolean;
  terminateExisting?: boolean;
}

export interface SessionStartResponse {
  sessionId: string;
  examId: string;
  examTitle?: string;
  shiftId?: string;
  candidateId: string;
  startedAt?: string;
  scheduledEndAt?: string;
  durationSeconds: number;
  totalQuestions: number;
  navigationMode: NavigationMode;
  questions: QuestionDto[];
  serverTime: string;
  expiresAt: string;
  kioskModeEnforced?: boolean;
  heartbeatIntervalSeconds?: number;
  autosaveIntervalSeconds?: number;
  maxDisconnectGraceSeconds?: number;
  tamperDetectionEnabled?: boolean;
  availableLanguages?: ExamLanguage[];
  defaultLanguageCode?: LanguageCode;
}

export interface NavigationRequest {
  sessionId: string;
  targetQuestionIndex: number;
  targetSectionIndex?: number;
}

export interface NavigationResponse {
  currentQuestionIndex: number;
  currentSectionIndex: number;
  allowedActions: string[];
}

// ─── Response Service DTOs ───────────────────────────────────

export type ResponseType = 'MCQ' | 'INTEGER' | 'DESCRIPTIVE';

export interface SaveResponseRequest {
  questionId: string;
  responseType: ResponseType;
  selectedOptionIndex?: number;
  integerAnswer?: number;
  markedForReview: boolean;
  timeTakenSeconds: number;
  revisionSequence: number;
}

export interface SaveResponseResponse {
  responseId: string;
  questionId: string;
  savedAt: string;
  revisionSequence: number;
}

export interface BulkSaveRequest {
  responses: SaveResponseRequest[];
}

// ─── Result Service DTOs ─────────────────────────────────────

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
  id: string;
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
  cognitiveBreakdown?: CognitiveBreakdown;
  topicBreakdown?: Record<string, TopicScore>;
  timeAnalysis?: TimeAnalysis;
  categoryRank?: number;
}

// ─── Asset Service DTOs ──────────────────────────────────────

export type AssetType = 'IMAGE' | 'DOCUMENT' | 'VIDEO' | 'AUDIO';

export interface AssetUploadResponse {
  id: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  assetType: AssetType;
  downloadUrl: string;
  createdAt: string;
}

// ─── Notification DTOs ───────────────────────────────────────

export type NotificationType =
  | 'EXAM_APPLIED'
  | 'EXAM_RESULT_PUBLISHED'
  | 'ADMIT_CARD_READY'
  | 'PROFILE_INCOMPLETE'
  | 'SYSTEM'
  | 'ANNOUNCEMENT';

export interface NotificationDto {
  id: string;
  userId: string;
  title: string;
  body: string;
  type: NotificationType;
  isRead: boolean;
  createdAt: string;
  actionUrl?: string;
}

// ─── Post-Exam Review Types ──────────────────────────────────

export interface ReviewOption {
  id: string;
  text: string;
  imageUrl?: string;
  imageAltText?: string;
  isCorrect: boolean;
}

export interface ReviewQuestion {
  questionId: string;
  questionNumber: number;
  content: string;
  imageUrl?: string;
  imageAltText?: string;
  subject: string;
  topic: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  bloomsLevel: 'REMEMBER' | 'UNDERSTAND' | 'APPLY' | 'ANALYZE' | 'EVALUATE' | 'CREATE';
  options: ReviewOption[];
  candidateSelectedOptionIds: string[];
  isCorrect: boolean;
  marksAwarded: number;
  timeSpentMs: number;
  peerAccuracyPct: number;
  explanation: string;
  passageId?: string;
  passageContent?: string;
  passageOrderIndex?: number;
}

export interface ExamReviewResponse {
  examId: string;
  candidateId: string;
  questions: ReviewQuestion[];
}

// ─── Extended ResultDto fields ───────────────────────────────

export interface CognitiveBreakdown {
  REMEMBER?: number;
  UNDERSTAND?: number;
  APPLY?: number;
  ANALYZE?: number;
  EVALUATE?: number;
  CREATE?: number;
}

export interface TopicScore {
  score: number;
  maxScore: number;
}

export interface TimeAnalysis {
  avgTimePerQuestionMs: number;
  timeOnCorrectMs: number;
  timeOnIncorrectMs: number;
  totalQuestions: number;
}
