import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import {
  MathRendererComponent,
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';
import { AuthoringSubQuestion } from '../../models/authoring.model';

@Component({
  selector: 'nag-passage-preview-viewport',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MathRendererComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './passage-preview-viewport.component.html',
  styleUrl: './passage-preview-viewport.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PassagePreviewViewportComponent {
  readonly passageTitle = input<string>('');
  readonly passageContent = input<string>('');
  readonly subQuestions = input<AuthoringSubQuestion[]>([]);
  readonly activeSubQuestionIndex = input<number>(0);

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
