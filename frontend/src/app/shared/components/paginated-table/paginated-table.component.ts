import {
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
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef,
  TemplateRef,
  ChangeDetectionStrategy
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  PaginatedRequest,
  PaginatedResponse,
  PaginatedDataFetcher,
  ColumnDef,
  FilterCategory,
  FilterOption
} from './pagination.model';

@Component({
  selector: 'app-paginated-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  templateUrl: './paginated-table.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./paginated-table.component.scss']
})
export class PaginatedTableComponent<T = any> implements OnInit, OnDestroy, OnChanges {

  @Input() fetcher!: PaginatedDataFetcher<T>;
  @Input() columns: ColumnDef<T>[] = [];
  @Input() title: string = '';
  @Input() enableSearch: boolean = true;
  @Input() enableFilter: boolean = true;
  @Input() searchPlaceholder: string = 'Search...';
  @Input() pageSizeOptions: number[] = [10, 20, 50];
  @Input() defaultPageSize: number = 10;
  @Input() filters: Record<string, any> = {};
  @Input() filterCategories?: FilterCategory[];
  @Input() actionsTemplate?: TemplateRef<any>;

  @Output() rowClick = new EventEmitter<T>();
  @Output() filterChange = new EventEmitter<Record<string, any>>();

  dataSource = new MatTableDataSource<T>([]);
  loading = false;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 10;
  searchQuery = '';
  sortColumn = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  drawerOpen = false;
  activeCategories: FilterCategory[] = [];
  activeFilterCount = 0;

  private searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.pageSize = this.defaultPageSize;

    this.initFilterCategories();

