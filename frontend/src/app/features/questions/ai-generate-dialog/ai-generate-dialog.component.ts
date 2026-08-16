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

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSliderModule } from '@angular/material/slider';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  QuestionService,
  QuestionGenerationRequest,
  QuestionGenerationResponse,
  GeneratedQuestion
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
    MatSlideToggleModule,
    MatSliderModule,
    MatChipsModule,
    MatSnackBarModule,
    RightDrawerComponent,
    MathRendererComponent
  ],
  templateUrl: './ai-generate-dialog.component.html',
  styleUrls: ['./ai-generate-dialog.component.scss']
})
export class AiGenerateDialogComponent implements OnInit, OnChanges {
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

  generating = false;
  generationError = '';
  response: QuestionGenerationResponse | null = null;
  savingIds = new Set<number>();
  savedIndices = new Set<number>();

  constructor(
    private fb: FormBuilder,
    private subjectTopicService: SubjectTopicService,
    private questionService: QuestionService,
    private snackBar: MatSnackBar
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

  private initForm(): void {
    this.form = this.fb.group({
      subject: ['', Validators.required],
      topic: ['', Validators.required],
      subtopic: [''],
      difficulty: ['', Validators.required],
      cognitiveLevel: ['', Validators.required],
      questionType: ['', Validators.required],
      count: [3, [Validators.required, Validators.min(1), Validators.max(10)]],
      avoidDuplicate: [true],
      autoSave: [false]
    });
  }

  private resetState(): void {
    this.initForm();
    this.response = null;
    this.generationError = '';
    this.generating = false;
    this.savingIds.clear();
    this.savedIndices.clear();
    this.topics = [];
    this.subtopics = [];
    this.selectedSubject = null;
    this.selectedTopic = null;
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

    this.generating = true;
    this.generationError = '';
    this.response = null;
    this.savedIndices.clear();

    const request: QuestionGenerationRequest = this.form.value;
    this.questionService.generateQuestions(request).subscribe({
      next: (res) => {
        this.generating = false;
        this.response = res;
      },
      error: (err) => {
        this.generating = false;
        this.generationError = err?.error?.message || err?.error?.error || 'Failed to generate questions. Please try again.';
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
    const hasSaved = this.savedIndices.size > 0 || (this.response?.questions?.some(q => q.savedQuestionId) ?? false);
    this.close.emit(hasSaved);
  }
}
