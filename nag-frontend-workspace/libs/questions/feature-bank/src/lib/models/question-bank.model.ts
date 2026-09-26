export type QuestionDifficultyFilter = 'ALL' | 'EASY' | 'MEDIUM' | 'HARD';
export type QuestionStatusFilter = 'ALL' | 'APPROVED' | 'REVIEW' | 'DRAFT' | 'REJECTED';

export interface QuestionBankFilterCriteria {
  searchQuery: string;
  selectedSubject: string;
  selectedDifficulty: QuestionDifficultyFilter | string;
  selectedStatus: QuestionStatusFilter | string;
  pageSize: number;
  currentPage: number;
}
