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
import {
  QuestionPaperMetadata,
  ExtractedQuestion,
  ExtractedQuestionStatus,
  UpdateExtractedQuestionRequest,
} from '../../models';
import { PaperPageViewerComponent } from '../paper-page-viewer/paper-page-viewer.component';
import { SplitQuestionDialogComponent } from '../split-question-dialog/split-question-dialog.component';
import { ExtractedQuestionCardComponent } from '../extracted-question-card/extracted-question-card.component';
import { ExtractedQuestionEditDialogComponent } from '../extracted-question-edit-dialog/extracted-question-edit-dialog.component';

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
    PaperPageViewerComponent,
    SplitQuestionDialogComponent,
    ExtractedQuestionCardComponent,
    ExtractedQuestionEditDialogComponent,
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
  merging = input<boolean>(false);
  splitting = input<boolean>(false);
  resequencing = input<boolean>(false);

  backToList = output<void>();
  refresh = output<void>();
  statusFilterChange = output<string>();
  pageChange = output<{ pageIndex: number; pageSize: number }>();
  updateQuestion = output<{ questionId: string; request: UpdateExtractedQuestionRequest }>();
  promoteApproved = output<{ paperId: string; questionIds?: string[] }>();
  openAnswerKeyDialog = output<QuestionPaperMetadata>();
  mergeQuestions = output<{ paperId: string; questionIds: string[] }>();
  splitQuestion = output<{ paperId: string; questionId: string; firstContent: string; secondContent: string }>();
  resequenceQuestions = output<{ paperId: string; orderedQuestionIds: string[]; startNumber?: number }>();

  readonly Math = Math;

  searchQuery = signal<string>('');
  selectedStatusFilter = signal<string>('ALL');
  bilingualMode = signal<boolean>(false);
  pdfViewerOpen = signal<boolean>(false);
  inspectedPage = signal<number>(1);

  selectedQuestionIds = signal<Set<string>>(new Set<string>());

  splitDialogOpen = signal<boolean>(false);
  activeQuestionForSplit = signal<ExtractedQuestion | null>(null);
  editDialogOpen = signal<boolean>(false);
  activeQuestionForEdit = signal<ExtractedQuestion | null>(null);
  mergeDialogOpen = signal<boolean>(false);

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
    return (this.questions() || []).filter((q) => q && q.status === 'APPROVED').length;
  });

  readonly allVisibleSelected = computed(() => {
    const visible = this.filteredQuestions();
    if (visible.length === 0) return false;
    const selected = this.selectedQuestionIds();
    return visible.every((q) => selected.has(q.id));
  });

  readonly canMergeSelected = computed(() => {
    const selected = this.selectedQuestionIds();
    if (selected.size < 2) return false;
    const list = this.questions() || [];
    const selectedItems = list.filter((q) => selected.has(q.id));
    return selectedItems.length >= 2 && selectedItems.every((q) => q && q.status !== 'PROMOTED');
  });

  readonly canResequence = computed(() => {
    const list = this.filteredQuestions();
    return list.length > 0 && list.some((q) => q && q.status !== 'PROMOTED');
  });

  toggleCompareWithPdf(): void {
    const nextState = !this.pdfViewerOpen();
    this.pdfViewerOpen.set(nextState);
    if (nextState) {
      const visible = this.filteredQuestions();
      if (visible.length > 0 && visible[0].sourcePageNumber) {
        this.inspectedPage.set(visible[0].sourcePageNumber);
      }
    }
  }

  onInspectPage(pageNumber: number): void {
    if (pageNumber > 0) {
      this.inspectedPage.set(pageNumber);
      this.pdfViewerOpen.set(true);
    }
  }

  toggleSelectAll(): void {
    const current = new Set(this.selectedQuestionIds());
    const visible = this.filteredQuestions();
    if (this.allVisibleSelected()) {
      for (const q of visible) current.delete(q.id);
    } else {
      for (const q of visible) current.add(q.id);
    }
    this.selectedQuestionIds.set(current);
  }

  toggleSelectQuestion(id: string): void {
    const current = new Set(this.selectedQuestionIds());
    if (current.has(id)) current.delete(id);
    else current.add(id);
    this.selectedQuestionIds.set(current);
  }

  isQuestionSelected(id: string): boolean {
    return this.selectedQuestionIds().has(id);
  }

  onQuickStatusChange(event: { question: ExtractedQuestion; status: ExtractedQuestionStatus }): void {
    this.updateQuestion.emit({ questionId: event.question.id, request: { status: event.status } });
  }

  onOpenEditDialog(q: ExtractedQuestion): void {
    this.activeQuestionForEdit.set(q);
    this.editDialogOpen.set(true);
  }

  onCloseEditDialog(): void {
    this.editDialogOpen.set(false);
    this.activeQuestionForEdit.set(null);
  }

  onSaveEdit(event: { questionId: string; request: UpdateExtractedQuestionRequest }): void {
    this.updateQuestion.emit(event);
    this.onCloseEditDialog();
  }

  onUpdateQuestionNumber(event: { question: ExtractedQuestion; questionNumber: number }): void {
    this.updateQuestion.emit({ questionId: event.question.id, request: { questionNumber: event.questionNumber } });
  }

  onOpenSplitDialog(q: ExtractedQuestion): void {
    this.activeQuestionForSplit.set(q);
    this.splitDialogOpen.set(true);
  }

  onCloseSplitDialog(): void {
    this.splitDialogOpen.set(false);
    this.activeQuestionForSplit.set(null);
  }

  onConfirmSplit(event: { questionId: string; firstContent: string; secondContent: string }): void {
    this.splitQuestion.emit({ paperId: this.paper().id, ...event });
    this.onCloseSplitDialog();
  }

  onOpenMergeDialog(): void {
    if (this.canMergeSelected()) this.mergeDialogOpen.set(true);
  }

  onCloseMergeDialog(): void {
    this.mergeDialogOpen.set(false);
  }

  onConfirmMerge(): void {
    const selected = Array.from(this.selectedQuestionIds());
    if (selected.length < 2) return;
    this.mergeQuestions.emit({ paperId: this.paper().id, questionIds: selected });
    this.selectedQuestionIds.set(new Set());
    this.onCloseMergeDialog();
  }

  onResequenceSequentially(): void {
    if (!this.canResequence()) return;
    const orderedIds = this.filteredQuestions().map((q) => q.id);
    this.resequenceQuestions.emit({ paperId: this.paper().id, orderedQuestionIds: orderedIds, startNumber: 1 });
  }

  bulkApproveSelected(): void {
    const selected = Array.from(this.selectedQuestionIds());
    for (const qId of selected) {
      this.updateQuestion.emit({ questionId: qId, request: { status: 'APPROVED' } });
    }
    this.selectedQuestionIds.set(new Set());
  }

  triggerPromotion(): void {
    const selected = Array.from(this.selectedQuestionIds());
    const paperId = this.paper().id;
    this.promoteApproved.emit(selected.length > 0 ? { paperId, questionIds: selected } : { paperId });
  }

  onFilterStatus(status: string): void {
    this.selectedStatusFilter.set(status);
    this.statusFilterChange.emit(status);
  }

  onPrevPage(): void {
    const current = this.page();
    if (current > 0) this.pageChange.emit({ pageIndex: current - 1, pageSize: this.pageSize() });
  }

  onNextPage(): void {
    const current = this.page();
    if (current < this.totalPages() - 1) this.pageChange.emit({ pageIndex: current + 1, pageSize: this.pageSize() });
  }
}
