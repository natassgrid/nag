import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { ReviewQuestionItem } from '../../models';

@Component({
  selector: 'app-review-question-palette',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  templateUrl: './review-question-palette.component.html',
  styleUrl: './review-question-palette.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewQuestionPaletteComponent {
  readonly questions = input.required<ReviewQuestionItem[]>();
  readonly currentIndex = input.required<number>();

  readonly selectQuestion = output<number>();
}
