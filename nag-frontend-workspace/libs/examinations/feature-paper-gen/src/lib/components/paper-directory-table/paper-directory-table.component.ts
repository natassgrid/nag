import { ChangeDetectionStrategy, Component, input, output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PaperSummary, ExaminationResponse } from '@nag-frontend-workspace/examinations-data-access';
import { PaperStatusFilter } from '../../models';
import { PaperTableToolbarComponent } from '../paper-table-toolbar/paper-table-toolbar.component';

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
    PaperTableToolbarComponent,
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
  readonly bulkApprove = output<string[]>();
  readonly bulkPublish = output<string[]>();

  // Pagination State
  readonly pageIndex = signal<number>(0);
  readonly pageSize = signal<number>(10);
  readonly pageSizeOptions = [10, 20, 50];

  readonly totalPages = computed(() => {
    const total = (this.papers() || []).length;
    return Math.max(1, Math.ceil(total / this.pageSize()));
  });

  readonly paginatedPapers = computed(() => {
    const list = this.papers() || [];
    const start = this.pageIndex() * this.pageSize();
    return list.slice(start, start + this.pageSize());
  });

  readonly displayStart = computed(() => {
    const total = (this.papers() || []).length;
    return total === 0 ? 0 : this.pageIndex() * this.pageSize() + 1;
  });

  readonly displayEnd = computed(() => {
    const total = (this.papers() || []).length;
    return Math.min((this.pageIndex() + 1) * this.pageSize(), total);
  });

  // Row Selection State
  readonly selectedIds = signal<Set<string>>(new Set<string>());

  getPaperKey(paper: PaperSummary): string {
    return paper.paperId || paper.id || '';
  }

  readonly isAllSelected = computed(() => {
    const items = this.paginatedPapers();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    return items.every((p) => ids.has(this.getPaperKey(p)));
  });

  readonly isPartiallySelected = computed(() => {
    const items = this.paginatedPapers();
    if (items.length === 0) return false;
    const ids = this.selectedIds();
    const count = items.filter((p) => ids.has(this.getPaperKey(p))).length;
    return count > 0 && count < items.length;
  });

  toggleSelectAll(): void {
    const items = this.paginatedPapers();
    const current = new Set(this.selectedIds());
    if (this.isAllSelected()) {
      for (const item of items) current.delete(this.getPaperKey(item));
    } else {
      for (const item of items) {
        const key = this.getPaperKey(item);
        if (key) current.add(key);
      }
    }
    this.selectedIds.set(current);
  }

  toggleSelect(id: string, event: Event): void {
    event.stopPropagation();
    if (!id) return;
    const current = new Set(this.selectedIds());
    if (current.has(id)) current.delete(id);
    else current.add(id);
    this.selectedIds.set(current);
  }

  deselectAll(): void {
    this.selectedIds.set(new Set<string>());
  }

  onBulkApprove(): void {
    this.bulkApprove.emit(Array.from(this.selectedIds()));
    this.deselectAll();
  }

  onBulkPublish(): void {
    this.bulkPublish.emit(Array.from(this.selectedIds()));
    this.deselectAll();
  }

  exportSelectedCsv(): void {
    const selected = (this.papers() || []).filter((p) =>
      this.selectedIds().has(this.getPaperKey(p))
    );
    if (selected.length === 0) return;

    const headers = [
      'Paper ID', 'Paper Name', 'Examination', 'Shift',
      'Questions', 'Marks', 'Difficulty Score', 'Status', 'Practice'
    ];
    const rows = selected.map((p) => [
      `"${this.getPaperKey(p)}"`,
      `"${(p.name || '').replace(/"/g, '""')}"`,
      `"${(p.examName || this.getExamName(p.examId) || '').replace(/"/g, '""')}"`,
      `"${(p.shiftName || '').replace(/"/g, '""')}"`,
      `"${p.totalQuestions ?? ''}"`,
      `"${p.totalMarks ?? ''}"`,
      `"${p.difficultyScore ?? ''}"`,
      `"${p.status || ''}"`,
      `"${p.isPractice ? 'Yes' : 'No'}"`,
    ]);

    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n');
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `papers-export-${new Date().toISOString().slice(0, 10)}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  goToFirstPage(): void { this.pageIndex.set(0); }
  goToPreviousPage(): void { this.pageIndex.update((p) => Math.max(0, p - 1)); }
  goToNextPage(): void { this.pageIndex.update((p) => Math.min(this.totalPages() - 1, p + 1)); }
  goToLastPage(): void { this.pageIndex.set(this.totalPages() - 1); }

  onPageSizeChange(newSize: string | number): void {
    this.pageSize.set(Number(newSize));
    this.pageIndex.set(0);
  }

  getExamName(examId?: string): string {
    if (!examId) return 'Target Examination';
    const exam = (this.exams() || []).find((e) => e?.id === examId);
    return exam ? exam.name : 'Target Examination';
  }
}
