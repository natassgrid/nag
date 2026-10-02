import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { QuestionResult } from '../../models';

@Component({
  selector: 'app-practice-question-review',
  standalone: true,
  imports: [CommonModule, MatListModule, MatIconModule, MatChipsModule],
  templateUrl: './practice-question-review.component.html',
  styleUrl: './practice-question-review.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PracticeQuestionReviewComponent {
  readonly questionResults = input.required<QuestionResult[]>();
}
