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

import { Component, OnInit, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { of, catchError } from 'rxjs';
import {
  PaperService,
  PaperGenerationRequest,
  PaperSummary,
  PaperGenerationResponse
} from './paper.service';
import { PaperGenerateDialogComponent } from './paper-generate-dialog.component';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher,
  FilterCategory
} from '../../shared/components/paginated-table';
import { ColumnDef } from '../../shared/components/paginated-table/pagination.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-paper-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    PaperGenerateDialogComponent
  ],
  templateUrl: './paper-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./paper-list.component.scss']
})
export class PaperListComponent implements OnInit {

  @ViewChild('paperTable') paperTable?: PaginatedTableComponent<PaperSummary>;

  generateDrawerOpen = false;
  activeFilters: Record<string, any> = {};
  approvingId: string | null = null;
  lastResult: (PaperGenerationResponse & { status: string }) | null = null;

  filterCategories: FilterCategory[] = [
    {
      key: 'status',
      label: 'Status',
      expanded: true,
      options: [
        { label: 'Draft', value: 'DRAFT' },
        { label: 'Approved', value: 'APPROVED' },
        { label: 'Encrypted', value: 'ENCRYPTED' }
      ]
    },
    {
      key: 'createdAt',
      label: 'Created Date',
      expanded: false,
      options: [
        { label: 'Today', value: 'TODAY' },
        { label: 'Last 7 Days', value: 'LAST_7_DAYS' },
        { label: 'Last 30 Days', value: 'LAST_30_DAYS' }
      ]
    }
  ];

  columns: ColumnDef<PaperSummary>[] = [
    {
      key: 'paperId',
      header: 'Paper ID',
      cell: (row) => row.paperId?.substring(0, 8) + '…'
    },
    { key: 'examId', header: 'Exam ID', cell: (row) => row.examId?.substring(0, 8) + '…' },
    { key: 'shiftId', header: 'Shift', sortable: true },
    {
      key: 'difficultyScore',
      header: 'Difficulty Score',
      cell: (row) => row.difficultyScore?.toFixed(2) ?? '—'
    },
    {
      key: 'status',
      header: 'Status',
      type: 'chip',
      chipClass: (val: string) => 'status-' + (val ?? '').toLowerCase(),
      sortable: true
    },
    { key: 'createdAt', header: 'Created', type: 'date', sortable: true },
    { key: 'actions', header: 'Actions', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<PaperSummary> = (req) => {
    const statusVal = Array.isArray(this.activeFilters['status'])
      ? this.activeFilters['status'][0]
      : this.activeFilters['status'];

    return this.paperService.getPapers(
      req.page,
      req.size,
      this.activeFilters['examId'],
      statusVal
    );
  };

  constructor(
    private paperService: PaperService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {}

  navigateToBlueprints(): void {
    this.router.navigate(['/papers/blueprints']);
  }

  onFilterChange(filters: Record<string, any>): void {
    this.activeFilters = { ...filters };
  }

  // ── Generate ─────────────────────────────────────────────────────────

  openGenerateDrawer(): void {
    this.generateDrawerOpen = true;
  }

  onGenerateDrawerClose(request: PaperGenerationRequest | null): void {
    this.generateDrawerOpen = false;
    if (!request) return;

    this.paperService
      .generatePaper(request)
      .pipe(
        catchError((err) => {
          const detail =
            err?.error?.detail ?? err?.error?.message ?? err?.message ?? 'Unknown error';
          const gapDetails: any[] = err?.error?.gapDetails ?? [];
          const gapMsg = gapDetails.length
            ? ' Gaps: ' +
              gapDetails
                .map(
                  (g: any) =>
                    `${g.subject}/${g.topic}/${g.difficulty} (need ${g.needed}, have ${g.available})`
                )
                .join('; ')
            : '';
          this.lastResult = {
            paperId: '',
            status: 'ERROR',
            message: detail + gapMsg
          };
          this.snackBar.open('Paper generation failed: ' + detail, 'Dismiss', {
            duration: 6000,
            panelClass: 'snack-error'
          });
          return of(null);
        })
      )
      .subscribe((res) => {
        if (!res) return;
        this.lastResult = { ...res, status: res.status ?? 'DRAFT' };
        this.snackBar.open(
          `Paper generated — ID: ${res.paperId.substring(0, 8)}…`,
          'OK',
          { duration: 5000 }
        );
        this.paperTable?.reload();
      });
  }

  // ── Approve ──────────────────────────────────────────────────────────

  approvePaper(paper: PaperSummary): void {
    this.approvingId = paper.paperId;

    this.paperService
      .approvePaper(paper.paperId)
      .pipe(
        catchError((err) => {
          const msg =
            err?.error?.detail ?? err?.error?.message ?? 'Approval failed';
          this.snackBar.open(msg, 'Dismiss', { duration: 5000, panelClass: 'snack-error' });
          this.approvingId = null;
          return of(null);
        })
      )
      .subscribe((res) => {
        this.approvingId = null;
        if (!res) return;
        this.snackBar.open(
          `Paper ${res.paperId.substring(0, 8)}… is now ${res.status}`,
          'OK',
          { duration: 4000 }
        );
        this.paperTable?.reload();
      });
  }
}
