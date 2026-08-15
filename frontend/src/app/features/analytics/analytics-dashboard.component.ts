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
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { AnalyticsService, ExamAnalytics } from './analytics.service';

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatProgressBarModule
  ],
  template: `
    <div class="analytics-container">
      <h1 class="page-title">Exam Analytics Dashboard</h1>

      <!-- Exam ID Input -->
      <mat-card class="search-card">
        <mat-card-content>
          <form (ngSubmit)="loadAnalytics()" class="search-form">
            <mat-form-field appearance="outline" class="exam-input">
              <mat-label>Exam ID</mat-label>
              <input matInput [(ngModel)]="examId" name="examId"
                     placeholder="Enter exam ID" required>
            </mat-form-field>
            <button mat-raised-button color="primary" type="submit" [disabled]="!examId.trim() || isLoading">
              <mat-icon>search</mat-icon>
              Load Analytics
            </button>
          </form>
        </mat-card-content>
      </mat-card>

      <!-- Loading -->
      <div *ngIf="isLoading" class="loading-container" role="status" aria-live="polite">
        <mat-spinner diameter="48"></mat-spinner>
        <p>Loading analytics...</p>
      </div>

      <!-- Error -->
      <div *ngIf="errorMessage && !isLoading" class="error-message" role="alert">
        <mat-icon>error_outline</mat-icon>
        <span>{{ errorMessage }}</span>
      </div>

      <!-- Analytics Data -->
      <ng-container *ngIf="!isLoading && !errorMessage && analytics">
        <!-- Summary Cards -->
        <div class="summary-cards">
          <mat-card class="summary-card">
            <mat-card-content>
              <div class="summary-value">{{ analytics.totalRegistered }}</div>
              <div class="summary-label">Total Registered</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="summary-card">
            <mat-card-content>
              <div class="summary-value">{{ analytics.totalAppeared }}</div>
              <div class="summary-label">Total Appeared</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="summary-card">
            <mat-card-content>
              <div class="summary-value">{{ getAttendancePercent() | number:'1.1-1' }}%</div>
              <div class="summary-label">Attendance</div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Score Distribution -->
        <mat-card class="distribution-card">
          <mat-card-header>
            <mat-card-title>Score Distribution</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="bar-chart">
              <div *ngFor="let entry of analytics.scoreDistribution" class="bar-row">
                <span class="bar-label">{{ entry.range }}</span>
                <mat-progress-bar mode="determinate"
                                  [value]="getBarPercent(entry.count)"
                                  class="bar-progress">
                </mat-progress-bar>
                <span class="bar-count">{{ entry.count }}</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Section Averages -->
        <mat-card class="section-card">
          <mat-card-header>
            <mat-card-title>Section Averages</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="analytics.sectionAverages" class="section-table"
                   aria-label="Section average scores">
              <ng-container matColumnDef="sectionName">
                <th mat-header-cell *matHeaderCellDef>Section</th>
                <td mat-cell *matCellDef="let row">{{ row.sectionName }}</td>
              </ng-container>
              <ng-container matColumnDef="average">
                <th mat-header-cell *matHeaderCellDef>Average Score</th>
                <td mat-cell *matCellDef="let row">{{ row.average | number:'1.2-2' }}</td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="['sectionName', 'average']"></tr>
              <tr mat-row *matRowDef="let row; columns: ['sectionName', 'average'];"></tr>
            </table>
          </mat-card-content>
        </mat-card>

        <!-- Percentile Thresholds -->
        <mat-card class="threshold-card">
          <mat-card-header>
            <mat-card-title>Percentile Thresholds</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="thresholds">
              <div class="threshold-item">
                <mat-icon class="top-icon">emoji_events</mat-icon>
                <div>
                  <span class="threshold-label">Top 10% Threshold</span>
                  <span class="threshold-value">{{ analytics.top10PercentileThreshold | number:'1.2-2' }}</span>
                </div>
              </div>
              <div class="threshold-item">
                <mat-icon class="bottom-icon">trending_down</mat-icon>
                <div>
                  <span class="threshold-label">Bottom 10% Threshold</span>
                  <span class="threshold-value">{{ analytics.bottom10PercentileThreshold | number:'1.2-2' }}</span>
                </div>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Export CSV -->
        <div class="export-section">
          <button mat-raised-button color="accent" (click)="exportCsv()">
            <mat-icon>download</mat-icon>
            Export CSV
          </button>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .analytics-container {
      padding: 24px;
      max-width: 1000px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .page-title {
      font-size: 1.5rem;
      font-weight: 500;
      margin-bottom: 8px;
    }
    .search-form {
      display: flex;
      align-items: center;
      gap: 16px;
      flex-wrap: wrap;
    }
    .exam-input {
      flex: 1;
      min-width: 200px;
    }
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      gap: 16px;
    }
    .error-message {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 16px;
      color: #d32f2f;
      background: #fdecea;
      border-radius: 8px;
    }
    .summary-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
    }
    .summary-card {
      text-align: center;
    }
    .summary-value {
      font-size: 2rem;
      font-weight: 700;
      color: #1565c0;
    }
    .summary-label {
      font-size: 0.85rem;
      color: #666;
      text-transform: uppercase;
      margin-top: 4px;
    }
    .bar-chart {
      display: flex;
      flex-direction: column;
      gap: 12px;
      margin-top: 8px;
    }
    .bar-row {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .bar-label {
      width: 100px;
      font-size: 0.85rem;
      text-align: right;
      flex-shrink: 0;
    }
    .bar-progress {
      flex: 1;
    }
    .bar-count {
      width: 40px;
      font-size: 0.85rem;
      font-weight: 500;
    }
    .section-table {
      width: 100%;
      margin-top: 8px;
    }
    .thresholds {
      display: flex;
      gap: 32px;
      margin-top: 8px;
      flex-wrap: wrap;
    }
    .threshold-item {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .threshold-item div {
      display: flex;
      flex-direction: column;
    }
    .threshold-label {
      font-size: 0.8rem;
      color: #666;
    }
    .threshold-value {
      font-size: 1.25rem;
      font-weight: 600;
    }
    .top-icon { color: #f9a825; }
    .bottom-icon { color: #d32f2f; }
    .export-section {
      display: flex;
      justify-content: flex-end;
    }
  `]
})
export class AnalyticsDashboardComponent {
  examId = '';
  analytics: ExamAnalytics | null = null;
  isLoading = false;
  errorMessage = '';
  private maxCount = 1;

