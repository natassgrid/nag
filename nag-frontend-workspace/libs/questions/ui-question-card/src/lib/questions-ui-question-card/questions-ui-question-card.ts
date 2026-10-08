import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  MathRendererComponent,
  StatusBadgeComponent,
  StatusVariant,
} from '@nag-frontend-workspace/shared-ui-components';

export interface QuestionCardOption {
  id: string;
  text: string;
  isCorrect?: boolean;
}

export interface QuestionCardData {
  id: string;
  code?: string;
  content: string;
  type?: string;
  difficulty?: string;
  /** Lifecycle state: DRAFT | REVIEW | APPROVED | PUBLISHED | REJECTED */
  status?: string;
  /** UUID of the question author — used for four-eyes enforcement */
  authorId?: string;
  /** Reviewer rejection feedback, visible to the author when status is DRAFT */
  reviewComments?: string;
  marks?: number;
  negativeMarks?: number;
  options?: QuestionCardOption[];
  tags?: string[];
}

@Component({
  selector: 'nag-question-card',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MathRendererComponent,
    StatusBadgeComponent,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './questions-ui-question-card.component.html',
  styleUrl: './questions-ui-question-card.component.scss',
})
export class QuestionsUiQuestionCard {
  data = input.required<QuestionCardData>();
  showActions = input<boolean>(true);
  showCorrectOption = input<boolean>(false);
  selected = input<boolean>(false);

  /**
   * Current authenticated user's UUID.
   * Used to enforce the four-eyes rule: reviewer cannot be the question author.
   */
  currentUserId = input<string | null>(null);

  /**
   * Whether the current user has a reviewer/approver role.
   * Controls visibility of Approve/Reject actions.
   */
  isReviewer = input<boolean>(false);

  /**
   * Whether the current user has an author role.
   * Controls visibility of the Submit for Review action.
   */
  isAuthor = input<boolean>(false);

  // Existing outputs
  cardEdit = output<QuestionCardData>();
  cardDelete = output<string>();
  cardSelect = output<QuestionCardData>();

  // Review workflow outputs (Issue #275)
  cardSubmit = output<string>();
  cardApprove = output<string>();
  cardReject = output<string>();
  cardPublish = output<string>();

  difficultyVariant(): StatusVariant {
    const diff = (this.data().difficulty || '').toUpperCase();
    if (diff === 'EASY') return 'success';
    if (diff === 'MEDIUM') return 'warn';
    if (diff === 'HARD') return 'error';
    return 'neutral';
  }

  statusVariant(): StatusVariant {
    const s = (this.data().status || '').toUpperCase();
    if (s === 'APPROVED' || s === 'PUBLISHED') return 'success';
    if (s === 'REVIEW' || s === 'IN_REVIEW') return 'warn';
    if (s === 'DRAFT') return 'neutral';
    return 'error'; // REJECTED
  }

  formatType(type?: string): string {
    if (!type) return '';
    return type.replace(/_/g, ' ').toLowerCase();
  }

  getOptionLabel(idx: number): string {
    return String.fromCharCode(65 + idx);
  }

  /** True if the current user is the author — used to enforce four-eyes on approve */
  get isOwnQuestion(): boolean {
    const uid = this.currentUserId();
    const authorId = this.data().authorId;
    return !!(uid && authorId && uid === authorId);
  }

  /** Author can submit DRAFT questions for review */
  get canSubmit(): boolean {
    return (
      this.isAuthor() &&
      (this.data().status || '').toUpperCase() === 'DRAFT'
    );
  }

  /** Reviewer can approve REVIEW questions — but NOT their own */
  get canApprove(): boolean {
    return (
      this.isReviewer() &&
      (this.data().status || '').toUpperCase() === 'REVIEW' &&
      !this.isOwnQuestion
    );
  }

  /** Reviewer can reject REVIEW questions */
  get canReject(): boolean {
    return (
      this.isReviewer() &&
      (this.data().status || '').toUpperCase() === 'REVIEW'
    );
  }

  /**
   * Approver can publish APPROVED questions — but NOT the same reviewer who approved it.
   * Four-eyes: publisher ≠ reviewer (enforced by backend; here we do a best-effort client check).
   */
  get canPublish(): boolean {
    return (
      this.isReviewer() &&
      (this.data().status || '').toUpperCase() === 'APPROVED'
    );
  }

  /** Show rejection feedback banner when a question is back in DRAFT with comments */
  get hasRejectionFeedback(): boolean {
    const status = (this.data().status || '').toUpperCase();
    return status === 'DRAFT' && !!(this.data().reviewComments);
  }

  onEditClicked(e: Event): void {
    e.stopPropagation();
    this.cardEdit.emit(this.data());
  }

  onDeleteClicked(e: Event): void {
    e.stopPropagation();
    this.cardDelete.emit(this.data().id);
  }

  onSubmitClicked(e: Event): void {
    e.stopPropagation();
    this.cardSubmit.emit(this.data().id);
  }

  onApproveClicked(e: Event): void {
    e.stopPropagation();
    this.cardApprove.emit(this.data().id);
  }

  onRejectClicked(e: Event): void {
    e.stopPropagation();
    this.cardReject.emit(this.data().id);
  }

  onPublishClicked(e: Event): void {
    e.stopPropagation();
    this.cardPublish.emit(this.data().id);
  }
}
