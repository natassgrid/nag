import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { Subject, SubjectTopicService, Subtopic, Topic } from '@nag-frontend-workspace/questions-data-access';
import { FormulaEntry, MicroLessonStep, QuickCheckItem, TrapAlert } from '../../../../models/micro-lesson.model';
import {
  DEFAULT_EXAM_OPTIONS, DEFAULT_FORMULA_ENTRIES, MICRO_LESSON_WIZARD_STEPS,
  MicroLessonWizardDialogData, MicroLessonWizardDialogResult, MicroLessonWizardState,
  SubjectTopicFormulaPolicy, TargetExamOption, resolveFormulaApplicability,
} from '../../models';
import { LessonMetadataBarComponent } from '../lesson-metadata-bar/lesson-metadata-bar.component';
import { CoreConceptEditorComponent } from '../core-concept-editor/core-concept-editor.component';
import { TrapAlertEditorComponent } from '../trap-alert-editor/trap-alert-editor.component';
import { FormulaSheetBuilderComponent } from '../formula-sheet-builder/formula-sheet-builder.component';
import { StepBreakdownBuilderComponent } from '../step-breakdown-builder/step-breakdown-builder.component';
import { QuickCheckBuilderComponent } from '../quick-check-builder/quick-check-builder.component';
import { LessonLivePreviewComponent } from '../lesson-live-preview/lesson-live-preview.component';

