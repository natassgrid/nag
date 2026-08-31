/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSliderModule } from '@angular/material/slider';
import { MatChipsModule } from '@angular/material/chips';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
import {
  QuestionService,
  QuestionGenerationRequest,
  QuestionGenerationResponse,
  GeneratedQuestion,
  BatchGenerationRequest,
  BatchItem,
  BatchJobResponse
} from '../question.service';
import { SubjectTopicService, Subject, Topic, Subtopic } from '../subject-topic.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import { MathRendererComponent } from '../../../shared/components/math-renderer/math-renderer.component';

@Component({
  selector: 'app-ai-generate-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatSlideToggleModule,
    MatSliderModule,
    MatChipsModule,
    MatRadioModule,
    MatSnackBarModule,
    RightDrawerComponent,
    MathRendererComponent
  ],
  templateUrl: './ai-generate-dialog.component.html',
  styleUrls: ['./ai-generate-dialog.component.scss']
})
export class AiGenerateDialogComponent implements OnInit, OnChanges, OnDestroy {
  @Input() isOpen = false;
  @Output() close = new EventEmitter<boolean>();

  form!: FormGroup;

  difficulties = ['EASY', 'MEDIUM', 'HARD'];
  cognitiveLevels = ['REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'];
  questionTypes = [
    { value: 'SINGLE_MCQ', label: 'MCQ (Single Correct)' },
    { value: 'MULTI_MCQ', label: 'MSQ (Multiple Correct)' },
    { value: 'NUMERICAL', label: 'Numerical' },
    { value: 'DESCRIPTIVE', label: 'Descriptive' },
    { value: 'MATRIX_MATCH', label: 'Matrix Match' },
    { value: 'ASSERTION_REASON', label: 'Assertion & Reason' },
    { value: 'CODING', label: 'Coding' },
    { value: 'CASE_STUDY', label: 'Case Study' }
  ];

  subjects: Subject[] = [];
  topics: Topic[] = [];
  subtopics: Subtopic[] = [];

  selectedSubject: Subject | null = null;
  selectedTopic: Topic | null = null;

  // Generation mode: 'batch' (Bedrock async) or 'realtime' (LiteLLM sync)
  generationMode: 'batch' | 'realtime' = 'batch';

  generating = false;
  generationError = '';
  response: QuestionGenerationResponse | null = null;
  savingIds = new Set<number>();
  savedIndices = new Set<number>();

  // Batch job tracking
  batchItems: BatchItem[] = [];
  batchJob: BatchJobResponse | null = null;
  batchPolling = false;
  private pollSubscription: Subscription | null = null;

  constructor(
    private fb: FormBuilder,
    private subjectTopicService: SubjectTopicService,
    private questionService: QuestionService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadSubjects();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.resetState();
    }
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  private initForm(): void {
    this.form = this.fb.group({
      subject: ['', Validators.required],
      topic: ['', Validators.required],
      subtopic: [''],
      difficulty: ['', Validators.required],
      cognitiveLevel: ['', Validators.required],
      questionType: ['', Validators.required],
      count: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      avoidDuplicate: [true],
      autoSave: [true]
    });
  }

  private resetState(): void {
    this.stopPolling();
    this.initForm();
    this.generationMode = 'batch';
    this.response = null;
    this.batchJob = null;
    this.batchItems = [];
    this.generationError = '';
    this.generating = false;
    this.batchPolling = false;
    this.savingIds.clear();
    this.savedIndices.clear();
    this.topics = [];
    this.subtopics = [];
    this.selectedSubject = null;
    this.selectedTopic = null;
  }

  onModeChange(): void {
    // Both modes use the same max (5), no validator update needed
  }

  addToBatch(): void {
    this.form.markAllAsTouched();
    if (!this.form.valid) return;

    const formVal = this.form.value;
    this.batchItems.push({
      subject: formVal.subject,
      topic: formVal.topic,
      subtopic: formVal.subtopic || undefined,
      difficulty: formVal.difficulty,
      cognitiveLevel: formVal.cognitiveLevel,
      questionType: formVal.questionType,
      count: formVal.count
    });
    this.snackBar.open(`Added to batch (${this.batchItems.length} items, ${this.totalBatchQuestions} questions)`, 'OK', { duration: 2000 });
  }

  removeFromBatch(index: number): void {
    this.batchItems.splice(index, 1);
  }

  get totalBatchQuestions(): number {
    return this.batchItems.reduce((sum, item) => sum + item.count, 0);
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe(subjects => {
      this.subjects = subjects;
    });
  }

  onSubjectChange(subjectName: string): void {
    const match = this.subjects.find(s => s.name === subjectName);
    this.selectedSubject = match || null;
    this.selectedTopic = null;
    this.topics = [];
    this.subtopics = [];
    this.form.patchValue({ topic: '', subtopic: '' });
    if (match) {
      this.subjectTopicService.getTopics(match.id).subscribe(topics => {
        this.topics = topics;
      });
    }
  }

