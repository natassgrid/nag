export type ExplanationStatus = 'DRAFT' | 'APPROVED' | 'PUBLISHED' | 'REJECTED';

export type StepType =
  | 'GIVEN'
  | 'APPLY_RULE'
  | 'TRANSFORM'
  | 'CONCLUDE'
  | 'CALCULATE'
  | 'SUBSTITUTE';

export interface StepOperation {
  name: string;
  target?: string;
  value?: unknown;
  params?: Record<string, unknown>;
}

export interface StepJustification {
  rule?: string;
  formula?: string;
  rationale?: string;
  why?: string;
}

export interface VerificationResult {
  passed: boolean;
  confidence?: number;
  checks?: string[];
  details?: string;
}

export interface ExplanationHint {
  id: string;
  level: number;
  text: string;
  mathExpression?: string;
}

export interface ExplanationMisconception {
  id: string;
  incorrectReasoning: string;
  whyWrong: string;
  correctedReasoning: string;
}

export interface MathExpression {
  latex?: string;
  ascii?: string;
  mathMl?: string;
  canonical?: string;
}

export interface ExplanationStep {
  id: string;
  sequence: number;
  type: StepType;
  inputs?: string[];
  operation?: StepOperation;
  before?: string;
  after?: string;
  outputs?: string[];
  justification?: StepJustification;
  representations?: string[];
  math?: MathExpression;
  verification?: VerificationResult;
  misconception?: ExplanationMisconception;
}

export interface Problem {
  statement: string;
  context?: string;
  authoritativeAnswer?: string;
}

export interface ReasoningGraph {
  nodes?: Array<{ id: string; label?: string; type?: string }>;
  edges?: Array<{ source: string; target: string; relation?: string }>;
}

export interface ExplanationDiagram {
  id?: string;
  type: string;
  title?: string;
  svgContent?: string;
  data?: Record<string, unknown>;
}

export interface ExplanationAnimation {
  type?: string;
  durationMs?: number;
  keyframes?: unknown[];
}

export interface ExplanationIr {
  schemaVersion?: string;
  explanationId: string;
  questionId: string;
  status: ExplanationStatus;
  problem?: Problem;
  concepts?: string[];
  facts?: string[];
  assumptions?: string[];
  reasoningGraph?: ReasoningGraph;
  steps: ExplanationStep[];
  verification?: VerificationResult;
  hints?: ExplanationHint[];
  misconceptions?: ExplanationMisconception[];
  diagrams?: ExplanationDiagram[];
  animation?: ExplanationAnimation;
  metadata?: Record<string, unknown>;
}
