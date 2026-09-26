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
import { CreateScheduleRequest } from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-create-schedule-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './create-schedule-modal.component.html',
  styleUrl: './create-schedule-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreateScheduleModalComponent {
  isOpen = input<boolean>(false);
  examName = input<string>('');

  closeModal = output<void>();
  submitSchedule = output<CreateScheduleRequest>();

  scheduleName = '';
  notificationNumber = '';
  examDate = '';
  reserveDate = '';
  timeZone = 'Asia/Kolkata';

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        const today = new Date();
        const nextMonth = new Date(today.getFullYear(), today.getMonth() + 1, 15);
        const reserve = new Date(today.getFullYear(), today.getMonth() + 1, 16);

        this.scheduleName = `Session 1 - ${this.examName() || 'Examination'} 2026`;
        this.notificationNumber = `NAG-NOTIF-${Date.now().toString().slice(-6)}`;
        this.examDate = nextMonth.toISOString().split('T')[0];
        this.reserveDate = reserve.toISOString().split('T')[0];
        this.timeZone = 'Asia/Kolkata';
      }
    });
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onSubmit(): void {
    if (!this.scheduleName.trim() || !this.examDate) return;

    this.submitSchedule.emit({
      scheduleName: this.scheduleName.trim(),
      notificationNumber: this.notificationNumber.trim() || undefined,
      examDate: this.examDate,
      reserveDate: this.reserveDate || undefined,
      timeZone: this.timeZone,
    });
  }
}
