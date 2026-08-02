import { Observable } from 'rxjs';
import { TemplateRef } from '@angular/core';

/**
 * Standard parameters for server-side paginated and searched requests.
 */
export interface PaginatedRequest {
  page: number;
  size: number;
  search?: string;
  sort?: string;
  order?: 'asc' | 'desc';
  filters?: Record<string, any>;
}

/**
 * Standard generic envelope returned by paginated API endpoints.
 */
export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number; // 0-indexed current page number
}

/**
 * Function interface for fetching paginated data from a backend service.
 */
export type PaginatedDataFetcher<T> = (params: PaginatedRequest) => Observable<PaginatedResponse<T>>;

/**
 * Column definition schema for configuring the paginated table.
 */
export interface ColumnDef<T> {
  /** Unique key or property name on T */
  key: string;
  /** Header label displayed in table header cell */
  header: string;
  /** Optional custom value extractor / formatter */
  cell?: (row: T) => string | number | boolean | null | undefined;
  /** Whether the column supports sorting */
  sortable?: boolean;
  /** Visual type styling for cell rendering */
  type?: 'text' | 'badge' | 'date' | 'chip' | 'actions' | 'custom';
  /** Optional class provider for 'badge' or 'chip' types */
  chipClass?: (value: any, row: T) => string;
  /** Custom template reference for 'custom' column types */
  template?: TemplateRef<any>;
}
