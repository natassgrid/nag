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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  PaginatedRequest,
  PaginatedResponse,
  PaginatedDataFetcher,
  ColumnDef
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
    MatProgressSpinnerModule,
    MatTooltipModule
  ],
  template: `
    <div class="paginated-table-wrapper">

      <!-- Table Header & Search Toolbar -->
      <div class="toolbar" *ngIf="title || enableSearch">
        <div class="toolbar-title" *ngIf="title">
          <h3>{{ title }}</h3>
          <span class="count-badge" *ngIf="totalElements > 0">{{ totalElements }}</span>
        </div>

        <div class="toolbar-actions">
          <mat-form-field appearance="outline" class="search-field" *ngIf="enableSearch">
            <mat-label>{{ searchPlaceholder }}</mat-label>
            <mat-icon matPrefix>search</mat-icon>
            <input
              matInput
              [(ngModel)]="searchQuery"
              (ngModelChange)="onSearchChange($event)"
              [placeholder]="searchPlaceholder"
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
    }

    .toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      border-bottom: 1px solid #f0f0f0;
      flex-wrap: wrap;
      gap: 16px;
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
      margin-bottom: -1.25em; /* Alignment tweak for Material Form Field */
    }

    .table-container {
      position: relative;
      overflow-x: auto;
      min-height: 200px;
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
  `]
})
export class PaginatedTableComponent<T = any> implements OnInit, OnDestroy, OnChanges {

  @Input() fetcher!: PaginatedDataFetcher<T>;
  @Input() columns: ColumnDef<T>[] = [];
  @Input() title: string = '';
  @Input() enableSearch: boolean = true;
  @Input() searchPlaceholder: string = 'Search...';
  @Input() pageSizeOptions: number[] = [10, 20, 50];
  @Input() defaultPageSize: number = 20;
  @Input() filters: Record<string, any> = {};
  @Input() actionsTemplate?: TemplateRef<any>;

  @Output() rowClick = new EventEmitter<T>();

  dataSource = new MatTableDataSource<T>([]);
  loading = false;
  totalElements = 0;
  pageIndex = 0;
  pageSize = 20;
  searchQuery = '';
  sortColumn = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  private searchSubject = new Subject<string>();
  private searchSub?: Subscription;

  constructor(private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.pageSize = this.defaultPageSize;

    this.searchSub = this.searchSubject
      .pipe(debounceTime(300), distinctUntilChanged())
      .subscribe(() => {
        this.pageIndex = 0;
        this.loadData();
      });

    this.loadData();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['filters'] && !changes['filters'].firstChange) {
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
