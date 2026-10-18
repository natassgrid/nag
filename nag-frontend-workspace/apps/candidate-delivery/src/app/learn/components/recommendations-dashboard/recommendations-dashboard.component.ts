import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { catchError, of, combineLatest, map, switchMap, forkJoin, BehaviorSubject } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';

import { RecommendationService } from '../../services';
import { LearningPathService } from '../../services/learning-path.service';
import { AdaptiveLearningService } from '../../services/adaptive-learning.service';
import { RecommendationCardComponent } from '../recommendation-card/recommendation-card.component';
import { LearnerProfileInsightsComponent } from '../learner-profile-insights/learner-profile-insights.component';
import { ReadinessScoreMeterComponent } from '../readiness-score-meter/readiness-score-meter.component';
import { NextActivityHeroComponent } from '../next-activity-hero/next-activity-hero.component';
import { SpacedReviewsPanelComponent } from '../spaced-reviews-panel/spaced-reviews-panel.component';
import { JourneyStepperBannerComponent } from '../journey-stepper-banner/journey-stepper-banner.component';
import { DynamicLearningPath, LearningPathSummary } from '../../models/learning-path.model';
import { NextActivity, SubtopicMastery } from '../../models/learner.model';

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
    ReadinessScoreMeterComponent,
    NextActivityHeroComponent,
    SpacedReviewsPanelComponent,
    JourneyStepperBannerComponent,
  ],
  templateUrl: './recommendations-dashboard.component.html',
  styleUrl: './recommendations-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecommendationsDashboardComponent {
  private readonly recommendationService = inject(RecommendationService);
  private readonly learningPathService = inject(LearningPathService);
  private readonly learningService = inject(AdaptiveLearningService);
  private readonly router = inject(Router);

  readonly isGenerating = signal<boolean>(false);
  readonly completingActivity = signal<boolean>(false);
  readonly selectedExam = signal<string>('SSC_CGL');
  readonly generateError = signal<string | null>(null);

  readonly activeEnrollment = this.learningService.activeEnrollment;
  readonly readinessScore = this.learningService.readinessScore;
  readonly nextActivity = this.learningService.nextActivity;
  readonly progress = this.learningService.progress;

  readonly dueReviews = computed<SubtopicMastery[]>(() => {
    return this.progress()?.dueReviews || [];
  });

  private readonly refresh$ = new BehaviorSubject<void>(undefined);

  private readonly dashboardData$ = this.refresh$.pipe(
    switchMap(() =>
      combineLatest([
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
      )
    )
  );

  readonly dashboardData = toSignal(this.dashboardData$, {
    initialValue: {
      recommendation: null,
      profile: null,
      learningPath: null,
      summary: null,
      loading: true,
    },
  });

  readonly isColdStart = computed<boolean>(() => {
    const data = this.dashboardData();
    const phase = data.learningPath?.milestoneGraph?.currentPhase;
    const isDiag = !data.learningPath || phase === 'DIAGNOSTIC_REQUIRED' || phase === 0;
    const totalSessions = data.profile?.totalPracticeSessions ?? 0;
    return isDiag && totalSessions === 0;
  });

  readonly activeJourneyStep = computed<number>(() => {
    if (this.isColdStart()) return 1;
    const data = this.dashboardData();
    const phase = data.learningPath?.milestoneGraph?.currentPhase;
    if (phase === 'PHASE_1_REMEDIATION' || phase === 1) return 3;
    if (phase === 'PHASE_2_ADAPTIVE_QUIZ' || phase === 2 || phase === 'PHASE_3_GATEKEEPER' || phase === 3 || phase === 'COMPLETED') {
      return 4;
    }
    return 2;
  });

  onStartNextActivity(act: NextActivity): void {
    if (act.type === 'LESSON') {
      this.router.navigate(['/learn/lesson', act.subtopicId]);
    } else if (act.type === 'PRACTICE') {
      this.router.navigate(['/practice']);
    } else if (act.type === 'REVISION') {
      this.router.navigate(['/learn/lesson', act.subtopicId]);
    } else if (act.type === 'MOCK_SECTION') {
      this.router.navigate(['/delivery']);
    } else {
      this.router.navigate(['/learn/lesson', act.subtopicId]);
    }
  }

  onCompleteNextActivity(act: NextActivity): void {
    const enrollment = this.activeEnrollment();
    if (!enrollment) return;
    this.completingActivity.set(true);
    this.learningService
      .completeActivity(enrollment.id, act.activityId, { status: 'COMPLETED' })
      .subscribe({
        next: () => {
          this.completingActivity.set(false);
          this.learningService.getNextActivity(enrollment.id).subscribe();
          this.learningService.getProgress(enrollment.id).subscribe();
        },
        error: () => this.completingActivity.set(false),
      });
  }

  onReviewSubtopic(subtopic: SubtopicMastery): void {
    this.router.navigate(['/learn/lesson', subtopic.subtopicId]);
  }

  onRefreshRecommendations(): void {
    this.isGenerating.set(true);
    this.generateError.set(null);
    const target = this.selectedExam();
    forkJoin([
      this.learningPathService.recalculate(target).pipe(catchError(() => of(null))),
      this.recommendationService.generate(target).pipe(catchError(() => of(null))),
    ]).subscribe({
      next: () => {
        this.isGenerating.set(false);
        this.refresh$.next();
      },
      error: () => this.isGenerating.set(false),
    });
  }

  onStartDiagnosticTest(): void {
    this.router.navigate(['/learn/onboarding'], { queryParams: { step: 'diagnostic' } });
  }

  scrollToStudyPlan(): void {
    const el = document.getElementById('study-plan');
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
