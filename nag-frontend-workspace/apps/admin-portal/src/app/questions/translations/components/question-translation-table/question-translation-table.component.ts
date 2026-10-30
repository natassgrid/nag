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
  bulkTranslate = output<Question[]>();

  readonly selectedIds = signal<Set<string>>(new Set<string>());

  readonly isAllSelected = computed(() => {
    const list = this.questions() || [];
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    return list.every((q) => q && selected.has(q.id));
  });

  readonly isPartiallySelected = computed(() => {
    const list = this.questions() || [];
    if (list.length === 0) return false;
    const selected = this.selectedIds();
    const count = list.filter((q) => q && selected.has(q.id)).length;
    return count > 0 && count < list.length;
  });

  readonly selectedQuestions = computed(() => {
    const selected = this.selectedIds();
    const list = this.questions() || [];
    return list.filter((q) => q && selected.has(q.id));
  });

  isSelected(id: string): boolean {
    return this.selectedIds().has(id);
  }

  toggleSelect(id: string): void {
    this.selectedIds.update((set) => {
      const next = new Set(set);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  }

  toggleSelectAll(): void {
    const list = this.questions() || [];
    if (this.isAllSelected()) {
      this.selectedIds.update((set) => {
        const next = new Set(set);
        for (const q of list) {
          if (q) next.delete(q.id);
        }
        return next;
      });
    } else {
      this.selectedIds.update((set) => {
        const next = new Set(set);
        for (const q of list) {
          if (q) next.add(q.id);
        }
        return next;
      });
    }
  }

  deselectAll(): void {
    this.selectedIds.set(new Set<string>());
  }

  onBulkTranslate(): void {
    const selected = this.selectedQuestions();
    if (selected.length > 0) {
      this.bulkTranslate.emit(selected);
    }
  }

  exportSelectedCsv(): void {
    const selected = this.selectedQuestions();
    if (selected.length === 0) return;

    const escapeCsv = (str: string | null | undefined): string => {
      if (!str) return '""';
      return `"${str.replace(/"/g, '""')}"`;
    };

    const lang = this.selectedLanguage();
    const headers = ['ID', 'Code', 'Content', 'Subject', 'Topic', 'Difficulty', 'Target Language', 'Status'];
    const rows = selected.map((q) => [
      escapeCsv(q.id),
      escapeCsv(q.code || ''),
      escapeCsv(q.content || ''),
      escapeCsv(q.subject || ''),
      escapeCsv(q.topic || ''),
      escapeCsv(q.difficulty || ''),
      escapeCsv(lang),
      escapeCsv(this.getQuestionTranslationStatus(q)),
    ].join(','));

    const csvContent = [headers.join(','), ...rows].join('\r\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `translations_${lang}_${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  }

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
