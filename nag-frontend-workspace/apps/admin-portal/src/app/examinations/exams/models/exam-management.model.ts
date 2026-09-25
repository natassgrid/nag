export interface ExamFilterCriteria {
  searchQuery: string;
  status: string;
}

export interface ExamKpiSummary {
  total: number;
  published: number;
  draft: number;
}
