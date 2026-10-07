import {
  Component,
  inject,
  signal,
  computed,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  QuestionAiService,
  QuestionBankService,
  SubjectTopicService,
  QuestionGenerationRequest,
  QuestionGenerationResponse,
  GeneratedQuestion,
  BatchGenerationRequest,
  BatchItem,
  BatchGenerationJob,
  Subject,
  Topic,
  Subtopic,
} from '@nag-frontend-workspace/questions-data-access';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import {
  DEFAULT_AI_DIFFICULTIES,
  AI_COGNITIVE_LEVELS,
  AI_QUESTION_TYPES,
} from './models';
import {
  AiPromptConfigFormComponent,
  AiGenerationResultsComponent,
  AiBatchJobsPanelComponent,
} from './components';

@Component({
  selector: 'nag-admin-ai-question-generation',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MatIconModule,
    MatSnackBarModule,
    AiPromptConfigFormComponent,
    AiGenerationResultsComponent,
    AiBatchJobsPanelComponent,
  ],
  templateUrl: './admin-ai-question-generation.component.html',
  styleUrls: ['./admin-ai-question-generation.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminAiQuestionGenerationComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly aiService = inject(QuestionAiService);
  private readonly questionBankService = inject(QuestionBankService);
  private readonly subjectTopicService = inject(SubjectTopicService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  // View state
  readonly activeTab = signal<'realtime' | 'batch'>('realtime');
  readonly generating = signal<boolean>(false);
  readonly response = signal<QuestionGenerationResponse | null>(null);
  readonly generationError = signal<string>('');
  /**
   * Tracks indices of questions that have been saved (either via manual save or
   * returned as autoSaved via savedQuestionId in the response).
   */
  readonly savedQuestionIds = signal<Set<number>>(new Set());
  readonly savingIndices = signal<Set<number>>(new Set());

  // Taxonomy state
  readonly subjects = signal<Subject[]>([]);
  readonly topics = signal<Topic[]>([]);
  readonly subtopics = signal<Subtopic[]>([]);
  readonly taxonomyLoading = signal<boolean>(false);
  readonly taxonomyError = signal<string>('');

  // Currently selected taxonomy nodes (denormalized for form/payload use)
  readonly selectedSubject = signal<Subject | null>(null);
  readonly selectedTopic = signal<Topic | null>(null);

  // Batch state
  readonly batchItems = signal<BatchItem[]>([]);
  readonly batchJobs = signal<BatchGenerationJob[]>([]);
  readonly loadingBatchJobs = signal<boolean>(false);
  readonly submittingBatch = signal<boolean>(false);
  private pollSub: Subscription | null = null;

  // Constants for the form
  readonly difficulties = DEFAULT_AI_DIFFICULTIES;
  readonly cognitiveLevels = AI_COGNITIVE_LEVELS;
  readonly questionTypes = AI_QUESTION_TYPES;

  // Reactive generation form — uses ids for subject/topic/subtopic
  readonly form = this.fb.group({
    subjectId: [null as number | null, Validators.required],
    topicId: [null as number | null, Validators.required],
    subtopicId: [null as number | null],
    rawTextInput: [''],
    description: [''],
    difficulty: ['MEDIUM', Validators.required],
    cognitiveLevel: ['APPLY', Validators.required],
    questionType: ['SINGLE_MCQ', Validators.required],
    count: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    avoidDuplicate: [true],
    autoSave: [false],
  });

  readonly totalBatchQuestions = computed(() =>
    this.batchItems().reduce((acc, item) => acc + item.count, 0)
  );

  ngOnInit(): void {
    this.loadTaxonomy();
    this.loadBatchJobs();
    this.resumeBatchPollingIfNeeded();
  }

  ngOnDestroy(): void {
    this.stopBatchPolling();
  }

  // -------------------------------------------------------------------------
  // Taxonomy loading
  // -------------------------------------------------------------------------

  loadTaxonomy(): void {
    this.taxonomyLoading.set(true);
    this.taxonomyError.set('');
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs) => {
        this.subjects.set(subs);
        this.taxonomyLoading.set(false);
      },
      error: (err) => {
        this.taxonomyLoading.set(false);
        this.taxonomyError.set(
          err?.error?.message || err?.message || 'Failed to load subjects. Check API connectivity.'
        );
      },
    });
  }

  onSubjectChange(subject: Subject): void {
    this.selectedSubject.set(subject);
    this.selectedTopic.set(null);
    this.topics.set([]);
    this.subtopics.set([]);
    this.form.patchValue({ subjectId: subject.id, topicId: null, subtopicId: null });

    this.taxonomyLoading.set(true);
    this.subjectTopicService.getTopics(subject.id).subscribe({
      next: (ts) => {
        this.topics.set(ts);
        this.taxonomyLoading.set(false);
      },
      error: () => {
        this.taxonomyLoading.set(false);
        this.taxonomyError.set('Failed to load topics for selected subject.');
      },
    });
  }

  onTopicChange(topic: Topic): void {
    this.selectedTopic.set(topic);
    this.subtopics.set([]);
    this.form.patchValue({ topicId: topic.id, subtopicId: null });

    const sub = this.selectedSubject();
    if (sub) {
      this.subjectTopicService.getSubtopics(sub.id, topic.id).subscribe({
        next: (sts) => this.subtopics.set(sts),
        error: () => {
          /* subtopics are optional — silently ignore load failure */
        },
      });
    }
  }

  // -------------------------------------------------------------------------
  // Real-time LLM Generation
  // -------------------------------------------------------------------------

  generateQuestions(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.generating.set(true);
    this.generationError.set('');
    this.response.set(null);
    this.savedQuestionIds.set(new Set());

    const v = this.form.value;
    const req: QuestionGenerationRequest = {
      subjectId: v.subjectId ?? undefined,
      topicId: v.topicId ?? undefined,
      subtopicId: v.subtopicId ?? undefined,
      subject: this.selectedSubject()?.name ?? '',
      topic: this.selectedTopic()?.name ?? '',
      subtopic: this.subtopics().find((s) => s.id === v.subtopicId)?.name,
      rawTextInput: v.rawTextInput?.trim() || undefined,
      description: v.description?.trim() || undefined,
      difficulty: v.difficulty!,
      cognitiveLevel: v.cognitiveLevel!,
      questionType: v.questionType!,
      count: v.count || 3,
      avoidDuplicate: v.avoidDuplicate ?? true,
      autoSave: v.autoSave ?? false,
    };

    this.aiService.generateQuestions(req).subscribe({
      next: (res) => {
        this.generating.set(false);
        this.response.set(res);

        // Mark any questions that were auto-saved by the backend
        const autoSaved = new Set<number>();
        res.questions.forEach((q, idx) => {
          if (q.savedQuestionId) {
            autoSaved.add(idx);
          }
        });
        if (autoSaved.size > 0) {
          this.savedQuestionIds.set(autoSaved);
        }

        this.snackBar.open(
          `Successfully synthesized ${res.totalGenerated} questions via ${res.modelUsed || 'AI Engine'}!`,
          'OK',
          { duration: 4000 }
        );
      },
      error: (err) => {
        this.generating.set(false);
        const msg = this.mapHttpError(err);
        this.generationError.set(msg);
        this.snackBar.open(msg, 'Dismiss', { duration: 6000 });
      },
    });
  }

  // -------------------------------------------------------------------------
  // Save single generated question into Question Bank as DRAFT
  // -------------------------------------------------------------------------

  saveQuestionToBank(question: GeneratedQuestion, index: number): void {
    if (this.savingIndices().has(index) || this.savedQuestionIds().has(index)) return;

    this.savingIndices.update((s) => new Set(s).add(index));

    const v = this.form.value;
    const subtopic = this.subtopics().find((s) => s.id === v.subtopicId);

    const payload = {
      content: question.content,
      subjectId: v.subjectId,
      topicId: v.topicId,
      subtopicId: v.subtopicId ?? undefined,
      subject: this.selectedSubject()?.name,
      topic: this.selectedTopic()?.name,
      subtopic: subtopic?.name,
      difficulty: question.difficulty || v.difficulty,
      cognitiveLevel: question.cognitiveLevel || v.cognitiveLevel,
      questionType: question.questionType || v.questionType,
      marks: 4,
      negativeMarks: 1,
      options: question.options || [],
      answerKey: question.answerKey,
      explanation: question.explanation,
      status: 'DRAFT',
      tags: [
        'AI-Generated',
        this.selectedSubject()?.name ?? '',
        this.selectedTopic()?.name ?? '',
      ].filter(Boolean),
    };

    this.questionBankService.createQuestion(payload).subscribe({
      next: (created) => {
        this.savedQuestionIds.update((s) => new Set(s).add(index));
        this.savingIndices.update((s) => {
          const next = new Set(s);
          next.delete(index);
          return next;
        });
        this.snackBar.open(`Question saved as DRAFT to Bank (ID: ${created.id || 'new'})`, 'OK', {
          duration: 3000,
        });
      },
      error: (err) => {
        this.savingIndices.update((s) => {
          const next = new Set(s);
          next.delete(index);
          return next;
        });
        this.snackBar.open(err?.error?.message || 'Failed to save question to bank', 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }

  // -------------------------------------------------------------------------
  // Save all valid generated questions
  // -------------------------------------------------------------------------

  saveAllValid(): void {
    const res = this.response();
    if (!res) return;
    res.questions.forEach((q, idx) => {
      const isValid = !q.validation || q.validation.valid;
      if (isValid && !this.savedQuestionIds().has(idx)) {
        this.saveQuestionToBank(q, idx);
      }
    });
  }

  // -------------------------------------------------------------------------
  // Edit in Authoring View (prefills editor with generated question state)
  // -------------------------------------------------------------------------

  openInAuthoring(question: GeneratedQuestion): void {
    this.router.navigate(['/questions/authoring'], {
      state: {
        prefill: {
          content: question.content,
          answerKey: question.answerKey,
          explanation: question.explanation,
          options: question.options,
          difficulty: question.difficulty,
          cognitiveLevel: question.cognitiveLevel,
          questionType: question.questionType,
          subjectId: this.form.value.subjectId,
          topicId: this.form.value.topicId,
          subtopicId: this.form.value.subtopicId ?? undefined,
          subject: this.selectedSubject()?.name,
          topic: this.selectedTopic()?.name,
        },
      },
    });
  }

  // -------------------------------------------------------------------------
  // BATCH GENERATION WORKFLOW
  // -------------------------------------------------------------------------

  addCurrentToBatch(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const v = this.form.value;
    const subtopic = this.subtopics().find((s) => s.id === v.subtopicId);

    const item: BatchItem = {
      subjectId: v.subjectId ?? undefined,
      topicId: v.topicId ?? undefined,
      subtopicId: v.subtopicId ?? undefined,
      subject: this.selectedSubject()?.name ?? '',
      topic: this.selectedTopic()?.name ?? '',
      subtopic: subtopic?.name,
      rawTextInput: v.rawTextInput?.trim() || undefined,
      description: v.description?.trim() || undefined,
      difficulty: v.difficulty!,
      cognitiveLevel: v.cognitiveLevel!,
      questionType: v.questionType!,
      count: v.count || 5,
    };

    this.batchItems.update((items) => [...items, item]);
    this.snackBar.open(
      `Added item to batch queue (${this.totalBatchQuestions()} total questions)`,
      'OK',
      { duration: 2500 }
    );
  }

  removeBatchItem(index: number): void {
    this.batchItems.update((items) => items.filter((_, i) => i !== index));
  }

  submitBatch(): void {
    if (this.batchItems().length === 0) return;

    this.submittingBatch.set(true);
    const req: BatchGenerationRequest = {
      items: this.batchItems(),
      avoidDuplicates: true,
    };

    this.aiService.submitBatchJob(req).subscribe({
      next: (job) => {
        this.submittingBatch.set(false);
        this.batchItems.set([]);
        this.snackBar.open(
          `Batch Job #${job.id} dispatched to Bedrock background cluster!`,
          'OK',
          { duration: 5000 }
        );
        this.loadBatchJobs();
        this.startBatchPolling();
      },
      error: (err) => {
        this.submittingBatch.set(false);
        this.snackBar.open(err?.error?.message || 'Failed to submit batch job', 'Dismiss', {
          duration: 5000,
        });
      },
    });
  }

  loadBatchJobs(): void {
    this.loadingBatchJobs.set(true);
    this.aiService.listBatchJobs(0, 20).subscribe({
      next: (res) => {
        this.batchJobs.set(res.content);
        this.loadingBatchJobs.set(false);
      },
      error: () => {
        this.loadingBatchJobs.set(false);
      },
    });
  }

  cancelBatchJob(jobId: string): void {
    this.aiService.cancelBatchJob(jobId).subscribe({
      next: () => {
        this.snackBar.open('Batch generation job cancelled.', 'OK', { duration: 3000 });
        this.loadBatchJobs();
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to cancel job', 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private mapHttpError(err: any): string {
    const status = err?.status;
    const msg = err?.error?.message || err?.error?.error;
    if (msg) return msg;
    if (status === 400) return 'Bad request — check your generation parameters.';
    if (status === 403) return 'Access denied — insufficient permissions to generate questions.';
    if (status === 429) return 'Rate limit reached — please wait a moment and try again.';
    if (status >= 500)
      return 'AI service is temporarily unavailable. Ensure LiteLLM / Bedrock are operational.';
    return 'Failed to generate questions. Ensure the AI backend service is running.';
  }

  private resumeBatchPollingIfNeeded(): void {
    this.aiService.listBatchJobs(0, 20).subscribe({
      next: (res) => {
        this.batchJobs.set(res.content);
        const hasActive = res.content.some(
          (j) => j.status === 'PENDING' || j.status === 'PROCESSING'
        );
        if (hasActive) {
          this.startBatchPolling();
        }
      },
      error: () => {/* silent — polling is best-effort */},
    });
  }

  private startBatchPolling(): void {
    this.stopBatchPolling();
    this.pollSub = interval(6000)
      .pipe(
        switchMap(() => this.aiService.listBatchJobs(0, 20)),
        takeWhile(
          (res) => res.content.some((j) => j.status === 'PENDING' || j.status === 'PROCESSING'),
          true
        )
      )
      .subscribe({
        next: (res) => {
          this.batchJobs.set(res.content);
        },
      });
  }

  private stopBatchPolling(): void {
    if (this.pollSub) {
      this.pollSub.unsubscribe();
      this.pollSub = null;
    }
  }
}
