import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import {
  LearnerExamSummary,
  LearnerEnrollment,
} from '../../models/learner.model';

@Component({
  selector: 'app-exam-picker',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatExpansionModule,
  ],
  templateUrl: './exam-picker.component.html',
  styleUrl: './exam-picker.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamPickerComponent {
  readonly exams = input<LearnerExamSummary[]>([]);
  readonly activeEnrollment = input<LearnerEnrollment | null>(null);
  readonly isSwitchMode = input<boolean>(false);
  readonly selected = output<{ examId: string; targetDate?: string }>();

  readonly searchQuery = signal<string>('');
  readonly selectedCategory = signal<string>('ALL');
  readonly expandedExamId = signal<string | null>(null);

  readonly categories = computed<string[]>(() => {
    const list = this.exams() || [];
    const set = new Set<string>();
    list.forEach((e) => {
      if (e?.category) set.add(e.category);
    });
    return ['ALL', ...Array.from(set)];
  });

  readonly filteredExams = computed<LearnerExamSummary[]>(() => {
    const list = this.exams() || [];
    const query = (this.searchQuery() || '').toLowerCase().trim();
    const cat = this.selectedCategory();

    return list.filter((exam) => {
      if (!exam) return false;
      const matchesCategory = cat === 'ALL' || exam.category === cat;
      const matchesSearch =
        !query ||
        (exam.title && exam.title.toLowerCase().includes(query)) ||
        (exam.code && exam.code.toLowerCase().includes(query)) ||
        (exam.conductingAuthority &&
          exam.conductingAuthority.toLowerCase().includes(query));
      return matchesCategory && matchesSearch;
    });
  });

  toggleSyllabus(examId: string): void {
    this.expandedExamId.update((curr) => (curr === examId ? null : examId));
  }

  isExamActive(examId: string): boolean {
    const active = this.activeEnrollment();
    return !!active && active.examinationId === examId;
  }

  onSelectExam(examId: string): void {
    this.selected.emit({ examId });
  }
}
