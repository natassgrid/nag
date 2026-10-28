import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import {
  DifficultyLevel, QuestionOption, QuestionType, Subject, SubjectTopicService, Subtopic, Topic,
} from '@nag-frontend-workspace/questions-data-access';
import { MathRendererComponent } from '@nag-frontend-workspace/shared-ui-components';
import {
  AuthoringMode, AuthoringSubQuestion, PASSAGE_WIZARD_STEPS,
  QuestionWizardDialogData, QuestionWizardDialogResult, QuestionWizardState, STANDALONE_WIZARD_STEPS,
} from '../../models/authoring.model';
import { TaxonomySelectorComponent } from '../taxonomy-selector/taxonomy-selector.component';
import { StandaloneTaxonomyScoringComponent } from '../standalone-taxonomy-scoring/standalone-taxonomy-scoring.component';
import { StandaloneOptionsEditorComponent } from '../standalone-options-editor/standalone-options-editor.component';
import { StandaloneLivePreviewComponent } from '../standalone-live-preview/standalone-live-preview.component';
import { FormulaSymbolPaletteComponent } from '../formula-symbol-palette/formula-symbol-palette.component';
import { PassageTextBoxComponent } from '../passage-text-box/passage-text-box.component';
import { PassageSubquestionEditorComponent } from '../passage-subquestion-editor/passage-subquestion-editor.component';
import { PassagePreviewViewportComponent } from '../passage-preview-viewport/passage-preview-viewport.component';

