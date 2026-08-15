import { Component, OnInit, ViewChild } from '@angular/core';
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
  template: `
    <div class="page-layout">

      <!-- ── Page header ─────────────────────────────────────────────── -->
      <app-page-header
        title="Paper Generation"
        subtitle="Generate, review, and approve examination papers from blueprints."
        icon="description"
      >
        <button
          mat-raised-button
          color="primary"
          (click)="openGenerateDrawer()"
        >
          <mat-icon>auto_awesome</mat-icon>
          Generate Paper
        </button>
      </app-page-header>

      <!-- ── Paper table ──────────────────────────────────────────────── -->
      <app-paginated-table
        #paperTable
        [fetcher]="fetcher"
        [columns]="columns"
        [actionsTemplate]="actionsTmpl"
        [filters]="activeFilters"
        [filterCategories]="filterCategories"
        (filterChange)="onFilterChange($event)"
        title="Generated Papers"
        searchPlaceholder="Search by shift ID or paper ID…"
      ></app-paginated-table>

      <!-- Actions template ─────────────────────────────────────── -->
      <ng-template #actionsTmpl let-row>
        <!-- Approve: only DRAFT papers -->
        <button
          mat-icon-button
          color="primary"
          matTooltip="Approve & Encrypt"
          *ngIf="row.status === 'DRAFT'"
          (click)="approvePaper(row)"
          [disabled]="approvingId === row.paperId"
          aria-label="Approve and encrypt paper"
        >
          <mat-spinner
            diameter="20"
            *ngIf="approvingId === row.paperId"
          ></mat-spinner>
          <mat-icon *ngIf="approvingId !== row.paperId">verified</mat-icon>
        </button>

        <!-- Already approved / encrypted: show lock icon, no action -->
        <mat-icon
          *ngIf="row.status === 'APPROVED'"
          class="status-icon approved"
          matTooltip="Approved — awaiting encryption"
        >check_circle_outline</mat-icon>

        <mat-icon
          *ngIf="row.status === 'ENCRYPTED'"
          class="status-icon encrypted"
          matTooltip="Encrypted and sealed"
        >lock</mat-icon>
      </ng-template>

      <!-- ── Last generation result banner ────────────────────────────── -->
      <div class="result-banner" *ngIf="lastResult" [class.result-banner--success]="lastResult.status !== 'ERROR'">
        <mat-icon>{{ lastResult.status !== 'ERROR' ? 'check_circle' : 'error' }}</mat-icon>
        <div class="result-text">
          <strong>{{ lastResult.status !== 'ERROR' ? 'Paper generated' : 'Generation failed' }}</strong>
          <span class="result-detail">{{ lastResult.message }}</span>
          <span class="result-id" *ngIf="lastResult.paperId">
            Paper ID: <code>{{ lastResult.paperId }}</code>
          </span>
        </div>
        <button mat-icon-button (click)="lastResult = null" aria-label="Dismiss">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- ── RIGHT COLLAPSIBLE GENERATE PAPER DRAWER ── -->
      <app-paper-generate-dialog
        [isOpen]="generateDrawerOpen"
        [examId]="activeFilters['examId'] || undefined"
        (close)="onGenerateDrawerClose($event)"
      ></app-paper-generate-dialog>

    </div>
  `,
  styles: [`
    /* ── Status icons ───────────────────────────────────────────────── */
    .status-icon {
      font-size: 20px;
      height: 20px;
      width: 20px;
      vertical-align: middle;
      margin: 0 8px;
    }

    .status-icon.approved { color: #f57c00; }
    .status-icon.encrypted { color: #388e3c; }

    /* ── Result banner ──────────────────────────────────────────────── */
    .result-banner {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 18px;
      border-radius: 8px;
      background: #ffebee;
      border-left: 4px solid #e53935;
      color: #b71c1c;
      margin-top: 16px;
    }

    .result-banner--success {
      background: #e8f5e9;
      border-color: #388e3c;
      color: #1b5e20;
    }

    .result-banner mat-icon {
      flex-shrink: 0;
      margin-top: 2px;
    }

    .result-text {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 2px;
      font-size: 14px;
    }

    .result-detail {
      opacity: 0.85;
    }

    .result-id code {
      font-family: monospace;
      font-size: 12px;
      background: rgba(0,0,0,0.06);
      padding: 1px 4px;
      border-radius: 3px;
    }

    /* ── Status chip overrides ──────────────────────────────────────── */
    ::ng-deep .status-draft {
      background-color: #fff3e0 !important;
      color: #e65100 !important;
    }
    ::ng-deep .status-approved {
      background-color: #e3f2fd !important;
      color: #1565c0 !important;
    }
    ::ng-deep .status-encrypted {
      background-color: #e8f5e9 !important;
      color: #2e7d32 !important;
    }
  `]
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
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {}

  onFilterChange(filters: Record<string, any>): void {
    this.activeFilters = { ...filters };
  }

  // ── Generate ─────────────────────────────────────────────────────────────

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

  // ── Approve ───────────────────────────────────────────────────────────────

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
