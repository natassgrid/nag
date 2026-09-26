import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import {
  StatusBadgeComponent,
  StatusVariant,
  EmptyStateComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { DisputeRecord } from '@nag-frontend-workspace/evaluation-data-access';

@Component({
  selector: 'nag-evaluation-disputes-table',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    StatusBadgeComponent,
    EmptyStateComponent,
  ],
  templateUrl: './evaluation-disputes-table.component.html',
  styleUrl: './evaluation-disputes-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationDisputesTableComponent {
  readonly disputes = input<DisputeRecord[]>([]);

  readonly resolve = output<{ dispute: DisputeRecord; resolution: 'RESOLVED' | 'REJECTED' }>();

  disputeStatusVariant(status: string): StatusVariant {
    switch (status) {
      case 'OPEN':
        return 'warn';
      case 'RESOLVED':
        return 'success';
      case 'REJECTED':
        return 'error';
      default:
        return 'neutral';
    }
  }
}
