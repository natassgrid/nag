import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { StatusBadgeComponent } from '@nag-frontend-workspace/shared-ui-components';
import { CatalogExam } from '../../models';

@Component({
  selector: 'nag-exam-details-drawer',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, StatusBadgeComponent],
  templateUrl: './exam-details-drawer.component.html',
  styleUrl: './exam-details-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamDetailsDrawerComponent {
  readonly exam = input.required<CatalogExam>();

  readonly closeDrawer = output<void>();
  readonly applyExam = output<CatalogExam>();

  @HostListener('window:keydown.escape')
  handleEscapeKey(): void {
    this.closeDrawer.emit();
  }

  downloadNotification(): void {
    // Generates a mock notification brochure download
    const blob = new Blob(
      [
        `========================================================================\n` +
        `GOVERNMENT OF INDIA - NATIONAL ASSESSMENT GRID (NAG)\n` +
        `OFFICIAL NOTIFICATION: ${this.exam().title} (${this.exam().code})\n` +
        `========================================================================\n\n` +
        `Conducting Authority: ${this.exam().conductingAuthority}\n` +
        `Academic/Recruitment Session: 2026\n` +
        `Exam Date: ${this.exam().examDate}\n` +
        `Application Deadline: ${this.exam().applicationDeadline}\n` +
        `Registration Fee: Rs. ${this.exam().feeAmount}\n` +
        `Duration: ${this.exam().durationMinutes} Minutes\n` +
        `Total Marks: ${this.exam().totalMarks}\n` +
        `Negative Marking: ${this.exam().negativeMarkingEnabled ? 'YES (' + this.exam().negativeMarkingValue + ')' : 'NO'}\n` +
        `Eligibility: ${this.exam().eligibility}\n` +
        `Total Estimated Seats: ${this.exam().totalSeats.toLocaleString()}\n` +
        `\nInstructions:\n` +
        `1. Candidates must verify identity via Aadhaar or DigiLocker before final registration.\n` +
        `2. Valid biometric or QR-signed Admit Card is required at the examination center.\n` +
        `3. Test centres adhere strictly to the National Assessment Grid DPI guidelines.\n`
      ],
      { type: 'text/plain;charset=utf-8' }
    );
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Notification-${this.exam().code}.txt`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
