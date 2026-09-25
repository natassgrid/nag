export interface BlueprintRule {
  id?: string;
  subject: string;
  topic?: string;
  subtopic?: string;
  difficulty?: 'EASY' | 'MEDIUM' | 'HARD' | string;
  cognitiveLevel?: string;
  questionType?: string;
  questionCount?: number;
  targetCount?: number;
  marksPerQuestion?: number;
  negativeMarks?: number;
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
  totalMarks?: number;
  createdBy?: string;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

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
