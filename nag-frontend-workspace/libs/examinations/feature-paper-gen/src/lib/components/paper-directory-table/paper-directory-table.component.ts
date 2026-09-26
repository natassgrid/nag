import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  PaperSummary,
  ExaminationResponse,
} from '@nag-frontend-workspace/examinations-data-access';
import { PaperStatusFilter } from '../../models';

@Component({
  selector: 'nag-paper-directory-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './paper-directory-table.component.html',
  styleUrl: './paper-directory-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperDirectoryTableComponent {
  readonly papers = input<PaperSummary[]>([]);
  readonly exams = input<ExaminationResponse[]>([]);
  readonly loading = input<boolean>(false);
  readonly searchQuery = input<string>('');
  readonly selectedExamFilter = input<string>('ALL');
  readonly statusFilter = input<PaperStatusFilter>('ALL');

  readonly searchQueryChange = output<string>();
  readonly examFilterChange = output<string>();
  readonly statusFilterChange = output<PaperStatusFilter>();
  readonly inspectPaper = output<string>();
  readonly approvePaper = output<string>();
  readonly publishPaper = output<string>();
  readonly assembleNew = output<void>();

  getExamName(examId?: string): string {
    if (!examId) return 'Target Examination';
    const exam = this.exams().find((e) => e.id === examId);
    return exam ? exam.name : 'Target Examination';
  }
}
