import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { map } from 'rxjs/operators';
import { ExamManagementService, ExaminationResponse, CreateExamRequest } from './exam-management.service';
import { ExamFormDialogComponent, ExamFormDialogData } from './exam-form-dialog.component';
import {
  PaginatedTableComponent,
  ColumnDef,
  PaginatedDataFetcher
} from '../../../shared/components/paginated-table';

@Component({
  selector: 'app-exam-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule,
    PaginatedTableComponent
  ],
  template: `
    <div class="exam-list-container">
      <mat-card class="exam-list-card">
        <mat-card-header>
          <mat-card-title>Exam Management</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <app-paginated-table
            #paginatedTable
            [fetcher]="fetcher"
            [columns]="columns"
            [actionsTemplate]="actionsTmpl"
            searchPlaceholder="Search exams by name, status, or policy..."
          ></app-paginated-table>

          <!-- Custom Actions Column Template -->
          <ng-template #actionsTmpl let-row>
            <button mat-icon-button matTooltip="Edit" (click)="openEditDialog(row)">
              <mat-icon>edit</mat-icon>
            </button>
            <button
              mat-icon-button
              matTooltip="Schedule"
              [routerLink]="['/exam/scheduling', row.id]"
            >
              <mat-icon>event</mat-icon>
            </button>
            <button
              mat-icon-button
              matTooltip="Publish"
              *ngIf="row.status === 'DRAFT'"
              (click)="publishExam(row)"
            >
              <mat-icon>publish</mat-icon>
            </button>
          </ng-template>

        </mat-card-content>
      </mat-card>

      <button
        mat-fab
        color="primary"
        class="fab-create"
        matTooltip="Create Exam"
        (click)="openCreateDialog()"
      >
        <mat-icon>add</mat-icon>
      </button>
    </div>
  `,
  styles: [`
    .exam-list-container {
      padding: 24px;
      position: relative;
    }
    .exam-list-card {
      margin: 0;
    }
    .fab-create {
      position: fixed;
      bottom: 32px;
      right: 32px;
    }
    ::ng-deep .status-draft {
      background-color: #fff3e0 !important;
      color: #e65100 !important;
    }
    ::ng-deep .status-published {
      background-color: #e8f5e9 !important;
      color: #2e7d32 !important;
    }
  `]
})
export class ExamListComponent {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<ExaminationResponse>;

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
    return this.examService.getExams().pipe(
      map(exams => {
        let filtered = exams;
        if (req.search) {
          const query = req.search.toLowerCase();
          filtered = exams.filter(e =>
            e.name.toLowerCase().includes(query) ||
            e.status.toLowerCase().includes(query) ||
            (e.navigationPolicy && e.navigationPolicy.toLowerCase().includes(query))
          );
        }
        const start = req.page * req.size;
        const paged = filtered.slice(start, start + req.size);
        return {
          content: paged,
          totalElements: filtered.length,
          totalPages: Math.ceil(filtered.length / req.size),
          size: req.size,
          number: req.page
        };
      })
    );
  };

  constructor(
    private examService: ExamManagementService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  reload(): void {
    this.paginatedTable?.reload();
  }

  openCreateDialog(): void {
    const dialogRef = this.dialog.open(ExamFormDialogComponent, {
      width: '600px',
      data: {} as ExamFormDialogData
    });

    dialogRef.afterClosed().subscribe((result: CreateExamRequest | undefined) => {
      if (result) {
        this.examService.createExam(result).subscribe({
          next: () => {
            this.snackBar.open('Examination created successfully', 'OK', { duration: 3000 });
            this.reload();
          },
          error: (err) => {
            this.snackBar.open('Failed to create examination', 'Dismiss', { duration: 3000 });
            console.error('Error creating exam:', err);
          }
        });
      }
    });
  }

  openEditDialog(exam: ExaminationResponse): void {
    const dialogRef = this.dialog.open(ExamFormDialogComponent, {
      width: '600px',
      data: { exam } as ExamFormDialogData
    });

    dialogRef.afterClosed().subscribe((result: CreateExamRequest | undefined) => {
      if (result) {
        this.examService.updateExam(exam.id, result).subscribe({
          next: () => {
            this.snackBar.open('Examination updated successfully', 'OK', { duration: 3000 });
            this.reload();
          },
          error: (err) => {
            this.snackBar.open('Failed to update examination', 'Dismiss', { duration: 3000 });
            console.error('Error updating exam:', err);
          }
        });
      }
    });
  }

  publishExam(exam: ExaminationResponse): void {
    this.examService.publishExam(exam.id).subscribe({
      next: () => {
        this.snackBar.open('Examination published successfully', 'OK', { duration: 3000 });
        this.reload();
      },
      error: (err) => {
        this.snackBar.open('Failed to publish examination', 'Dismiss', { duration: 3000 });
        console.error('Error publishing exam:', err);
      }
    });
  }
}
