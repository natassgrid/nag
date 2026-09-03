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

import { Component, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-report-export',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatFormFieldModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './report-export.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./report-export.component.scss']
})
export class ReportExportComponent {
  examList: { id: string; name: string }[] = [];
  selectedExamId = '';
  reportType = 'results';
  isExporting = false;
  exportMessage = '';

  constructor(private http: HttpClient) {
    this.http.get<{ id: string; name: string }[]>('/api/v1/admin/exams').subscribe({
      next: (exams) => this.examList = exams
    });
  }

  exportCSV(): void {
    this.doExport('csv');
  }

  exportPDF(): void {
    this.doExport('pdf');
  }

  private doExport(format: 'csv' | 'pdf'): void {
    this.isExporting = true;
    this.exportMessage = '';

    this.http.get(
      `/api/v1/analytics/exams/${this.selectedExamId}/export`,
      { params: { format, reportType: this.reportType }, responseType: 'blob' }
    ).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `report-${this.selectedExamId}-${this.reportType}.${format}`;
        anchor.click();
        window.URL.revokeObjectURL(url);
        this.isExporting = false;
        this.exportMessage = `${format.toUpperCase()} report downloaded successfully.`;
      },
      error: () => {
        this.isExporting = false;
        this.exportMessage = 'Export failed. Please try again.';
      }
    });
  }
}
