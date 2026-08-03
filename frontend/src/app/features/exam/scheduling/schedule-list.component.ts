import { Component, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { map } from 'rxjs/operators';
import { ExamManagementService, ExaminationResponse } from '../exam-manage/exam-management.service';
import {
  PaginatedTableComponent,
  PaginatedDataFetcher
} from '../../../shared/components/paginated-table';
import { ColumnDef } from '../../../shared/components/paginated-table/pagination.model';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    PaginatedTableComponent,
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1 class="page-title">
            <mat-icon class="title-icon">event</mat-icon>
            Examination Scheduling
          </h1>
          <p class="page-subtitle">Select an examination to manage its schedules, shifts, and seat allocations.</p>
        </div>
      </div>

      <mat-card>
        <mat-card-content>
          <app-paginated-table
            #paginatedTable
            [fetcher]="fetcher"
            [columns]="columns"
            [actionsTemplate]="actionsTmpl"
            title="Examinations"
            searchPlaceholder="Search by name, code, or status..."
            (rowClick)="viewSchedules($event)"
          ></app-paginated-table>

          <ng-template #actionsTmpl let-row>
            <button mat-stroked-button color="primary"
                    (click)="viewSchedules(row); $event.stopPropagation()"
                    matTooltip="Manage schedules"
                    aria-label="View schedules">
              <mat-icon>calendar_month</mat-icon> Schedules
            </button>
          </ng-template>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; max-width: 1200px; margin: 0 auto; }
    .page-header { margin-bottom: 20px; }
    .page-title { margin: 0; font-size: 24px; font-weight: 600; display: flex; align-items: center; gap: 10px; }
    .title-icon { font-size: 28px; height: 28px; width: 28px; color: #1976d2; }
    .page-subtitle { margin: 4px 0 0; font-size: 14px; color: #757575; }
    ::ng-deep .status-draft { background-color: #fff3e0 !important; color: #e65100 !important; }
    ::ng-deep .status-published { background-color: #e8f5e9 !important; color: #1b5e20 !important; }
  `]
})
export class ScheduleListComponent {

  @ViewChild('paginatedTable') paginatedTable!: PaginatedTableComponent<ExaminationResponse>;

  columns: ColumnDef<ExaminationResponse>[] = [
    { key: 'name', header: 'Name', sortable: true },
    { key: 'code', header: 'Code', cell: (row) => row.code || '—' },
    { key: 'examinationMode', header: 'Mode', cell: (row) => row.examinationMode || '—' },
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
        // Backend now returns paginated content; wrap if needed
        return {
          content: exams,
          totalElements: exams.length, // will be overridden when backend returns Page
          totalPages: 1,
          size: req.size,
          number: req.page
        };
      })
    );
  };

  constructor(
    private examService: ExamManagementService,
    private router: Router
  ) {}

  viewSchedules(exam: ExaminationResponse): void {
    this.router.navigate(['/exam/scheduling', exam.id]);
  }
}
