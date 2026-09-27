import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ExaminationResponse } from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-exam-grid-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './exam-grid-list.component.html',
  styleUrl: './exam-grid-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamGridListComponent {
  exams = input<ExaminationResponse[]>([]);
  loading = input<boolean>(false);
  hasSearchFilter = input<boolean>(false);

  editExam = output<ExaminationResponse>();
  publishExam = output<{ exam: ExaminationResponse; event: Event }>();
  createExam = output<void>();

  onEdit(exam: ExaminationResponse): void {
    this.editExam.emit(exam);
  }

  onPublish(exam: ExaminationResponse, event: Event): void {
    this.publishExam.emit({ exam, event });
  }

  onCreate(): void {
    this.createExam.emit();
  }
}