  constructor(private analyticsService: AnalyticsService) {}

  loadAnalytics(): void {
    if (!this.examId.trim()) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.analytics = null;

    this.analyticsService.getExamAnalytics(this.examId.trim()).subscribe({
      next: (data) => {
        this.analytics = data;
        this.maxCount = Math.max(
          ...data.scoreDistribution.map(e => e.count),
          1
        );
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load analytics data.';
      }
    });
  }

  getAttendancePercent(): number {
    if (!this.analytics || this.analytics.totalRegistered === 0) return 0;
    return (this.analytics.totalAppeared / this.analytics.totalRegistered) * 100;
  }

  getBarPercent(count: number): number {
    return (count / this.maxCount) * 100;
  }

  exportCsv(): void {
    if (!this.analytics) return;

    const lines: string[] = [];
    lines.push('Exam Analytics Report');
    lines.push(`Exam ID,${this.analytics.examId}`);
    lines.push(`Total Registered,${this.analytics.totalRegistered}`);
    lines.push(`Total Appeared,${this.analytics.totalAppeared}`);
    lines.push(`Attendance %,${this.getAttendancePercent().toFixed(1)}`);
    lines.push(`Top 10% Threshold,${this.analytics.top10PercentileThreshold}`);
    lines.push(`Bottom 10% Threshold,${this.analytics.bottom10PercentileThreshold}`);
    lines.push('');
    lines.push('Score Distribution');
    lines.push('Range,Count');
    for (const entry of this.analytics.scoreDistribution) {
      lines.push(`${entry.range},${entry.count}`);
    }
    lines.push('');
    lines.push('Section Averages');
    lines.push('Section,Average');
    for (const section of this.analytics.sectionAverages) {
      lines.push(`${section.sectionName},${section.average}`);
    }

    const blob = new Blob([lines.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `exam-analytics-${this.analytics.examId}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
