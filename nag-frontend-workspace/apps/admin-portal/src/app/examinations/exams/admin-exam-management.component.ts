import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  ExaminationService,
  ExaminationResponse,
  CreateExamRequest,
} from '@nag-frontend-workspace/examinations-data-access';
import {
  ExamKpiCardsComponent,
  ExamGridListComponent,
  ExamFormDrawerComponent,
} from './components';

@Component({
  selector: 'nag-admin-exam-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    ExamKpiCardsComponent,
    ExamGridListComponent,
    ExamFormDrawerComponent,
  ],
  templateUrl: './admin-exam-management.component.html',
  styleUrls: ['./admin-exam-management.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminExamManagementComponent implements OnInit {
  private readonly examService = inject(ExaminationService);
  private readonly snackBar = inject(MatSnackBar);

  readonly exams = this.examService.exams;
  readonly loading = this.examService.loading;

  // Search & Filter
  readonly searchQuery = signal<string>('');
  readonly statusFilter = signal<string>('ALL');

  // Drawer / Form State
  readonly drawerOpen = signal<boolean>(false);
  readonly editingExam = signal<ExaminationResponse | null>(null);
  readonly isSaving = signal<boolean>(false);

  // Computed KPIs
  readonly totalExamsCount = computed(() => (this.exams() || []).length);
  readonly publishedExamsCount = computed(
    () => (this.exams() || []).filter((e) => e?.status === 'PUBLISHED').length
  );
  readonly draftExamsCount = computed(
    () => (this.exams() || []).filter((e) => e?.status === 'DRAFT').length
  );

  readonly filteredExams = computed(() => {
    const list = this.exams() || [];
    const q = this.searchQuery().toLowerCase().trim();
    const st = this.statusFilter();

    return list.filter((exam) => {
      if (!exam) return false;
      const matchSearch =
        !q ||
        (exam.name && exam.name.toLowerCase().includes(q)) ||
        (exam.code && exam.code.toLowerCase().includes(q)) ||
        (exam.conductingAuthority &&
          exam.conductingAuthority.toLowerCase().includes(q));

      const matchStatus = st === 'ALL' || exam.status === st;

      return matchSearch && matchStatus;
    });
  });

  ngOnInit(): void {
    this.loadExams();
  }

  loadExams(): void {
    this.examService.getExams(0, 50).subscribe({
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to retrieve examinations',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  openCreate(): void {
    this.editingExam.set(null);
    this.drawerOpen.set(true);
  }

  openEdit(exam: ExaminationResponse): void {
    this.editingExam.set(exam);
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.editingExam.set(null);
  }

  handleSaveExam(payload: CreateExamRequest): void {
    this.isSaving.set(true);
    const existing = this.editingExam();

    if (existing) {
      this.examService.updateExam(existing.id, payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeDrawer();
          this.snackBar.open('Examination updated successfully', 'OK', { duration: 3000 });
        },
        error: (err) => {
          this.isSaving.set(false);
          this.snackBar.open(
            err?.error?.message || 'Failed to update examination',
            'Dismiss',
            { duration: 4000 }
          );
        },
      });
    } else {
      this.examService.createExam(payload).subscribe({
        next: () => {
          this.isSaving.set(false);
          this.closeDrawer();
          this.snackBar.open('Examination created successfully', 'OK', { duration: 3000 });
        },
        error: (err) => {
          this.isSaving.set(false);
          this.snackBar.open(
            err?.error?.message || 'Failed to create examination',
            'Dismiss',
            { duration: 4000 }
          );
        },
      });
    }
  }

  handlePublishExam(eventData: { exam: ExaminationResponse; event: Event }): void {
    const { exam, event } = eventData;
    event.stopPropagation();
    if (exam.status === 'PUBLISHED') return;

    this.examService.publishExam(exam.id).subscribe({
      next: () => {
        this.snackBar.open(`Examination "${exam.name}" is now PUBLISHED!`, 'OK', {
          duration: 3000,
        });
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to publish examination',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }
}
