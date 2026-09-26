import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  effect,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  ScheduleResponse,
  ScheduleTransitionRequest,
} from '@nag-frontend-workspace/examinations-data-access';
import { SCHEDULE_STATUS_TRANSITIONS } from '../../models';

@Component({
  selector: 'nag-schedule-transition-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './schedule-transition-modal.component.html',
  styleUrl: './schedule-transition-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScheduleTransitionModalComponent {
  isOpen = input<boolean>(false);
  schedule = input<ScheduleResponse | null>(null);

  closeModal = output<void>();
  submitTransition = output<ScheduleTransitionRequest>();

  targetStatus = '';
  transitionComment = '';

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        const sched = this.schedule();
        const available = this.getNextStatuses(sched?.status);
        this.targetStatus = available[0] || '';
        this.transitionComment = '';
      }
    });
  }

  getNextStatuses(status?: string): string[] {
    if (!status) return [];
    return SCHEDULE_STATUS_TRANSITIONS[status] || [];
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onSubmit(): void {
    if (!this.targetStatus) return;

    this.submitTransition.emit({
      targetStatus: this.targetStatus,
      comment: this.transitionComment.trim() || undefined,
    });
  }
}
