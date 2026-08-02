import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatBadgeModule } from '@angular/material/badge';
import { of, catchError } from 'rxjs';
import {
  PaperService,
  PaperGenerationRequest,
  PaperSummary,
  PaperGenerationResponse
} from './paper.service';
import {
  PaperGenerateDialogComponent,
  PaperGenerateDialogData
} from './paper-generate-dialog.component';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher
} from '../../shared/components/paginated-table';
import { ColumnDef } from '../../shared/components/paginated-table/pagination.model';

@Component({
  selector: 'app-paper-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatBadgeModule,
    PaginatedTableComponent,
  ],
  template: `
    <div class="papers-container">

      <!-- ── Page header ─────────────────────────────────────────────── -->
      <div class="page-header">
        <div class="page-title-group">
          <h1 class="page-title">
            <mat-icon class="title-icon">description</mat-icon>
            Paper Generation
          </h1>
          <p class="page-subtitle">
            Generate, review, and approve examination papers from blueprints.
          </p>
        </div>

        <button
          mat-raised-button
          color="primary"
          (click)="openGenerateDialog()"
          class="generate-btn"
        >
          <mat-icon>auto_awesome</mat-icon>
          Generate Paper
        </button>
      </div>

      <!-- ── Filter bar ───────────────────────────────────────────────── -->
      <mat-card class="filter-card">
        <mat-card-content>
          <div class="filter-row">
            <mat-form-field appearance="outline" class="filter-field">
              <mat-label>Filter by Exam ID</mat-label>
              <mat-icon matPrefix>search</mat-icon>
              <input
                matInput
                [(ngModel)]="filterExamId"
                (keyup.enter)="applyFilter()"
                placeholder="Paste exam UUID…"
              />
              <button
                *ngIf="filterExamId"
                mat-icon-button
                matSuffix
                (click)="filterExamId = ''; applyFilter()"
                aria-label="Clear exam filter"
              >
                <mat-icon>close</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline" class="filter-field filter-status">
              <mat-label>Status</mat-label>
              <mat-select [(ngModel)]="filterStatus" (ngModelChange)="applyFilter()">
                <mat-option value="">All</mat-option>
                <mat-option value="DRAFT">Draft</mat-option>
                <mat-option value="APPROVED">Approved</mat-option>
                <mat-option value="ENCRYPTED">Encrypted</mat-option>
              </mat-select>
            </mat-form-field>

            <button mat-stroked-button (click)="applyFilter()" matTooltip="Apply filters">
              <mat-icon>filter_list</mat-icon> Filter
            </button>
            <button mat-stroked-button (click)="clearFilters()" *ngIf="filterExamId || filterStatus">
              <mat-icon>clear</mat-icon> Clear
            </button>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- ── Paper table ──────────────────────────────────────────────── -->
      <mat-card class="table-card">
        <mat-card-content>
          <app-paginated-table
            #paperTable
            [fetcher]="fetcher"
            [columns]="columns"
            [actionsTemplate]="actionsTmpl"
            [filters]="activeFilters"
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

        </mat-card-content>
      </mat-card>

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

    </div>
  `,
  styles: [`
    .papers-container {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 20px;
      max-width: 1400px;
      margin: 0 auto;
    }

    /* ── Header ─────────────────────────────────────────────────────── */
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      gap: 16px;
      flex-wrap: wrap;
    }

    .page-title-group {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      display: flex;
      align-items: center;
      gap: 10px;
      color: #212121;
    }

    .title-icon {
      font-size: 28px;
      height: 28px;
      width: 28px;
      color: #1976d2;
    }

    .page-subtitle {
      margin: 0;
      font-size: 14px;
      color: #757575;
    }

    .generate-btn {
      flex-shrink: 0;
      height: 42px;
    }

    /* ── Filter bar ─────────────────────────────────────────────────── */
    .filter-card {
      margin: 0;
    }

    .filter-row {
      display: flex;
      gap: 12px;
      align-items: center;
      flex-wrap: wrap;
    }

    .filter-field {
      flex: 1 1 260px;
      margin-bottom: -1.25em;
    }

    .filter-status {
      flex: 0 0 160px;
    }

    /* ── Table card ─────────────────────────────────────────────────── */
    .table-card {
      margin: 0;
    }

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

  filterExamId = '';
  filterStatus = '';
  activeFilters: Record<string, any> = {};
  approvingId: string | null = null;
  lastResult: (PaperGenerationResponse & { status: string }) | null = null;

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
    return this.paperService.getPapers(
      req.page,
      req.size,
      this.activeFilters['examId'],
      this.activeFilters['status']
    );
  };

  constructor(
    private paperService: PaperService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {}

  // ── Filters ──────────────────────────────────────────────────────────────

  applyFilter(): void {
    const filters: Record<string, any> = {};
    if (this.filterExamId.trim()) filters['examId'] = this.filterExamId.trim();
    if (this.filterStatus) filters['status'] = this.filterStatus;
    this.activeFilters = filters;
  }

  clearFilters(): void {
    this.filterExamId = '';
    this.filterStatus = '';
    this.activeFilters = {};
  }

  // ── Generate ─────────────────────────────────────────────────────────────

  openGenerateDialog(): void {
    const dialogRef = this.dialog.open(PaperGenerateDialogComponent, {
      width: '780px',
      maxWidth: '95vw',
      disableClose: false,
      data: {
        examId: this.filterExamId || undefined
      } as PaperGenerateDialogData
    });

    dialogRef.afterClosed().subscribe((request: PaperGenerationRequest | undefined) => {
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
