import { BlueprintRule } from '@nag-frontend-workspace/examinations-data-access';

export type PaperTabType = 'PAPERS' | 'GENERATOR' | 'TEMPLATES';
export type PaperStatusFilter = 'ALL' | 'DRAFT' | 'APPROVED' | 'ENCRYPTED';

export interface PaperKpiStats {
  total: number;
  approved: number;
  draft: number;
  practice: number;
}

export interface PaperGenFormData {
  examId: string;
  scheduleId: string;
  shiftId: string;
  paperName: string;
  isPractice: boolean;
  useTemplate: boolean;
  selectedTemplateId: string;
  rules: BlueprintRule[];
}
