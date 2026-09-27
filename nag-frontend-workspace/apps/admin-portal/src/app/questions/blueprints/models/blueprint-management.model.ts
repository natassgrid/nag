import {
  BlueprintRule,
  BlueprintTemplateResponse,
  BlueprintTemplateRequest,
  BlueprintFeasibilityResponse,
} from '@nag-frontend-workspace/questions-data-access';

export interface BlueprintFilterCriteria {
  searchQuery: string;
}

export interface BlueprintStatsSummary {
  activeBlueprintsCount: number;
  totalQuestionsCount: number;
  taxonomySubjectsCount: number;
}