@Component({
  selector: 'app-micro-lesson-wizard-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatDialogModule,
    LessonMetadataBarComponent, CoreConceptEditorComponent, TrapAlertEditorComponent,
    FormulaSheetBuilderComponent, StepBreakdownBuilderComponent, QuickCheckBuilderComponent, LessonLivePreviewComponent,
  ],
  templateUrl: './micro-lesson-wizard-dialog.component.html',
  styleUrl: './micro-lesson-wizard-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonWizardDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<MicroLessonWizardDialogComponent, MicroLessonWizardDialogResult>);
  private readonly data = inject<MicroLessonWizardDialogData>(MAT_DIALOG_DATA);
  private readonly subjectTopicService = inject(SubjectTopicService);

  readonly stepsList = MICRO_LESSON_WIZARD_STEPS;
  readonly currentStep = signal<number>(this.data?.initialStep ?? 0);
  readonly showSidePreview = signal<boolean>(false);
  readonly validationError = signal<string | null>(null);
  readonly editingLessonId = signal<string | null>(this.data?.editingLessonId ?? null);
  readonly subjects = signal<Subject[]>(this.data?.subjects || []);
  readonly topics = signal<Topic[]>(this.data?.topics || []);
  readonly subtopics = signal<Subtopic[]>(this.data?.subtopics || []);
  readonly selectedSubjectId = signal<number | null>(this.data?.selectedSubjectId ?? null);
  readonly selectedTopicId = signal<number | null>(this.data?.selectedTopicId ?? null);
  readonly selectedSubtopicId = signal<number | null>(this.data?.selectedSubtopicId ?? null);
  readonly customSubtopicName = signal<string>(this.data?.customSubtopicName ?? '');
  readonly examOptions = signal<TargetExamOption[]>(this.data?.examOptions || DEFAULT_EXAM_OPTIONS);
  readonly targetExam = signal<string>(this.data?.targetExam || 'JEE_MAIN');
  readonly lessonType = signal<string>(this.data?.lessonType || 'CONCEPT');
  readonly difficultyLevel = signal<string>(this.data?.difficultyLevel || 'CORE');
  readonly sequenceNo = signal<number>(this.data?.sequenceNo ?? 1);
  readonly estimatedMinutes = signal<number>(this.data?.estimatedMinutes ?? 5);
  readonly status = signal<string>(this.data?.status || 'APPROVED');
  readonly coreConcept = signal<string>(this.data?.coreConcept ?? '');
  readonly trapAlert = signal<TrapAlert>({ ...(this.data?.trapAlert || { title: '', description: '', commonMistake: '', correctApproach: '' }) });
  readonly formulaEntries = signal<FormulaEntry[]>((this.data?.formulaEntries || []).map((f) => ({ ...f, variables: [...(f.variables || [])] })));
  readonly problemStatement = signal<string>(this.data?.problemStatement ?? '');
  readonly steps = signal<MicroLessonStep[]>((this.data?.steps || []).map((s) => ({ ...s })));
  readonly finalAnswer = signal<string>(this.data?.finalAnswer ?? '');
  readonly keyTakeaways = signal<string[]>([...(this.data?.keyTakeaways || [])]);
  readonly quickCheckItems = signal<QuickCheckItem[]>((this.data?.quickCheckItems || []).map((q) => ({ ...q, options: [...(q.options || [])] })));
  readonly reviewerNotes = signal<string>(this.data?.reviewerNotes ?? '');
  readonly isLastStep = computed(() => this.currentStep() >= this.stepsList.length - 1);
  readonly resolvedSubjectName = computed(() => this.subjects().find((s) => s.id === this.selectedSubjectId())?.name || 'General');
  readonly resolvedTopicName = computed(() => this.topics().find((t) => t.id === this.selectedTopicId())?.name || 'General');
  readonly resolvedSubtopicName = computed(() => this.customSubtopicName().trim() || this.subtopics().find((st) => st.id === this.selectedSubtopicId())?.name || 'General');
  readonly formulaPolicy = computed<SubjectTopicFormulaPolicy>(() =>
    resolveFormulaApplicability(this.resolvedSubjectName(), this.resolvedTopicName(), this.resolvedSubtopicName(), this.lessonType())
  );
  readonly isFormulaRequired = computed(() => this.formulaPolicy().required);

  goToStep(idx: number): void {
    if (idx >= 0 && idx < this.stepsList.length) {
      this.currentStep.set(idx);
      this.validationError.set(null);
    }
  }
  prevStep(): void { if (this.currentStep() > 0) this.goToStep(this.currentStep() - 1); }
  nextStep(): void { if (this.currentStep() < this.stepsList.length - 1) this.goToStep(this.currentStep() + 1); }

  onSubjectChange(subjectId: number): void {
    this.selectedSubjectId.set(subjectId);
    this.selectedTopicId.set(null);
    this.selectedSubtopicId.set(null);
    this.topics.set([]);
    this.subtopics.set([]);
    this.syncFormulaPolicy();
    if (subjectId) {
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (tops) => {
          this.topics.set(tops || []);
          if (tops?.length) this.onTopicChange(tops[0].id);
          else this.syncFormulaPolicy();
        },
      });
    }
  }

  onTopicChange(topicId: number): void {
    this.selectedTopicId.set(topicId);
    this.selectedSubtopicId.set(null);
    this.subtopics.set([]);
    this.syncFormulaPolicy();
    const subjectId = this.selectedSubjectId();
    if (subjectId && topicId) {
      this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({
        next: (subs) => {
          this.subtopics.set(subs || []);
          if (subs?.length) {
            this.selectedSubtopicId.set(subs[0].id);
            this.customSubtopicName.set(subs[0].name);
          }
          this.syncFormulaPolicy();
        },
      });
    }
  }

  onSubtopicChange(subtopicId: number | null): void {
    this.selectedSubtopicId.set(subtopicId);
    const found = this.subtopics().find((s) => s.id === subtopicId);
    if (found) this.customSubtopicName.set(found.name);
    this.syncFormulaPolicy();
  }

  onLessonTypeChange(nextType: string): void {
    this.lessonType.set(nextType);
    this.syncFormulaPolicy();
  }

  private syncFormulaPolicy(): void {
    const entries = this.formulaEntries() || [];
    const required = this.isFormulaRequired();
    const isBlank = entries.length === 1 && !(entries[0].latex || '').trim() && !(entries[0].description || '').trim();
    const isDefault = !this.editingLessonId() && entries.length === 1 && entries[0].latex === DEFAULT_FORMULA_ENTRIES[0].latex;
    if (required && entries.length === 0) {
      this.formulaEntries.set([{ name: 'Formula 1', latex: '', description: '', variables: [] }]);
    } else if (!required && (isBlank || isDefault)) {
      this.formulaEntries.set([]);
    }
  }

  createQuickSubject(name: string): void {
    this.subjectTopicService.createSubject({ name }).subscribe({
      next: (sub) => { this.subjects.update((list) => [...list, sub]); this.onSubjectChange(sub.id); },
    });
  }

  createQuickTopic(name: string): void {
    const subjId = this.selectedSubjectId();
    if (!subjId) return;
    this.subjectTopicService.createTopic(subjId, { name }).subscribe({
      next: (top) => { this.topics.update((list) => [...list, top]); this.onTopicChange(top.id); },
    });
  }

  switchToInPage(): void { this.dialogRef.close(this.buildResult('switch-to-inpage')); }
  closeWizard(): void { this.dialogRef.close(this.buildResult('close')); }

  saveFromWizard(): void {
    if (!this.coreConcept().trim()) {
      this.validationError.set('Core Concept explanation is required in Step 2.');
      this.currentStep.set(1);
      return;
    }
    const nonEmptyFormulas = (this.formulaEntries() || []).filter((f) => f && (f.latex || '').trim().length > 0);
    if (this.isFormulaRequired() && nonEmptyFormulas.length === 0) {
      this.validationError.set('Part 3 (KaTeX Formula Cheat Sheet) requires at least 1 formula for Mathematics, Science, and Numerical topics.');
      this.currentStep.set(3);
      return;
    }
    this.dialogRef.close(this.buildResult('save'));
  }

  private buildState(): MicroLessonWizardState {
    return {
      editingLessonId: this.editingLessonId(), selectedSubjectId: this.selectedSubjectId(),
      selectedTopicId: this.selectedTopicId(), selectedSubtopicId: this.selectedSubtopicId(),
      customSubtopicName: this.customSubtopicName(), targetExam: this.targetExam(),
      lessonType: this.lessonType(), difficultyLevel: this.difficultyLevel(),
      sequenceNo: this.sequenceNo(), estimatedMinutes: this.estimatedMinutes(),
      status: this.status(), coreConcept: this.coreConcept(), trapAlert: this.trapAlert(),
      formulaEntries: this.formulaEntries(), problemStatement: this.problemStatement(),
      steps: this.steps(), finalAnswer: this.finalAnswer(), keyTakeaways: this.keyTakeaways(),
      quickCheckItems: this.quickCheckItems(), reviewerNotes: this.reviewerNotes(),
    };
  }

  private buildResult(action: 'save' | 'switch-to-inpage' | 'close'): MicroLessonWizardDialogResult {
    return { action, state: this.buildState(), subjects: this.subjects(), topics: this.topics(), subtopics: this.subtopics() };
  }
}
