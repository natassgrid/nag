import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatTooltipModule } from '@angular/material/tooltip';
import { map, Observable, of } from 'rxjs';

import { ExamManagementService, ExaminationResponse } from '../exam-manage/exam-management.service';
import { PaginatedTableComponent } from '../../../shared/components/paginated-table/paginated-table.component';
import { ColumnDef, PaginatedDataFetcher, PaginatedResponse } from '../../../shared/components/paginated-table/pagination.model';

@Component({
  selector: 'app-schedule-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatTooltipModule,
    PaginatedTableComponent
  ],
  template: `
    <div class="page-container">
      <div class="page-header">
        <div>
          <h1>Examination Scheduling</h1>
          <p class="subtitle">Select an examination to configure schedule versions, shifts, and centre seat allocations.</p>
        </div>
      </div>

      <app-paginated-table
        #table
        [fetcher]="fetcher"
        [columns]="columns"
        [actionsTemplate]="actionsTmpl"
        title="Examinations"
        searchPlaceholder="Search examination name..."
        (rowClick)="onRowClick($event)"
      >
      </app-paginated-table>

      <ng-template #actionsTmpl let-row>
        <button
          mat-flat-button
          color="primary"
          (click)="viewSchedules(row, $event)"
          matTooltip="View and manage schedules for this exam"
        >
          <mat-icon>event</mat-icon>
          View Schedules
        </button>
      </ng-template>
    </div>
  `,
  styles: [`
    .page-container {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    .page-header h1 {
      margin: 0 0 4px 0;
      font-size: 24px;
      font-weight: 600;
    }
    .subtitle {
      margin: 0;
      color: #666;
      font-size: 14px;
    }
  `]
})
export class ScheduleListComponent implements OnInit {
  @ViewChild('table') table!: PaginatedTableComponent<ExaminationResponse>;

  columns: ColumnDef<ExaminationResponse>[] = [
    { key: 'name', header: 'Exam Name', sortable: true },
    { key: 'durationMinutes', header: 'Duration (min)', sortable: true, cell: row => `${row.durationMinutes} mins` },
    { key: 'totalMarks', header: 'Total Marks', sortable: true },
    { key: 'status', header: 'Status', type: 'chip', chipClass: val => (val === 'PUBLISHED' ? 'status-published' : 'status-draft') },
    { key: 'actions', header: 'Actions', type: 'actions' }
  ];

  fetcher: PaginatedDataFetcher<ExaminationResponse> = (params) => {
    return this.examService.getExams().pipe(
      map(exams => {
        let filtered = exams || [];
        if (params.search) {
          const q = params.search.toLowerCase();
          filtered = filtered.filter(e => e.name.toLowerCase().includes(q));
        }

        const total = filtered.length;
        const start = params.page * params.size;
        const pageData = filtered.slice(start, start + params.size);

        return {
          content: pageData,
          totalElements: total,
          totalPages: Math.ceil(total / params.size) || 1,
          size: params.size,
          number: params.page
        } as PaginatedResponse<ExaminationResponse>;
      })
    );
  };

  constructor(
    private examService: ExamManagementService,
    private router: Router
  ) {}

  ngOnInit(): void {}

  onRowClick(exam: ExaminationResponse): void {
    this.router.navigate(['/exam/scheduling', exam.id]);
  }

  viewSchedules(exam: ExaminationResponse, event: MouseEvent): void {
    event.stopPropagation();
    this.router.navigate(['/exam/scheduling', exam.id]);
  }
}
