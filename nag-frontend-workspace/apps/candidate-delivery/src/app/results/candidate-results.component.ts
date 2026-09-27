import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  NotificationService,
} from '@nag-frontend-workspace/shared-ui-components';
import { ScorecardRecord } from './models';
import { CandidateResultsService } from './services';
import {
  ResultsExamSelectorComponent,
  ResultsPerformanceSummaryComponent,
  ResultsSectionTableComponent,
  ResultsCryptographicProofComponent,
} from './components';

export * from './models';
export * from './services';
export * from './components';

@Component({
  selector: 'app-candidate-results',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    ResultsExamSelectorComponent,
    ResultsPerformanceSummaryComponent,
    ResultsSectionTableComponent,
    ResultsCryptographicProofComponent,
  ],
  templateUrl: './candidate-results.component.html',
  styleUrl: './candidate-results.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateResultsComponent implements OnInit {
  readonly resultsService = inject(CandidateResultsService);
  private readonly notification = inject(NotificationService);

  ngOnInit(): void {
    this.resultsService.loadScorecards().subscribe();
  }

  onSelectScorecard(scorecard: ScorecardRecord): void {
    this.resultsService.selectScorecard(scorecard);
  }

  onPushDigiLocker(resultId: string): void {
    this.resultsService.pushToDigiLocker(resultId).subscribe({
      next: (res) => {
        this.notification.success(
          'DigiLocker Credential Deposited',
          `Verified academic scorecard doc issued (ID: ${res.docId}).`
        );
      },
      error: () => {
        this.notification.error(
          'DigiLocker Push Failed',
          'Unable to deposit certificate into DigiLocker at this time.'
        );
      },
    });
  }

  downloadScorecard(): void {
    window.print();
  }

  retryLoad(): void {
    this.resultsService.loadScorecards().subscribe();
  }
}
