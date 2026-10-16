import { Component, ChangeDetectionStrategy, inject, signal, computed } from '@angular/core';
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
import { catchError, of, combineLatest, map, switchMap, forkJoin, BehaviorSubject } from 'rxjs';
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
  readonly selectedExamTarget = this.selectedExam;
  readonly generateError = signal<string | null>(null);

  readonly examTargets = [
    { value: 'JEE_MAIN', label: 'JEE Main' },
    { value: 'JEE_ADVANCED', label: 'JEE Advanced' },
    { value: 'NEET', label: 'NEET UG' },
    { value: 'UPSC_PRELIMS', label: 'UPSC CSE Prelims' },
    { value: 'IBPS_PO', label: 'IBPS PO / Banking' },
    { value: 'SSC_CGL', label: 'SSC CGL' },
    { value: 'GATE_CS', label: 'GATE CS / Engineering' },
    { value: 'CAT', label: 'CAT / Management' },
    { value: 'GENERAL_COMPETITIVE', label: 'General Competitive' },
  ];

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

  dashboardData = toSignal(this.dashboardData$, {
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
    const isDiag =
      !data.learningPath ||
      phase === 'DIAGNOSTIC_REQUIRED' ||
      phase === 0;
    const totalSessions =
      data.profile?.totalPracticeSessions ??
      (data.profile as any)?.totalSessionsCompleted ??
      0;
    return isDiag && totalSessions === 0;
  });

  readonly activeJourneyStep = computed<number>(() => {
    if (this.isColdStart()) {
      return 1;
    }
    const data = this.dashboardData();
    const phase = data.learningPath?.milestoneGraph?.currentPhase;
    if (phase === 'PHASE_1_REMEDIATION' || phase === 1) {
      return 3;
    }
    if (
      phase === 'PHASE_2_ADAPTIVE_QUIZ' ||
      phase === 2 ||
      phase === 'PHASE_3_GATEKEEPER' ||
      phase === 3 ||
      phase === 'COMPLETED' ||
      phase === 4
    ) {
      return 4;
    }
    return 2;
  });

  readonly hasNoRecommendations = computed<boolean>(() => {
    const rec = this.dashboardData().recommendation;
    if (!rec) return true;
    const weakCount = rec.weakTopicRecommendations?.length ?? 0;
    const planCount = rec.studyPlanItems?.length ?? 0;
    return (
      (rec.status === 'PENDING' || !rec.status || weakCount === 0) &&
      weakCount === 0 &&
      planCount === 0
    );
  });

  getTargetExam(lp: DynamicLearningPath | null): string {
    if (!lp) return 'Competitive Exam';
    return lp.targetExam || lp.examTarget || 'Competitive Exam';
  }

  isGatekeeperPassed(lp: DynamicLearningPath | null): boolean {
    if (!lp) return false;
    return !!(lp.gatekeeperPassed || lp.milestoneGraph?.gatekeeperPassed);
  }

  getPhaseLabel(phase: string | number | undefined): string {
    if (phase === 'PHASE_1_REMEDIATION' || phase === 1) return 'Phase 1: Concept Remediation';
    if (phase === 'PHASE_2_ADAPTIVE_QUIZ' || phase === 2) return 'Phase 2: Adaptive Quiz';
    if (phase === 'PHASE_3_GATEKEEPER' || phase === 3) return 'Phase 3: Gatekeeper Assessment';
    if (phase === 'COMPLETED' || phase === 4) return 'Completed';
    if (phase === 'DIAGNOSTIC_REQUIRED' || phase === 0) return 'Diagnostic Needed';
    return phase ? `Phase ${phase}` : 'Phase 1: Remediation';
  }

  onRefreshRecommendations(): void {
    this.isGenerating.set(true);
    this.generateError.set(null);
    const target = this.selectedExam();
    forkJoin([
      this.learningPathService.recalculate(target).pipe(catchError(() => of(null))),
      this.recommendationService.generate(target).pipe(
        catchError((err) => {
          this.generateError.set(
            err?.error?.message ||
              'Unable to generate recommendations. Please try again.'
          );
          return of(null);
        })
      ),
    ]).subscribe({
      next: () => {
        this.isGenerating.set(false);
        this.refresh$.next();
      },
      error: (err) => {
        this.isGenerating.set(false);
        this.generateError.set(
          err?.error?.message ||
            'Unable to refresh learning path and recommendations.'
        );
      },
    });
  }

  onGenerateLearningPath(): void {
    this.onRefreshRecommendations();
  }

  scrollToStudyPlan(): void {
    const el = document.getElementById('study-plan');
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }

  getSubjectSummaryList(summary: LearningPathSummary | null) {
    if (!summary?.subjectSummaries) return [];
    return Object.values(summary.subjectSummaries);
  }
}
