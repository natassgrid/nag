import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  ExaminationResponse,
  CreateExamRequest,
  ExamSection,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-exam-form-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatButtonModule],
  templateUrl: './exam-form-drawer.component.html',
  styleUrl: './exam-form-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamFormDrawerComponent implements OnChanges {
  isOpen = input<boolean>(false);
  editingExam = input<ExaminationResponse | null>(null);
  isSaving = input<boolean>(false);

  closeDrawer = output<void>();
  saveExam = output<CreateExamRequest>();

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

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] || changes['editingExam']) {
      const exam = this.editingExam();
      if (exam) {
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
      } else {
        this.resetForm();
      }
    }
  }

  onClose(): void {
    this.closeDrawer.emit();
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

  onSave(): void {
    if (!this.formName.trim()) return;

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

    this.saveExam.emit(payload);
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
