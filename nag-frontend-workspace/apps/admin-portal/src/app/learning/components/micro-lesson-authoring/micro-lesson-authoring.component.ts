import { Component, ChangeDetectionStrategy, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import { Subject, SubjectTopicService, Subtopic, Topic } from '@nag-frontend-workspace/questions-data-access';
import { MicroLessonAdminService } from '../../services/micro-lesson-admin.service';
import { FormulaEntry, MicroLessonStep, QuickCheckItem, TrapAlert } from '../../models/micro-lesson.model';
import {
  DEFAULT_CORE_CONCEPT, DEFAULT_EXAM_OPTIONS, DEFAULT_FINAL_ANSWER, DEFAULT_FORMULA_ENTRIES,
  DEFAULT_KEY_TAKEAWAYS, DEFAULT_PROBLEM_STATEMENT, DEFAULT_QUICK_CHECKS,
  DEFAULT_STEP_ITEMS, DEFAULT_TRAP_ALERT, TargetExamOption, SubjectTopicFormulaPolicy,
  buildCreateOrUpdateRequest, parseLessonForAuthoring, ParsedAuthoringLessonState,
  resolveFormulaApplicability, MicroLessonWizardDialogResult,
} from './models';
import {
  CoreConceptEditorComponent, FormulaSheetBuilderComponent, LessonLivePreviewComponent,
  LessonMetadataBarComponent, QuickCheckBuilderComponent, StepBreakdownBuilderComponent,
  TrapAlertEditorComponent, MicroLessonWizardDialogComponent,
} from './components';
import {
  MicroLessonPromptImportDialogComponent,
  MicroLessonPromptImportDialogResult,
  MicroLessonPromptImportTaxonomySelection,
} from '../micro-lesson-prompt-import-dialog/micro-lesson-prompt-import-dialog.component';

@Component({
  selector: 'app-micro-lesson-authoring',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule, MatButtonModule, MatIconModule, MatDialogModule, PageHeaderComponent,
    LessonMetadataBarComponent, CoreConceptEditorComponent, TrapAlertEditorComponent,
    FormulaSheetBuilderComponent, StepBreakdownBuilderComponent, QuickCheckBuilderComponent, LessonLivePreviewComponent,
  ],
  templateUrl: './micro-lesson-authoring.component.html',
  styleUrl: './micro-lesson-authoring.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MicroLessonAuthoringComponent implements OnInit {
  private readonly microLessonService = inject(MicroLessonAdminService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  readonly editingLessonId = signal<string | null>(null);
  readonly loadingLesson = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  readonly subjects = signal<Subject[]>([]);
  readonly topics = signal<Topic[]>([]);
  readonly subtopics = signal<Subtopic[]>([]);
  readonly selectedSubjectId = signal<number | null>(null);
  readonly selectedTopicId = signal<number | null>(null);
  readonly selectedSubtopicId = signal<number | null>(null);
  readonly customSubtopicName = signal<string>('');

  readonly examOptions = signal<TargetExamOption[]>(DEFAULT_EXAM_OPTIONS);
  readonly targetExam = signal<string>('JEE_MAIN');
  readonly lessonType = signal<string>('CONCEPT');
  readonly difficultyLevel = signal<string>('CORE');
  readonly sequenceNo = signal<number>(1);
  readonly estimatedMinutes = signal<number>(5);
  readonly status = signal<string>('APPROVED');

  readonly coreConcept = signal<string>(DEFAULT_CORE_CONCEPT);
  readonly trapAlert = signal<TrapAlert>({ ...DEFAULT_TRAP_ALERT });
  readonly formulaEntries = signal<FormulaEntry[]>([...DEFAULT_FORMULA_ENTRIES]);
  readonly problemStatement = signal<string>(DEFAULT_PROBLEM_STATEMENT);
  readonly steps = signal<MicroLessonStep[]>([...DEFAULT_STEP_ITEMS]);
  readonly finalAnswer = signal<string>(DEFAULT_FINAL_ANSWER);
  readonly keyTakeaways = signal<string[]>([...DEFAULT_KEY_TAKEAWAYS]);
  readonly quickCheckItems = signal<QuickCheckItem[]>([...DEFAULT_QUICK_CHECKS]);
  readonly reviewerNotes = signal<string>('');

  readonly resolvedSubjectName = computed(() => this.subjects().find((s) => s.id === this.selectedSubjectId())?.name || 'General');
  readonly resolvedTopicName = computed(() => this.topics().find((t) => t.id === this.selectedTopicId())?.name || 'General');
  readonly resolvedSubtopicName = computed(() => {
    const custom = this.customSubtopicName().trim();
    return custom || this.subtopics().find((st) => st.id === this.selectedSubtopicId())?.name || 'General';
  });

  readonly formulaPolicy = computed<SubjectTopicFormulaPolicy>(() =>
    resolveFormulaApplicability(
      this.resolvedSubjectName(),
      this.resolvedTopicName(),
      this.resolvedSubtopicName(),
      this.lessonType()
    )
  );
  readonly isFormulaRequired = computed(() => this.formulaPolicy().required);

  ngOnInit(): void {
    const editId = this.route.snapshot?.queryParamMap?.get('edit');
    if (editId) {
      this.editingLessonId.set(editId);
      this.loadLessonForEdit(editId);
    }
    this.loadTaxonomy(!!editId);
    this.subjectTopicService.getExaminations().subscribe({
      next: (exams) => {
        if (exams?.length) this.examOptions.set(exams.map((e) => ({ label: `${e.name} (${e.code})`, value: e.code })));
      },
      error: () => {},
    });

    const action = this.route.snapshot?.queryParamMap?.get('action');
    if (action === 'prompt-import' || action === 'import') {
      setTimeout(() => this.openAiPromptImportDialog(action === 'import' ? 'import' : 'prompt'), 100);
    }

    const wizardParam = this.route.snapshot?.queryParamMap?.get('wizard');
    if (wizardParam === 'true') {
      setTimeout(() => this.openWizardMode(), 100);
    }
  }

  /**
   * Opens the popup Micro-Lesson Authoring Wizard Dialog with current studio state.
   */
  openWizardMode(initialStep = 0): void {
    const dialogRef = this.dialog.open(MicroLessonWizardDialogComponent, {
      data: {
        editingLessonId: this.editingLessonId(),
        subjects: this.subjects(),
        topics: this.topics(),
        subtopics: this.subtopics(),
        selectedSubjectId: this.selectedSubjectId(),
        selectedTopicId: this.selectedTopicId(),
        selectedSubtopicId: this.selectedSubtopicId(),
        customSubtopicName: this.customSubtopicName(),
        examOptions: this.examOptions(),
        targetExam: this.targetExam(),
        lessonType: this.lessonType(),
        difficultyLevel: this.difficultyLevel(),
        sequenceNo: this.sequenceNo(),
        estimatedMinutes: this.estimatedMinutes(),
        status: this.status(),
        coreConcept: this.coreConcept(),
        trapAlert: { ...this.trapAlert() },
        formulaEntries: this.formulaEntries().map((f) => ({ ...f, variables: [...(f.variables || [])] })),
        problemStatement: this.problemStatement(),
        steps: this.steps().map((s) => ({ ...s })),
        finalAnswer: this.finalAnswer(),
        keyTakeaways: [...this.keyTakeaways()],
        quickCheckItems: this.quickCheckItems().map((q) => ({ ...q, options: [...(q.options || [])] })),
        reviewerNotes: this.reviewerNotes(),
        initialStep,
      },
      width: '1160px',
      maxWidth: '96vw',
      panelClass: 'dialog-panel-no-padding',
    });

    dialogRef.afterClosed().subscribe((result?: MicroLessonWizardDialogResult | null) => {
      if (!result || !result.state) return;
      this.applyWizardResult(result);
      if (result.action === 'save') {
        this.saveLesson();
      }
    });
  }

  /**
   * Syncs state from the popup Wizard Dialog back into the in-page Micro-Lesson Authoring Studio.
   */
  applyWizardResult(result: MicroLessonWizardDialogResult): void {
    const s = result.state;
    if (result.subjects?.length) this.subjects.set(result.subjects);
    if (result.topics) this.topics.set(result.topics);
    if (result.subtopics) this.subtopics.set(result.subtopics);

    this.selectedSubjectId.set(s.selectedSubjectId);
    this.selectedTopicId.set(s.selectedTopicId);
    this.selectedSubtopicId.set(s.selectedSubtopicId);
    this.customSubtopicName.set(s.customSubtopicName);
    this.targetExam.set(s.targetExam);
    this.lessonType.set(s.lessonType);
    this.difficultyLevel.set(s.difficultyLevel);
    this.sequenceNo.set(s.sequenceNo);
    this.estimatedMinutes.set(s.estimatedMinutes);
    this.status.set(s.status);
    this.coreConcept.set(s.coreConcept);
    this.trapAlert.set({ ...s.trapAlert });
    this.formulaEntries.set(s.formulaEntries.map((f) => ({ ...f, variables: [...(f.variables || [])] })));
    this.problemStatement.set(s.problemStatement);
    this.steps.set(s.steps.map((st) => ({ ...st })));
    this.finalAnswer.set(s.finalAnswer);
    this.keyTakeaways.set([...s.keyTakeaways]);
    this.quickCheckItems.set(s.quickCheckItems.map((q) => ({ ...q, options: [...(q.options || [])] })));
    this.reviewerNotes.set(s.reviewerNotes);
  }

  loadTaxonomy(isEditMode: boolean): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs) => {
        this.subjects.set(subs || []);
        if (!isEditMode && subs?.length) this.onSubjectChange(subs[0].id);
        else if (isEditMode && this.selectedSubjectId()) this.loadCascadingForEdit(this.selectedSubjectId()!, this.selectedTopicId());
      },
      error: () => {},
    });
  }

  loadLessonForEdit(id: string): void {
    this.loadingLesson.set(true);
    this.feedback.set(null);
    this.microLessonService.getMicroLessonById(id).subscribe({
      next: (lesson) => {
        this.loadingLesson.set(false);
        const p = parseLessonForAuthoring(lesson);
        this.targetExam.set(p.targetExam);
        this.lessonType.set(p.lessonType);
        this.difficultyLevel.set(p.difficultyLevel);
        this.sequenceNo.set(p.sequenceNo);
        this.estimatedMinutes.set(p.estimatedMinutes);
        this.status.set(p.status);
        this.customSubtopicName.set(p.subtopicCustomName);
        this.coreConcept.set(p.coreConcept);
        this.trapAlert.set(p.trapAlert);
        this.formulaEntries.set(p.formulaEntries);
        this.problemStatement.set(p.problemStatement);
        this.steps.set(p.steps);
        this.finalAnswer.set(p.finalAnswer);
        this.keyTakeaways.set(p.keyTakeaways);
        this.quickCheckItems.set(p.quickCheckItems);
        this.reviewerNotes.set(p.reviewerNotes);
        if (lesson.subjectId) this.selectedSubjectId.set(Number(lesson.subjectId));
        if (lesson.topicId) this.selectedTopicId.set(Number(lesson.topicId));
        if (lesson.subtopicId) this.selectedSubtopicId.set(Number(lesson.subtopicId));
        if (this.selectedSubjectId()) this.loadCascadingForEdit(this.selectedSubjectId()!, this.selectedTopicId());
      },
      error: (err) => {
        this.loadingLesson.set(false);
        this.feedback.set({ type: 'error', message: err?.error?.message || `Could not load micro-lesson ${id}.` });
      },
    });
  }

  private loadCascadingForEdit(subjectId: number, topicId: number | null): void {
    this.subjectTopicService.getTopics(subjectId).subscribe({
      next: (tops) => {
        this.topics.set(tops || []);
        if (topicId) this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({ next: (subs) => this.subtopics.set(subs || []) });
      },
    });
  }

  onSubjectChange(subjectId: number): void {
    this.selectedSubjectId.set(subjectId);
    this.selectedTopicId.set(null);
    this.selectedSubtopicId.set(null);
    this.topics.set([]);
    this.subtopics.set([]);
    this.syncFormulaPlaceholderWithPolicy();
    if (subjectId) {
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (tops) => {
          this.topics.set(tops || []);
          if (tops?.length) this.onTopicChange(tops[0].id);
          else this.syncFormulaPlaceholderWithPolicy();
        },
      });
    }
  }

  onTopicChange(topicId: number): void {
    this.selectedTopicId.set(topicId);
    this.selectedSubtopicId.set(null);
    this.subtopics.set([]);
    this.syncFormulaPlaceholderWithPolicy();
    const subjectId = this.selectedSubjectId();
    if (subjectId && topicId) {
      this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({
        next: (subs) => {
          this.subtopics.set(subs || []);
          if (subs?.length) {
            this.selectedSubtopicId.set(subs[0].id);
            this.customSubtopicName.set(subs[0].name);
          }
          this.syncFormulaPlaceholderWithPolicy();
        },
      });
    }
  }

  onSubtopicChange(subtopicId: number | null): void {
    this.selectedSubtopicId.set(subtopicId);
    const found = this.subtopics().find((s) => s.id === subtopicId);
    if (found) this.customSubtopicName.set(found.name);
    this.syncFormulaPlaceholderWithPolicy();
  }

  onLessonTypeChange(nextType: string): void {
    this.lessonType.set(nextType);
    this.syncFormulaPlaceholderWithPolicy();
  }

  private syncFormulaPlaceholderWithPolicy(): void {
    const entries = this.formulaEntries() || [];
    const required = this.isFormulaRequired();
    const isSingleBlankPlaceholder =
      entries.length === 1 &&
      !(entries[0].latex || '').trim() &&
      !(entries[0].description || '').trim() &&
      (!(entries[0].name || '').trim() || /^formula\s+\d+$/i.test((entries[0].name || '').trim()));
    const isUntouchedDefaultSample =
      !this.editingLessonId() &&
      entries.length === 1 &&
      entries[0].name === DEFAULT_FORMULA_ENTRIES[0].name &&
      entries[0].latex === DEFAULT_FORMULA_ENTRIES[0].latex;

    if (required && entries.length === 0) {
      this.formulaEntries.set([{ name: 'Formula 1', latex: '', description: '', variables: [] }]);
    } else if (!required && (isSingleBlankPlaceholder || isUntouchedDefaultSample)) {
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

  resetForm(): void {
    this.coreConcept.set('');
    this.trapAlert.set({ title: '', description: '', commonMistake: '', correctApproach: '' });
    this.formulaEntries.set(
      this.isFormulaRequired()
        ? [{ name: 'Formula 1', latex: '', description: '', variables: [] }]
        : []
    );
    this.problemStatement.set('');
    this.steps.set([{ stepNumber: 1, title: 'Step 1', explanation: '', formula: '', tip: '' }]);
    this.finalAnswer.set('');
    this.keyTakeaways.set(['']);
    this.quickCheckItems.set([{ question: '', options: [], answer: '', explanation: '' }]);
    this.feedback.set(null);
  }

  /**
   * Opens the External AI Micro-Lesson Prompt & JSON Import Dialog.
   */
  openAiPromptImportDialog(defaultTab: 'prompt' | 'import' = 'prompt'): void {
    const dialogRef = this.dialog.open(MicroLessonPromptImportDialogComponent, {
      data: {
        subject: this.resolvedSubjectName(),
        topic: this.resolvedTopicName(),
        subtopic: this.resolvedSubtopicName(),
        subjectId: this.selectedSubjectId() ?? undefined,
        topicId: this.selectedTopicId() ?? undefined,
        subtopicId: this.selectedSubtopicId() ?? undefined,
        targetExam: this.targetExam(),
        activeTab: defaultTab,
      },
      width: '900px',
      maxWidth: '95vw',
      panelClass: 'dialog-panel-no-padding',
    });

    dialogRef.afterClosed().subscribe((result: MicroLessonPromptImportDialogResult | ParsedAuthoringLessonState | null) => {
      if (!result) return;
      if ('action' in result) {
        if (result.taxonomy) {
          this.syncTaxonomyFromDialog(result.taxonomy);
        }
        if (result.action === 'import' && result.importedLesson) {
          this.applyImportedLesson(result.importedLesson);
        }
      } else if ('coreConcept' in result) {
        this.applyImportedLesson(result);
      }
    });
  }

  /**
   * Synchronizes taxonomy selection from dialog into the parent authoring form.
   */
  syncTaxonomyFromDialog(tax: {
    subjectId?: number | null;
    topicId?: number | null;
    subtopicId?: number | null;
    subjectName?: string;
    topicName?: string;
    subtopicName?: string;
  }): void {
    let resolvedSubjectId = tax.subjectId ?? null;
    if (!resolvedSubjectId && tax.subjectName) {
      const foundSub = this.subjects().find(
        (s) => s.name.toLowerCase() === tax.subjectName?.toLowerCase()
      );
      if (foundSub) resolvedSubjectId = foundSub.id;
    }

    if (!resolvedSubjectId) {
      if (tax.subjectName && tax.subjectName.trim()) {
        this.createQuickSubject(tax.subjectName.trim());
      }
      return;
    }

    this.selectedSubjectId.set(resolvedSubjectId);
    this.syncFormulaPlaceholderWithPolicy();

    this.subjectTopicService.getTopics(resolvedSubjectId).subscribe({
      next: (tops) => {
        this.topics.set(tops || []);
        let targetTopicId = tax.topicId ?? null;
        if (!targetTopicId && tax.topicName) {
          const foundTop = tops?.find(
            (t) => t.name.toLowerCase() === tax.topicName?.toLowerCase()
          );
          if (foundTop) targetTopicId = foundTop.id;
        }

        const validTopic = tops?.find((t) => t.id === targetTopicId);
        if (validTopic) {
          this.selectedTopicId.set(validTopic.id);
          this.syncFormulaPlaceholderWithPolicy();

          this.subjectTopicService.getSubtopics(resolvedSubjectId!, validTopic.id).subscribe({
            next: (subs) => {
              this.subtopics.set(subs || []);
              let targetSubtopicId = tax.subtopicId ?? null;
              if (!targetSubtopicId && tax.subtopicName) {
                const foundSubtop = subs?.find(
                  (st) => st.name.toLowerCase() === tax.subtopicName?.toLowerCase()
                );
                if (foundSubtop) targetSubtopicId = foundSubtop.id;
              }

              const validSub = subs?.find((st) => st.id === targetSubtopicId);
              if (validSub) {
                this.selectedSubtopicId.set(validSub.id);
                this.customSubtopicName.set(validSub.name);
              } else if (subs && subs.length > 0) {
                this.selectedSubtopicId.set(subs[0].id);
                this.customSubtopicName.set(subs[0].name);
              } else {
                this.selectedSubtopicId.set(null);
                if (tax.subtopicName) {
                  this.customSubtopicName.set(tax.subtopicName);
                }
              }
              this.syncFormulaPlaceholderWithPolicy();
            },
          });
        } else if (tops && tops.length > 0) {
          this.selectedTopicId.set(tops[0].id);
          this.onTopicChange(tops[0].id);
        } else {
          this.selectedTopicId.set(null);
          this.selectedSubtopicId.set(null);
          this.subtopics.set([]);
          if (tax.subtopicName) {
            this.customSubtopicName.set(tax.subtopicName);
          }
          this.syncFormulaPlaceholderWithPolicy();
        }
      },
    });
  }

  /**
   * Populates the micro-lesson authoring form signals in UI memory ONLY.
   * Does NOT save to database yet — triggers live preview rendering and allows
   * author to review and save as usual.
   */
  applyImportedLesson(data: ParsedAuthoringLessonState): void {
    if (data.targetExam !== undefined) this.targetExam.set(data.targetExam);
    if (data.lessonType) this.lessonType.set(data.lessonType);
    if (data.difficultyLevel) this.difficultyLevel.set(data.difficultyLevel);
    if (data.sequenceNo) this.sequenceNo.set(data.sequenceNo);
    if (data.estimatedMinutes) this.estimatedMinutes.set(data.estimatedMinutes);
    if (data.status) this.status.set(data.status);
    if (data.subtopicCustomName) this.customSubtopicName.set(data.subtopicCustomName);
    this.coreConcept.set(data.coreConcept);
    this.trapAlert.set(data.trapAlert);
    this.formulaEntries.set(data.formulaEntries);
    this.problemStatement.set(data.problemStatement);
    this.steps.set(data.steps);
    this.finalAnswer.set(data.finalAnswer);
    this.keyTakeaways.set(data.keyTakeaways);
    this.quickCheckItems.set(data.quickCheckItems);
    if (data.reviewerNotes) this.reviewerNotes.set(data.reviewerNotes);

    this.feedback.set({
      type: 'success',
      message: 'Micro-lesson loaded into studio from external JSON! Review the live preview and formulas, then click "Save Micro-Lesson" to commit to the catalog.',
    });
  }

  saveLesson(): void {
    if (!this.coreConcept().trim()) {
      this.feedback.set({ type: 'error', message: 'Core Concept explanation is required.' });
      return;
    }

    const nonEmptyFormulas = (this.formulaEntries() || []).filter(
      (f) => f && (f.latex || '').trim().length > 0
    );
    if (this.isFormulaRequired() && nonEmptyFormulas.length === 0) {
      this.feedback.set({
        type: 'error',
        message:
          'Part 3 (KaTeX Formula Cheat Sheet) requires at least 1 formula for Mathematics, Science, and Numerical topics.',
      });
      return;
    }

    const effectiveFormulas = this.isFormulaRequired()
      ? this.formulaEntries()
      : (this.formulaEntries() || []).filter((f) => f && (f.latex || '').trim().length > 0);

    const payload = buildCreateOrUpdateRequest({
      subjectId: this.selectedSubjectId(), subjectName: this.resolvedSubjectName(),
      topicId: this.selectedTopicId(), topicName: this.resolvedTopicName(),
      subtopicId: this.selectedSubtopicId(), subtopicName: this.resolvedSubtopicName(),
      targetExam: this.targetExam(),
      lessonType: this.lessonType(),
      difficultyLevel: this.difficultyLevel(),
      sequenceNo: this.sequenceNo(),
      estimatedMinutes: this.estimatedMinutes(),
      status: this.status(), coreConcept: this.coreConcept(),
      trapAlert: this.trapAlert(), formulaEntries: effectiveFormulas,
      problemStatement: this.problemStatement(), steps: this.steps(), finalAnswer: this.finalAnswer(),
      keyTakeaways: this.keyTakeaways(), quickCheckItems: this.quickCheckItems(), reviewerNotes: this.reviewerNotes(),
    });
    this.saving.set(true);
    this.feedback.set(null);
    const editId = this.editingLessonId();
    const req$ = editId ? this.microLessonService.updateMicroLesson(editId, payload) : this.microLessonService.createMicroLesson(payload);
    req$.subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.feedback.set({ type: 'success', message: `Micro-lesson "${saved.subtopicName || saved.id}" ${editId ? 'updated' : 'created'} successfully!` });
      },
      error: (err) => {
        this.saving.set(false);
        this.feedback.set({ type: 'error', message: err?.error?.message || 'Failed to save micro-lesson. Verify KaTeX syntax and retry.' });
      },
    });
  }

  backToModerationDesk(): void {
    this.router.navigate(['/learning/micro-lessons']);
  }
}
