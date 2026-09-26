import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { StatCardComponent } from '@nag-frontend-workspace/shared-ui-components';

@Component({
  selector: 'app-dashboard-kpi-stats',
  standalone: true,
  imports: [CommonModule, StatCardComponent],
  templateUrl: './dashboard-kpi-stats.component.html',
  styleUrl: './dashboard-kpi-stats.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardKpiStatsComponent {
  readonly totalRegistered = input<number>(0);
  readonly liveCount = input<number>(0);
  readonly scorecardsCount = input<number>(1);
}
