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
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, OnInit, ViewChild, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
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
  PaperSummary,
  PaperGenerationResponse,
  PaperDetail
} from './paper.service';
import { PaperGenerateDialogComponent } from './paper-generate-dialog.component';
import { PaperSummaryDrawerComponent } from './paper-summary-drawer.component';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher,
  FilterCategory
} from '../../shared/components/paginated-table';
import { ColumnDef } from '../../shared/components/paginated-table/pagination.model';
import { PageHeaderComponent } from '../../shared/components/page-header/page-header.component';
import { ExamManagementService } from '../exam/exam-manage/exam-management.service';

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
    PaperGenerateDialogComponent,
    PaperSummaryDrawerComponent
  ],
  templateUrl: './paper-list.component.html',
  changeDetection: ChangeDetectionStrategy.Default,
  styleUrls: ['./paper-list.component.scss']
})
export class PaperListComponent implements OnInit {

  @ViewChild('paperTable') paperTable?: PaginatedTableComponent<PaperSummary>;

  generateDrawerOpen = false;
  summaryDrawerOpen = false;
  selectedPaperId: string | null = null;
  activeFilters: Record<string, any> = {};
  approvingId: string | null = null;
  lastResult: (PaperGenerationResponse & { status: string }) | null = null;
  examMap = new Map<string, string>();

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
      key: 'name',
      header: 'Paper Name',
      sortable: true,
      cell: (row) => row.name || `Paper ${row.paperId.substring(0, 8)}…`
    },
    {
      key: 'examName',
      header: 'Examination',
      cell: (row) => row.examName || this.examMap.get(row.examId) || row.examId.substring(0, 8) + '…',
      sortable: true
    },
    {
      key: 'shiftName',
      header: 'Shift',
      cell: (row) => row.shiftName || row.shiftId || '—',
      sortable: true
    },
    {
      key: 'difficultyScore',
      header: 'Difficulty',
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
    private examService: ExamManagementService,
    private snackBar: MatSnackBar,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadExams();
  }

  loadExams(): void {
    this.examService.getExams(0, 100).pipe(catchError(() => of([]))).subscribe((exams) => {
      this.examMap.clear();
      (exams || []).forEach((e) => this.examMap.set(e.id, e.name));
      this.cdr.detectChanges();
    });
  }

  onFilterChange(filters: Record<string, any>): void {
    this.activeFilters = { ...filters };
    this.paperTable?.reload();
  }

  // ── Paper Summary Drawer ──────────────────────────────────────────────────

  viewPaperSummary(row: PaperSummary): void {
    this.selectedPaperId = row.paperId;
    this.summaryDrawerOpen = true;
    this.cdr.detectChanges();
  }

  openSummaryById(paperId: string): void {
    this.selectedPaperId = paperId;
    this.summaryDrawerOpen = true;
    this.cdr.detectChanges();
  }

  onSummaryDrawerClose(): void {
    this.summaryDrawerOpen = false;
    this.selectedPaperId = null;
    this.cdr.detectChanges();
  }

  onPaperApprovedFromDrawer(paper: PaperDetail): void {
    this.paperTable?.reload();
    this.cdr.detectChanges();
  }

  // ── Generate ──────────────────────────────────────────────────────────────

  openGenerateDrawer(): void {
    this.generateDrawerOpen = true;
    this.cdr.detectChanges();
  }

  onGenerateDrawerClose(): void {
    this.generateDrawerOpen = false;
    this.cdr.detectChanges();
  }

  onPaperGenerated(res: PaperGenerationResponse): void {
    this.generateDrawerOpen = false;
    this.lastResult = { ...res, status: res.status ?? 'DRAFT' };
    this.selectedPaperId = res.paperId;
    this.summaryDrawerOpen = true;
    this.paperTable?.reload();
    this.cdr.detectChanges();
  }

  // ── Approve & Encrypt ─────────────────────────────────────────────────────

  approvePaper(row: PaperSummary): void {
    this.approvingId = row.paperId;
    this.paperService
      .approvePaper(row.paperId)
      .pipe(
        catchError((err) => {
          this.snackBar.open('Approval failed: ' + (err.error?.detail || err.message), 'Close', {
            duration: 5000
          });
          return of(null);
        })
      )
      .subscribe((res) => {
        this.approvingId = null;
        if (res) {
          this.snackBar.open(
            `Paper approved and encrypted successfully. Key: ${res.encryptionKeyId ?? 'Generated'}`,
            'Close',
            { duration: 4000 }
          );
          this.paperTable?.reload();
        }
        this.cdr.detectChanges();
      });
  }

  navigateToBlueprints(): void {
    this.router.navigate(['/papers/blueprints']);
  }
}
