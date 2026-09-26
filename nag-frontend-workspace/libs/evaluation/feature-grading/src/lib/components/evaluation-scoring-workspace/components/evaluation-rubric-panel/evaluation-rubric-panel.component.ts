import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import { RubricCriterion } from '../../../../models';

@Component({
  selector: 'nag-evaluation-rubric-panel',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MathRendererComponent],
  templateUrl: './evaluation-rubric-panel.component.html',
  styleUrl: './evaluation-rubric-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EvaluationRubricPanelComponent {
  readonly questionContent = input<string>('');
  readonly maxMarks = input<number>(0);
  readonly rubricCriteria = input<RubricCriterion[]>([]);
  readonly comments = input<string>('');

  readonly criterionScoreChange = output<{ criterionId: string; value: number }>();
  readonly commentsChange = output<string>();

  onScoreChange(criterionId: string, value: number): void {
    this.criterionScoreChange.emit({ criterionId, value: Number(value) });
  }

  onCommentsInput(value: string): void {
    this.commentsChange.emit(value);
  }
}
