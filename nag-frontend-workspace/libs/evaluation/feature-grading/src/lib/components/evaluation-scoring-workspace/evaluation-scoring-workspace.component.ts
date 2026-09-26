import {
  ChangeDetectionStrategy,
  Component,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { GradingTask } from '@nag-frontend-workspace/evaluation-data-access';
import { GradeSubmissionPayload } from '../../models';

@Component({
  selector: 'nag-evaluation-scoring-workspace',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MathRendererComponent,
  ],
  templateUrl: './evaluation-scoring-workspace.component.html',
  styleUrl: './evaluation-scoring-workspace.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationScoringWorkspaceComponent {
  readonly task = input<GradingTask | null>(null);
  readonly submitting = input<boolean>(false);

  readonly awardedMarks = signal<number>(0);
  readonly comments = signal<string>('');

  readonly commitGrade = output<GradeSubmissionPayload>();

  constructor() {
    effect(() => {
      const current = this.task();
      if (current) {
        this.awardedMarks.set(current.maxMarks);
        this.comments.set('');
      }
    });
  }

  onSubmit(): void {
    const current = this.task();
    if (!current) return;
    this.commitGrade.emit({
      taskId: current.id,
      awardedMarks: this.awardedMarks(),
      evaluatorComments: this.comments(),
    });
  }
}
