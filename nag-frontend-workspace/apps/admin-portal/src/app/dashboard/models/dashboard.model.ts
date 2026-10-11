export interface DashboardKpiMetrics {
  totalQuestions: number;
  pendingReviewQuestions: number;
  activeExaminations: number;
  registeredCandidates: number;
  activeSessions: number;
  pendingGradingTasks: number;
  activeBatchJobs: number;
  questionTrend: string;
  examTrend: string;
  candidateTrend: string;
  gradingTrend: string;
}

export interface ExamStatusBreakdown {
  draft: number;
  scheduled: number;
  liveInProgress: number;
  evaluation: number;
  completed: number;
}

export interface QuestionBankBreakdown {
  total: number;
  draft: number;
  submitted: number;
  approved: number;
  rejected: number;
}

export interface EvaluationQueueBreakdown {
  pending: number;
  autoEvaluated: number;
  manualEvaluated: number;
  arbitration: number;
  completed: number;
}

export interface SystemServiceHealth {
  name: string;
  status: 'UP' | 'DEGRADED' | 'DOWN';
  latencyMs: number;
  uptime: string;
  details?: string;
}

export interface SecurityEvent {
  id: string;
  timestamp: string;
  actor: string;
  action: string;
  resource: string;
  hash: string;
}

export interface DashboardSummary {
  tenantId: string;
  lastRefreshed: string;
  kpis: DashboardKpiMetrics;
  examBreakdown: ExamStatusBreakdown;
  questionBreakdown: QuestionBankBreakdown;
  evaluationBreakdown: EvaluationQueueBreakdown;
  systemServices: SystemServiceHealth[];
  recentAuditEvents: SecurityEvent[];
}

export const DEFAULT_DASHBOARD_SUMMARY: DashboardSummary = {
  tenantId: 'default',
  lastRefreshed: new Date().toISOString(),
  kpis: {
    totalQuestions: 48290,
    pendingReviewQuestions: 124,
    activeExaminations: 14,
    registeredCandidates: 1480200,
    activeSessions: 8420,
    pendingGradingTasks: 342,
    activeBatchJobs: 3,
    questionTrend: '+120 this week',
    examTrend: '2 running live',
    candidateTrend: '99.8% seat allocated',
    gradingTrend: 'avg 18m turn-around',
  },
  examBreakdown: {
    draft: 4,
    scheduled: 8,
    liveInProgress: 2,
    evaluation: 5,
    completed: 42,
  },
  questionBreakdown: {
    total: 48290,
    draft: 380,
    submitted: 124,
    approved: 47520,
    rejected: 266,
  },
  evaluationBreakdown: {
    pending: 342,
    autoEvaluated: 12800,
    manualEvaluated: 8920,
    arbitration: 18,
    completed: 21702,
  },
  systemServices: [
    { name: 'Monolith Core', status: 'UP', latencyMs: 4, uptime: '99.99%', details: 'Spring Boot 3.4 runtime healthy' },
    { name: 'PostgreSQL + Vector', status: 'UP', latencyMs: 2, uptime: '100%', details: 'Replicas active and synchronized' },
    { name: 'Redis Cache', status: 'UP', latencyMs: 1, uptime: '100%', details: 'Session & near-cache optimal' },
    { name: 'HashiCorp Vault', status: 'UP', latencyMs: 3, uptime: '100%', details: 'Transit encryption unsealed' },
    { name: 'Keycloak OIDC', status: 'UP', latencyMs: 5, uptime: '99.95%', details: 'JWT signing & introspect healthy' },
    { name: 'LiteLLM AI Core', status: 'UP', latencyMs: 18, uptime: '99.9%', details: 'Embeddings & translation ready' },
    { name: 'Kafka Event Bus', status: 'UP', latencyMs: 3, uptime: '99.99%', details: 'Zero consumer group lag' },
  ],
  recentAuditEvents: [
    {
      id: 'SEC-1092',
      timestamp: '2026-10-03 19:42:10',
      actor: 'system.scheduler',
      action: 'MERKLE_ROOT_MINT',
      resource: 'Exam NES-2026-S1',
      hash: '0x8f22e1b4c90192a5433d849202af019b882371a2384a92c8192a838192a839a',
    },
    {
      id: 'SEC-1091',
      timestamp: '2026-10-03 19:35:00',
      actor: 'admin@nag.gov.in',
      action: 'ROLE_ELEVATION',
      resource: 'User: dr.gupta@nag.gov.in',
      hash: '0x1c84b23290ddfae38910bc49281a8c82910a928410294829103859201938591',
    },
    {
      id: 'SEC-1090',
      timestamp: '2026-10-03 18:50:22',
      actor: 'audit.evaluator',
      action: 'DISPUTE_FINALIZED',
      resource: 'Candidate #849202',
      hash: '0x3a9f82d1c9b4e78a221fbcd9203847291029482910385920193859182910294',
    },
  ],
};
