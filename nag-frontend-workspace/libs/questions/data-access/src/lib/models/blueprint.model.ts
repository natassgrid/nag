export interface BlueprintRule {
  id?: string;
  subjectId?: number;
  topicId?: number;
  subtopicId?: number;
  subject?: string;
  topic?: string;
  subtopic?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD' | string;
  cognitiveLevel?: string;
  questionType?: string;
  questionCount?: number;
  targetCount?: number;
  marksOverride?: number;
  marksPerQuestion?: number;
  negativeMarks?: number;
  orderIndex?: number;
  paragraphQuestionCount?: number;
  subQuestionsPerPassage?: number;
}

export interface BlueprintSectionDto {
  id?: string;
  name: string;
  orderIndex?: number;
  subjectId?: number;
  subject?: string;
  rules: BlueprintRule[];
}

export interface BlueprintRequest {
  code?: string;
  name: string;
  description?: string;
  examId?: string;
  sections?: BlueprintSectionDto[];
  rules?: BlueprintRule[];
}

export type BlueprintTemplateRequest = BlueprintRequest;

export interface BlueprintResponse {
  id: string;
  code?: string;
  name: string;
  description?: string;
  examId?: string;
  examName?: string;
  version?: number;
  status?: string;
  totalQuestions?: number;
  totalMarks?: number;
  platformDefault?: boolean;
  sections?: BlueprintSectionDto[];
  rules: BlueprintRule[];
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
}

export type BlueprintTemplateResponse = BlueprintResponse;

export interface RuleFeasibilityDetail {
  subject: string;
  topic?: string;
  difficulty?: string;
  cognitiveLevel?: string;
  requested?: number;
  targetCount?: number;
  questionCount?: number;
  available?: number;
  needed?: number;
  deficit?: number;
  surplus?: number;
  sufficient?: boolean;
  status?: string;
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
  rules?: RuleFeasibilityDetail[];
  ruleDetails?: RuleFeasibilityDetail[];
  insufficientRules?: RuleFeasibilityDetail[];
  overallSufficiency?: number;
}
