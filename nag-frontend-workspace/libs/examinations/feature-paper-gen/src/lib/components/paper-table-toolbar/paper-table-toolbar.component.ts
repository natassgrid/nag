import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { ExaminationResponse } from '@nag-frontend-workspace/examinations-data-access';
import { PaperStatusFilter } from '../../models';

@Component({
  selector: 'nag-paper-table-toolbar',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule],
  templateUrl: './paper-table-toolbar.component.html',
  styleUrl: './paper-table-toolbar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaperTableToolbarComponent {
  readonly searchQuery = input<string>('');
  readonly selectedExamFilter = input<string>('ALL');
  readonly statusFilter = input<PaperStatusFilter>('ALL');
  readonly exams = input<ExaminationResponse[]>([]);

  readonly searchQueryChange = output<string>();
  readonly examFilterChange = output<string>();
  readonly statusFilterChange = output<PaperStatusFilter>();
}
