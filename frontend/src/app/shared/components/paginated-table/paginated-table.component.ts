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

import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  OnChanges,
  SimpleChanges,
  ChangeDetectorRef,
  TemplateRef
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
  template: `
    <div class="paginated-table-wrapper">

      <!-- Table Header & Search Toolbar -->
      <div class="toolbar" *ngIf="title || enableSearch || enableFilter">
        <div class="toolbar-title" *ngIf="title">
          <h3>{{ title }}</h3>
        </div>

        <div class="toolbar-actions">
          <mat-form-field appearance="outline" class="search-field" *ngIf="enableSearch">
            <mat-label>{{ searchPlaceholder }}</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input
              matInput
              [(ngModel)]="searchQuery"
              (ngModelChange)="onSearchChange($event)"
            />
            <button
              *ngIf="searchQuery"
              mat-icon-button
              matSuffix
              (click)="clearSearch()"
              aria-label="Clear search"
            >
              <mat-icon>close</mat-icon>
            </button>
          </mat-form-field>

          <button
            mat-stroked-button
            class="filter-toggle-btn"
            [class.active-filter]="activeFilterCount > 0 || drawerOpen"
            (click)="toggleDrawer()"
            *ngIf="enableFilter"
            matTooltip="Toggle Filters"
            aria-label="Toggle Filters"
          >
            <mat-icon>filter_list</mat-icon>
            <span>Filters</span>
            <span class="filter-badge" *ngIf="activeFilterCount > 0">{{ activeFilterCount }}</span>
          </button>

          <button
            mat-icon-button
            matTooltip="Refresh"
            (click)="reload()"
            [disabled]="loading"
            aria-label="Refresh table data"
          >
            <mat-icon>refresh</mat-icon>
          </button>
        </div>
      </div>

      <!-- Main Layout with Table Container and Filter Drawer -->
      <div class="table-content-layout">

        <!-- Table Container -->
        <div class="table-container" [class.loading-state]="loading">

          <div class="spinner-overlay" *ngIf="loading">
            <mat-spinner diameter="40"></mat-spinner>
          </div>

          <table
            mat-table
            [dataSource]="dataSource"
            matSort
            (matSortChange)="onSortChange($event)"
            class="custom-paginated-table"
          >

            <!-- Render Configured Columns -->
            <ng-container *ngFor="let col of columns" [matColumnDef]="col.key">
              <th
                mat-header-cell
                *matHeaderCellDef
                [mat-sort-header]="col.sortable ? col.key : ''"
                [disabled]="!col.sortable"
              >
                {{ col.header }}
              </th>

              <td mat-cell *matCellDef="let row">

                <!-- Custom Template -->
                <ng-container *ngIf="col.type === 'custom' && col.template">
                  <ng-container
                    *ngTemplateOutlet="col.template; context: { $implicit: row, row: row, value: getCellValue(row, col) }"
                  ></ng-container>
                </ng-container>

                <!-- Actions Template -->
                <ng-container *ngIf="col.type === 'actions' && actionsTemplate">
                  <ng-container
                    *ngTemplateOutlet="actionsTemplate; context: { $implicit: row, row: row }"
                  ></ng-container>
                </ng-container>

                <!-- Chip / Badge -->
                <ng-container *ngIf="col.type === 'chip' || col.type === 'badge'">
                  <mat-chip-set>
                    <mat-chip [class]="col.chipClass ? col.chipClass(getCellValue(row, col), row) : ''">
                      {{ getCellValue(row, col) }}
                    </mat-chip>
                  </mat-chip-set>
                </ng-container>

                <!-- Date -->
                <ng-container *ngIf="col.type === 'date'">
                  {{ getCellValue(row, col) | date:'short' }}
                </ng-container>

                <!-- Standard Text -->
                <ng-container *ngIf="!col.type || col.type === 'text'">
                  {{ getCellValue(row, col) }}
                </ng-container>

              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumnKeys"></tr>
            <tr
              mat-row
              *matRowDef="let row; columns: displayedColumnKeys;"
              (click)="onRowClick(row)"
              class="clickable-row"
            ></tr>

            <!-- Empty State -->
            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell no-data-cell" [attr.colspan]="displayedColumnKeys.length">
                <div class="empty-state">
                  <mat-icon>inbox</mat-icon>
                  <p>No records found.</p>
                </div>
              </td>
            </tr>

          </table>
        </div>

        <!-- Filter Drawer Overlay / Panel -->
        <div class="filter-drawer" [class.open]="drawerOpen" *ngIf="enableFilter">
          <div class="filter-drawer-header">
            <span class="drawer-title">Filters</span>
            <button mat-icon-button class="close-btn" (click)="toggleDrawer()" aria-label="Close filters">
              <mat-icon>close</mat-icon>
            </button>
          </div>

          <div class="filter-drawer-body">
            <div *ngFor="let cat of activeCategories" class="filter-section">
              <div class="section-header" (click)="toggleCategory(cat)">
                <span class="section-title">{{ cat.label }}</span>
                <span class="expand-icon">{{ cat.expanded ? '˄' : '˅' }}</span>
              </div>

              <div class="section-content" *ngIf="cat.expanded">
                <div *ngFor="let opt of cat.options" class="filter-option-row">
                  <mat-checkbox
                    [(ngModel)]="opt.checked"
                    color="primary"
                  >
                    {{ opt.label }}
                  </mat-checkbox>
                </div>
                <div *ngIf="!cat.options || cat.options.length === 0" class="no-options">
                  No options available
                </div>
              </div>
            </div>
          </div>

          <div class="filter-drawer-footer">
            <button mat-stroked-button class="btn-reset" (click)="resetDrawerFilters()">Reset</button>
            <button mat-raised-button color="primary" class="btn-apply" (click)="applyDrawerFilters()">Apply</button>
          </div>
        </div>

      </div>

      <!-- Paginator -->
      <mat-paginator
        [length]="totalElements"
        [pageSize]="pageSize"
        [pageIndex]="pageIndex"
        [pageSizeOptions]="pageSizeOptions"
        (page)="onPageChange($event)"
        showFirstLastButtons
        aria-label="Select page"
      ></mat-paginator>

    </div>
  `,
  styles: [`
    .paginated-table-wrapper {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      background: white;
      overflow: hidden;
      position: relative;
    }

    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      height: 56px;
      min-height: 56px;
      padding: 0 16px;
      border-bottom: 1px solid #e0e0e0;
      flex-wrap: wrap;
      gap: 16px;
      box-sizing: border-box;
    }

    .toolbar-title {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .toolbar-title h3 {
      margin: 0;
      font-size: 18px;
      font-weight: 600;
    }

    .count-badge {
      background: #1976d2;
      color: white;
      border-radius: 12px;
      padding: 2px 8px;
      font-size: 12px;
      font-weight: 600;
    }

    .toolbar-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .search-field {
      min-width: 260px;
      height: 38px;
      margin-bottom: 0;
    }

    .filter-toggle-btn {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
      height: 38px;
      min-height: 38px;
      box-sizing: border-box;
    }
    .filter-toggle-btn.active-filter {
      background-color: #e3f2fd;
      border-color: #1976d2;
      color: #1565c0;
    }
    .filter-badge {
      background: #1976d2;
      color: white;
      border-radius: 10px;
      padding: 0 6px;
      font-size: 11px;
      font-weight: 700;
    }

    .table-content-layout {
      display: flex;
      position: relative;
      overflow: hidden;
      min-height: 250px;
    }

    .table-container {
      flex: 1;
      position: relative;
      overflow-x: auto;
      min-height: 200px;
      transition: width 0.2s ease;
    }

    .table-container.loading-state {
      opacity: 0.6;
    }

    .spinner-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.7);
      z-index: 10;
      display: flex;
      justify-content: center;
      align-items: center;
    }

    .custom-paginated-table {
      width: 100%;
    }

    .custom-paginated-table th.mat-mdc-header-cell {
      height: 48px;
      font-weight: 600;
      background-color: #fafafa;
    }

    .clickable-row {
      cursor: pointer;
      transition: background 0.15s;
    }
    .clickable-row:hover {
      background-color: #f5f5f5;
    }

    .no-data-cell {
      padding: 48px;
      text-align: center;
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      color: #9e9e9e;
    }

    .empty-state mat-icon {
      font-size: 48px;
      height: 48px;
      width: 48px;
      margin-bottom: 8px;
    }

    /* ── FILTER DRAWER SIDEBAR ───────────────────────────────────────── */
    .filter-drawer {
      width: 0;
      max-width: 320px;
      background: #fafafa;
      border-left: 1px solid #e0e0e0;
      display: flex;
      flex-direction: column;
      transition: width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
      overflow: hidden;
      white-space: nowrap;
      z-index: 15;
    }

    .filter-drawer.open {
      width: 300px;
    }

    .filter-drawer-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      height: 56px;
      min-height: 56px;
      padding: 0 16px;
      border-bottom: 1px solid #e0e0e0;
      background: #ffffff;
      box-sizing: border-box;
    }

    .drawer-title {
      font-size: 16px;
      font-weight: 600;
      color: #212121;
    }

    .close-btn {
      width: 32px;
      height: 32px;
      line-height: 32px;
    }

    .filter-drawer-body {
      flex: 1;
      overflow-y: auto;
      padding: 8px 0;
    }

    .filter-section {
      border-bottom: 1px solid #eeeeee;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      cursor: pointer;
      user-select: none;
      background: #ffffff;
      font-weight: 500;
      color: #333333;
      transition: background 0.15s;
    }

    .section-header:hover {
      background: #f0f0f0;
    }

    .section-title {
      font-size: 14px;
    }

    .expand-icon {
      font-size: 16px;
      font-weight: bold;
      color: #666666;
    }

    .section-content {
      padding: 8px 16px 12px 24px;
      background: #fafafa;
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .filter-option-row {
      display: flex;
      align-items: center;
      font-size: 13px;
    }

    .no-options {
      font-size: 12px;
      color: #999;
      font-style: italic;
      padding: 4px 0;
    }

    .filter-drawer-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-top: 1px solid #e0e0e0;
      background: #ffffff;
      gap: 12px;
    }

    .btn-reset {
      flex: 1;
    }

    .btn-apply {
      flex: 1;
    }
  `]
})
export class PaginatedTableComponent<T = any> implements OnInit, OnDestroy, OnChanges {

  @Input() fetcher!: PaginatedDataFetcher<T>;
  @Input() columns: ColumnDef<T>[] = [];
  @Input() title: string = '';
  @Input() enableSearch: boolean = true;
  @Input() enableFilter: boolean = true;
  @Input() searchPlaceholder: string = 'Search...';
  @Input() pageSizeOptions: number[] = [10, 20, 50];
  @Input() defaultPageSize: number = 20;
  @Input() filters: Record<string, any> = {};
  @Input() filterCategories?: FilterCategory[];
  @Input() actionsTemplate?: TemplateRef<any>;

  @Output() rowClick = new EventEmitter<T>();
  @Output() filterChange = new EventEmitter<Record<string, any>>();

  dataSource = new MatTableDataSource<T>([]);
  loading = false;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
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

