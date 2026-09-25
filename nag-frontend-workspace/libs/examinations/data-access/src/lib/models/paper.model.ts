export interface BlueprintRule {
  subject: string;
  topic: string;
  difficulty?: string;
  cognitiveLevel?: string;
  questionType?: string;
  targetCount?: number;
  questionCount?: number;
}

export interface PaperSummary {
  id: string;
  paperId?: string;
  name: string;
  examId?: string;
  examName?: string;
  shiftId?: string;
  shiftName?: string;
  status: string;
  isPractice?: boolean;
  totalMarks?: number;
  totalQuestions?: number;
  difficultyScore?: number;
  generatedBy?: string;
  approvedBy?: string;
  createdAt: string;
  updatedAt: string;
}

export interface QuestionDetail {
  questionId: string;
  content: string;
  answerKey: string;
  subject: string;
  topic: string;
  difficulty: string;
  cognitiveLevel: string;
  orderIndex: number;
  marks: number;
  negativeMarks: number;
  explanation?: string;
  options?: any[];
  usageCount?: number;
  lastUsedAt?: string;
}

export interface PaperDetail {
  id?: string;
  paperId?: string;
  name?: string;
  examId?: string;
  examName?: string;
  shiftId?: string;
  shiftName?: string;
  status?: string;
  isPractice?: boolean;
  totalMarks?: number;
  difficultyScore?: number;
  encryptedPackageRef?: string;
  encryptionKeyId?: string;
  generatedBy?: string;
  createdAt?: string;
  updatedAt?: string;
  totalQuestions?: number;
  topicDistribution?: Record<string, number>;
  questions?: QuestionDetail[];
  paperDefinitionJson?: string;
}

export interface PaperGenerationRequest {
  examId: string;
  shiftId: string;
  name?: string;
  paperName?: string;
  isPractice?: boolean;
  blueprintRules: BlueprintRule[];
}

export interface PaperGenerationResponse {
  paperId: string;
  name?: string;
  status: string;
  isPractice?: boolean;
  message: string;
}

export interface PaperApprovalResponse {
  paperId: string;
  name?: string;
  status: string;
  isPractice?: boolean;
  encryptionKeyId?: string;
  message: string;
}

export interface PaperTranslateRequest {
  targetLanguage?: string;
  sourceLanguage?: string;
  targetStatus?: string;
  overwriteExisting?: boolean;
  maxConcurrency?: number;
  throttleDelayMs?: number;
}

export interface PaperTranslateResponse {
  jobId: string;
  paperId: string;
  status: string;
  sourceLanguage: string;
  targetLanguage: string;
  targetStatus: string;
  overwriteExisting: boolean;
  totalQuestions: number;
  processedQuestions: number;
  successfulQuestions: number;
  failedQuestions: number;
  progressPercentage: number;
  errorMessage?: string;
  message?: string;
  createdAt?: string;
  completedAt?: string;
}

export interface BlueprintTemplateRequest {
  name: string;
  description?: string;
  examId?: string;
  rules: BlueprintRule[];
}

export interface BlueprintTemplateResponse {
  id: string;
  name: string;
  description?: string;
  examId?: string;
  examName?: string;
  rules: BlueprintRule[];
  totalQuestions?: number;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface GapDetail {
  subject?: string;
  topic?: string;
  difficulty?: string;
  cognitiveLevel?: string;
  needed?: number;
  available?: number;
  deficit?: number;
  message?: string;
}

export interface RuleFeasibility {
  subject: string;
  topic: string;
  difficulty?: string;
  cognitiveLevel?: string;
  requested?: number;
  available?: number;
  needed?: number;
  surplus?: number;
  sufficient?: boolean;
  status?: string;
  deficit?: number;
  targetCount?: number;
  questionCount?: number;
}

export interface BlueprintFeasibilityRequest {
  examId?: string;
  shiftId?: string;
  isPractice?: boolean;
  blueprintRules?: BlueprintRule[];
  rules?: BlueprintRule[];
  notifyAdminOnDeficit?: boolean;
}

export interface BlueprintFeasibilityResponse {
  feasible: boolean;
  totalRequested?: number;
  totalAvailable?: number;
  totalQuestionsNeeded?: number;
  totalQuestionsAvailable?: number;
  deficitRuleCount?: number;
  summary?: string;
  checkedAt?: string;
  rules?: RuleFeasibility[];
  ruleDetails?: RuleFeasibility[];
  insufficientRules?: RuleFeasibility[];
  gaps?: GapDetail[];
  overallSufficiency?: number;
  notificationDispatched?: boolean;
}

export interface PaperListParams {
  page?: number;
  size?: number;
  sort?: string;
  order?: string;
  search?: string;
  status?: string;
  examId?: string;
  shiftId?: string;
  isPractice?: boolean;
}

export interface PaperSectionConfig {
  id: string;
  name: string;
  questionCount: number;
  marksPerQuestion: number;
}

export interface PaperGenerationConfig {
  examId: string;
  totalQuestions: number;
  difficultyDistribution: {
    easy: number;
    medium: number;
    hard: number;
  };
  sections: PaperSectionConfig[];
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  ledgerProofHash?: string;
}

export interface LedgerProofResult {
  proofHash: string;
  blockNumber: number;
  timestamp: string;
  merkleRoot: string;
  verified: boolean;
}
