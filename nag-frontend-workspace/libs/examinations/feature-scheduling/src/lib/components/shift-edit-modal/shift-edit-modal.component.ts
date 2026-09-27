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
  ShiftResponse,
  CreateShiftRequest,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-shift-edit-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, MatButtonModule, MatIconModule],
  templateUrl: './shift-edit-modal.component.html',
  styleUrl: './shift-edit-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShiftEditModalComponent {
  isOpen = input<boolean>(false);
  shift = input<ShiftResponse | null>(null);
  defaultDuration = input<number>(180);
  nextShiftNumber = input<number>(1);

  closeModal = output<void>();
  saveShift = output<{ shiftId: string | null; request: CreateShiftRequest }>();

  editingShiftId: string | null = null;
  shiftNumber = 1;
  shiftName = 'Morning Session (Shift 1)';
  reportingTime = '07:30:00';
  gateClosingTime = '08:30:00';
  loginStartTime = '08:45:00';
  examStartTime = '09:00:00';
  examEndTime = '12:00:00';
  exitTime = '12:15:00';
  durationMinutes = 180;
  bufferMinutes = 30;

  constructor() {
    effect(() => {
      if (this.isOpen()) {
        const s = this.shift();
        if (s) {
          this.editingShiftId = s.id;
          this.shiftNumber = s.shiftNumber;
          this.shiftName = s.shiftName || '';
          this.reportingTime = s.reportingTime || '07:30:00';
          this.gateClosingTime = s.gateClosingTime || '08:30:00';
          this.loginStartTime = s.loginStartTime || '08:45:00';
          this.examStartTime = s.examStartTime || '09:00:00';
          this.examEndTime = s.examEndTime || '12:00:00';
          this.exitTime = s.exitTime || '';
          this.durationMinutes = s.durationMinutes || this.defaultDuration();
          this.bufferMinutes = s.bufferMinutes || 30;
        } else {
          this.editingShiftId = null;
          const nextNum = this.nextShiftNumber();
          this.shiftNumber = nextNum;
          this.shiftName = `Shift ${nextNum} (${nextNum === 1 ? 'Morning' : nextNum === 2 ? 'Afternoon' : 'Evening'})`;
          this.reportingTime = nextNum === 1 ? '07:30:00' : '13:00:00';
          this.gateClosingTime = nextNum === 1 ? '08:30:00' : '14:00:00';
          this.loginStartTime = nextNum === 1 ? '08:45:00' : '14:15:00';
          this.examStartTime = nextNum === 1 ? '09:00:00' : '14:30:00';
          this.examEndTime = nextNum === 1 ? '12:00:00' : '17:30:00';
          this.exitTime = nextNum === 1 ? '12:15:00' : '17:45:00';
          this.durationMinutes = this.defaultDuration() || 180;
          this.bufferMinutes = 30;
        }
      }
    });
  }

  onClose(): void {
    this.closeModal.emit();
  }

  onSubmit(): void {
    const payload: CreateShiftRequest = {
      shiftNumber: this.shiftNumber,
      shiftName: this.shiftName.trim() || undefined,
      reportingTime: this.reportingTime,
      gateClosingTime: this.gateClosingTime,
      loginStartTime: this.loginStartTime,
      examStartTime: this.examStartTime,
      examEndTime: this.examEndTime,
      exitTime: this.exitTime || undefined,
      durationMinutes: this.durationMinutes,
      bufferMinutes: this.bufferMinutes,
    };

    this.saveShift.emit({
      shiftId: this.editingShiftId,
      request: payload,
    });
  }
}
