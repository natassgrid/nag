import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RecommendationService } from '../../services';
import { RecommendationCardComponent } from '../recommendation-card/recommendation-card.component';
import { LearnerProfileInsightsComponent } from '../learner-profile-insights/learner-profile-insights.component';
import { catchError, of, combineLatest, map } from 'rxjs';

@Component({
  selector: 'app-recommendations-dashboard',
  standalone: true,
  imports: [
    CommonModule, 
    MatProgressSpinnerModule, 
    RecommendationCardComponent, 
    LearnerProfileInsightsComponent
  ],
  templateUrl: './recommendations-dashboard.component.html',
  styleUrl: './recommendations-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RecommendationsDashboardComponent {
  private readonly recommendationService = inject(RecommendationService);

  private readonly dashboardData$ = combineLatest([
    this.recommendationService.getLatest().pipe(catchError(() => of(null))),
    this.recommendationService.getLearnerProfile().pipe(catchError(() => of(null)))
  ]).pipe(
    map(([recommendation, profile]) => ({ recommendation, profile, loading: false }))
  );

  dashboardData = toSignal(this.dashboardData$, { initialValue: { recommendation: null, profile: null, loading: true } });
}
