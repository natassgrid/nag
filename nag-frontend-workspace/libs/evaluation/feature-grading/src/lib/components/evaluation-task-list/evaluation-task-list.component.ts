import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import { GradingTask } from '@nag-frontend-workspace/evaluation-data-access';

@Component({
  selector: 'nag-evaluation-task-list',
  standalone: true,
  imports: [CommonModule, StatusBadgeComponent],
  templateUrl: './evaluation-task-list.component.html',
  styleUrl: './evaluation-task-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationTaskListComponent {
  readonly tasks = input<GradingTask[]>([]);
  readonly selectedTask = input<GradingTask | null>(null);

  readonly selectTask = output<GradingTask>();

  taskStatusVariant(status: string): StatusVariant {
    switch (status) {
      case 'PENDING':
      case 'SUBMITTED':
        return 'warn';
      case 'GRADED':
        return 'success';
      case 'DISPUTED':
      case 'IN_REVIEW':
        return 'primary';
      default:
        return 'neutral';
    }
  }
}
