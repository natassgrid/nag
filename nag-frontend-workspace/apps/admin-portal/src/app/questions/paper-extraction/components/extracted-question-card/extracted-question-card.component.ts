import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  ExtractedQuestion,
  ExtractedQuestionOption,
  ExtractedQuestionStatus,
} from '../../models';

@Component({
  selector: 'nag-extracted-question-card',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MathRendererComponent,
  ],
  templateUrl: './extracted-question-card.component.html',
  styleUrl: './extracted-question-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExtractedQuestionCardComponent {
  question = input.required<ExtractedQuestion>();
  isSelected = input<boolean>(false);
  bilingualMode = input<boolean>(false);

  readonly Math = Math;

  toggleSelect = output<string>();
  quickStatusChange = output<{ question: ExtractedQuestion; status: ExtractedQuestionStatus }>();
  openEdit = output<ExtractedQuestion>();
  splitQuestion = output<ExtractedQuestion>();
  inspectPage = output<number>();
  updateQuestionNumber = output<{ question: ExtractedQuestion; questionNumber: number }>();
  delete = output<string>();

  // Inline Quick Number Edit State
  editingNumber = signal<boolean>(false);
  tempQuestionNumber = signal<number>(1);

  getStatusClass(status: ExtractedQuestionStatus | string): string {
    switch (status) {
      case 'APPROVED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'PROMOTED':
        return 'bg-cyan-50 text-cyan-800 border-cyan-200';
      case 'NEEDS_REVIEW':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'EXTRACTED':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'DUPLICATE':
        return 'bg-slate-100 text-slate-600 border-slate-200';
      default:
        return 'bg-slate-50 text-slate-600 border-slate-200';
    }
  }

  isOptionCorrect(opt: ExtractedQuestionOption): boolean {
    const q = this.question();
    if (opt.correct === true) return true;
    if (q.answerKey && opt.id) {
      const cleanKey = q.answerKey.trim().toUpperCase();
      const cleanId = opt.id.trim().toUpperCase();
      return cleanKey === cleanId || cleanKey.includes(cleanId);
    }
    return false;
  }

  getHindiTranslation(): { content?: string; options?: ExtractedQuestionOption[]; explanation?: string } | null {
    const q = this.question();
    if (q.translations && q.translations['hi']) {
      return q.translations['hi'];
    }
    return null;
  }

  getHindiOptionText(optionId: string): string | null {
    const hi = this.getHindiTranslation();
    if (!hi || !hi.options || !Array.isArray(hi.options)) return null;
    const cleanId = (optionId || '').trim().toUpperCase();
    const match = hi.options.find(
      (o: any) => o && o.id && o.id.toString().trim().toUpperCase() === cleanId
    );
    return match ? match.text : null;
  }

  startQuickNumberEdit(): void {
    if (this.question().status === 'PROMOTED') return;
    this.tempQuestionNumber.set(this.question().questionNumber || 1);
    this.editingNumber.set(true);
  }

  saveQuickNumberEdit(): void {
    const newNum = this.tempQuestionNumber();
    this.editingNumber.set(false);
    if (newNum > 0 && newNum !== this.question().questionNumber) {
      this.updateQuestionNumber.emit({
        question: this.question(),
        questionNumber: newNum,
      });
    }
  }

  cancelQuickNumberEdit(): void {
    this.editingNumber.set(false);
  }

  onDelete(): void {
    if (window.confirm('Delete this extracted question from staging? This cannot be undone.')) {
      this.delete.emit(this.question().id);
    }
  }
}
