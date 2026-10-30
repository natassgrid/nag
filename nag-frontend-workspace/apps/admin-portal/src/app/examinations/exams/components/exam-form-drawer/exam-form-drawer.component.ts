import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
  inject,
  OnChanges,
  SimpleChanges,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import {
  ExaminationResponse,
  CreateExamRequest,
  ExamSection,
  ExamSyllabusNode,
  ExaminationService,
} from '@nag-frontend-workspace/examinations-data-access';
import {
  BlueprintTemplateService,
  TaxonomyStore,
} from '@nag-frontend-workspace/questions-data-access';
import { ExamFormGeneralComponent } from '../exam-form-general/exam-form-general.component';
import { ExamFormRulesComponent } from '../exam-form-rules/exam-form-rules.component';
import { ExamFormSectionsComponent } from '../exam-form-sections/exam-form-sections.component';
import { ExamFormSyllabusComponent } from '../exam-form-syllabus/exam-form-syllabus.component';

@Component({
  selector: 'nag-exam-form-drawer',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    ExamFormGeneralComponent,
    ExamFormRulesComponent,
    ExamFormSectionsComponent,
    ExamFormSyllabusComponent,
  ],
  templateUrl: './exam-form-drawer.component.html',
  styleUrl: './exam-form-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExamFormDrawerComponent implements OnChanges {
  private readonly blueprintService = inject(BlueprintTemplateService);
  private readonly taxonomyStore = inject(TaxonomyStore);
  private readonly examService = inject(ExaminationService);

  readonly isOpen = input<boolean>(false);
  readonly editingExam = input<ExaminationResponse | null>(null);
  readonly isSaving = input<boolean>(false);

  readonly closeDrawer = output<void>();
  readonly saveExam = output<CreateExamRequest>();

  readonly availableBlueprints = signal<Array<{ id: string; name: string; code?: string }>>([]);
  readonly taxonomyTree = this.taxonomyStore.tree;
  readonly taxonomySubjects = this.taxonomyStore.subjects;
  readonly validationError = signal<string | null>(null);

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
  formNavPolicy = 'FLEXIBLE';
  formCalcPolicy = 'NONE';
  formReviewFlag = true;
  formIsPractice = false;
  formBlueprintId = '';
  formSyllabusNodes: ExamSyllabusNode[] = [];
  formSections: ExamSection[] = [
    { name: 'Physics', questionCount: 25, marksPerQuestion: 4 },
    { name: 'Chemistry', questionCount: 25, marksPerQuestion: 4 },
    { name: 'Mathematics', questionCount: 25, marksPerQuestion: 4 },
  ];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen()) {
      this.loadBlueprints();
      if (this.taxonomyTree().length === 0) {
        this.taxonomyStore.loadTree().subscribe();
      }
    }

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
        this.formNavPolicy = this.normalizeNavPolicy(exam.navigationPolicy);
        this.formCalcPolicy = this.normalizeCalcPolicy(exam.calculatorPolicy);
        this.formReviewFlag = !!exam.reviewFlagEnabled;
        this.formIsPractice = !!exam.isPractice;
        this.formBlueprintId = exam.blueprintId || '';
        this.formSyllabusNodes = exam.syllabusNodes?.length
          ? JSON.parse(JSON.stringify(exam.syllabusNodes))
          : [];

        this.formSections = exam.sections?.length
          ? JSON.parse(JSON.stringify(exam.sections))
          : [{ name: 'General Section', questionCount: 50, marksPerQuestion: 2 }];

        // Load persisted syllabus nodes if not on root object
        if (exam.id && this.formSyllabusNodes.length === 0) {
          this.examService.getSyllabus(exam.id).subscribe({
            next: (s) => {
              if (s?.nodes && Array.isArray(s.nodes)) {
                this.formSyllabusNodes = s.nodes;
              }
            },
            error: () => {
              // Silently ignore if syllabus not yet saved
            },
          });
        }
      } else {
        this.resetForm();
      }
    }
  }

  loadBlueprints(): void {
    this.blueprintService.listTemplates().subscribe({
      next: (list) => {
        this.availableBlueprints.set(
          (list || []).map((b) => ({
            id: String(b.id),
            name: b.name,
            code: (b as any).code,
          }))
        );
      },
      error: () => {
        // Fallback or empty
      },
    });
  }

  onClose(): void {
    this.validationError.set(null);
    this.closeDrawer.emit();
  }

  addSection(): void {
    this.formSections = [
      ...this.formSections,
      {
        name: `Section ${this.formSections.length + 1}`,
        questionCount: 20,
        marksPerQuestion: 4,
      },
    ];
  }

  removeSection(index: number): void {
    if (this.formSections.length > 1) {
      this.formSections = this.formSections.filter((_, idx) => idx !== index);
    }
  }

  onSave(): void {
    this.validationError.set(null);
    if (!this.formName.trim() && !this.formCode.trim()) {
      this.validationError.set('Examination Name and Examination Code are required.');
      return;
    }
    if (!this.formName.trim()) {
      this.validationError.set('Examination Name is required.');
      return;
    }
    if (!this.formCode.trim()) {
      this.validationError.set('Examination Code is required.');
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
      blueprintId: this.formBlueprintId.trim() || undefined,
      sections: this.formSections,
      syllabusNodes: this.formSyllabusNodes,
    };

    this.saveExam.emit(payload);
  }

  private resetForm(): void {
    this.validationError.set(null);
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
    this.formNavPolicy = 'FLEXIBLE';
    this.formCalcPolicy = 'NONE';
    this.formReviewFlag = true;
    this.formIsPractice = false;
    this.formBlueprintId = '';
    this.formSyllabusNodes = [];
    this.formSections = [
      { name: 'Physics', questionCount: 25, marksPerQuestion: 4 },
      { name: 'Chemistry', questionCount: 25, marksPerQuestion: 4 },
      { name: 'Mathematics', questionCount: 25, marksPerQuestion: 4 },
    ];
  }

  private normalizeNavPolicy(policy?: string): string {
    if (!policy) return 'FLEXIBLE';
    const p = policy.toUpperCase();
    if (p === 'FREE_NAVIGATION' || p === 'FREE_FORWARD_BACKWARD' || p === 'FLEXIBLE') return 'FLEXIBLE';
    if (p === 'SECTION_LOCKED' || p === 'RESTRICTED' || p === 'LINEAR_SECTION_TIMED') return 'RESTRICTED';
    if (p === 'STRICT_SEQUENTIAL' || p === 'LINEAR_ONLY' || p === 'SEQUENTIAL') return 'SEQUENTIAL';
    return 'FLEXIBLE';
  }

  private normalizeCalcPolicy(policy?: string): string {
    if (!policy) return 'NONE';
    const p = policy.toUpperCase();
    if (p === 'NONE' || p === 'DISALLOWED' || p === 'NOT_PERMITTED') return 'NONE';
    if (p === 'BASIC' || p === 'VIRTUAL_BASIC' || p === 'STANDARD_VIRTUAL') return 'BASIC';
    if (p === 'SCIENTIFIC' || p === 'VIRTUAL_SCIENTIFIC' || p === 'SCIENTIFIC_VIRTUAL') return 'SCIENTIFIC';
    return 'NONE';
  }
}
