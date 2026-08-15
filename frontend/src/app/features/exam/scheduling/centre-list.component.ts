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

import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { map } from 'rxjs/operators';
import { SchedulingService, CentreResponse, CreateCentreRequest } from './scheduling.service';
import { CentreFormDialogComponent } from './centre-form-dialog.component';
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
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    CentreFormDialogComponent
  ],
  template: `
    <div class="page-layout">
      <app-page-header
        title="Examination Centres"
        subtitle="Manage centres where examinations are conducted."
        icon="location_on"
      >
        <button mat-raised-button color="primary" (click)="openCreateDrawer()">
          <mat-icon>add</mat-icon> New Centre
        </button>
      </app-page-header>

      <app-paginated-table
        #centreTable
        [fetcher]="fetcher"
        [columns]="columns"
        [actionsTemplate]="actionsTmpl"
        title="Centres List"
        searchPlaceholder="Search by name, city, or state..."
      ></app-paginated-table>

      <ng-template #actionsTmpl let-row>
        <button mat-icon-button color="warn" matTooltip="Deactivate"
                *ngIf="row.active" (click)="deactivate(row)"
                aria-label="Deactivate centre">
          <mat-icon>block</mat-icon>
        </button>
      </ng-template>

      <!-- ── RIGHT COLLAPSIBLE DRAWER FORM ── -->
      <app-centre-form-dialog
        [isOpen]="drawerOpen"
        (close)="onDrawerClose($event)"
      ></app-centre-form-dialog>
    </div>
  `,
  styles: [`
    ::ng-deep .chip-active { background-color: #e8f5e9 !important; color: #2e7d32 !important; }
    ::ng-deep .chip-inactive { background-color: #ffebee !important; color: #b71c1c !important; }
  `]
})
export class CentreListComponent {

  @ViewChild('centreTable') centreTable!: PaginatedTableComponent<CentreResponse>;

  drawerOpen = false;

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
    private snackBar: MatSnackBar
  ) {}

  reload(): void {
    this.centreTable?.reload();
  }

  openCreateDrawer(): void {
    this.drawerOpen = true;
  }

  onDrawerClose(result: CreateCentreRequest | null): void {
    this.drawerOpen = false;
    if (!result) return;

    this.schedulingService.createCentre(result).subscribe({
      next: () => {
        this.snackBar.open('Centre created', 'OK', { duration: 3000 });
        this.reload();
      },
      error: (e) => this.snackBar.open(e?.error?.message || 'Error creating centre', 'Dismiss', { duration: 4000 })
    });
  }

  deactivate(centre: CentreResponse): void {
    this.schedulingService.deactivateCentre(centre.id).subscribe({
      next: () => {
        this.snackBar.open('Centre deactivated', 'OK', { duration: 3000 });
        this.reload();
      },
      error: (e) => this.snackBar.open(e?.error?.message || 'Error', 'Dismiss', { duration: 4000 })
    });
  }
}
