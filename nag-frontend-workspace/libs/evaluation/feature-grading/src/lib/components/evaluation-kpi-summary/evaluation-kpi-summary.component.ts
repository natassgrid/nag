import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'nag-evaluation-kpi-summary',
  standalone: true,
  imports: [CommonModule, StatCardComponent],
  templateUrl: './evaluation-kpi-summary.component.html',
  styleUrl: './evaluation-kpi-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationKpiSummaryComponent {
  readonly pendingTasksCount = input.required<number>();
  readonly openDisputesCount = input.required<number>();
  readonly meanCohortScore = input<string>('74.2');
  readonly scoringReliability = input<string>('0.94');
}