  onTopicChange(topicName: string): void {
    const match = this.topics.find(t => t.name === topicName);
    this.selectedTopic = match || null;
    this.subtopics = [];
    this.form.patchValue({ subtopic: '' });
    if (match && this.selectedSubject) {
      this.subjectTopicService.getSubtopics(match.id, this.selectedSubject.id).subscribe(subtopics => {
        this.subtopics = subtopics;
      });
    }
  }

  generate(): void {
    this.form.markAllAsTouched();
    if (!this.form.valid) return;

    if (this.generationMode === 'batch') {
      this.submitBatchJob();
    } else {
      this.generateRealtime();
    }
  }

  private submitBatchJob(): void {
    if (this.batchItems.length === 0) {
      this.generationError = 'Add at least one item to the batch before submitting.';
      return;
    }

    this.generating = true;
    this.generationError = '';
    this.batchJob = null;

    const request: BatchGenerationRequest = {
      items: this.batchItems,
      avoidDuplicates: this.form.value.avoidDuplicate
    };

    this.questionService.submitBatchJob(request).subscribe({
      next: (job) => {
        this.generating = false;
        this.batchJob = job;
        this.snackBar.open('Batch job submitted. Processing will continue in the background.', 'OK', { duration: 4000 });
        this.startPolling(job.id);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.generating = false;
        this.generationError = err?.error?.message || err?.error?.error || 'Failed to submit batch job.';
        this.cdr.detectChanges();
      }
    });
  }

  private generateRealtime(): void {
    this.generating = true;
    this.generationError = '';
    this.response = null;
    this.savedIndices.clear();

    const request: QuestionGenerationRequest = this.form.value;
    this.questionService.generateQuestions(request).subscribe({
      next: (res) => {
        this.generating = false;
        this.response = res;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.generating = false;
        this.generationError = err?.error?.message || err?.error?.error || 'Failed to generate questions. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  private startPolling(jobId: string): void {
    this.batchPolling = true;
    this.pollSubscription = interval(5000).pipe(
      switchMap(() => this.questionService.getBatchJobStatus(jobId)),
      takeWhile(job => this.isJobActive(job), true)
    ).subscribe({
      next: (job) => {
        this.batchJob = job;
        if (!this.isJobActive(job)) {
          this.batchPolling = false;
          if (job.status === 'COMPLETED' || job.status === 'PARTIALLY_COMPLETED') {
            this.snackBar.open(
              `Batch complete: ${job.totalGenerated} questions generated.`, 'OK', { duration: 5000 });
          } else if (job.status === 'FAILED') {
            this.snackBar.open('Batch job failed: ' + (job.errorMessage || 'Unknown error'), 'Dismiss', { duration: 6000 });
          }
        }
        this.cdr.detectChanges();
      },
      error: () => {
        this.batchPolling = false;
        this.cdr.detectChanges();
      }
    });
  }

  stopPolling(): void {
    if (this.pollSubscription) {
      this.pollSubscription.unsubscribe();
      this.pollSubscription = null;
    }
    this.batchPolling = false;
  }

  private isJobActive(job: BatchJobResponse): boolean {
    return job.status === 'PENDING' || job.status === 'PROCESSING';
  }

  cancelBatchJob(): void {
    if (!this.batchJob) return;
    this.questionService.cancelBatchJob(this.batchJob.id).subscribe({
      next: (job) => {
        this.batchJob = job;
        this.stopPolling();
        this.snackBar.open('Batch job cancelled.', 'OK', { duration: 3000 });
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.snackBar.open(err?.error?.message || 'Failed to cancel job', 'Dismiss', { duration: 4000 });
      }
    });
  }

  saveAsDraft(question: GeneratedQuestion, index: number): void {
    if (this.savingIds.has(index) || this.savedIndices.has(index)) return;

    this.savingIds.add(index);
    const formVal = this.form.value;
    this.questionService.createQuestion({
      subject: formVal.subject,
      topic: formVal.topic,
      subtopic: formVal.subtopic || undefined,
      difficulty: question.difficulty,
      cognitiveLevel: question.cognitiveLevel,
      questionType: question.questionType,
      content: question.content,
      answerKey: question.answerKey,
      options: question.options
    }).subscribe({
      next: () => {
        this.savingIds.delete(index);
        this.savedIndices.add(index);
        this.snackBar.open('Question saved as draft', 'OK', { duration: 3000 });
      },
      error: (err) => {
        this.savingIds.delete(index);
        this.snackBar.open(err?.error?.message || 'Failed to save question', 'Dismiss', { duration: 4000 });
      }
    });
  }

  canSave(question: GeneratedQuestion, index: number): boolean {
    return question.validation?.valid && !question.duplicate && !this.savedIndices.has(index) && !question.savedQuestionId;
  }

  onClose(): void {
    this.stopPolling();
    const hasSaved = this.savedIndices.size > 0
      || (this.response?.questions?.some(q => q.savedQuestionId) ?? false)
      || (this.batchJob?.totalGenerated ?? 0) > 0;
    this.close.emit(hasSaved);
  }
}
