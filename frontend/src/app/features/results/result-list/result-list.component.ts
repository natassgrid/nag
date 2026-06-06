import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ResultService, ResultResponse } from '../result.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-result-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  template: `
    <div class="result-list-container">
      <h1 class="page-title">My Exam Results</h1>

      <div *ngIf="isLoading" class="loading-container" role="status" aria-live="polite">
        <mat-spinner diameter="48"></mat-spinner>
        <p>Loading results...</p>
      </div>

      <div *ngIf="errorMessage && !isLoading" class="error-message" role="alert">
        <mat-icon>error_outline</mat-icon>
        <span>{{ errorMessage }}</span>
      </div>

      <div *ngIf="!isLoading && !errorMessage && results.length === 0" class="empty-state" role="status">
        <mat-icon class="empty-icon">assignment</mat-icon>
        <p>No results available yet.</p>
      </div>

      <div class="results-grid" *ngIf="!isLoading && results.length > 0">
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
    .result-list-container {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
    .page-title {
      font-size: 1.5rem;
      font-weight: 500;
      margin-bottom: 24px;
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
  isLoading = true;
  errorMessage = '';

  constructor(
    private resultService: ResultService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const candidateId = this.authService.getUserId();
    if (!candidateId) {
      this.isLoading = false;
      this.errorMessage = 'Unable to identify candidate. Please log in again.';
      return;
    }

    this.resultService.getResults(candidateId).subscribe({
      next: (data) => {
        this.results = data || [];
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = err.error?.message || 'Failed to load results.';
      }
    });
  }
}
