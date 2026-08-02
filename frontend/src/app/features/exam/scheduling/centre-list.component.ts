import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { map } from 'rxjs';

import { SchedulingService, CentreResponse } from './scheduling.service';
import { CentreFormDialogComponent } from './centre-form-dialog.component';
import { PaginatedTableComponent } from '../../../shared/components/paginated-table/paginated-table.component';
import { ColumnDef, PaginatedDataFetcher, PaginatedResponse } from '../../../shared/components/paginated-table/pagination.model';

@Component({
  selector: 'app-centre-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule,
    PaginatedTableComponent
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>Examination Centres</h1>
          <p class="subtitle">Manage examination centres across states, cities, and districts.</p>
        </div>
        <button mat-raised-button color="primary" (click)="openCreateCentreDialog()">
          <mat-icon>add</mat-icon> New Centre
        </button>
      </div>

      <!-- Filter Bar -->
      <div class="filter-bar">
        <mat-form-field appearance="outline">
          <mat-label>State</mat-label>
          <input matInput [(ngModel)]="filterState" placeholder="Filter by State" (keyup.enter)="applyFilters()" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>City</mat-label>
          <input matInput [(ngModel)]="filterCity" placeholder="Filter by City" (keyup.enter)="applyFilters()" />
        </mat-form-field>

        <button mat-raised-button color="primary" (click)="applyFilters()">Apply Filters</button>
        <button mat-button (click)="clearFilters()">Clear</button>
      </div>

      <app-paginated-table
        #table
        [fetcher]="fetcher"
        [columns]="columns"
        [actionsTemplate]="actionsTmpl"
        [filters]="activeFilters"
        title="Centres List"
        searchPlaceholder="Search centre name..."
      >
      </app-paginated-table>

      <ng-template #actionsTmpl let-row>
        <button
          *ngIf="row.active"
          mat-stroked-button
          color="warn"
          (click)="deactivateCentre(row)"
          matTooltip="Deactivate this centre"
        >
          Deactivate
        </button>
        <span *ngIf="!row.active" class="inactive-label">Deactivated</span>
      </ng-template>
    </div>
  `,
  styles: [`
    .page-container {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    .page-header h1 {
      margin: 0 0 4px 0;
      font-size: 24px;
      font-weight: 600;
    }
    .subtitle {
      margin: 0;
      color: #666;
      font-size: 14px;
    }

    .filter-bar {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 20px;
      flex-wrap: wrap;
    }
    .filter-bar mat-form-field {
      margin-bottom: -1.25em;
      min-width: 200px;
    }

    .inactive-label {
      color: #9e9e9e;
      font-style: italic;
      font-size: 13px;
    }

    ::ng-deep .chip-active {
      background: #e8f5e9 !important;
      color: #2e7d32 !important;
    }
    ::ng-deep .chip-inactive {
      background: #ffebee !important;
      color: #c62828 !important;
    }
  `]
})
export class CentreListComponent implements OnInit {
  @ViewChild('table') table!: PaginatedTableComponent<CentreResponse>;

  filterState = '';
  filterCity = '';
  activeFilters: Record<string, any> = {};

  columns: ColumnDef<CentreResponse>[] = [
    { key: 'centreName', header: 'Centre Name', sortable: true },
    { key: 'city', header: 'City', sortable: true },
    { key: 'state', header: 'State', sortable: true },
    { key: 'district', header: 'District', sortable: true, cell: r => r.district || '-' },
    { key: 'totalCapacity', header: 'Total Capacity', sortable: true },
    {
      key: 'active',
      header: 'Status',
      type: 'chip',
      cell: r => (r.active ? 'Active' : 'Inactive'),
      chipClass: val => (val === 'Active' ? 'chip-active' : 'chip-inactive')
    },
    { key: 'actions', header: 'Actions', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<CentreResponse> = (params) => {
    const state = params.filters?.['state'];
    const city = params.filters?.['city'];

    return this.schedulingService.listCentres(state, city).pipe(
      map(centres => {
        let filtered = centres || [];
        if (params.search) {
          const q = params.search.toLowerCase();
          filtered = filtered.filter(
            c => c.centreName.toLowerCase().includes(q) || c.city.toLowerCase().includes(q)
          );
        }

        const total = filtered.length;
        const start = params.page * params.size;
        const pageData = filtered.slice(start, start + params.size);

        return {
          content: pageData,
          totalElements: total,
          totalPages: Math.ceil(total / params.size) || 1,
          size: params.size,
          number: params.page
        } as PaginatedResponse<CentreResponse>;
      })
    );
  };

  constructor(
    private schedulingService: SchedulingService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {}

  applyFilters(): void {
    this.activeFilters = {};
    if (this.filterState.trim()) this.activeFilters['state'] = this.filterState.trim();
    if (this.filterCity.trim()) this.activeFilters['city'] = this.filterCity.trim();
  }

  clearFilters(): void {
    this.filterState = '';
    this.filterCity = '';
    this.activeFilters = {};
  }

  openCreateCentreDialog(): void {
    const dialogRef = this.dialog.open(CentreFormDialogComponent, { width: '500px' });
    dialogRef.afterClosed().subscribe(req => {
      if (req) {
        this.schedulingService.createCentre(req).subscribe({
          next: () => {
            this.snackBar.open('Centre created successfully', 'OK', { duration: 3000 });
            this.table.reload();
          },
          error: err => this.snackBar.open(err?.error?.message || 'Failed to create centre', 'Dismiss', { duration: 4000 })
        });
      }
    });
  }

  deactivateCentre(centre: CentreResponse): void {
    if (confirm(`Are you sure you want to deactivate ${centre.centreName}?`)) {
      this.schedulingService.deactivateCentre(centre.id).subscribe({
        next: () => {
          this.snackBar.open('Centre deactivated', 'OK', { duration: 3000 });
          this.table.reload();
        },
        error: err => this.snackBar.open(err?.error?.message || 'Failed to deactivate centre', 'Dismiss', { duration: 4000 })
      });
    }
  }
}
