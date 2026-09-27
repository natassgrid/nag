import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import {
  MathRendererComponent,
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  DifficultyLevel,
  QuestionType,
  QuestionOption,
} from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-standalone-live-preview',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MathRendererComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './standalone-live-preview.component.html',
  styleUrl: './standalone-live-preview.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StandaloneLivePreviewComponent {
  readonly difficulty = input<DifficultyLevel>('MEDIUM');
  readonly marks = input<number>(4);
  readonly negativeMarks = input<number>(1);
  readonly type = input<QuestionType>('MULTIPLE_CHOICE');
  readonly content = input<string>('');
  readonly options = input<QuestionOption[]>([]);
  readonly explanation = input<string>('');

  difficultyVariant(diff: string): StatusVariant {
    switch (diff) {
      case 'EASY':
        return 'success';
      case 'MEDIUM':
        return 'warn';
      case 'HARD':
      case 'EXPERT':
        return 'error';
      default:
        return 'neutral';
    }
  }

  getOptionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }
}
