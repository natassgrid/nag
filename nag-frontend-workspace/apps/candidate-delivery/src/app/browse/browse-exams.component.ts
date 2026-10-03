import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  CatalogExam,
  CatalogFilterState,
  PublicCentre,
  ApplicationReceipt,
} from './models';
import { CandidateBrowseService } from './services';
import {
  BrowseFilterBarComponent,
  ExamCatalogCardComponent,
  ExamDetailsDrawerComponent,
  ExamApplyDialogComponent,
} from './components';

export * from './models';
export * from './services';
export * from './components';

@Component({
  selector: 'app-browse-exams',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    BrowseFilterBarComponent,
    ExamCatalogCardComponent,
    ExamDetailsDrawerComponent,
    ExamApplyDialogComponent,
  ],
  templateUrl: './browse-exams.component.html',
  styleUrl: './browse-exams.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrowseExamsComponent implements OnInit {
  readonly browseService = inject(CandidateBrowseService);

  readonly filters = signal<CatalogFilterState>({
    searchQuery: '',
    category: 'ALL',
    statusFilter: 'ALL',
    sortBy: 'DATE_ASC',
  });

  readonly selectedDetailExam = signal<CatalogExam | null>(null);
  readonly selectedApplyExam = signal<CatalogExam | null>(null);

  readonly filteredExams = computed(() => {
    const list = this.browseService.catalog();
    const f = this.filters();
    const q = (f.searchQuery || '').toLowerCase().trim();

    const filtered = list.filter((exam) => {
      // Search filter
      const matchQuery =
        !q ||
        exam.title.toLowerCase().includes(q) ||
        exam.code.toLowerCase().includes(q) ||
        exam.conductingAuthority.toLowerCase().includes(q) ||
        exam.eligibility.toLowerCase().includes(q);

      // Category filter
      const matchCategory =
        f.category === 'ALL' ||
        exam.category.toUpperCase() === f.category.toUpperCase();

      // Status filter
      let matchStatus = true;
      if (f.statusFilter === 'OPEN') {
        matchStatus = !exam.applied && exam.status === 'OPEN';
      } else if (f.statusFilter === 'CLOSING_SOON') {
        matchStatus = exam.status === 'CLOSING_SOON';
      } else if (f.statusFilter === 'APPLIED') {
        matchStatus = exam.applied;
      } else if (f.statusFilter === 'PRACTICE') {
        matchStatus = !!exam.isPractice;
      }

      return matchQuery && matchCategory && matchStatus;
    });

    // Sorting
    return [...filtered].sort((a, b) => {
      switch (f.sortBy) {
        case 'DATE_ASC':
          return a.examDate.localeCompare(b.examDate);
        case 'FEE_ASC':
          return a.feeAmount - b.feeAmount;
        case 'FEE_DESC':
          return b.feeAmount - a.feeAmount;
        case 'TITLE_ASC':
          return a.title.localeCompare(b.title);
        default:
          return 0;
      }
    });
  });

  readonly totalMatches = computed(() => this.filteredExams().length);

  ngOnInit(): void {
    this.browseService.loadPublicCatalog().subscribe();
  }

  onSearchChange(query: string): void {
    this.filters.update((f) => ({ ...f, searchQuery: query }));
  }

  onCategoryChange(catId: string): void {
    this.filters.update((f) => ({ ...f, category: catId }));
  }

  onStatusChange(statusId: string): void {
    this.filters.update((f) => ({ ...f, statusFilter: statusId }));
  }

  onSortChange(sortBy: 'DATE_ASC' | 'FEE_ASC' | 'FEE_DESC' | 'TITLE_ASC'): void {
    this.filters.update((f) => ({ ...f, sortBy }));
  }

  resetFilters(): void {
    this.filters.set({
      searchQuery: '',
      category: 'ALL',
      statusFilter: 'ALL',
      sortBy: 'DATE_ASC',
    });
  }

  retryLoad(): void {
    this.browseService.loadPublicCatalog().subscribe();
  }

  openExamDetails(exam: CatalogExam): void {
    this.selectedDetailExam.set(exam);
  }

  closeExamDetails(): void {
    this.selectedDetailExam.set(null);
  }

  openDetails(exam: CatalogExam): void {
    this.openExamDetails(exam);
  }

  closeDetails(): void {
    this.closeExamDetails();
  }

  openApplyModal(exam: CatalogExam): void {
    this.selectedApplyExam.set(exam);
  }

  closeApplyModal(): void {
    this.selectedApplyExam.set(null);
  }

  onApplicationCompleted(receipt: ApplicationReceipt): void {
    this.handleApplicationSuccess(receipt);
  }

  handleApplicationSuccess(receipt: ApplicationReceipt): void {
    this.closeApplyModal();
    if (this.selectedDetailExam()?.id === receipt.examId) {
      this.selectedDetailExam.update((e) => (e ? { ...e, applied: true } : null));
    }
  }
}
