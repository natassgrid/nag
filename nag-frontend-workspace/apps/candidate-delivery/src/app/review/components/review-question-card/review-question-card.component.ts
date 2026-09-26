import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  StatusBadgeComponent,
  MathRendererComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { ReviewQuestionItem } from '../../models';

@Component({
  selector: 'app-review-question-card',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    StatusBadgeComponent,
    MathRendererComponent,
  ],
  templateUrl: './review-question-card.component.html',
  styleUrl: './review-question-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewQuestionCardComponent {
  readonly question = input.required<ReviewQuestionItem>();
  readonly currentIndex = input.required<number>();
  readonly totalFiltered = input.required<number>();

  readonly prev = output<void>();
  readonly next = output<void>();
  readonly dispute = output<ReviewQuestionItem>();

  getOptionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }
}
