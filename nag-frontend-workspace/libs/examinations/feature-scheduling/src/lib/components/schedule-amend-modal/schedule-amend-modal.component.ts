import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  effect,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  ScheduleResponse,
  AmendScheduleRequest,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-schedule-amend-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './schedule-amend-modal.component.html',
  styleUrl: './schedule-amend-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleAmendModalComponent {
  isOpen = input<boolean>(false);
  schedule = input<ScheduleResponse | null>(null);

  closeModal = output<void>();
  submitAmend = output<AmendScheduleRequest>();

  readonly validationError = signal<string | null>(null);

  amendReason = '';
  amendScheduleName = '';
  amendNotificationNumber = '';
  amendExamDate = '';
  amendReserveDate = '';
  amendEffectiveFrom = '';
  amendTimeZone = 'Asia/Kolkata';

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        this.validationError.set(null);
        const sched = this.schedule();
        this.amendReason = '';
        this.amendScheduleName = sched?.scheduleName || '';
        this.amendNotificationNumber = sched?.notificationNumber || '';
        this.amendExamDate = sched?.examDate || '';
        this.amendReserveDate = sched?.reserveDate || '';
        this.amendEffectiveFrom = new Date().toISOString();
        this.amendTimeZone = sched?.timeZone || 'Asia/Kolkata';
      }
    });
  }

  onClose(): void {
    this.validationError.set(null);
    this.closeModal.emit();
  }

  onSubmit(): void {
    this.validationError.set(null);
    if (!this.amendReason.trim()) {
      this.validationError.set('Change reason is required for audit trail.');
      return;
    }

    this.submitAmend.emit({
      changeReason: this.amendReason.trim(),
      scheduleName: this.amendScheduleName.trim(),
      notificationNumber: this.amendNotificationNumber.trim() || undefined,
      examDate: this.amendExamDate,
      reserveDate: this.amendReserveDate || undefined,
      effectiveFrom: this.amendEffectiveFrom || undefined,
      timeZone: this.amendTimeZone,
    });
  }
}
