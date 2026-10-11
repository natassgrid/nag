import {
  ChangeDetectionStrategy,
  Component,
  OnChanges,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

/**
 * Inline modal for capturing mandatory reviewer rejection comments.
 *
 * Validates: Requirement 5.3 — rejection requires non-empty comments.
 *
 * @example
 *   <nag-reject-comments-dialog
 *     [open]="showRejectDialog()"
 *     (confirmed)="onRejectConfirmed($event)"
 *     (cancelled)="showRejectDialog.set(false)"
 *   />
 */
@Component({
  selector: 'nag-reject-comments-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reject-comments-dialog.component.html',
  styleUrl: './reject-comments-dialog.component.scss',
})
export class RejectCommentsDialogComponent implements OnChanges {
  /** Whether the dialog is visible */
  open = input<boolean>(false);

  /** Emits the comments string when reviewer confirms rejection */
  confirmed = output<string>();

  /** Emits when reviewer cancels */
  cancelled = output<void>();

  readonly comments = signal<string>('');
  readonly submitted = signal<boolean>(false);

  ngOnChanges(): void {
    // Reset form state whenever dialog opens
    if (this.open()) {
      this.comments.set('');
      this.submitted.set(false);
    }
  }

  get isValid(): boolean {
    return this.comments().trim().length >= 5;
  }

  onConfirm(): void {
    this.submitted.set(true);
    if (!this.isValid) return;
    this.confirmed.emit(this.comments().trim());
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
