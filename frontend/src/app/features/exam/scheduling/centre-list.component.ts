import { Component, ViewChild, ChangeDetectionStrategy } from '@angular/core';
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
  templateUrl: './centre-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./centre-list.component.scss']
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
    if (!result) {
      this.drawerOpen = false;
      return;
    }

    this.schedulingService.createCentre(result).subscribe({
      next: () => {
        this.drawerOpen = false;
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
