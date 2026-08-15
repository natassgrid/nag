/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

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

export interface FilterOption {
  label: string;
  value: any;
  checked?: boolean;
}

export interface FilterCategory {
  key: string;
  label: string;
  expanded?: boolean;
  type?: 'checkbox' | 'select' | 'text' | 'date-range';
  options?: FilterOption[];
  selectedValue?: any;
}

