import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  NotificationService,
  PageHeaderComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  QuestionBankService,
  SubjectTopicService,
  Subject,
} from '@nag-frontend-workspace/questions-data-access';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { QuestionCardData } from '@nag-frontend-workspace/questions-ui-question-card';
import {
  BankFilterBarComponent,
  BankItemsListComponent,
  BankSemanticSearchDrawerComponent,
  RejectCommentsDialogComponent,
} from '../components';

/** Tab options for the question bank view */
type BankTab = 'ALL' | 'DRAFT' | 'REVIEW' | 'APPROVED' | 'PUBLISHED';

@Component({
  selector: 'nag-questions-feature-bank',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    BankFilterBarComponent,
    BankItemsListComponent,
    BankSemanticSearchDrawerComponent,
    RejectCommentsDialogComponent,
  ],
  templateUrl: './questions-feature-bank.component.html',
  styleUrl: './questions-feature-bank.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionsFeatureBank implements OnInit {
  private readonly router = inject(Router);
  readonly questionService = inject(QuestionBankService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly notificationService = inject(NotificationService);
  private readonly authService = inject(AuthService);

  // -------------------------------------------------------------------------
  // Auth-derived properties for four-eyes enforcement
  // -------------------------------------------------------------------------

  /** Current logged-in user's UUID — passed down to each card for four-eyes check */
  readonly currentUserId = computed<string | null>(
    () => this.authService.currentUser()?.userId ?? null
  );

  readonly isReviewer = computed<boolean>(() =>
    this.authService.hasAnyRole(['REVIEWER', 'APPROVER', 'EXAM_CONTROLLER', 'ADMIN', 'SUPER_ADMIN'])
  );

  readonly isAuthor = computed<boolean>(() =>
    this.authService.hasAnyRole(['QUESTION_AUTHOR', 'ADMIN', 'SUPER_ADMIN'])
  );

  // -------------------------------------------------------------------------
  // Review-queue tab state
  // -------------------------------------------------------------------------
  readonly activeTab = signal<BankTab>('ALL');

  readonly tabs: { id: BankTab; label: string; icon: string }[] = [
    { id: 'ALL', label: 'All Items', icon: 'menu_book' },
    { id: 'DRAFT', label: 'Drafts', icon: 'edit_note' },
    { id: 'REVIEW', label: 'Review Queue', icon: 'rate_review' },
    { id: 'APPROVED', label: 'Approved', icon: 'check_circle' },
    { id: 'PUBLISHED', label: 'Published', icon: 'publish' },
  ];

  // -------------------------------------------------------------------------
  // Reject dialog state
  // -------------------------------------------------------------------------
  readonly showRejectDialog = signal<boolean>(false);
  private pendingRejectId = signal<string | null>(null);

  // -------------------------------------------------------------------------
  // Filter / search state
  // -------------------------------------------------------------------------
  readonly showVectorDrawer = signal<boolean>(false);
  readonly searchingVector = signal<boolean>(false);

  readonly selectedSubject = signal<string>('ALL');
  readonly selectedDifficulty = signal<string>('ALL');
  readonly selectedStatus = signal<string>('ALL');
  readonly searchQuery = signal<string>('');

  readonly subjects = signal<string[]>([
    'ALL',
    'Quantitative Aptitude / Mathematical Abilities',
    'General Intelligence and Reasoning',
    'English Language and Comprehension',
    'General Awareness',
    'Data Interpretation and Logical Analysis',
  ]);

  readonly difficulties = ['ALL', 'EASY', 'MEDIUM', 'HARD'];
  readonly statuses = ['ALL', 'APPROVED', 'REVIEW', 'DRAFT', 'REJECTED', 'PUBLISHED'];
  readonly pageSizes = [10, 20, 50, 100];

  readonly showingStart = computed(() => {
    const total = this.questionService.total();
    if (total === 0) return 0;
    return this.questionService.currentPage() * this.questionService.pageSize() + 1;
  });

  readonly showingEnd = computed(() => {
    const total = this.questionService.total();
    return Math.min(
      (this.questionService.currentPage() + 1) * this.questionService.pageSize(),
      total
    );
  });

  readonly hasActiveFilters = computed(() => {
    return (
      !!this.searchQuery() ||
      this.selectedSubject() !== 'ALL' ||
      this.selectedDifficulty() !== 'ALL' ||
      this.selectedStatus() !== 'ALL' ||
      this.activeTab() !== 'ALL'
    );
  });

  // -------------------------------------------------------------------------
  // Lifecycle
  // -------------------------------------------------------------------------

  ngOnInit(): void {
    this.loadSubjects();
    this.applyFilters(0);
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs: Subject[]) => {
        if (subs && subs.length > 0) {
          const names = subs.map((s) => s.name.trim()).filter((name) => !!name);
          const uniqueNames = Array.from(new Set(names));
          this.subjects.set(['ALL', ...uniqueNames]);
        }
      },
      error: () => {
        // Keep default fallback subjects
      },
    });
  }

  // -------------------------------------------------------------------------
  // Tab navigation
  // -------------------------------------------------------------------------

  switchTab(tab: BankTab): void {
    this.activeTab.set(tab);
    // Sync selectedStatus with the tab
    const tabStatusMap: Record<BankTab, string> = {
      ALL: 'ALL',
      DRAFT: 'DRAFT',
      REVIEW: 'REVIEW',
      APPROVED: 'APPROVED',
      PUBLISHED: 'PUBLISHED',
    };
    this.selectedStatus.set(tabStatusMap[tab]);
    this.applyFilters(0);
  }

  // -------------------------------------------------------------------------
  // Filter handlers
  // -------------------------------------------------------------------------

  onSearchTextChange(query: string): void {
    this.searchQuery.set(query);
    this.applyFilters(0);
  }

  onSubjectChange(subject: string): void {
    this.selectedSubject.set(subject);
    this.applyFilters(0);
  }

  onDifficultyChange(difficulty: string): void {
    this.selectedDifficulty.set(difficulty);
    this.applyFilters(0);
  }

  onStatusChange(status: string): void {
    this.selectedStatus.set(status);
    this.applyFilters(0);
  }

  onPageSizeChange(size: number): void {
    this.questionService.filter.update((f) => ({ ...f, size }));
    this.applyFilters(0);
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.questionService.totalPages()) {
      this.applyFilters(page);
    }
  }

  resetFilters(): void {
    this.searchQuery.set('');
    this.selectedSubject.set('ALL');
    this.selectedDifficulty.set('ALL');
    this.selectedStatus.set('ALL');
    this.activeTab.set('ALL');
    this.applyFilters(0);
  }

  runVectorSearch(prompt: string): void {
    if (!prompt.trim()) return;

    this.searchingVector.set(true);
    this.questionService.searchVectorSimilar(prompt).subscribe({
      next: () => {
        this.searchingVector.set(false);
        this.showVectorDrawer.set(false);
      },
      error: () => {
        this.searchingVector.set(false);
      },
    });
  }

  applyFilters(page: number): void {
    const sub = this.selectedSubject();
    const diff = this.selectedDifficulty();
    const stat = this.selectedStatus();
    const search = this.searchQuery();

    this.questionService
      .loadQuestions({
        page,
        size: this.questionService.pageSize(),
        search: search ? search : undefined,
        subject: sub !== 'ALL' ? sub : undefined,
        difficulty: diff !== 'ALL' ? diff : undefined,
        status: stat !== 'ALL' ? stat : undefined,
        sort: 'createdAt',
        order: 'desc',
      })
      .subscribe();
  }

  // -------------------------------------------------------------------------
  // Card action handlers
  // -------------------------------------------------------------------------

  onEditQuestion(card: QuestionCardData): void {
    this.router.navigate(['/questions/authoring'], {
      queryParams: { edit: card.id },
    });
  }

  async onDeleteQuestion(id: string): Promise<void> {
    const confirmed = await this.notificationService.confirm({
      title: 'Delete Question Item?',
      message:
        'Are you sure you want to delete this question item? This action will remove it from active item pools.',
      confirmText: 'Delete Question',
      cancelText: 'Cancel',
      type: 'danger',
    });

    if (confirmed) {
      this.questionService.deleteQuestion(id).subscribe(() => {
        this.applyFilters(this.questionService.currentPage());
        this.notificationService.success(
          'Question Deleted',
          'Question item was successfully removed.'
        );
      });
    }
  }

  // -------------------------------------------------------------------------
  // Review workflow handlers (Issue #275)
  // -------------------------------------------------------------------------

  onSubmitQuestion(id: string): void {
    this.questionService.submitForReview(id).subscribe({
      next: () => {
        this.notificationService.success(
          'Submitted for Review',
          'Question has been sent to the review queue.'
        );
        this.applyFilters(this.questionService.currentPage());
      },
      error: (err) => {
        this.notificationService.error(
          'Submission Failed',
          err?.error?.message || 'Could not submit question for review.'
        );
      },
    });
  }

  onApproveQuestion(id: string): void {
    this.questionService.approveQuestion(id).subscribe({
      next: () => {
        this.notificationService.success(
          'Question Approved',
          'The question has been approved and is ready for publication.'
        );
        this.applyFilters(this.questionService.currentPage());
      },
      error: (err) => {
        const msg = err?.error?.message || 'Could not approve question.';
        // Four-eyes violation returns 403
        if (err?.status === 403) {
          this.notificationService.error(
            'Four-Eyes Principle Violation',
            'You cannot approve a question that you authored.'
          );
        } else {
          this.notificationService.error('Approval Failed', msg);
        }
      },
    });
  }

  /** Opens the reject-comments dialog; actual rejection happens in onRejectConfirmed */
  onRejectQuestion(id: string): void {
    this.pendingRejectId.set(id);
    this.showRejectDialog.set(true);
  }

  onRejectConfirmed(comments: string): void {
    const id = this.pendingRejectId();
    if (!id) return;

    this.questionService.rejectQuestion(id, comments).subscribe({
      next: () => {
        this.notificationService.warning(
          'Question Rejected',
          'The question has been returned to the author with your feedback.'
        );
        this.pendingRejectId.set(null);
        this.applyFilters(this.questionService.currentPage());
      },
      error: (err) => {
        this.notificationService.error(
          'Rejection Failed',
          err?.error?.message || 'Could not reject question.'
        );
        this.pendingRejectId.set(null);
      },
    });
  }

  onRejectCancelled(): void {
    this.pendingRejectId.set(null);
    this.showRejectDialog.set(false);
  }

  onPublishQuestion(id: string): void {
    this.questionService.publishQuestion(id).subscribe({
      next: () => {
        this.notificationService.success(
          'Question Published',
          'The question is now live in the assessment bank and eligible for paper assembly.'
        );
        this.applyFilters(this.questionService.currentPage());
      },
      error: (err) => {
        const msg = err?.error?.message || 'Could not publish question.';
        if (err?.status === 403) {
          this.notificationService.error(
            'Four-Eyes Principle Violation',
            'You cannot publish a question that you reviewed and approved.'
          );
        } else {
          this.notificationService.error('Publication Failed', msg);
        }
      },
    });
  }
}
