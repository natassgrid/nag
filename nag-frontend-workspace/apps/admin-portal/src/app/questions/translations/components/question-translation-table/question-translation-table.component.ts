import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { Question } from '@nag-frontend-workspace/questions-data-access';

@Component({
  selector: 'nag-question-translation-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatPaginatorModule,
  ],
  templateUrl: './question-translation-table.component.html',
  styleUrl: './question-translation-table.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionTranslationTableComponent {
  questions = input<Question[]>([]);
  availableSubjects = input<string[]>([]);
  searchQuery = input<string>('');
  selectedSubject = input<string>('ALL');
  activeLanguageName = input<string>('');
  selectedLanguage = input<string>('hi');
  totalElements = input<number>(0);
  page = input<number>(0);
  pageSize = input<number>(20);
  pageSizeOptions = input<number[]>([10, 25, 50]);
  loading = input<boolean>(false);

  searchQueryChange = output<string>();
  selectedSubjectChange = output<string>();
  openTranslation = output<Question>();
  pageChange = output<{ pageIndex: number; pageSize: number }>();

  onPageChange(event: PageEvent): void {
    this.pageChange.emit({
      pageIndex: event.pageIndex,
      pageSize: event.pageSize,
    });
  }

  getQuestionTranslationStatus(q: Question): string {
    const lang = this.selectedLanguage();
    if (q.translationStatusMap && q.translationStatusMap[lang]) {
      return q.translationStatusMap[lang];
    }
    if (q.translationStatus && q.translationStatus !== 'MISSING') {
      return q.translationStatus;
    }
    return 'READY_FOR_AI';
  }

  getStatusBadgeClass(status: string): string {
    switch (status) {
      case 'PUBLISHED':
        return 'bg-emerald-50 text-emerald-700 border-emerald-200';
      case 'APPROVED':
        return 'bg-teal-50 text-teal-700 border-teal-200';
      case 'IN_REVIEW':
        return 'bg-violet-50 text-violet-700 border-violet-200';
      case 'DRAFT':
        return 'bg-sky-50 text-sky-700 border-sky-200';
      case 'REJECTED':
        return 'bg-rose-50 text-rose-700 border-rose-200';
      case 'STALE':
        return 'bg-orange-50 text-orange-700 border-orange-200';
      case 'READY_FOR_AI':
      case 'MISSING':
      default:
        return 'bg-amber-50 text-amber-700 border-amber-200';
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'PUBLISHED':
        return 'Published';
      case 'APPROVED':
        return 'Approved';
      case 'IN_REVIEW':
        return 'In Review';
      case 'DRAFT':
        return 'Draft';
      case 'REJECTED':
        return 'Rejected';
      case 'STALE':
        return 'Stale';
      case 'READY_FOR_AI':
      case 'MISSING':
      default:
        return 'Ready for AI';
    }
  }
}
