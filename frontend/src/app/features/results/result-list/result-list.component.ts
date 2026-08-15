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
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ResultService, ResultResponse } from '../result.service';
import { AuthService } from '../../../core/services/auth.service';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';

@Component({
  selector: 'app-result-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent
  ],
  template: `
    <div class="page-layout">
      <app-page-header
        title="My Exam Results"
        subtitle="View your scores, ranks, and percentiles for completed examinations."
        icon="grade"
      ></app-page-header>

      <div *ngIf="results.length === 0" class="empty-state" role="status">
        <mat-icon class="empty-icon">assignment</mat-icon>
        <p>No results available yet.</p>
      </div>

      <div class="results-grid" *ngIf="results.length > 0">
        <mat-card *ngFor="let result of results" class="result-card">
          <mat-card-header>
            <mat-icon mat-card-avatar>school</mat-icon>
            <mat-card-title>Exam: {{ result.examId }}</mat-card-title>
            <mat-card-subtitle>{{ result.createdAt | date:'mediumDate' }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <div class="result-stats">
              <div class="stat">
                <span class="stat-label">Total Score</span>
                <span class="stat-value">{{ result.totalScore }}</span>
              </div>
              <div class="stat">
                <span class="stat-label">Rank</span>
                <span class="stat-value">#{{ result.overallRank }}</span>
              </div>
              <div class="stat">
                <span class="stat-label">Percentile</span>
                <span class="stat-value">{{ result.overallPercentile | number:'1.1-1' }}%</span>
              </div>
            </div>
          </mat-card-content>
          <mat-card-actions align="end">
            <a mat-button color="primary" [routerLink]="[result.examId]">
              <mat-icon>visibility</mat-icon>
              View Details
            </a>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .empty-state {
      text-align: center;
      padding: 48px;
      color: #666;
    }
    .empty-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #bbb;
    }
    .results-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 16px;
    }
    .result-card {
      transition: box-shadow 0.2s;
    }
    .result-card:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }
    .result-stats {
      display: flex;
      gap: 24px;
      margin-top: 16px;
      flex-wrap: wrap;
    }
    .stat {
      display: flex;
      flex-direction: column;
    }
    .stat-label {
      font-size: 0.75rem;
      color: #666;
      text-transform: uppercase;
    }
    .stat-value {
      font-size: 1.25rem;
      font-weight: 500;
    }
  `]
})
export class ResultListComponent implements OnInit {
  results: ResultResponse[] = [];

  constructor(
    private resultService: ResultService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const candidateId = this.authService.getUserId();
    if (!candidateId) return;

    this.resultService.getResults(candidateId).subscribe(data => {
      this.results = data || [];
    });
  }
}
