import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  SubjectMastery,
  SubtopicMastery,
  SubtopicMasteryStatus,
} from '../../models/learner.model';

@Component({
  selector: 'app-syllabus-mastery-tree',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressBarModule,
  ],
  templateUrl: './syllabus-mastery-tree.component.html',
  styleUrl: './syllabus-mastery-tree.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SyllabusMasteryTreeComponent {
  readonly syllabus = input<SubjectMastery[]>([]);
  readonly activeSubtopicId = input<number | null>(null);

  readonly subtopicSelected = output<SubtopicMastery>();
  readonly practiceRequested = output<SubtopicMastery>();

  readonly expandedSubjectIds = signal<Set<number>>(new Set([101, 104, 106]));
  readonly filterStatus = signal<string>('ALL');

  toggleSubject(subjectId: number): void {
    const set = new Set(this.expandedSubjectIds());
    if (set.has(subjectId)) {
      set.delete(subjectId);
    } else {
      set.add(subjectId);
    }
    this.expandedSubjectIds.set(set);
  }

  isSubjectExpanded(subjectId: number): boolean {
    return this.expandedSubjectIds().has(subjectId);
  }

  getStatusBadgeClass(status: SubtopicMasteryStatus): string {
    switch (status) {
      case 'MASTERED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'PRACTISING':
        return 'bg-amber-50 text-amber-700 border-amber-200';
      case 'LEARNING':
        return 'bg-indigo-50 text-indigo-700 border-indigo-200';
      case 'REVIEW_DUE':
        return 'bg-purple-50 text-purple-700 border-purple-200 animate-pulse';
      case 'NOT_STARTED':
      default:
        return 'bg-slate-100 text-slate-600 border-slate-200';
    }
  }

  getStatusIcon(status: SubtopicMasteryStatus): string {
    switch (status) {
      case 'MASTERED':
        return 'verified';
      case 'PRACTISING':
        return 'quiz';
      case 'LEARNING':
        return 'menu_book';
      case 'REVIEW_DUE':
        return 'notifications_active';
      case 'NOT_STARTED':
      default:
        return 'radio_button_unchecked';
    }
  }

  onSelectSubtopic(subtopic: SubtopicMastery): void {
    this.subtopicSelected.emit(subtopic);
  }

  onPractice(subtopic: SubtopicMastery, event: Event): void {
    event.stopPropagation();
    this.practiceRequested.emit(subtopic);
  }
}
