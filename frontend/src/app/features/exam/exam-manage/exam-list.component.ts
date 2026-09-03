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

import { Component, ViewChild, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { map } from 'rxjs/operators';
import { ExamManagementService, ExaminationResponse, CreateExamRequest } from './exam-management.service';
import { ExamFormDialogComponent } from './exam-form-dialog.component';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher
} from '../../../shared/components/paginated-table';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-exam-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    MatTooltipModule,
    PaginatedTableComponent,
    PageHeaderComponent,
    ExamFormDialogComponent
  ],
  templateUrl: './exam-list.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./exam-list.component.scss']
})
export class ExamListComponent {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<ExaminationResponse>;

  drawerOpen = false;
  editingExam?: ExaminationResponse;

  columns: ColumnDef<ExaminationResponse>[] = [
    { key: 'name', header: 'Name', sortable: true },
    { key: 'durationMinutes', header: 'Duration (mins)', sortable: true },
    { key: 'totalMarks', header: 'Total Marks', sortable: true },
    {
      key: 'negativeMarkingEnabled',
      header: 'Neg Marking',
      cell: (row) => row.negativeMarkingEnabled ? `Yes (${row.negativeMarkingValue})` : 'No'
    },
    { key: 'navigationPolicy', header: 'Nav Policy', sortable: true },
    {
      key: 'status',
      header: 'Status',
      type: 'chip',
      chipClass: (val) => 'status-' + (val || '').toLowerCase(),
      sortable: true
    },
    { key: 'createdAt', header: 'Created', type: 'date', sortable: true },
    { key: 'actions', header: 'Actions', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<ExaminationResponse> = (req) => {
    return this.examService.getExams(req.page, req.size, req.search).pipe(
      map(exams => {
        return {
          content: exams,
          totalElements: exams.length,
          totalPages: 1,
          size: req.size,
          number: req.page
        };
      })
    );
  };

  constructor(
    private examService: ExamManagementService,
    private snackBar: MatSnackBar
  ) {}

  reload(): void {
    this.paginatedTable?.reload();
  }

  openCreateDrawer(): void {
    this.editingExam = undefined;
    this.drawerOpen = true;
  }

  openEditDrawer(exam: ExaminationResponse): void {
    this.editingExam = exam;
    this.drawerOpen = true;
  }

  onDrawerClose(result: CreateExamRequest | null): void {
    if (!result) {
      this.drawerOpen = false;
      return;
    }

    if (this.editingExam) {
      this.examService.updateExam(this.editingExam.id, result).subscribe({
        next: () => {
          this.drawerOpen = false;
          this.snackBar.open('Examination updated successfully', 'OK', { duration: 3000 });
          this.reload();
        },
        error: (err) => {
          this.snackBar.open(err?.error?.message || 'Failed to update examination', 'Dismiss', { duration: 3000 });
          console.error('Error updating exam:', err);
        }
      });
    } else {
      this.examService.createExam(result).subscribe({
        next: () => {
          this.drawerOpen = false;
          this.snackBar.open('Examination created successfully', 'OK', { duration: 3000 });
          this.reload();
        },
        error: (err) => {
          this.snackBar.open(err?.error?.message || 'Failed to create examination', 'Dismiss', { duration: 3000 });
          console.error('Error creating exam:', err);
        }
      });
    }
  }

  publishExam(exam: ExaminationResponse): void {
    this.examService.publishExam(exam.id).subscribe({
      next: () => {
        this.snackBar.open('Examination published successfully', 'OK', { duration: 3000 });
        this.reload();
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to publish examination', 'Dismiss', { duration: 3000 });
        console.error('Error publishing exam:', err);
      }
    });
  }
}
