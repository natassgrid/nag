import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { I18nService } from '@nag-frontend-workspace/shared-util-i18n';
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
    MatIconModule,
    MatProgressBarModule,
    PracticeResultSummaryComponent,
    PracticeQuestionReviewComponent,
  ],
  templateUrl: './practice-result-panel.component.html',
  styleUrl: './practice-result-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeResultPanelComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly practiceService = inject(PracticeService);
  private readonly router = inject(Router);
  readonly i18nService = inject(I18nService, { optional: true });

  readonly result = signal<PracticeResult | null>(null);
  readonly isLoading = signal(true);
  readonly error = signal<string | null>(null);
  readonly sessionId = signal<string | null>(null);

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const sId = params.get('sessionId');
      if (!sId) {
        this.error.set('No session ID provided');
        this.isLoading.set(false);
        return;
      }
      this.sessionId.set(sId);
      this.loadResult(sId);
    });
  }

  loadResult(id: string): void {
    this.isLoading.set(true);
    this.error.set(null);
    const lang = this.i18nService?.currentLanguage() || 'en';
    this.practiceService.getResult(id, lang).subscribe({
      next: (res) => {
        this.result.set(res);
        this.isLoading.set(false);
      },
      error: () => {
        this.error.set('Failed to load practice results. Please verify your connection.');
        this.isLoading.set(false);
      },
    });
  }

  retry(): void {
    const sId = this.sessionId();
    if (sId) {
      this.loadResult(sId);
    }
  }
}
