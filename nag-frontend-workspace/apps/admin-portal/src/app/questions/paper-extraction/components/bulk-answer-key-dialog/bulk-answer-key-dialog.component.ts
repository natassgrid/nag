import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { QuestionPaperMetadata, BulkAnswerKeyRequest } from '../../models';

@Component({
  selector: 'nag-bulk-answer-key-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './bulk-answer-key-dialog.component.html',
  styleUrl: './bulk-answer-key-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BulkAnswerKeyDialogComponent {
  isOpen = input<boolean>(false);
  paper = input<QuestionPaperMetadata | null>(null);
  applying = input<boolean>(false);

  closeModal = output<void>();
  apply = output<{ paperId: string; request: BulkAnswerKeyRequest }>();

  rawInputText = signal<string>('');
  autoApprove = signal<boolean>(true);

  readonly parsedMap = computed(() => {
    const raw = (this.rawInputText() || '').trim();
    const result: Record<number, string> = {};
    if (!raw) return result;

    // Try parsing as JSON first
    if (raw.startsWith('{') && raw.endsWith('}')) {
      try {
        const parsed = JSON.parse(raw);
        for (const [k, v] of Object.entries(parsed)) {
          const qNum = parseInt(k, 10);
          if (!isNaN(qNum) && v != null) {
            result[qNum] = String(v).trim().toUpperCase();
          }
        }
        return result;
      } catch {
        // Fall back to line-by-line parsing
      }
    }

    // Split on newlines, semicolons, or commas
    const tokens = raw.split(/[\r\n;,]+/);
    for (const token of tokens) {
      const trimmed = token.trim();
      if (!trimmed) continue;

      // Match formats: "1: A", "1. B", "1 - C", "1 A", "Q1: D"
      const match = trimmed.match(/^Q?(\d+)\s*[:.\-=\s]\s*([A-Za-z0-9,]+)/i);
      if (match) {
        const qNum = parseInt(match[1], 10);
        const ans = match[2].trim().toUpperCase();
        if (!isNaN(qNum) && ans) {
          result[qNum] = ans;
        }
      }
    }

    return result;
  });

  readonly parsedCount = computed(() => {
    return Object.keys(this.parsedMap()).length;
  });

  readonly parsedPreviewList = computed(() => {
    const map = this.parsedMap();
    return Object.entries(map)
      .map(([k, v]) => ({ qNum: parseInt(k, 10), answer: v }))
      .sort((a, b) => a.qNum - b.qNum);
  });

  loadSampleTemplate(): void {
    const lines: string[] = [];
    for (let i = 1; i <= 20; i++) {
      const opt = ['A', 'B', 'C', 'D'][(i - 1) % 4];
      lines.push(`${i}: ${opt}`);
    }
    this.rawInputText.set(lines.join('\n'));
  }

  handleClose(): void {
    if (!this.applying()) {
      this.rawInputText.set('');
      this.closeModal.emit();
    }
  }

  onSubmit(): void {
    const currentPaper = this.paper();
    if (!currentPaper || this.parsedCount() === 0 || this.applying()) return;

    const request: BulkAnswerKeyRequest = {
      answersByQuestionNumber: this.parsedMap(),
      autoApproveMatched: this.autoApprove(),
    };

    this.apply.emit({
      paperId: currentPaper.id,
      request,
    });
  }
}
