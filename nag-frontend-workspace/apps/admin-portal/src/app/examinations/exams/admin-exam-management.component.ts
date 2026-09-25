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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  ExaminationService,
  ExaminationResponse,
  CreateExamRequest,
  ExamSection,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-admin-exam-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
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

  // Form Fields
  formName = '';
  formCode = '';
  formAuthority = '';
  formCategory = '';
  formType = 'STANDARDIZED';
  formAcademicYear = '2025-2026';
  formMode = 'COMPUTER_BASED_TEST';
  formDuration = 180;
  formTotalMarks = 300;
  formNegMarking = true;
  formNegMarkingValue = 1.0;
  formNavPolicy = 'FREE_NAVIGATION';
  formCalcPolicy = 'VIRTUAL_SCIENTIFIC';
  formReviewFlag = true;
  formIsPractice = false;
  formSections: ExamSection[] = [
    { name: 'Physics', questionCount: 25, marksPerQuestion: 4 },
    { name: 'Chemistry', questionCount: 25, marksPerQuestion: 4 },
    { name: 'Mathematics', questionCount: 25, marksPerQuestion: 4 },
  ];

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
    this.resetForm();
    this.drawerOpen.set(true);
  }

  openEdit(exam: ExaminationResponse): void {
    this.editingExam.set(exam);
    this.formName = exam.name || '';
    this.formCode = exam.code || '';
    this.formAuthority = exam.conductingAuthority || '';
    this.formCategory = exam.category || '';
    this.formType = exam.examinationType || 'STANDARDIZED';
    this.formAcademicYear = exam.academicYear || '2025-2026';
    this.formMode = exam.examinationMode || 'COMPUTER_BASED_TEST';
    this.formDuration = exam.durationMinutes || 180;
    this.formTotalMarks = exam.totalMarks || 300;
    this.formNegMarking = !!exam.negativeMarkingEnabled;
    this.formNegMarkingValue = exam.negativeMarkingValue || 0;
    this.formNavPolicy = exam.navigationPolicy || 'FREE_NAVIGATION';
    this.formCalcPolicy = exam.calculatorPolicy || 'NONE';
    this.formReviewFlag = !!exam.reviewFlagEnabled;
    this.formIsPractice = !!exam.isPractice;
    this.formSections = exam.sections?.length
      ? JSON.parse(JSON.stringify(exam.sections))
      : [{ name: 'General Section', questionCount: 50, marksPerQuestion: 2 }];
    this.drawerOpen.set(true);
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
    this.editingExam.set(null);
  }

  addSection(): void {
    this.formSections.push({
      name: `Section ${this.formSections.length + 1}`,
      questionCount: 20,
      marksPerQuestion: 4,
    });
  }

  removeSection(index: number): void {
    if (this.formSections.length > 1) {
      this.formSections.splice(index, 1);
    }
  }

  saveExam(): void {
    if (!this.formName.trim()) {
      this.snackBar.open('Examination Name is required', 'Dismiss', { duration: 3000 });
      return;
    }

    const payload: CreateExamRequest = {
      name: this.formName.trim(),
      code: this.formCode.trim() || undefined,
      conductingAuthority: this.formAuthority.trim() || undefined,
      category: this.formCategory.trim() || undefined,
      examinationType: this.formType,
      academicYear: this.formAcademicYear,
      examinationMode: this.formMode,
      durationMinutes: this.formDuration,
      totalMarks: this.formTotalMarks,
      negativeMarkingEnabled: this.formNegMarking,
      negativeMarkingValue: this.formNegMarking ? this.formNegMarkingValue : 0,
      navigationPolicy: this.formNavPolicy,
      calculatorPolicy: this.formCalcPolicy,
      reviewFlagEnabled: this.formReviewFlag,
      isPractice: this.formIsPractice,
      sections: this.formSections,
    };

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

  publishExam(exam: ExaminationResponse, event: Event): void {
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

  private resetForm(): void {
    this.formName = '';
    this.formCode = '';
    this.formAuthority = 'National Assessment Agency';
    this.formCategory = 'Entrance Examination';
    this.formType = 'STANDARDIZED';
    this.formAcademicYear = '2025-2026';
    this.formMode = 'COMPUTER_BASED_TEST';
    this.formDuration = 180;
    this.formTotalMarks = 300;
    this.formNegMarking = true;
    this.formNegMarkingValue = 1.0;
    this.formNavPolicy = 'FREE_NAVIGATION';
    this.formCalcPolicy = 'VIRTUAL_SCIENTIFIC';
    this.formReviewFlag = true;
    this.formIsPractice = false;
    this.formSections = [
      { name: 'Physics', questionCount: 25, marksPerQuestion: 4 },
      { name: 'Chemistry', questionCount: 25, marksPerQuestion: 4 },
      { name: 'Mathematics', questionCount: 25, marksPerQuestion: 4 },
    ];
  }
}
