export interface ApiResponse<T> {
  status?: string;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
