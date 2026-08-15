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
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

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
    PageHeaderComponent,
  ],
  templateUrl: './schedule-list.component.html',
  styleUrls: ['./schedule-list.component.scss']
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
