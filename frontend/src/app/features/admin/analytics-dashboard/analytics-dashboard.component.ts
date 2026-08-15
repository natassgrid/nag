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

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTableModule } from '@angular/material/table';
import { HttpClient } from '@angular/common/http';

interface ExamAnalytics {
  examId: string;
  examName: string;
  totalRegistered: number;
  totalAppeared: number;
  scoreDistribution: HistogramBin[];
  sectionAverages: SectionAverage[];
  top10Percentile: number;
  bottom10Percentile: number;
}

interface HistogramBin {
  rangeLabel: string;
  count: number;
  percentage: number;
}

interface SectionAverage {
  sectionName: string;
  averageScore: number;
  maxScore: number;
}

@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTableModule
  ],
  template: `
    <section class="analytics-container" role="main" aria-labelledby="analytics-heading">
      <h1 id="analytics-heading">Exam Analytics Dashboard</h1>

      <!-- Exam Selector -->
      <mat-form-field appearance="outline" class="exam-selector">
        <mat-label>Select Exam</mat-label>
        <mat-select (selectionChange)="loadAnalytics($event.value)" aria-label="Select exam for analytics">
          <mat-option *ngFor="let exam of examList" [value]="exam.id">{{ exam.name }}</mat-option>
        </mat-select>
      </mat-form-field>

      <div *ngIf="analytics" class="analytics-content">
        <!-- Summary Cards -->
        <div class="summary-cards">
          <mat-card class="summary-card">
            <mat-card-content>
              <div class="metric-value">{{ analytics.totalRegistered }}</div>
              <div class="metric-label">Registered</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="summary-card">
            <mat-card-content>
              <div class="metric-value">{{ analytics.totalAppeared }}</div>
              <div class="metric-label">Appeared</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="summary-card">
            <mat-card-content>
              <div class="metric-value">{{ analytics.top10Percentile | number:'1.1-1' }}</div>
              <div class="metric-label">Top 10th Percentile Score</div>
            </mat-card-content>
          </mat-card>
          <mat-card class="summary-card">
            <mat-card-content>
              <div class="metric-value">{{ analytics.bottom10Percentile | number:'1.1-1' }}</div>
              <div class="metric-label">Bottom 10th Percentile Score</div>
            </mat-card-content>
          </mat-card>
        </div>

        <!-- Score Distribution Histogram -->
        <mat-card class="histogram-card">
          <mat-card-header>
            <mat-card-title>Score Distribution</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div class="histogram" role="img" aria-label="Score distribution histogram">
              <div *ngFor="let bin of analytics.scoreDistribution" class="histogram-bar-container">
                <div class="histogram-bar"
                     [style.height.%]="getBarHeight(bin.count)"
                     [attr.aria-label]="bin.rangeLabel + ': ' + bin.count + ' candidates (' + bin.percentage + '%)'">
                </div>
                <div class="histogram-label">{{ bin.rangeLabel }}</div>
                <div class="histogram-count">{{ bin.count }}</div>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Section Averages -->
        <mat-card class="section-averages-card">
          <mat-card-header>
            <mat-card-title>Section-wise Averages</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="analytics.sectionAverages" aria-label="Section-wise average scores">
              <ng-container matColumnDef="sectionName">
                <th mat-header-cell *matHeaderCellDef>Section</th>
                <td mat-cell *matCellDef="let section">{{ section.sectionName }}</td>
              </ng-container>
              <ng-container matColumnDef="averageScore">
                <th mat-header-cell *matHeaderCellDef>Average Score</th>
                <td mat-cell *matCellDef="let section">{{ section.averageScore | number:'1.2-2' }}</td>
              </ng-container>
              <ng-container matColumnDef="maxScore">
                <th mat-header-cell *matHeaderCellDef>Max Score</th>
                <td mat-cell *matCellDef="let section">{{ section.maxScore }}</td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="['sectionName', 'averageScore', 'maxScore']"></tr>
              <tr mat-row *matRowDef="let row; columns: ['sectionName', 'averageScore', 'maxScore']"></tr>
            </table>
          </mat-card-content>
        </mat-card>
      </div>
    </section>
  `,
  styles: [`
    .analytics-container { padding: var(--spacing-lg); max-width: 1200px; margin: 0 auto; }
    .exam-selector { width: 300px; margin-bottom: var(--spacing-lg); }
    .summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: var(--spacing-md); margin-bottom: var(--spacing-lg); }
    .summary-card { text-align: center; }
    .metric-value { font-size: 2rem; font-weight: 500; color: var(--color-primary); }
    .metric-label { color: var(--color-text-secondary); margin-top: var(--spacing-xs); }
    .histogram { display: flex; align-items: flex-end; gap: var(--spacing-xs); height: 200px; padding-top: var(--spacing-md); }
    .histogram-bar-container { display: flex; flex-direction: column; align-items: center; flex: 1; }
    .histogram-bar { width: 100%; background: var(--color-primary); border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.3s; }
    .histogram-label { font-size: 0.75rem; margin-top: var(--spacing-xs); color: var(--color-text-secondary); }
    .histogram-count { font-size: 0.75rem; font-weight: 500; }
    table { width: 100%; }
  `]
})
export class AnalyticsDashboardComponent implements OnInit {
  examList: { id: string; name: string }[] = [];
  analytics: ExamAnalytics | null = null;
  private maxCount = 1;

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    this.http.get<{ id: string; name: string }[]>('/api/v1/admin/exams').subscribe({
      next: (exams) => this.examList = exams
    });
  }

  loadAnalytics(examId: string): void {
    this.http.get<ExamAnalytics>(`/api/v1/analytics/exams/${examId}`).subscribe({
      next: (data) => {
        this.analytics = data;
        this.maxCount = Math.max(...data.scoreDistribution.map(b => b.count), 1);
      }
    });
  }

  getBarHeight(count: number): number {
    return (count / this.maxCount) * 100;
  }
}
