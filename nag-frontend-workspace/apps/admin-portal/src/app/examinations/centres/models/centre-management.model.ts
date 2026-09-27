export interface CentreFilterCriteria {
  searchQuery: string;
  stateFilter: string;
  statusFilter: string;
}

export interface CentreStatsSummary {
  totalCentresCount: number;
  totalCapacitySum: number;
  activeCentresCount: number;
}
