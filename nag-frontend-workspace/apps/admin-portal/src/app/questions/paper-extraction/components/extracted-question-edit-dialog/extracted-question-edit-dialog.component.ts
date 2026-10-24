import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  ExtractedQuestion,
  ExtractedQuestionOption,
  ExtractedQuestionStatus,
  UpdateExtractedQuestionRequest,
} from '../../models';

@Component({
  selector: 'nag-extracted-question-edit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './extracted-question-edit-dialog.component.html',
  styleUrl: './extracted-question-edit-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExtractedQuestionEditDialogComponent {
  isOpen = input<boolean>(false);
  question = input<ExtractedQuestion | null>(null);
  updating = input<boolean>(false);

  closeModal = output<void>();
  saveEdit = output<{ questionId: string; request: UpdateExtractedQuestionRequest }>();

  editQuestionNumber = signal<number>(1);
  editContent = signal<string>('');
  editAnswerKey = signal<string>('');
  editExplanation = signal<string>('');
  editOptions = signal<ExtractedQuestionOption[]>([]);
  editStatus = signal<ExtractedQuestionStatus>('APPROVED');
  editReviewComments = signal<string>('');

  constructor() {
    effect(() => {
      const q = this.question();
      const open = this.isOpen();
      if (open && q) {
        this.editQuestionNumber.set(q.questionNumber || 1);
        this.editContent.set(q.content || '');
        this.editAnswerKey.set(q.answerKey || '');
        this.editExplanation.set(q.explanation || '');
        this.editOptions.set(
          (q.options || []).map((opt) => ({
            id: opt.id,
            text: opt.text,
            correct: opt.correct,
          }))
        );
        this.editStatus.set(q.status);
        this.editReviewComments.set(q.reviewComments || '');
      }
    });
  }

  updateOptionText(index: number, text: string): void {
    const list = [...this.editOptions()];
    if (list[index]) {
      list[index] = { ...list[index], text };
      this.editOptions.set(list);
    }
  }

  toggleOptionCorrect(index: number): void {
    const list = [...this.editOptions()];
    if (list[index]) {
      const isCurrentlyCorrect = !!list[index].correct;
      list[index] = { ...list[index], correct: !isCurrentlyCorrect };
      if (!isCurrentlyCorrect) {
        this.editAnswerKey.set(list[index].id);
      }
      this.editOptions.set(list);
    }
  }

  handleClose(): void {
    if (!this.updating()) {
      this.closeModal.emit();
    }
  }

  onSave(): void {
    const q = this.question();
    if (!q || this.updating()) return;

    const req: UpdateExtractedQuestionRequest = {
      questionNumber: this.editQuestionNumber(),
      content: (this.editContent() || '').trim(),
      answerKey: (this.editAnswerKey() || '').trim() || undefined,
      explanation: (this.editExplanation() || '').trim() || undefined,
      options: this.editOptions(),
      status: this.editStatus(),
      reviewComments: (this.editReviewComments() || '').trim() || undefined,
    };

    this.saveEdit.emit({
      questionId: q.id,
      request: req,
    });
  }
}
