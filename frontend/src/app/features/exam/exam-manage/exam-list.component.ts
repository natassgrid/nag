import { Component, OnInit, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule, MatTableDataSource } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator } from '@angular/material/paginator';
import { MatSortModule, MatSort } from '@angular/material/sort';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ExamManagementService, ExaminationResponse, CreateExamRequest } from './exam-management.service';
import { ExamFormDialogComponent, ExamFormDialogData } from './exam-form-dialog.component';

@Component({
  selector: 'app-exam-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule,
    MatDialogModule,
    MatTooltipModule
  ],
  template: `
    <mat-card class="exam-list-card">
      <mat-card-header>
        <mat-card-title>Exam Management</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <div class="table-container">
          <table mat-table [dataSource]="dataSource" matSort class="full-width">

            <ng-container matColumnDef="name">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Name</th>
              <td mat-cell *matCellDef="let row">{{ row.name }}</td>
            </ng-container>

            <ng-container matColumnDef="durationMinutes">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Duration (mins)</th>
              <td mat-cell *matCellDef="let row">{{ row.durationMinutes }}</td>
            </ng-container>

            <ng-container matColumnDef="totalMarks">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Total Marks</th>
              <td mat-cell *matCellDef="let row">{{ row.totalMarks }}</td>
            </ng-container>

            <ng-container matColumnDef="negativeMarkingEnabled">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Neg Marking</th>
              <td mat-cell *matCellDef="let row">{{ row.negativeMarkingEnabled ? 'Yes (' + row.negativeMarkingValue + ')' : 'No' }}</td>
            </ng-container>

            <ng-container matColumnDef="navigationPolicy">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Nav Policy</th>
              <td mat-cell *matCellDef="let row">{{ row.navigationPolicy }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Status</th>
              <td mat-cell *matCellDef="let row">
                <span [class]="'status-badge status-' + row.status.toLowerCase()">{{ row.status }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="createdAt">
              <th mat-header-cell *matHeaderCellDef mat-sort-header>Created</th>
              <td mat-cell *matCellDef="let row">{{ row.createdAt | date:'short' }}</td>
            </ng-container>

            <ng-container matColumnDef="actions">
              <th mat-header-cell *matHeaderCellDef>Actions</th>
              <td mat-cell *matCellDef="let row">
                <button mat-icon-button matTooltip="Edit" (click)="openEditDialog(row)">
                  <mat-icon>edit</mat-icon>
                </button>
                <button mat-icon-button matTooltip="Publish" *ngIf="row.status === 'DRAFT'" (click)="publishExam(row)">
                  <mat-icon>publish</mat-icon>
                </button>
              </td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>

            <tr class="mat-row" *matNoDataRow>
              <td class="mat-cell no-data" [attr.colspan]="displayedColumns.length">
                No examinations found. Click the + button to create one.
              </td>
            </tr>
          </table>
        </div>
        <mat-paginator [pageSizeOptions]="[5, 10, 25]" showFirstLastButtons></mat-paginator>
      </mat-card-content>
    </mat-card>

    <button mat-fab color="primary" class="fab-create" matTooltip="Create Exam" (click)="openCreateDialog()">
      <mat-icon>add</mat-icon>
    </button>
  `,
  styles: [`
    .exam-list-card {
      margin: 24px;
    }
    .table-container {
      overflow-x: auto;
    }
    .full-width {
      width: 100%;
    }
    .fab-create {
      position: fixed;
      bottom: 32px;
      right: 32px;
    }
    .status-badge {
      padding: 4px 8px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
      text-transform: uppercase;
    }
    .status-draft {
      background-color: #fff3e0;
      color: #e65100;
    }
    .status-published {
      background-color: #e8f5e9;
      color: #2e7d32;
    }
    .no-data {
      text-align: center;
      padding: 24px;
      color: rgba(0, 0, 0, 0.54);
    }
  `]
})
export class ExamListComponent implements OnInit, AfterViewInit {
  displayedColumns: string[] = [
    'name', 'durationMinutes', 'totalMarks', 'negativeMarkingEnabled',
    'navigationPolicy', 'status', 'createdAt', 'actions'
  ];

  dataSource = new MatTableDataSource<ExaminationResponse>([]);

  @ViewChild(MatPaginator) paginator!: MatPaginator;
  @ViewChild(MatSort) sort!: MatSort;

  constructor(
    private examService: ExamManagementService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadExams();
  }

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  loadExams(): void {
    this.examService.getExams().subscribe({
      next: (exams) => {
        this.dataSource.data = exams;
      },
      error: (err) => {
        this.snackBar.open('Failed to load examinations', 'Dismiss', { duration: 3000 });
        console.error('Error loading exams:', err);
      }
    });
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
            this.loadExams();
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
            this.loadExams();
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
        this.loadExams();
      },
      error: (err) => {
        this.snackBar.open('Failed to publish examination', 'Dismiss', { duration: 3000 });
        console.error('Error publishing exam:', err);
      }
    });
  }
}
