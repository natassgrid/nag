import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nag-paper-kpi-summary',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './paper-kpi-summary.component.html',
  styleUrl: './paper-kpi-summary.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperKpiSummaryComponent {
  readonly totalCount = input<number>(0);
  readonly approvedCount = input<number>(0);
  readonly draftCount = input<number>(0);
  readonly practiceCount = input<number>(0);
}