    this.searchSub = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadData();
      });

    this.loadData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['filterCategories']) {
      this.initFilterCategories();
    }
    if (changes['filters'] && !changes['filters'].firstChange) {
      this.syncCategoriesWithFilters();
      this.pageIndex = 0;
      this.loadData();
    }
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
  }

  get displayedColumnKeys(): string[] {
    return this.columns.map(col => col.key);
  }

  toggleDrawer(): void {
    this.drawerOpen = !this.drawerOpen;
  }

  toggleCategory(targetCat: FilterCategory): void {
    const currentState = !!targetCat.expanded;
    this.activeCategories.forEach(c => c.expanded = false);
    targetCat.expanded = !currentState;
  }

  initFilterCategories(): void {
    if (this.filterCategories && this.filterCategories.length > 0) {
      this.activeCategories = this.filterCategories.map((c, idx) => ({
        ...c,
        expanded: idx === 0,
        options: (c.options || []).map(o => ({ ...o, checked: !!o.checked }))
      }));
    } else {
      this.activeCategories = this.generateDefaultCategories();
    }
    this.syncCategoriesWithFilters();
  }

  generateDefaultCategories(): FilterCategory[] {
    const cats: FilterCategory[] = [];

    this.columns.forEach(col => {
      if (col.type === 'actions' || col.type === 'custom') return;

      if (col.key === 'questionType') {
        cats.push({
          key: 'questionType',
          label: 'Question Type',
          expanded: true,
          options: [
            { label: 'MCQ', value: 'SINGLE_MCQ' },
            { label: 'True / False', value: 'TRUE_FALSE' },
            { label: 'Descriptive', value: 'DESCRIPTIVE' },
            { label: 'Coding', value: 'CODING' }
          ]
        });
      } else if (col.key === 'status' || col.key === 'state' || col.key === 'accountStatus' || col.key === 'active') {
        cats.push({
          key: col.key,
          label: 'Status',
          expanded: false,
          options: [
            { label: 'Active', value: 'ACTIVE' },
            { label: 'Draft', value: 'DRAFT' },
            { label: 'Approved', value: 'APPROVED' },
            { label: 'Published', value: 'PUBLISHED' },
            { label: 'Archived', value: 'ARCHIVED' }
          ]
        });
      } else if (col.key === 'subject') {
        cats.push({
          key: 'subject',
          label: 'Subject',
          expanded: false,
          options: [
            { label: 'Mathematics', value: 'Mathematics' },
            { label: 'Physics', value: 'Physics' },
            { label: 'Chemistry', value: 'Chemistry' },
            { label: 'Computer Science', value: 'Computer Science' }
          ]
        });
      } else if (col.key === 'difficulty') {
        cats.push({
          key: 'difficulty',
          label: 'Difficulty',
          expanded: false,
          options: [
            { label: 'Easy', value: 'EASY' },
            { label: 'Medium', value: 'MEDIUM' },
            { label: 'Hard', value: 'HARD' }
          ]
        });
      } else if (col.key === 'createdAt') {
        cats.push({
          key: 'createdAt',
          label: 'Created Date',
          expanded: false,
          options: [
            { label: 'Today', value: 'TODAY' },
            { label: 'Last 7 Days', value: 'LAST_7_DAYS' },
            { label: 'Last 30 Days', value: 'LAST_30_DAYS' }
          ]
        });
      }
    });

    return cats;
  }

  syncCategoriesWithFilters(): void {
    let activeCount = 0;
    this.activeCategories.forEach(cat => {
      const currentVal = this.filters[cat.key];
      if (!currentVal) {
        cat.options?.forEach(o => o.checked = false);
      } else {
        const valArr = Array.isArray(currentVal) ? currentVal : [currentVal];
        cat.options?.forEach(o => {
          o.checked = valArr.includes(o.value) || valArr.includes(o.label);
        });
        if (cat.options?.some(o => o.checked)) {
          activeCount++;
        }
      }
    });
    this.activeFilterCount = activeCount;
  }

  applyDrawerFilters(): void {
    const updatedFilters: Record<string, any> = { ...this.filters };
    let activeCount = 0;

    this.activeCategories.forEach(cat => {
      const selectedOpts = cat.options?.filter(o => o.checked) || [];
      if (selectedOpts.length > 0) {
        updatedFilters[cat.key] = selectedOpts.length === 1 ? selectedOpts[0].value : selectedOpts.map(o => o.value);
        activeCount++;
      } else {
        delete updatedFilters[cat.key];
      }
    });

    this.filters = updatedFilters;
    this.activeFilterCount = activeCount;
    this.filterChange.emit(this.filters);
    this.pageIndex = 0;
    this.loadData();
  }

  resetDrawerFilters(): void {
    this.activeCategories.forEach(cat => {
      cat.options?.forEach(o => o.checked = false);
    });

    this.filters = {};
    this.activeFilterCount = 0;
    this.filterChange.emit(this.filters);
    this.pageIndex = 0;
    this.loadData();
  }

  loadData(): void {
    if (!this.fetcher) return;

    this.loading = true;
    this.cdr.detectChanges();

    const request: PaginatedRequest = {
      page: this.pageIndex,
      size: this.pageSize,
      search: this.searchQuery.trim() || undefined,
      sort: this.sortColumn || undefined,
      order: this.sortColumn ? this.sortDirection : undefined,
      filters: Object.keys(this.filters).length > 0 ? this.filters : undefined
    };

    this.fetcher(request).subscribe({
      next: (response: PaginatedResponse<T>) => {
        const content = response?.content ?? (Array.isArray(response) ? response : []);
        this.dataSource.data = [...content];
        this.totalElements = response?.totalElements ?? content.length;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[PaginatedTableComponent] Error fetching data:', err);
        this.dataSource.data = [];
        this.totalElements = 0;
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  reload(): void {
    this.loadData();
  }

  onSearchChange(value: string): void {
    this.searchSubject.next(value);
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchSubject.next('');
  }

  onSortChange(sort: Sort): void {
    this.sortColumn = sort.active;
    this.sortDirection = (sort.direction as 'asc' | 'desc') || 'asc';
    this.pageIndex = 0;
    this.loadData();
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadData();
  }

  onRowClick(row: T): void {
    this.rowClick.emit(row);
  }

  getCellValue(row: T, col: ColumnDef<T>): any {
    if (col.cell) {
      return col.cell(row);
    }
    return (row as any)[col.key];
  }
}

