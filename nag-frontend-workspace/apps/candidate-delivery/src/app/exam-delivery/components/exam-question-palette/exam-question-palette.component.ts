import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ExamItem, ExamSessionMetadata } from '../../models';

@Component({
  selector: 'nag-exam-question-palette',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './exam-question-palette.component.html',
  styleUrl: './exam-question-palette.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamQuestionPaletteComponent {
  readonly questions = input.required<ExamItem[]>();
  readonly currentIndex = input.required<number>();
  readonly countAnswered = input.required<number>();
  readonly countFlagged = input.required<number>();
  readonly countUnvisited = input.required<number>();
  readonly sessionMeta = input<ExamSessionMetadata>();

  readonly selectQuestion = output<number>();

  onSelectQuestion(index: number): void {
    this.selectQuestion.emit(index);
  }
}
