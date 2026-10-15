import { Component, ChangeDetectionStrategy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { RecommendationService } from '../../services';
import { LearningPathService } from '../../services/learning-path.service';
import { RecommendationCardComponent } from '../recommendation-card/recommendation-card.component';
import { LearnerProfileInsightsComponent } from '../learner-profile-insights/learner-profile-insights.component';
import { DynamicLearningPath, LearningPathSummary } from '../../models/learning-path.model';
import { catchError, of, combineLatest, map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-recommendations-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatProgressSpinnerModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatProgressBarModule,
    RecommendationCardComponent,
    LearnerProfileInsightsComponent,
  ],
  templateUrl: './recommendations-dashboard.component.html',
  styleUrl: './recommendations-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecommendationsDashboardComponent {
  private readonly recommendationService = inject(RecommendationService);
  private readonly learningPathService = inject(LearningPathService);
  private readonly router = inject(Router);

  readonly isGenerating = signal<boolean>(false);
  readonly selectedExam = signal<string>('JEE_MAIN');
  readonly generateError = signal<string | null>(null);

  private readonly refreshTrigger$ = signal<number>(0);

  private readonly dashboardData$ = combineLatest([
    this.recommendationService.getLatest().pipe(catchError(() => of(null))),
    this.recommendationService.getLearnerProfile().pipe(catchError(() => of(null))),
    this.learningPathService.getLearningPath().pipe(catchError(() => of(null))),
    this.learningPathService.getSummary().pipe(catchError(() => of(null))),
  ]).pipe(
    map(([recommendation, profile, learningPath, summary]) => ({
      recommendation,
      profile,
      learningPath: learningPath as DynamicLearningPath | null,
      summary: summary as LearningPathSummary | null,
      loading: false,
    }))
  );

  dashboardData = toSignal(this.dashboardData$, {
    initialValue: {
      recommendation: null,
      profile: null,
      learningPath: null,
      summary: null,
      loading: true,
    },
  });

  onGenerateLearningPath(): void {
    this.isGenerating.set(true);
    this.generateError.set(null);
    this.learningPathService.generateLearningPath(this.selectedExam()).subscribe({
      next: () => {
        this.isGenerating.set(false);
        this.router.navigate(['/learn/roadmap']);
      },
      error: (err) => {
        this.isGenerating.set(false);
        this.generateError.set(
          err?.error?.message ||
            'Unable to initialize Dynamic Learning Path. Please try taking a practice test first.'
        );
      },
    });
  }

  getSubjectSummaryList(summary: LearningPathSummary | null) {
    if (!summary?.subjectSummaries) return [];
    return Object.values(summary.subjectSummaries);
  }
}
