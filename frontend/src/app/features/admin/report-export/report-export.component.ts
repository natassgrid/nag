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

import { Component } from '@angular/core';
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
  template: `
    <section class="export-container" role="main" aria-labelledby="export-heading">
      <h1 id="export-heading">Report Export</h1>

      <mat-card class="export-card">
        <mat-card-header>
          <mat-card-title>Export Exam Reports</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="export-form">
            <mat-form-field appearance="outline">
              <mat-label>Select Exam</mat-label>
              <mat-select [(value)]="selectedExamId" aria-label="Select exam for export">
                <mat-option *ngFor="let exam of examList" [value]="exam.id">{{ exam.name }}</mat-option>
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Report Type</mat-label>
              <mat-select [(value)]="reportType" aria-label="Select report type">
                <mat-option value="results">Result Summary</mat-option>
                <mat-option value="analytics">Analytics Report</mat-option>
                <mat-option value="attendance">Attendance Report</mat-option>
              </mat-select>
            </mat-form-field>
          </div>

          <div class="export-actions">
            <button mat-raised-button color="primary"
                    (click)="exportCSV()"
                    [disabled]="!selectedExamId || isExporting"
                    aria-label="Export report as CSV">
              <mat-icon>table_chart</mat-icon>
              Export CSV
            </button>

            <button mat-raised-button color="accent"
                    (click)="exportPDF()"
                    [disabled]="!selectedExamId || isExporting"
                    aria-label="Export report as PDF">
              <mat-icon>picture_as_pdf</mat-icon>
              Export PDF
            </button>
          </div>

          <mat-progress-spinner *ngIf="isExporting" mode="indeterminate" diameter="32"
                                aria-label="Export in progress">
          </mat-progress-spinner>

          <div *ngIf="exportMessage" role="status" aria-live="polite" class="export-message">
            {{ exportMessage }}
          </div>
        </mat-card-content>
      </mat-card>
    </section>
  `,
  styles: [`
    .export-container { padding: var(--spacing-lg); max-width: 800px; margin: 0 auto; }
    .export-form { display: flex; gap: var(--spacing-md); flex-wrap: wrap; margin-bottom: var(--spacing-md); }
    .export-form mat-form-field { flex: 1; min-width: 200px; }
    .export-actions { display: flex; gap: var(--spacing-md); margin-bottom: var(--spacing-md); }
    .export-message { margin-top: var(--spacing-md); color: var(--color-success); font-weight: 500; }
  `]
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
