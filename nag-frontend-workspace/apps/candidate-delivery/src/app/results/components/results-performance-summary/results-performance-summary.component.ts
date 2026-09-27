import {
  ChangeDetectionStrategy,
  Component,
  input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { StatCardComponent, StatusBadgeComponent } from '@nag-frontend-workspace/shared-ui-components';
import { ScorecardRecord } from '../../models';

@Component({
  selector: 'nag-results-performance-summary',
  standalone: true,
  imports: [CommonModule, MatIconModule, StatCardComponent, StatusBadgeComponent],
  templateUrl: './results-performance-summary.component.html',
  styleUrl: './results-performance-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ResultsPerformanceSummaryComponent {
  readonly scorecard = input.required<ScorecardRecord>();
}
