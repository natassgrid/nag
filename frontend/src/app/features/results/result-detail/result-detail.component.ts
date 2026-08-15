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
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ResultService, ResultResponse } from '../result.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-result-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="result-detail-container">
      <div *ngIf="isLoading" class="loading-container" role="status" aria-live="polite">
        <mat-spinner diameter="48"></mat-spinner>
        <p>Loading result details...</p>
      </div>

      <div *ngIf="errorMessage && !isLoading" class="error-message" role="alert">
        <mat-icon>error_outline</mat-icon>
        <span>{{ errorMessage }}</span>
      </div>

      <ng-container *ngIf="!isLoading && !errorMessage && result">
        <!-- Back button -->
        <a mat-button routerLink="/results" class="back-btn">
          <mat-icon>arrow_back</mat-icon> Back to Results
        </a>

        <!-- Overall Score Card -->
        <mat-card class="score-card">
          <mat-card-header>
            <mat-card-title>Exam: {{ result.examId }}</mat-card-title>
            <mat-card-subtitle>{{ result.createdAt | date:'medium' }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="overall-score">
              <div class="score-circle">
                <span class="score-number">{{ result.totalScore }}</span>
                <span class="score-label">Total Score</span>
              </div>
              <div class="score-meta">
                <div class="meta-item">
                  <mat-icon>leaderboard</mat-icon>
                  <span>Rank: <strong>#{{ result.overallRank }}</strong></span>
                </div>
                <div class="meta-item">
                  <mat-icon>trending_up</mat-icon>
                  <span>Percentile: <strong>{{ result.overallPercentile | number:'1.1-1' }}%</strong></span>
                </div>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Section-wise Breakdown -->
        <mat-card class="breakdown-card">
          <mat-card-header>
            <mat-card-title>Section-wise Breakdown</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <table mat-table [dataSource]="result.sectionScores" class="section-table"
                   aria-label="Section-wise score breakdown">
              <ng-container matColumnDef="sectionName">
                <th mat-header-cell *matHeaderCellDef>Section</th>
                <td mat-cell *matCellDef="let row">{{ row.sectionName }}</td>
              </ng-container>

              <ng-container matColumnDef="score">
                <th mat-header-cell *matHeaderCellDef>Score</th>
                <td mat-cell *matCellDef="let row">{{ row.score }}</td>
              </ng-container>

              <ng-container matColumnDef="maxMarks">
                <th mat-header-cell *matHeaderCellDef>Max Marks</th>
                <td mat-cell *matCellDef="let row">{{ row.maxMarks }}</td>
              </ng-container>

              <ng-container matColumnDef="percentage">
                <th mat-header-cell *matHeaderCellDef>Percentage</th>
                <td mat-cell *matCellDef="let row">
                  {{ row.maxMarks > 0 ? (row.score / row.maxMarks * 100 | number:'1.1-1') : '0.0' }}%
                </td>
              </ng-container>

              <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
              <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
            </table>
          </mat-card-content>
        </mat-card>

        <!-- Download PDF -->
        <mat-card class="download-card" *ngIf="result.scorecardPdfRef">
          <mat-card-content class="download-content">
            <mat-icon class="pdf-icon">picture_as_pdf</mat-icon>
            <div>
              <p class="download-text">Download your detailed scorecard</p>
              <a mat-raised-button color="primary"
                 [href]="result.scorecardPdfRef"
                 target="_blank"
                 download
                 aria-label="Download scorecard PDF">
                <mat-icon>download</mat-icon>
                Download PDF
              </a>
            </div>
          </mat-card-content>
        </mat-card>
      </ng-container>
    </div>
  `,
  styles: [`
    .result-detail-container {
      padding: 24px;
      max-width: 900px;
      margin: 0 auto;
      display: flex;
      flex-direction: column;
      gap: 16px;
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
    .back-btn {
      align-self: flex-start;
    }
    .score-card .overall-score {
      display: flex;
      align-items: center;
      gap: 48px;
      margin-top: 16px;
      flex-wrap: wrap;
    }
    .score-circle {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 120px;
      height: 120px;
      border-radius: 50%;
      background: #e3f2fd;
      border: 3px solid #1565c0;
    }
    .score-number {
      font-size: 2rem;
      font-weight: 700;
      color: #1565c0;
    }
    .score-label {
      font-size: 0.7rem;
      color: #666;
      text-transform: uppercase;
    }
    .score-meta {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .meta-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 1rem;
    }
    .section-table {
      width: 100%;
      margin-top: 8px;
    }
    .download-card .download-content {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .pdf-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #d32f2f;
    }
    .download-text {
      margin: 0 0 8px 0;
      color: #666;
    }
  `]
})
export class ResultDetailComponent implements OnInit {
  result: ResultResponse | null = null;
  isLoading = true;
  errorMessage = '';
  displayedColumns = ['sectionName', 'score', 'maxMarks', 'percentage'];

  constructor(
    private route: ActivatedRoute,
    private resultService: ResultService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const examId = this.route.snapshot.paramMap.get('examId');
    const candidateId = this.authService.getUserId();

    if (!candidateId) {
      this.isLoading = false;
      this.errorMessage = 'Unable to identify candidate. Please log in again.';
      return;
    }

    if (!examId) {
      this.isLoading = false;
      this.errorMessage = 'No exam ID specified.';
      return;
    }

    this.resultService.getResult(candidateId, examId).subscribe({
      next: (data) => {
        this.result = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load result details.';
      }
    });
  }
}
