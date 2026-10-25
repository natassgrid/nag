import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  effect,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExtractedQuestion } from '../../models';

@Component({
  selector: 'nag-split-question-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './split-question-dialog.component.html',
  styleUrl: './split-question-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SplitQuestionDialogComponent {
  isOpen = input<boolean>(false);
  question = input<ExtractedQuestion | null>(null);
  splitting = input<boolean>(false);

  closeModal = output<void>();
  confirmSplit = output<{
    questionId: string;
    firstContent: string;
    secondContent: string;
  }>();

  firstContent = signal<string>('');
  secondContent = signal<string>('');

  constructor() {
    effect(() => {
      const q = this.question();
      const open = this.isOpen();
      if (open && q) {
        this.initializeContents(q.content || '');
      }
    });
  }

  private initializeContents(fullContent: string): void {
    const text = (fullContent || '').trim();
    if (!text) {
      this.firstContent.set('');
      this.secondContent.set('');
      return;
    }

    // Try splitting at double newline if available
    const doubleNewlineIdx = text.indexOf('\n\n');
    if (doubleNewlineIdx > 0 && doubleNewlineIdx < text.length - 2) {
      this.firstContent.set(text.substring(0, doubleNewlineIdx).trim());
      this.secondContent.set(text.substring(doubleNewlineIdx + 2).trim());
      return;
    }

    // Otherwise split roughly halfway at the nearest word boundary
    const mid = Math.floor(text.length / 2);
    const spaceAfterMid = text.indexOf(' ', mid);
    if (spaceAfterMid > 0 && spaceAfterMid < text.length - 1) {
      this.firstContent.set(text.substring(0, spaceAfterMid).trim());
      this.secondContent.set(text.substring(spaceAfterMid + 1).trim());
    } else {
      this.firstContent.set(text);
      this.secondContent.set('');
    }
  }

  readonly isValid = computed(() => {
    return (
      (this.firstContent() || '').trim().length > 0 &&
      (this.secondContent() || '').trim().length > 0
    );
  });

  handleClose(): void {
    if (!this.splitting()) {
      this.closeModal.emit();
    }
  }

  handleConfirm(): void {
    const q = this.question();
    if (!q || !this.isValid() || this.splitting()) return;

    this.confirmSplit.emit({
      questionId: q.id,
      firstContent: this.firstContent().trim(),
      secondContent: this.secondContent().trim(),
    });
  }
}