@Component({
  selector: 'nag-question-authoring-wizard-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatDialogModule, MathRendererComponent,
    TaxonomySelectorComponent, StandaloneTaxonomyScoringComponent, StandaloneOptionsEditorComponent,
    StandaloneLivePreviewComponent, FormulaSymbolPaletteComponent, PassageTextBoxComponent,
    PassageSubquestionEditorComponent, PassagePreviewViewportComponent,
  ],
  templateUrl: './question-authoring-wizard-dialog.component.html',
  styleUrl: './question-authoring-wizard-dialog.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QuestionAuthoringWizardDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<QuestionAuthoringWizardDialogComponent, QuestionWizardDialogResult>);
  private readonly data = inject<QuestionWizardDialogData>(MAT_DIALOG_DATA);
  private readonly subjectTopicService = inject(SubjectTopicService);

  readonly mode = signal<AuthoringMode>(this.data?.mode || 'STANDALONE');
  readonly editingQuestionId = signal<string | null>(this.data?.editingQuestionId ?? null);
  readonly currentStep = signal<number>(this.data?.initialStep ?? 0);
  readonly showSidePreview = signal<boolean>(false);
  readonly validationError = signal<string | null>(null);
  readonly showFormulaPalette = signal<boolean>(false);
  readonly showExplanationPalette = signal<boolean>(false);

  readonly subjects = signal<Subject[]>(this.data?.subjects || []);
  readonly topics = signal<Topic[]>(this.data?.topics || []);
  readonly subtopics = signal<Subtopic[]>(this.data?.subtopics || []);
  readonly selectedSubjectId = signal<number | null>(this.data?.selectedSubjectId ?? null);
  readonly selectedTopicId = signal<number | null>(this.data?.selectedTopicId ?? null);
  readonly selectedSubtopicId = signal<number | null>(this.data?.selectedSubtopicId ?? null);

  readonly cognitiveLevel = signal<string>(this.data?.cognitiveLevel || 'UNDERSTAND');
  readonly type = signal<QuestionType>(this.data?.type || 'MULTIPLE_CHOICE');
  readonly difficulty = signal<DifficultyLevel>(this.data?.difficulty || 'MEDIUM');
  readonly marks = signal<number>(this.data?.marks ?? 4);
  readonly negativeMarks = signal<number>(this.data?.negativeMarks ?? 1);
  readonly content = signal<string>(this.data?.content ?? '');
  readonly explanation = signal<string>(this.data?.explanation ?? '');
  readonly options = signal<QuestionOption[]>((this.data?.options || []).map((o) => ({ ...o })));

  readonly passageTitle = signal<string>(this.data?.passageTitle ?? '');
  readonly passageContent = signal<string>(this.data?.passageContent ?? '');
  readonly subQuestions = signal<AuthoringSubQuestion[]>(
    (this.data?.subQuestions || []).map((sq) => ({ ...sq, options: (sq.options || []).map((o) => ({ ...o })) }))
  );
  readonly activeSubQuestionIndex = signal<number>(this.data?.activeSubQuestionIndex ?? 0);

  readonly steps = computed(() => (this.mode() === 'STANDALONE' ? STANDALONE_WIZARD_STEPS : PASSAGE_WIZARD_STEPS));
  readonly isLastStep = computed(() => this.currentStep() >= this.steps().length - 1);

  setMode(nextMode: AuthoringMode): void {
    this.mode.set(nextMode);
    this.currentStep.set(0);
    this.validationError.set(null);
  }

  goToStep(idx: number): void {
    if (idx >= 0 && idx < this.steps().length) {
      this.currentStep.set(idx);
      this.validationError.set(null);
    }
  }

  prevStep(): void {
    if (this.currentStep() > 0) this.goToStep(this.currentStep() - 1);
  }

  nextStep(): void {
    if (this.currentStep() < this.steps().length - 1) this.goToStep(this.currentStep() + 1);
  }

  onSubjectChange(subjectId: number): void {
    this.selectedSubjectId.set(subjectId);
    this.selectedTopicId.set(null);
    this.selectedSubtopicId.set(null);
    this.topics.set([]);
    this.subtopics.set([]);
    if (subjectId) {
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (tops) => {
          this.topics.set(tops || []);
          if (tops?.length) this.onTopicChange(tops[0].id);
        },
      });
    }
  }

  onTopicChange(topicId: number): void {
    this.selectedTopicId.set(topicId);
    this.selectedSubtopicId.set(null);
    this.subtopics.set([]);
    const subjId = this.selectedSubjectId();
    if (subjId && topicId) {
      this.subjectTopicService.getSubtopics(subjId, topicId).subscribe({
        next: (subs) => {
          this.subtopics.set(subs || []);
          if (subs?.length) this.selectedSubtopicId.set(subs[0].id);
        },
      });
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

  insertSymbolIntoContent(symbol: string): void {
    const cur = this.content();
    this.content.set(cur ? `${cur} ${symbol}` : symbol);
  }

  insertSymbolIntoExplanation(symbol: string): void {
    const cur = this.explanation();
    this.explanation.set(cur ? `${cur} ${symbol}` : symbol);
  }

  switchToInPage(): void {
    this.dialogRef.close(this.buildResult('switch-to-inpage'));
  }

  closeWizard(): void {
    this.dialogRef.close(this.buildResult('close'));
  }

  saveFromWizard(): void {
    if (!this.selectedSubjectId() || !this.selectedTopicId() || !this.selectedSubtopicId()) {
      this.validationError.set('Please select Subject, Topic, and Subtopic in Step 1 before saving.');
      this.currentStep.set(0);
      return;
    }
    if (this.mode() === 'STANDALONE' && !this.content().trim()) {
      this.validationError.set('Question problem statement cannot be empty.');
      this.currentStep.set(1);
      return;
    }
    if (this.mode() === 'PASSAGE' && !this.passageContent().trim()) {
      this.validationError.set('Passage reading content cannot be empty.');
      this.currentStep.set(0);
      return;
    }
    this.dialogRef.close(this.buildResult('save'));
  }

  private buildState(): QuestionWizardState {
    return {
      mode: this.mode(), editingQuestionId: this.editingQuestionId(),
      selectedSubjectId: this.selectedSubjectId(), selectedTopicId: this.selectedTopicId(),
      selectedSubtopicId: this.selectedSubtopicId(), cognitiveLevel: this.cognitiveLevel(),
      type: this.type(), difficulty: this.difficulty(), marks: this.marks(),
      negativeMarks: this.negativeMarks(), content: this.content(), explanation: this.explanation(),
      options: this.options(), passageTitle: this.passageTitle(), passageContent: this.passageContent(),
      subQuestions: this.subQuestions(), activeSubQuestionIndex: this.activeSubQuestionIndex(),
    };
  }

  private buildResult(action: 'save' | 'switch-to-inpage' | 'close'): QuestionWizardDialogResult {
    return { action, state: this.buildState(), subjects: this.subjects(), topics: this.topics(), subtopics: this.subtopics() };
  }
}
