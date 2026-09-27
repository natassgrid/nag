import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ExamSubmissionReceipt } from '../../models';

@Component({
  selector: 'nag-exam-submission-modal',
  standalone: true,
  imports: [CommonModule, RouterModule, MatButtonModule, MatIconModule],
  templateUrl: './exam-submission-modal.component.html',
  styleUrl: './exam-submission-modal.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamSubmissionModalComponent {
  readonly receipt = input.required<ExamSubmissionReceipt>();
  readonly close = output<void>();

  onClose(): void {
    this.close.emit();
  }
}
