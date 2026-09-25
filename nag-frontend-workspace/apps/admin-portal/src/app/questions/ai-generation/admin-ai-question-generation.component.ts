import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  QuestionAiService,
  QuestionBankService,
  QuestionGenerationRequest,
  QuestionGenerationResponse,
  GeneratedQuestion,
  BatchGenerationRequest,
  BatchItem,
  BatchGenerationJob,
} from '@nag-frontend-workspace/questions-data-access';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';

@Component({
  selector: 'nag-admin-ai-question-generation',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  templateUrl: './admin-ai-question-generation.component.html',
  styleUrls: ['./admin-ai-question-generation.component.scss'],
})
export class AdminAiQuestionGenerationComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly aiService = inject(QuestionAiService);
  private readonly questionBankService = inject(QuestionBankService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);

  // View state
  readonly activeTab = signal<'realtime' | 'batch'>('realtime');
  readonly generating = signal<boolean>(false);
  readonly response = signal<QuestionGenerationResponse | null>(null);
  readonly generationError = signal<string>('');
  readonly savedQuestionIds = signal<Set<number>>(new Set());
  readonly savingIndices = signal<Set<number>>(new Set());

  // Batch state
  readonly batchItems = signal<BatchItem[]>([]);
  readonly batchJobs = signal<BatchGenerationJob[]>([]);
  readonly loadingBatchJobs = signal<boolean>(false);
  readonly submittingBatch = signal<boolean>(false);
  private pollSub: Subscription | null = null;

  // Curated Subjects & Topics
  readonly subjects = [
    'Mathematics',
    'Physics',
    'Chemistry',
    'Computer Science',
    'General Knowledge & Indian History',
    'Biology & Life Sciences',
    'Logical Reasoning & Aptitude',
  ];

  readonly difficulties = ['EASY', 'MEDIUM', 'HARD'];

  readonly cognitiveLevels = [
    { value: 'REMEMBER', label: 'Remember (Recall facts & basic concepts)' },
    { value: 'UNDERSTAND', label: 'Understand (Explain ideas or concepts)' },
    { value: 'APPLY', label: 'Apply (Use information in new situations)' },
    { value: 'ANALYZE', label: 'Analyze (Draw connections among ideas)' },
    { value: 'EVALUATE', label: 'Evaluate (Justify a stand or decision)' },
    { value: 'CREATE', label: 'Create (Produce new or original work)' },
  ];

  readonly questionTypes = [
    { value: 'SINGLE_MCQ', label: 'Single Choice MCQ' },
    { value: 'MULTI_MCQ', label: 'Multiple Correct (MSQ)' },
    { value: 'NUMERICAL', label: 'Numerical / Decimal Answer' },
    { value: 'DESCRIPTIVE', label: 'Descriptive / Long Answer' },
  ];

  // Reactive generation form
  readonly form = this.fb.group({
    subject: ['Mathematics', Validators.required],
    topic: ['Linear Algebra & Matrices', Validators.required],
    subtopic: ['Eigenvalues and Characteristic Equations'],
    difficulty: ['MEDIUM', Validators.required],
    cognitiveLevel: ['APPLY', Validators.required],
    questionType: ['SINGLE_MCQ', Validators.required],
    count: [3, [Validators.required, Validators.min(1), Validators.max(5)]],
    avoidDuplicate: [true],
    autoSave: [false],
  });

  ngOnInit(): void {
    this.loadBatchJobs();
  }

  ngOnDestroy(): void {
    this.stopBatchPolling();
  }

  // Preset topic helper
  onSubjectChange(subject: string): void {
    const defaultTopics: Record<string, { topic: string; subtopic: string }> = {
      Mathematics: { topic: 'Linear Algebra & Matrices', subtopic: 'Eigenvalues and Eigenvectors' },
      Physics: { topic: 'Electromagnetism & Waves', subtopic: 'Gauss Law and Flux' },
      Chemistry: { topic: 'Organic Chemistry', subtopic: 'Electrophilic Aromatic Substitution' },
      'Computer Science': { topic: 'Algorithms & Data Structures', subtopic: 'Graph Traversal & Shortest Path' },
      'General Knowledge & Indian History': { topic: 'Indian Constitution', subtopic: 'Fundamental Rights & Directive Principles' },
      'Biology & Life Sciences': { topic: 'Cell Biology & Genetics', subtopic: 'Mendelian Inheritance' },
      'Logical Reasoning & Aptitude': { topic: 'Deductive Logic', subtopic: 'Syllogisms and Venn Diagrams' },
    };

    if (defaultTopics[subject]) {
      this.form.patchValue({
        topic: defaultTopics[subject].topic,
        subtopic: defaultTopics[subject].subtopic,
      });
    }
  }

  // Real-time LLM Generation
  generateQuestions(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    this.generating.set(true);
    this.generationError.set('');
    this.response.set(null);
    this.savedQuestionIds.set(new Set());

    const req: QuestionGenerationRequest = {
      subject: this.form.value.subject!,
      topic: this.form.value.topic!,
      subtopic: this.form.value.subtopic || undefined,
      difficulty: this.form.value.difficulty!,
      cognitiveLevel: this.form.value.cognitiveLevel!,
      questionType: this.form.value.questionType!,
      count: this.form.value.count || 3,
      avoidDuplicate: this.form.value.avoidDuplicate ?? true,
      autoSave: this.form.value.autoSave ?? false,
    };

    this.aiService.generateQuestions(req).subscribe({
      next: (res) => {
        this.generating.set(false);
        this.response.set(res);
        this.snackBar.open(
          `Successfully synthesized ${res.totalGenerated} questions via ${res.modelUsed || 'AI Engine'}!`,
          'OK',
          { duration: 4000 }
        );
      },
      error: (err) => {
        this.generating.set(false);
        const errorMsg =
          err?.error?.message ||
          err?.error?.error ||
          'Failed to generate questions. Ensure backend AI service and LiteLLM/Bedrock are operational.';
        this.generationError.set(errorMsg);
        this.snackBar.open(errorMsg, 'Dismiss', { duration: 6000 });
      },
    });
  }

  // Save single generated question into Question Bank as DRAFT
  saveQuestionToBank(question: GeneratedQuestion, index: number): void {
    if (this.savingIndices().has(index) || this.savedQuestionIds().has(index)) return;

    this.savingIndices.update((set) => new Set(set).add(index));

    const payload = {
      content: question.content,
      subject: this.form.value.subject,
      topic: this.form.value.topic,
      subtopic: this.form.value.subtopic || undefined,
      difficulty: question.difficulty || this.form.value.difficulty,
      type: question.questionType || this.form.value.questionType,
      marks: 4,
      negativeMarks: 1,
      options: question.options || [],
      answerKey: question.answerKey,
      explanation: question.explanation,
      status: 'DRAFT',
      tags: ['AI-Generated', this.form.value.subject || '', this.form.value.topic || ''],
    };

    this.questionBankService.createQuestion(payload).subscribe({
      next: (created) => {
        this.savingIndices.update((set) => {
          const next = new Set(set);
          next.delete(index);
          return next;
        });
        this.savedQuestionIds.update((set) => new Set(set).add(index));
        this.snackBar.open(`Question saved as Draft in Question Bank! (ID: ${created.id.substring(0, 8)})`, 'OK', {
          duration: 3500,
        });
      },
      error: (err) => {
        this.savingIndices.update((set) => {
          const next = new Set(set);
          next.delete(index);
          return next;
        });
        this.snackBar.open(err?.error?.message || 'Failed to save question to bank', 'Dismiss', {
          duration: 4000,
        });
      },
    });
  }

  // Save all valid generated questions
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

  // Edit in Authoring View
  openInAuthoring(question: GeneratedQuestion): void {
    this.router.navigate(['/questions/authoring']);
  }

  // BATCH GENERATION WORKFLOW
  addCurrentToBatch(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid) return;

    const v = this.form.value;
    const item: BatchItem = {
      subject: v.subject!,
      topic: v.topic!,
      subtopic: v.subtopic || undefined,
      difficulty: v.difficulty!,
      cognitiveLevel: v.cognitiveLevel!,
      questionType: v.questionType!,
      count: v.count || 5,
    };

    this.batchItems.update((items) => [...items, item]);
    this.snackBar.open(`Added item to batch queue (${this.totalBatchQuestions} total questions)`, 'OK', {
      duration: 2500,
    });
  }

  removeBatchItem(index: number): void {
    this.batchItems.update((items) => items.filter((_, i) => i !== index));
  }

  get totalBatchQuestions(): number {
    return this.batchItems().reduce((acc, item) => acc + item.count, 0);
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
        this.snackBar.open(`Batch Job #${job.id} dispatched to Bedrock background cluster!`, 'OK', {
          duration: 5000,
        });
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
        this.snackBar.open(err?.error?.message || 'Failed to cancel job', 'Dismiss', { duration: 4000 });
      },
    });
  }

  private startBatchPolling(): void {
    this.stopBatchPolling();
    this.pollSub = interval(6000)
      .pipe(
        switchMap(() => this.aiService.listBatchJobs(0, 20)),
        takeWhile((res) => res.content.some((j) => j.status === 'PENDING' || j.status === 'PROCESSING'), true)
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
