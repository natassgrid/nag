import {
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import {
  PageHeaderComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import {
  QuestionBankService,
  PassageService,
  SubjectTopicService,
  Subject,
  Topic,
  Subtopic,
  QuestionOption,
  DifficultyLevel,
  QuestionType,
  PassageRequest,
  SubQuestionRequest,
} from '@nag-frontend-workspace/questions-data-access';
import {
  AuthoringMode,
  AuthoringSubQuestion,
  QuestionWizardDialogResult,
} from '../models/authoring.model';
import {
  TaxonomySelectorComponent,
  StandaloneQuestionFormComponent,
  PassageAuthoringPanelComponent,
  QuestionPromptImportDialogComponent,
  QuestionPromptImportDialogResult,
  PromptImportTaxonomySelection,
  QuestionAuthoringWizardDialogComponent,
} from '../components';
import { ImportedQuestion } from '../services/external-prompt.service';
import {
  DEFAULT_AUTHORING_CONTENT,
  DEFAULT_AUTHORING_EXPLANATION,
  DEFAULT_AUTHORING_OPTIONS,
  DEFAULT_PASSAGE_CONTENT,
  DEFAULT_PASSAGE_TITLE,
  DEFAULT_SUB_QUESTIONS,
} from '../data/authoring-seed.data';

@Component({
  selector: 'nag-questions-feature-authoring',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    PageHeaderComponent,
    TaxonomySelectorComponent,
    StandaloneQuestionFormComponent,
    PassageAuthoringPanelComponent,
  ],
  templateUrl: './questions-feature-authoring.component.html',
  styleUrl: './questions-feature-authoring.component.scss',
})
export class QuestionsFeatureAuthoring implements OnInit {
  private readonly questionBank = inject(QuestionBankService);
  private readonly passageService = inject(PassageService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly dialog = inject(MatDialog);

  /** When non-null the form is in edit mode for this question ID. */
  editingQuestionId = signal<string | null>(null);
  /** The original lifecycle state of the question being edited (e.g. 'REVIEW', 'APPROVED'). */
  editingQuestionOriginalState = signal<string | null>(null);
  /** True while loading the question to edit. */
  loadingQuestion = signal<boolean>(false);

  mode = signal<AuthoringMode>('STANDALONE');
  saving = signal<boolean>(false);
  feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  // Taxonomy data
  subjects = signal<Subject[]>([]);
  topics = signal<Topic[]>([]);
  subtopics = signal<Subtopic[]>([]);
  loadingTaxonomy = signal<boolean>(false);

  selectedSubjectId: number | null = null;
  selectedTopicId: number | null = null;
  selectedSubtopicId: number | null = null;

  // Standalone Question Form
  cognitiveLevel = 'UNDERSTAND';
  type: QuestionType = 'MULTIPLE_CHOICE';
  difficulty: DifficultyLevel = 'MEDIUM';
  marks = 4;
  negativeMarks = 1;

  content = DEFAULT_AUTHORING_CONTENT;
  explanation = DEFAULT_AUTHORING_EXPLANATION;
  options: QuestionOption[] = DEFAULT_AUTHORING_OPTIONS.map((o) => ({ ...o }));

  // Passage Comprehension Form
  passageTitle = DEFAULT_PASSAGE_TITLE;
  passageContent = DEFAULT_PASSAGE_CONTENT;

  activeSubQuestionIndex = 0;
  subQuestions: AuthoringSubQuestion[] = DEFAULT_SUB_QUESTIONS.map((sq) => ({
    ...sq,
    options: sq.options.map((o) => ({ ...o })),
  }));

  ngOnInit(): void {
    this.loadTaxonomy();
    // Detect edit query param: /questions/authoring?edit=<uuid>
    const editId = this.route.snapshot.queryParamMap.get('edit');
    if (editId) {
      this.editingQuestionId.set(editId);
      this.loadQuestionForEdit(editId);
    }

    // Detect history.state prefill (e.g. from AI generator or import)
    const prefill = history.state?.prefill;
    if (prefill) {
      this.applyPrefillState(prefill);
    }

    // Detect action query param (?action=prompt-import or ?action=import)
    const action = this.route.snapshot.queryParamMap.get('action');
    if (action === 'prompt-import' || action === 'import') {
      setTimeout(() => this.openAiPromptImportDialog(action === 'import' ? 'import' : 'prompt'), 100);
    }

    // Detect wizard query param (?wizard=true)
    const wizardParam = this.route.snapshot.queryParamMap.get('wizard');
    if (wizardParam === 'true') {
      setTimeout(() => this.openWizardMode(), 100);
    }
  }

  /**
   * Opens the popup Question & Passage Authoring Wizard Dialog with current in-page state.
   */
  openWizardMode(initialStep = 0): void {
    const dialogRef = this.dialog.open(QuestionAuthoringWizardDialogComponent, {
      data: {
        mode: this.mode(),
        editingQuestionId: this.editingQuestionId(),
        subjects: this.subjects(),
        topics: this.topics(),
        subtopics: this.subtopics(),
        selectedSubjectId: this.selectedSubjectId,
        selectedTopicId: this.selectedTopicId,
        selectedSubtopicId: this.selectedSubtopicId,
        cognitiveLevel: this.cognitiveLevel,
        type: this.type,
        difficulty: this.difficulty,
        marks: this.marks,
        negativeMarks: this.negativeMarks,
        content: this.content,
        explanation: this.explanation,
        options: this.options.map((o) => ({ ...o })),
        passageTitle: this.passageTitle,
        passageContent: this.passageContent,
        subQuestions: this.subQuestions.map((sq) => ({
          ...sq,
          options: sq.options.map((o) => ({ ...o })),
        })),
        activeSubQuestionIndex: this.activeSubQuestionIndex,
        initialStep,
      },
      width: '1120px',
      maxWidth: '96vw',
      panelClass: 'dialog-panel-no-padding',
    });

    dialogRef.afterClosed().subscribe((result?: QuestionWizardDialogResult | null) => {
      if (!result || !result.state) return;
      this.applyWizardResult(result);
      if (result.action === 'save') {
        this.saveItem();
      }
    });
  }

  /**
   * Syncs state from the popup Wizard Dialog back into the in-page Question Editor.
   */
  applyWizardResult(result: QuestionWizardDialogResult): void {
    const s = result.state;
    if (result.subjects?.length) this.subjects.set(result.subjects);
    if (result.topics) this.topics.set(result.topics);
    if (result.subtopics) this.subtopics.set(result.subtopics);

    this.mode.set(s.mode);
    this.selectedSubjectId = s.selectedSubjectId;
    this.selectedTopicId = s.selectedTopicId;
    this.selectedSubtopicId = s.selectedSubtopicId;
    this.cognitiveLevel = s.cognitiveLevel;
    this.type = s.type;
    this.difficulty = s.difficulty;
    this.marks = s.marks;
    this.negativeMarks = s.negativeMarks;
    this.content = s.content;
    this.explanation = s.explanation;
    this.options = s.options.map((o) => ({ ...o }));
    this.passageTitle = s.passageTitle;
    this.passageContent = s.passageContent;
    this.subQuestions = s.subQuestions.map((sq) => ({
      ...sq,
      options: sq.options.map((o) => ({ ...o })),
    }));
    this.activeSubQuestionIndex = s.activeSubQuestionIndex;
  }

  /**
   * Fetch an existing question by ID and populate all form fields so the
   * author can edit and re-save via PUT instead of POST.
   */
  loadQuestionForEdit(id: string): void {
    this.loadingQuestion.set(true);
    this.feedback.set(null);
    this.questionBank.getQuestionById(id).subscribe({
      next: (q) => {
        this.loadingQuestion.set(false);

        // Questions in PUBLISHED state cannot be edited (they are immutable once published).
        // Questions in DRAFT, REVIEW, APPROVED, or REJECTED state can be edited — upon save
        // they will be reset to DRAFT state for re-review.
        const state = (q.status || '').toUpperCase();
        if (state === 'PUBLISHED') {
          this.editingQuestionId.set(null);
          this.feedback.set({
            type: 'error',
            message: `PUBLISHED questions cannot be edited. This question has already been published and is now immutable.`,
          });
          return;
        }

        // Store the original state so the UI can show a warning if the question
        // was in REVIEW or APPROVED and will be reset to DRAFT on save.
        this.editingQuestionOriginalState.set(state);

        // Map the backend type back to the UI type token.
        const uiType: QuestionType =
          q.type === 'SINGLE_MCQ' ? 'MULTIPLE_CHOICE' :
          q.type === 'MULTI_MCQ' ? 'MULTIPLE_SELECT' :
          (q.type as QuestionType) || 'MULTIPLE_CHOICE';

        this.type = uiType;
        this.content = q.content || '';
        this.explanation = q.explanation || '';
        this.difficulty = (q.difficulty as DifficultyLevel) || 'MEDIUM';
        this.cognitiveLevel = (q as any).cognitiveLevel || 'UNDERSTAND';
        this.marks = q.marks ?? 4;
        this.negativeMarks = q.negativeMarks ?? 1;

        // Populate options — ensure at least 4 slots normalised with letter IDs.
        if (q.options && q.options.length > 0) {
          this.options = q.options.map((opt, idx) => ({
            id: opt.id || String.fromCharCode(65 + idx),
            text: opt.text || '',
            isCorrect: opt.isCorrect,
          }));
        }

        // Load taxonomy IDs so the dropdowns pre-select the right values.
        if (q.subjectId) {
          this.selectedSubjectId = Number(q.subjectId);
        }
        if (q.topicId) {
          this.selectedTopicId = Number(q.topicId);
        }
        if (q.subtopicId) {
          this.selectedSubtopicId = Number(q.subtopicId);
        }
      },
      error: (err) => {
        this.loadingQuestion.set(false);
        this.editingQuestionId.set(null);
        this.feedback.set({
          type: 'error',
          message: err?.error?.message || `Could not load question ${id} for editing.`,
        });
      },
    });
  }

  loadTaxonomy(): void {
    this.loadingTaxonomy.set(true);
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs) => {
        this.subjects.set(subs || []);
        const isEditMode = !!this.route.snapshot.queryParamMap.get('edit');
        if (!isEditMode && subs && subs.length > 0) {
          // Create mode: auto-select the first subject.
          this.selectedSubjectId = subs[0].id;
          this.onSubjectChange(subs[0].id);
        } else if (isEditMode && this.selectedSubjectId) {
          // Edit mode: load topics for the pre-selected subject so the
          // dropdown chain is populated correctly.
          this.loadTopicsForEdit(this.selectedSubjectId);
        }
        this.loadingTaxonomy.set(false);
      },
      error: () => this.loadingTaxonomy.set(false),
    });
  }

  /** Load topics (and subtopics) for a subject in edit mode without resetting the IDs. */
  private loadTopicsForEdit(subjectId: number): void {
    this.subjectTopicService.getTopics(subjectId).subscribe({
      next: (tops) => {
        this.topics.set(tops || []);
        if (this.selectedTopicId) {
          this.loadSubtopicsForEdit(subjectId, this.selectedTopicId);
        }
      },
    });
  }

  /** Load subtopics in edit mode without resetting the IDs. */
  private loadSubtopicsForEdit(subjectId: number, topicId: number): void {
    this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({
      next: (subs) => {
        this.subtopics.set(subs || []);
      },
    });
  }

  onSubjectChange(subjectId: number): void {
    this.selectedSubjectId = subjectId;
    this.selectedTopicId = null;
    this.selectedSubtopicId = null;
    this.topics.set([]);
    this.subtopics.set([]);

    if (subjectId) {
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (tops) => {
          this.topics.set(tops || []);
          if (tops && tops.length > 0) {
            this.selectedTopicId = tops[0].id;
            this.onTopicChange(tops[0].id);
          }
        },
      });
    }
  }

  onTopicChange(topicId: number): void {
    this.selectedTopicId = topicId;
    this.selectedSubtopicId = null;
    this.subtopics.set([]);

    if (this.selectedSubjectId && topicId) {
      this.subjectTopicService.getSubtopics(this.selectedSubjectId, topicId).subscribe({
        next: (subs) => {
          this.subtopics.set(subs || []);
          if (subs && subs.length > 0) {
            this.selectedSubtopicId = subs[0].id;
          }
        },
      });
    }
  }

  createQuickSubject(name: string): void {
    this.subjectTopicService.createSubject({ name }).subscribe({
      next: (sub) => {
        this.subjects.update((list) => [...list, sub]);
        this.selectedSubjectId = sub.id;
        this.onSubjectChange(sub.id);
      },
    });
  }

  createQuickTopic(name: string): void {
    if (!this.selectedSubjectId) return;
    this.subjectTopicService
      .createTopic(this.selectedSubjectId, { name })
      .subscribe({
        next: (top) => {
          this.topics.update((list) => [...list, top]);
          this.selectedTopicId = top.id;
          this.onTopicChange(top.id);
        },
      });
  }

  setMode(m: AuthoringMode): void {
    this.mode.set(m);
    this.feedback.set(null);
  }

  onValidationError(message: string): void {
    this.feedback.set({
      type: 'error',
      message,
    });
  }

  getOptionLetter(index: number): string {
    return String.fromCharCode(65 + index);
  }

  resetForm(): void {
    if (this.mode() === 'STANDALONE') {
      this.content = '';
      this.explanation = '';
      this.options = [
        { id: 'A', text: '', isCorrect: true },
        { id: 'B', text: '', isCorrect: false },
        { id: 'C', text: '', isCorrect: false },
        { id: 'D', text: '', isCorrect: false },
      ];
    } else {
      this.passageTitle = '';
      this.passageContent = '';
      this.subQuestions = [
        {
          passageOrderIndex: 1,
          content: '',
          questionType: 'MULTIPLE_CHOICE',
          difficulty: 'MEDIUM',
          cognitiveLevel: 'UNDERSTAND',
          marks: 4,
          negativeMarks: 1,
          explanation: '',
          options: [
            { id: 'A', text: '', isCorrect: true },
            { id: 'B', text: '', isCorrect: false },
            { id: 'C', text: '', isCorrect: false },
            { id: 'D', text: '', isCorrect: false },
          ],
        },
        {
          passageOrderIndex: 2,
          content: '',
          questionType: 'MULTIPLE_CHOICE',
          difficulty: 'MEDIUM',
          cognitiveLevel: 'UNDERSTAND',
          marks: 4,
          negativeMarks: 1,
          explanation: '',
          options: [
            { id: 'A', text: '', isCorrect: true },
            { id: 'B', text: '', isCorrect: false },
            { id: 'C', text: '', isCorrect: false },
            { id: 'D', text: '', isCorrect: false },
          ],
        },
      ];
      this.activeSubQuestionIndex = 0;
    }
    this.feedback.set(null);
  }

  /**
   * Opens the External AI Prompt & JSON Import Dialog.
   */
  openAiPromptImportDialog(defaultTab: 'prompt' | 'import' = 'prompt'): void {
    const selectedSub = this.subjects().find((s) => s.id === this.selectedSubjectId);
    const selectedTop = this.topics().find((t) => t.id === this.selectedTopicId);
    const selectedSubtop = this.subtopics().find((st) => st.id === this.selectedSubtopicId);

    const dialogRef = this.dialog.open(QuestionPromptImportDialogComponent, {
      data: {
        subject: selectedSub?.name,
        topic: selectedTop?.name,
        subtopic: selectedSubtop?.name,
        subjectId: this.selectedSubjectId ?? undefined,
        topicId: this.selectedTopicId ?? undefined,
        subtopicId: this.selectedSubtopicId ?? undefined,
        questionType: this.type,
        difficulty: this.difficulty,
        cognitiveLevel: this.cognitiveLevel,
        marks: this.marks,
        negativeMarks: this.negativeMarks,
        activeTab: defaultTab,
      },
      width: '900px',
      maxWidth: '95vw',
      panelClass: 'dialog-panel-no-padding',
    });

    dialogRef.afterClosed().subscribe((result: QuestionPromptImportDialogResult | ImportedQuestion | null) => {
      if (!result) return;

      if ('action' in result) {
        if (result.taxonomy) {
          this.syncTaxonomyFromDialog(result.taxonomy);
        }
        if (result.action === 'import' && result.importedQuestion) {
          this.applyImportedQuestion(result.importedQuestion);
        }
      } else if ('content' in result) {
        this.applyImportedQuestion(result);
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
    let resolvedSubjectId = tax.subjectId;
    if (!resolvedSubjectId && tax.subjectName) {
      const foundSub = this.subjects().find(
        (s) => s.name.toLowerCase() === tax.subjectName?.toLowerCase()
      );
      if (foundSub) resolvedSubjectId = foundSub.id;
    }

    if (!resolvedSubjectId) {
      if (tax.subjectName && tax.subjectName.trim()) {
        this.subjectTopicService.createSubject({ name: tax.subjectName.trim() }).subscribe({
          next: (newSub) => {
            this.subjects.update((list) => [...list, newSub]);
            this.selectedSubjectId = newSub.id;
            this.onSubjectChange(newSub.id);
          },
        });
      }
      return;
    }

    this.selectedSubjectId = resolvedSubjectId;

    this.subjectTopicService.getTopics(resolvedSubjectId).subscribe({
      next: (tops) => {
        this.topics.set(tops || []);
        let targetTopicId = tax.topicId;
        if (!targetTopicId && tax.topicName) {
          const foundTop = tops?.find(
            (t) => t.name.toLowerCase() === tax.topicName?.toLowerCase()
          );
          if (foundTop) targetTopicId = foundTop.id;
        }

        const validTopic = tops?.find((t) => t.id === targetTopicId);
        if (validTopic) {
          this.selectedTopicId = validTopic.id;
          this.subjectTopicService.getSubtopics(resolvedSubjectId!, validTopic.id).subscribe({
            next: (subs) => {
              this.subtopics.set(subs || []);
              let targetSubtopicId = tax.subtopicId;
              if (!targetSubtopicId && tax.subtopicName) {
                const foundSubtop = subs?.find(
                  (st) => st.name.toLowerCase() === tax.subtopicName?.toLowerCase()
                );
                if (foundSubtop) targetSubtopicId = foundSubtop.id;
              }
              const validSub = subs?.find((st) => st.id === targetSubtopicId);
              if (validSub) {
                this.selectedSubtopicId = validSub.id;
              } else if (subs && subs.length > 0) {
                this.selectedSubtopicId = subs[0].id;
              } else {
                this.selectedSubtopicId = null;
              }
            },
          });
        } else if (tops && tops.length > 0) {
          this.selectedTopicId = tops[0].id;
          this.onTopicChange(tops[0].id);
        } else {
          this.selectedTopicId = null;
          this.selectedSubtopicId = null;
          this.subtopics.set([]);
        }
      },
    });
  }

  /**
   * Applies validated question data from external JSON import to UI form ONLY (in-memory).
   * Does NOT save to database yet — allows author to review and click Save Question as usual.
   */
  applyImportedQuestion(data: ImportedQuestion): void {
    this.mode.set('STANDALONE');
    this.content = data.content;
    this.explanation = data.explanation || '';
    this.type = data.type;
    this.difficulty = data.difficulty;
    this.cognitiveLevel = data.cognitiveLevel;
    this.marks = data.marks;
    this.negativeMarks = data.negativeMarks;
    this.options = data.options;

    // Match taxonomy if available
    if (data.subjectId || data.subject) {
      this.syncTaxonomyFromDialog({
        subjectId: data.subjectId ?? null,
        topicId: data.topicId ?? null,
        subtopicId: data.subtopicId ?? null,
        subjectName: data.subject || '',
        topicName: data.topic || '',
        subtopicName: data.subtopic || '',
      });
    }

    this.feedback.set({
      type: 'success',
      message: 'Question loaded into authoring form from external JSON! Review the fields and formulas below, then click "Save Question" to commit to the Question Bank.',
    });
  }

  applyPrefillState(prefill: any): void {
    this.mode.set('STANDALONE');
    if (prefill.content) this.content = prefill.content;
    if (prefill.explanation) this.explanation = prefill.explanation;
    if (prefill.difficulty) this.difficulty = prefill.difficulty;
    if (prefill.cognitiveLevel) this.cognitiveLevel = prefill.cognitiveLevel;
    if (prefill.marks) this.marks = prefill.marks;
    if (prefill.negativeMarks !== undefined) this.negativeMarks = prefill.negativeMarks;

    if (prefill.questionType) {
      const q = prefill.questionType;
      this.type =
        q === 'SINGLE_MCQ' ? 'MULTIPLE_CHOICE' :
        q === 'MULTI_MCQ' ? 'MULTIPLE_SELECT' :
        q;
    }

    if (Array.isArray(prefill.options) && prefill.options.length > 0) {
      this.options = prefill.options.map((opt: any, idx: number) => ({
        id: opt.id || String.fromCharCode(65 + idx),
        text: opt.text || '',
        isCorrect: Boolean(opt.isCorrect),
      }));
    }

    if (prefill.subjectId) {
      this.selectedSubjectId = Number(prefill.subjectId);
      this.onSubjectChange(this.selectedSubjectId);
    }
  }

  saveItem(): void {
    if (this.mode() === 'STANDALONE') {
      this.saveStandaloneQuestion();
    } else {
      this.savePassage();
    }
  }

  private saveStandaloneQuestion(): void {
    if (!this.selectedSubjectId) {
      this.feedback.set({
        type: 'error',
        message: 'Please select a Subject from the taxonomy hierarchy before saving.',
      });
      return;
    }

    if (!this.selectedTopicId) {
      this.feedback.set({
        type: 'error',
        message: 'Please select a Topic from the taxonomy hierarchy before saving.',
      });
      return;
    }

    if (!this.selectedSubtopicId) {
      this.feedback.set({
        type: 'error',
        message: 'Under the Unified Exam Spine, every question must belong to a Subtopic. Please select a Subtopic.',
      });
      return;
    }

    if (!this.content.trim()) {
      this.feedback.set({
        type: 'error',
        message: 'Question problem statement cannot be empty.',
      });
      return;
    }

    if (this.marks <= 0) {
      this.feedback.set({
        type: 'error',
        message: 'Positive marks must be greater than 0.',
      });
      return;
    }

    if (this.negativeMarks < 0 || this.negativeMarks > this.marks) {
      this.feedback.set({
        type: 'error',
        message: 'Negative marks must be between 0 and positive marks.',
      });
      return;
    }

    const selectedSub = this.subjects().find((s) => s.id === this.selectedSubjectId);
    const selectedTop = this.topics().find((t) => t.id === this.selectedTopicId);
    const selectedSubtop = this.subtopics().find((st) => st.id === this.selectedSubtopicId);

    const formattedOptions = this.options.map((opt, idx) => ({
      id: this.getOptionLetter(idx),
      text: opt.text.trim(),
      isCorrect: opt.isCorrect,
    }));

    const correctLetters = formattedOptions
      .filter((opt) => opt.isCorrect)
      .map((opt) => opt.id);

    const isMcq = this.type === 'MULTIPLE_CHOICE' || this.type === 'SINGLE_MCQ';
    const isMsq = this.type === 'MULTIPLE_SELECT' || this.type === 'MULTI_MCQ' || this.type === 'MULTIPLE_MCQ';

    if (isMcq || isMsq) {
      if (formattedOptions.length < 2) {
        this.feedback.set({
          type: 'error',
          message: 'At least 2 options are required for multiple choice questions.',
        });
        return;
      }
      if (formattedOptions.some((opt) => !opt.text)) {
        this.feedback.set({
          type: 'error',
          message: 'All options must have non-empty text or formulas.',
        });
        return;
      }
      if (isMcq && correctLetters.length !== 1) {
        this.feedback.set({
          type: 'error',
          message: 'Single correct MCQ requires exactly one correct option selected.',
        });
        return;
      }
      if (isMsq && correctLetters.length === 0) {
        this.feedback.set({
          type: 'error',
          message: 'Multi correct MSQ requires at least one correct option selected.',
        });
        return;
      }
    }

    const backendQuestionType =
      this.type === 'MULTIPLE_CHOICE'
        ? 'SINGLE_MCQ'
        : this.type === 'MULTIPLE_SELECT'
        ? 'MULTI_MCQ'
        : this.type;

    const payload = {
      content: this.content.trim(),
      type: backendQuestionType,
      questionType: backendQuestionType,
      difficulty: this.difficulty,
      cognitiveLevel: this.cognitiveLevel,
      subject: selectedSub?.name || 'General',
      topic: selectedTop?.name || 'General',
      subtopic: selectedSubtop?.name || '',
      subjectId: this.selectedSubjectId ? Number(this.selectedSubjectId) : 0,
      topicId: this.selectedTopicId ? Number(this.selectedTopicId) : undefined,
      subtopicId: this.selectedSubtopicId ? Number(this.selectedSubtopicId) : undefined,
      marks: this.marks,
      negativeMarks: this.negativeMarks,
      options: formattedOptions,
      answerKey: correctLetters.join(','),
      explanation: this.explanation.trim() || undefined,
      state: 'DRAFT',
      status: 'DRAFT',
    };

    this.saving.set(true);
    this.feedback.set(null);

    const editId = this.editingQuestionId();
    if (editId) {
      // ── EDIT MODE: PUT /api/v1/questions/:id ──
      this.questionBank.updateQuestion(editId, payload).subscribe({
        next: (updated) => {
          this.saving.set(false);
          this.feedback.set({
            type: 'success',
            message: `Question ${updated.code || updated.id} successfully updated in the Question Bank!`,
          });
        },
        error: (err) => {
          this.saving.set(false);
          this.feedback.set({
            type: 'error',
            message: err?.error?.message || 'Failed to update question. Please verify all fields and retry.',
          });
        },
      });
    } else {
      // ── CREATE MODE: POST /api/v1/questions ──
      this.questionBank.createQuestion(payload).subscribe({
        next: (created) => {
          this.saving.set(false);
          this.feedback.set({
            type: 'success',
            message: `Question ${created.code || created.id} successfully created and committed to Question Bank!`,
          });
        },
        error: (err) => {
          this.saving.set(false);
          this.feedback.set({
            type: 'error',
            message: err?.error?.message || 'Failed to persist question. Please verify all fields and retry.',
          });
        },
      });
    }
  }

  private savePassage(): void {
    if (!this.passageContent.trim()) {
      this.feedback.set({
        type: 'error',
        message: 'Passage reading content cannot be empty.',
      });
      return;
    }

    if (!this.selectedSubjectId) {
      this.feedback.set({
        type: 'error',
        message: 'Please select a Subject for the comprehension passage.',
      });
      return;
    }

    if (this.subQuestions.length < 2 || this.subQuestions.length > 6) {
      this.feedback.set({
        type: 'error',
        message: 'Comprehension passages require between 2 and 6 sub-questions.',
      });
      return;
    }

    // Validate subquestions
    for (let i = 0; i < this.subQuestions.length; i++) {
      const sq = this.subQuestions[i];
      if (!sq.content.trim()) {
        this.feedback.set({
          type: 'error',
          message: `Sub-question ${i + 1} content cannot be empty.`,
        });
        return;
      }
      const hasCorrect = sq.options.some((o) => o.isCorrect);
      if ((sq.questionType === 'MULTIPLE_CHOICE' || sq.questionType === 'SINGLE_MCQ' || sq.questionType === 'MULTIPLE_SELECT') && !hasCorrect) {
        this.feedback.set({
          type: 'error',
          message: `Sub-question ${i + 1} must have at least one correct option selected.`,
        });
        return;
      }
    }

    const selectedSub = this.subjects().find((s) => s.id === this.selectedSubjectId);
    const selectedTop = this.topics().find((t) => t.id === this.selectedTopicId);
    const selectedSubtop = this.subtopics().find((st) => st.id === this.selectedSubtopicId);

    const formattedSubQuestions: SubQuestionRequest[] = this.subQuestions.map((sq, idx) => {
      const correct = sq.options.filter((o) => o.isCorrect).map((o) => o.id).join(',');
      const backendType =
        sq.questionType === 'MULTIPLE_CHOICE'
          ? 'SINGLE_MCQ'
          : sq.questionType === 'MULTIPLE_SELECT'
          ? 'MULTI_MCQ'
          : sq.questionType;

      return {
        passageOrderIndex: idx + 1,
        content: sq.content.trim(),
        type: backendType,
        questionType: backendType,
        difficulty: sq.difficulty,
        cognitiveLevel: sq.cognitiveLevel,
        marks: sq.marks,
        negativeMarks: sq.negativeMarks,
        options: sq.options.map((opt, oIdx) => ({
          id: this.getOptionLetter(oIdx),
          text: opt.text.trim(),
          isCorrect: opt.isCorrect,
        })),
        answerKey: correct,
        explanation: sq.explanation.trim() || undefined,
        state: 'DRAFT',
        status: 'DRAFT',
      };
    });

    const passagePayload: PassageRequest = {
      title: this.passageTitle.trim() || undefined,
      content: this.passageContent.trim(),
      subject: selectedSub?.name || 'General',
      topic: selectedTop?.name || 'General',
      subtopic: selectedSubtop?.name || undefined,
      subjectId: this.selectedSubjectId ? Number(this.selectedSubjectId) : 0,
      topicId: this.selectedTopicId ? Number(this.selectedTopicId) : undefined,
      subtopicId: this.selectedSubtopicId ? Number(this.selectedSubtopicId) : undefined,
      state: 'DRAFT',
      status: 'DRAFT',
      subQuestions: formattedSubQuestions,
    };

    this.saving.set(true);
    this.feedback.set(null);

    this.passageService.createPassage(passagePayload).subscribe({
      next: (created) => {
        this.saving.set(false);
        this.feedback.set({
          type: 'success',
          message: `Passage Set ${created.id} with ${(created.subQuestionCount || created.subQuestions?.length) || formattedSubQuestions.length} sub-questions successfully registered!`,
        });
      },
      error: (err) => {
        this.saving.set(false);
        this.feedback.set({
          type: 'error',
          message: err?.error?.message || 'Failed to persist passage set. Please verify all inputs and retry.',
        });
      },
    });
  }

  viewInBank(): void {
    this.router.navigate(['/questions/bank']);
  }
}
