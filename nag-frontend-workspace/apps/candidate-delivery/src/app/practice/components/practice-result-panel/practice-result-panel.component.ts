import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { PracticeService } from '../../services/practice.service';
import { PracticeResult } from '../../models';
import { PracticeResultSummaryComponent } from '../practice-result-summary/practice-result-summary.component';
import { PracticeQuestionReviewComponent } from '../practice-question-review/practice-question-review.component';

@Component({
  selector: 'app-practice-result-panel',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    PracticeResultSummaryComponent,
    PracticeQuestionReviewComponent
  ],
  templateUrl: './practice-result-panel.component.html',
  styleUrl: './practice-result-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeResultPanelComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly practiceService = inject(PracticeService);
  private readonly router = inject(Router);

  readonly result = signal<PracticeResult | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const sessionId = this.route.snapshot.paramMap.get('sessionId');
    if (!sessionId) {
      this.error.set('No session ID provided');
      this.isLoading.set(false);
      return;
    }

    this.practiceService.getResult(sessionId).subscribe({
      next: (res) => {
        this.result.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load practice results.');
        this.isLoading.set(false);
      }
    });
  }
}
