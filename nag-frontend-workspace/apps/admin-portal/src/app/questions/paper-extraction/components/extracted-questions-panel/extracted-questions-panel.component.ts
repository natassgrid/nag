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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  QuestionPaperMetadata,
  ExtractedQuestion,
  ExtractedQuestionOption,
  ExtractedQuestionStatus,
  UpdateExtractedQuestionRequest,
} from '../../models';

@Component({
  selector: 'nag-extracted-questions-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MathRendererComponent,
  ],
  templateUrl: './extracted-questions-panel.component.html',
  styleUrl: './extracted-questions-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExtractedQuestionsPanelComponent {
  paper = input.required<QuestionPaperMetadata>();
  questions = input<ExtractedQuestion[]>([]);
  loading = input<boolean>(false);
  totalQuestions = input<number>(0);
  page = input<number>(0);
  pageSize = input<number>(50);
  pageSizeOptions = input<number[]>([20, 50, 100]);
  promoting = input<boolean>(false);
  updating = input<boolean>(false);

  backToList = output<void>();
  refresh = output<void>();
  statusFilterChange = output<string>();
  pageChange = output<{ pageIndex: number; pageSize: number }>();
  updateQuestion = output<{ questionId: string; request: UpdateExtractedQuestionRequest }>();
  promoteApproved = output<{ paperId: string; questionIds?: string[] }>();
  openAnswerKeyDialog = output<QuestionPaperMetadata>();

  readonly Math = Math;

  // Filter & Toggle State
  searchQuery = signal<string>('');
  selectedStatusFilter = signal<string>('ALL');
  bilingualMode = signal<boolean>(false);

  // Selection State
  selectedQuestionIds = signal<Set<string>>(new Set<string>());

  // Inline Card Editor State
  editingQuestionId = signal<string | null>(null);
  editContent = signal<string>('');
  editAnswerKey = signal<string>('');
  editExplanation = signal<string>('');
  editOptions = signal<ExtractedQuestionOption[]>([]);
  editStatus = signal<ExtractedQuestionStatus>('APPROVED');
  editReviewComments = signal<string>('');

  readonly statusFilterOptions: { value: string; label: string }[] = [
    { value: 'ALL', label: 'All Staging Statuses' },
    { value: 'EXTRACTED', label: 'Extracted (Pending Verification)' },
    { value: 'NEEDS_REVIEW', label: 'Needs Review' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'PROMOTED', label: 'Promoted' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'DUPLICATE', label: 'Duplicate' },
  ];

  readonly filteredQuestions = computed(() => {
    const list = this.questions() || [];
    const q = (this.searchQuery() || '').toLowerCase().trim();
    const st = this.selectedStatusFilter();

    return list.filter((item) => {
      if (!item) return false;
      const matchesSearch =
        !q ||
        (item.content && item.content.toLowerCase().includes(q)) ||
        (item.questionNumber && item.questionNumber.toString().includes(q)) ||
        (item.sectionName && item.sectionName.toLowerCase().includes(q));

      const matchesStatus = st === 'ALL' || item.status === st;
      return matchesSearch && matchesStatus;
    });
  });

  readonly totalPages = computed(() => {
    const size = this.pageSize();
    if (size <= 0) return 1;
    return Math.max(1, Math.ceil(this.totalQuestions() / size));
  });

  readonly approvedCount = computed(() => {
    return (this.questions() || []).filter((q) => q.status === 'APPROVED').length;
  });

  readonly allVisibleSelected = computed(() => {
    const visible = this.filteredQuestions();
    if (visible.length === 0) return false;
    const selected = this.selectedQuestionIds();
    return visible.every((q) => selected.has(q.id));
  });

  toggleSelectAll(): void {
    const current = new Set(this.selectedQuestionIds());
    const visible = this.filteredQuestions();
    if (this.allVisibleSelected()) {
      for (const q of visible) {
        current.delete(q.id);
      }
    } else {
      for (const q of visible) {
        current.add(q.id);
      }
    }
    this.selectedQuestionIds.set(current);
  }

  toggleSelectQuestion(id: string): void {
    const current = new Set(this.selectedQuestionIds());
    if (current.has(id)) {
      current.delete(id);
    } else {
      current.add(id);
    }
    this.selectedQuestionIds.set(current);
  }

  isQuestionSelected(id: string): boolean {
    return this.selectedQuestionIds().has(id);
  }

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

  isOptionCorrect(q: ExtractedQuestion, opt: ExtractedQuestionOption): boolean {
    if (opt.correct === true) return true;
    if (q.answerKey && opt.id) {
      const cleanKey = q.answerKey.trim().toUpperCase();
      const cleanId = opt.id.trim().toUpperCase();
      return cleanKey === cleanId || cleanKey.includes(cleanId);
    }
    return false;
  }

  getHindiTranslation(q: ExtractedQuestion): { content?: string; options?: ExtractedQuestionOption[]; explanation?: string } | null {
    if (q.translations && q.translations['hi']) {
      return q.translations['hi'];
    }
    return null;
  }

  onQuickStatusChange(q: ExtractedQuestion, newStatus: ExtractedQuestionStatus): void {
    this.updateQuestion.emit({
      questionId: q.id,
      request: { status: newStatus },
    });
  }

  startEditing(q: ExtractedQuestion): void {
    this.editingQuestionId.set(q.id);
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

  cancelEditing(): void {
    this.editingQuestionId.set(null);
  }

  saveEditing(q: ExtractedQuestion): void {
    const req: UpdateExtractedQuestionRequest = {
      content: (this.editContent() || '').trim(),
      answerKey: (this.editAnswerKey() || '').trim() || undefined,
      explanation: (this.editExplanation() || '').trim() || undefined,
      options: this.editOptions(),
      status: this.editStatus(),
      reviewComments: (this.editReviewComments() || '').trim() || undefined,
    };

    this.updateQuestion.emit({
      questionId: q.id,
      request: req,
    });
    this.editingQuestionId.set(null);
  }

  updateEditOptionText(index: number, text: string): void {
    const list = [...this.editOptions()];
    if (list[index]) {
      list[index] = { ...list[index], text };
      this.editOptions.set(list);
    }
  }

  toggleEditOptionCorrect(index: number): void {
    const list = [...this.editOptions()];
    if (list[index]) {
      const isCurrentlyCorrect = !!list[index].correct;
      list[index] = { ...list[index], correct: !isCurrentlyCorrect };
      // Also update answerKey if single choice
      if (!isCurrentlyCorrect) {
        this.editAnswerKey.set(list[index].id);
      }
      this.editOptions.set(list);
    }
  }

  bulkApproveSelected(): void {
    const selected = Array.from(this.selectedQuestionIds());
    if (selected.length === 0) return;

    for (const qId of selected) {
      this.updateQuestion.emit({
        questionId: qId,
        request: { status: 'APPROVED' },
      });
    }
    this.selectedQuestionIds.set(new Set());
  }

  triggerPromotion(): void {
    const selected = Array.from(this.selectedQuestionIds());
    const paperId = this.paper().id;
    if (selected.length > 0) {
      this.promoteApproved.emit({ paperId, questionIds: selected });
    } else {
      // Promote all approved questions for this paper
      this.promoteApproved.emit({ paperId });
    }
  }

  onFilterStatus(status: string): void {
    this.selectedStatusFilter.set(status);
    this.statusFilterChange.emit(status);
  }

  onPrevPage(): void {
    const current = this.page();
    if (current > 0) {
      this.pageChange.emit({ pageIndex: current - 1, pageSize: this.pageSize() });
    }
  }

  onNextPage(): void {
    const current = this.page();
    if (current < this.totalPages() - 1) {
      this.pageChange.emit({ pageIndex: current + 1, pageSize: this.pageSize() });
    }
  }
}
