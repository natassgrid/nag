import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { map } from 'rxjs/operators';
import { SchedulingService, CentreResponse, CreateCentreRequest } from './scheduling.service';
import { CentreFormDialogComponent } from './centre-form-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher
} from '../../../shared/components/paginated-table';
import { ColumnDef } from '../../../shared/components/paginated-table/pagination.model';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-centre-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatDialogModule,
    PaginatedTableComponent,
    PageHeaderComponent,
  ],
  template: `
    <div class="page-layout">
      <app-page-header
        title="Examination Centres"
        subtitle="Manage centres where examinations are conducted."
        icon="location_on"
      >
        <button mat-raised-button color="primary" (click)="openCreate()">
          <mat-icon>add</mat-icon> New Centre
        </button>
      </app-page-header>

      <mat-card>
        <mat-card-content>
          <app-paginated-table
            #centreTable
            [fetcher]="fetcher"
            [columns]="columns"
            [actionsTemplate]="actionsTmpl"
            title="Centres"
            searchPlaceholder="Search by name, city, or state..."
          ></app-paginated-table>

          <ng-template #actionsTmpl let-row>
            <button mat-icon-button color="warn" matTooltip="Deactivate"
                    *ngIf="row.active" (click)="deactivate(row)"
                    aria-label="Deactivate centre">
              <mat-icon>block</mat-icon>
            </button>
          </ng-template>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    ::ng-deep .chip-active { background-color: #e8f5e9 !important; color: #2e7d32 !important; }
    ::ng-deep .chip-inactive { background-color: #ffebee !important; color: #b71c1c !important; }
  `]
})
export class CentreListComponent {

  @ViewChild('centreTable') centreTable!: PaginatedTableComponent<CentreResponse>;

  columns: ColumnDef<CentreResponse>[] = [
    { key: 'centreName', header: 'Name', sortable: true },
    { key: 'city', header: 'City', cell: (row) => row.cityName || row.city, sortable: true },
    { key: 'state', header: 'State', cell: (row) => row.stateName || row.state, sortable: true },
    { key: 'district', header: 'District', cell: (row) => row.district || '—' },
    { key: 'totalCapacity', header: 'Capacity', sortable: true },
    {
      key: 'active',
      header: 'Status',
      type: 'chip',
      cell: (row) => row.active ? 'Active' : 'Inactive',
      chipClass: (val) => val === 'Active' ? 'chip-active' : 'chip-inactive'
    },
    { key: 'actions', header: 'Actions', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<CentreResponse> = (req) => {
    return this.schedulingService.listCentres(undefined, undefined, req.page, req.size, req.search).pipe(
      map(centres => {
        return {
          content: centres,
          totalElements: centres.length,
          totalPages: 1,
          size: req.size,
          number: req.page
        };
      })
    );
  };

  constructor(
    private schedulingService: SchedulingService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  reload(): void {
    this.centreTable?.reload();
  }

  openCreate(): void {
    const ref = this.dialog.open(CentreFormDialogComponent, { width: '640px', data: {} });
    ref.afterClosed().subscribe((result: CreateCentreRequest | undefined) => {
      if (!result) return;
      this.schedulingService.createCentre(result).subscribe({
        next: () => { this.snackBar.open('Centre created', 'OK', { duration: 3000 }); this.reload(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error creating centre', 'Dismiss', { duration: 4000 })
      });
    });
  }

  deactivate(centre: CentreResponse): void {
    const ref = this.dialog.open(ConfirmDialogComponent, {
      data: {
        title: 'Deactivate Centre',
        message: `Deactivate centre "${centre.centreName}"? It will no longer be available for seat allocation.`,
        confirmText: 'Deactivate',
        color: 'warn',
        icon: 'block'
      } as ConfirmDialogData
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.schedulingService.deactivateCentre(centre.id).subscribe({
        next: () => { this.snackBar.open('Centre deactivated', 'OK', { duration: 3000 }); this.reload(); },
        error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
      });
    });
  }
}
