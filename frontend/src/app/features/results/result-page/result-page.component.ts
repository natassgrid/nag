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
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatDividerModule } from '@angular/material/divider';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationPanelComponent } from '../notification-panel/notification-panel.component';

interface ResultData {
  examName: string;
  candidateName: string;
  totalScore: number;
  maxScore: number;
  overallRank: number;
  overallPercentile: number;
  sections: SectionScore[];
  pdfDownloadUrl: string;
}

interface SectionScore {
  sectionName: string;
  score: number;
  maxScore: number;
  percentage: number;
  topics: TopicScore[];
}

interface TopicScore {
  topicName: string;
  score: number;
  maxScore: number;
}

@Component({
  selector: 'app-result-page',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatDividerModule,
    NotificationPanelComponent
  ],
  template: `
    <main id="main-content" class="result-container" role="main" aria-labelledby="result-heading">
      <div class="result-content">
        <!-- Scorecard Header -->
        <mat-card class="scorecard-header" *ngIf="result">
          <mat-card-header>
            <mat-card-title id="result-heading">{{ result.examName }} — Scorecard</mat-card-title>
            <mat-card-subtitle>{{ result.candidateName }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="score-summary" role="region" aria-label="Score summary">
              <div class="score-item">
                <span class="score-label">Total Score</span>
                <span class="score-value">{{ result.totalScore }} / {{ result.maxScore }}</span>
              </div>
              <div class="score-item">
                <span class="score-label">Rank</span>
                <span class="score-value">{{ result.overallRank }}</span>
              </div>
              <div class="score-item">
                <span class="score-label">Percentile</span>
                <span class="score-value">{{ result.overallPercentile | number:'1.2-2' }}%</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Section-wise Breakdown -->
        <mat-card class="section-breakdown" *ngIf="result">
          <mat-card-header>
            <mat-card-title>Subject/Section Breakdown</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <div *ngFor="let section of result.sections" class="section-item">
              <h3 class="section-name">{{ section.sectionName }}</h3>
              <div class="section-score">
                {{ section.score }} / {{ section.maxScore }}
                ({{ section.percentage | number:'1.1-1' }}%)
              </div>
              <mat-divider></mat-divider>

              <!-- Topic breakdown -->
              <table mat-table [dataSource]="section.topics" class="topic-table"
                     *ngIf="section.topics.length > 0"
                     aria-label="Topic-wise score breakdown">
                <ng-container matColumnDef="topicName">
                  <th mat-header-cell *matHeaderCellDef>Topic</th>
                  <td mat-cell *matCellDef="let topic">{{ topic.topicName }}</td>
                </ng-container>
                <ng-container matColumnDef="score">
                  <th mat-header-cell *matHeaderCellDef>Score</th>
                  <td mat-cell *matCellDef="let topic">{{ topic.score }} / {{ topic.maxScore }}</td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="['topicName', 'score']"></tr>
                <tr mat-row *matRowDef="let row; columns: ['topicName', 'score']"></tr>
              </table>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- PDF Download -->
        <mat-card class="download-section" *ngIf="result">
          <mat-card-content>
            <p>Download your detailed scorecard (PDF is password-protected with your DOB + Candidate ID).</p>
            <a mat-raised-button color="primary"
               [href]="result.pdfDownloadUrl"
               download
               aria-label="Download scorecard PDF">
              <mat-icon>download</mat-icon>
              Download PDF Scorecard
            </a>
          </mat-card-content>
        </mat-card>

        <!-- Loading / Error states -->
        <div *ngIf="isLoading" class="loading" role="status" aria-live="polite">
          Loading your results...
        </div>
        <div *ngIf="errorMessage" class="error-message" role="alert">
          {{ errorMessage }}
        </div>
      </div>

      <!-- Notification Panel -->
      <app-notification-panel></app-notification-panel>
    </main>
  `,
  styles: [`
    .result-container {
      display: flex;
      gap: var(--spacing-lg);
      padding: var(--spacing-lg);
      min-height: 100vh;
      flex-wrap: wrap;
    }
    .result-content { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: var(--spacing-md); }
    .score-summary { display: flex; gap: var(--spacing-xl); flex-wrap: wrap; margin-top: var(--spacing-md); }
    .score-item { display: flex; flex-direction: column; }
    .score-label { font-size: 0.875rem; color: var(--color-text-secondary); }
    .score-value { font-size: 1.5rem; font-weight: 500; }
    .section-item { margin-bottom: var(--spacing-md); }
    .section-name { margin: var(--spacing-sm) 0; }
    .section-score { color: var(--color-text-secondary); margin-bottom: var(--spacing-sm); }
    .topic-table { width: 100%; margin-top: var(--spacing-sm); }
    .loading { text-align: center; padding: var(--spacing-xl); }
    .error-message { color: var(--color-error); padding: var(--spacing-md); }
    .download-section { margin-top: var(--spacing-md); }
    @media (max-width: 320px) {
      .result-container { padding: var(--spacing-sm); }
      .score-summary { flex-direction: column; gap: var(--spacing-sm); }
    }
  `]
})
export class ResultPageComponent implements OnInit {
  result: ResultData | null = null;
  isLoading = true;
  errorMessage = '';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const candidateId = this.authService.getUserId();
    if (!candidateId) {
      this.isLoading = false;
      this.errorMessage = 'Unable to identify candidate.';
      return;
    }

    this.http.get<ResultData>(`/api/v1/results/${candidateId}`).subscribe({
      next: (data) => {
        this.result = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load results.';
      }
    });
  }
}
