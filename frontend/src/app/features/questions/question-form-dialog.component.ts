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

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CreateQuestionRequest, QuestionResponse, QuestionService } from './question.service';
import { SubjectTopicService, Subject, Topic, Subtopic } from './subject-topic.service';
import { ExamEditorComponent } from '../../shared/components/exam-editor';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';
import { MathRendererComponent } from '../../shared/components/math-renderer/math-renderer.component';

@Component({
  selector: 'app-question-form-dialog',
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
    MatCheckboxModule,
    MatSlideToggleModule,
    ExamEditorComponent,
    RightDrawerComponent,
    MathRendererComponent
  ],
  templateUrl: './question-form-dialog.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrls: ['./question-form-dialog.component.scss']
})
export class QuestionFormDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() question?: QuestionResponse;
  @Output() close = new EventEmitter<QuestionResponse | null>();

  form!: FormGroup;

  difficulties = ['EASY', 'MEDIUM', 'HARD'];
  cognitiveLevels = ['REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'];
  questionTypes = [
    { value: 'SINGLE_MCQ',      label: 'MCQ (Single Correct)' },
    { value: 'MULTI_MCQ',       label: 'MSQ (Multiple Correct)' },
    { value: 'NUMERICAL',       label: 'Numerical' },
    { value: 'DESCRIPTIVE',     label: 'Descriptive' },
    { value: 'MATRIX_MATCH',    label: 'Matrix Match' },
    { value: 'ASSERTION_REASON',label: 'Assertion & Reason' },
    { value: 'CODING',          label: 'Coding' },
    { value: 'CASE_STUDY',      label: 'Case Study' },
  ];

  subjects: Subject[] = [];
  topics: Topic[] = [];
  subtopics: Subtopic[] = [];

  selectedSubject: Subject | null = null;
  selectedTopic: Topic | null = null;

  showNewSubject = false;
  showNewTopic = false;
  showNewSubtopic = false;
  newSubjectName = '';
  newTopicName = '';
  newSubtopicName = '';
  creatingSubject = false;
  creatingTopic = false;
  creatingSubtopic = false;

  options: { id: string; text: string; isCorrect: boolean }[] = [];
  optionIds = ['A', 'B', 'C', 'D', 'E', 'F'];
  optionError = '';
  saving = false;
  saveError = '';
  currentQuestionType = '';

  editorContent: string = '';
  explanationContent: string = '';

  showContentPreview = false;
  showOptionPreviews = false;
  showExplanationPreview = false;

  constructor(
    private fb: FormBuilder,
    private subjectTopicService: SubjectTopicService,
    private questionService: QuestionService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadSubjects();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
    }
  }

  initForm(): void {
    const q = this.question;
    this.form = this.fb.group({
      subject: [q?.subject || '', Validators.required],
      topic: [q?.topic || '', Validators.required],
      subtopic: [q?.subtopic || ''],
      difficulty: [q?.difficulty || '', Validators.required],
      cognitiveLevel: [q?.cognitiveLevel || '', Validators.required],
      questionType: [q?.questionType || '', Validators.required],
      content: [q?.content || '', Validators.required],
      answerKey: [q?.answerKey || ''],
      explanation: [q?.explanation || '']
    });

    if (q?.content) {
      this.editorContent = q.content;
    } else {
      this.editorContent = '';
    }

    if (q?.explanation) {
      this.explanationContent = q.explanation;
    } else {
      this.explanationContent = '';
    }

    if (q?.options && q.options.length > 0) {
      this.options = q.options.map(o => ({
        id: o.id,
        text: o.text,
        isCorrect: o.isCorrect
      }));
    } else {
      this.options = [];
    }

    const initialType = this.form.get('questionType')?.value || '';
    if (initialType) {
      this.onQuestionTypeChange(initialType);
    }
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe(subjects => {
      this.subjects = subjects;
      if (this.question?.subject) {
        const match = subjects.find(s => s.name === this.question!.subject);
        if (match) {
          this.selectedSubject = match;
          this.loadTopics(match);
        }
      }
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
      this.loadTopics(match);
    }
  }

  loadTopics(subject: Subject): void {
    this.subjectTopicService.getTopics(subject.id).subscribe(topics => {
      this.topics = topics;
      if (this.question?.topic) {
        const match = topics.find(t => t.name === this.question!.topic);
        if (match) {
          this.selectedTopic = match;
          this.loadSubtopics(match);
        }
      }
    });
  }

  onTopicChange(topicName: string): void {
    const match = this.topics.find(t => t.name === topicName);
    this.selectedTopic = match || null;
    this.subtopics = [];
    this.form.patchValue({ subtopic: '' });
    if (match) {
      this.loadSubtopics(match);
    }
  }

  loadSubtopics(topic: Topic): void {
    if (!this.selectedSubject) return;
    this.subjectTopicService.getSubtopics(topic.id, this.selectedSubject.id).subscribe(subtopics => {
      this.subtopics = subtopics;
    });
  }

  toggleNewSubject(): void {
    this.showNewSubject = !this.showNewSubject;
    this.newSubjectName = '';
  }

  toggleNewTopic(): void {
    this.showNewTopic = !this.showNewTopic;
    this.newTopicName = '';
  }

  toggleNewSubtopic(): void {
    this.showNewSubtopic = !this.showNewSubtopic;
    this.newSubtopicName = '';
  }

  createNewSubject(): void {
    if (!this.newSubjectName) return;
    this.creatingSubject = true;
    this.subjectTopicService.createSubject({ name: this.newSubjectName }).subscribe({
      next: (subject) => {
        this.subjects = [...this.subjects, subject];
        this.form.patchValue({ subject: subject.name });
        this.onSubjectChange(subject.name);
        this.showNewSubject = false;
        this.newSubjectName = '';
        this.creatingSubject = false;
      },
      error: () => {
        this.creatingSubject = false;
      }
    });
  }

  createNewTopic(): void {
    if (!this.newTopicName || !this.selectedSubject) return;
    this.creatingTopic = true;
    this.subjectTopicService.createTopic(this.selectedSubject.id, { name: this.newTopicName }).subscribe({
      next: (topic) => {
        this.topics = [...this.topics, topic];
        this.form.patchValue({ topic: topic.name });
        this.onTopicChange(topic.name);
        this.showNewTopic = false;
        this.newTopicName = '';
        this.creatingTopic = false;
      },
      error: () => {
        this.creatingTopic = false;
      }
    });
  }

  createNewSubtopic(): void {
    if (!this.newSubtopicName || !this.selectedSubject || !this.selectedTopic) return;
    this.creatingSubtopic = true;
    this.subjectTopicService.createSubtopic(
      this.selectedSubject.id,
      this.selectedTopic.id,
      { name: this.newSubtopicName }
    ).subscribe({
      next: (subtopic) => {
        this.subtopics = [...this.subtopics, subtopic];
        this.form.patchValue({ subtopic: subtopic.name });
        this.showNewSubtopic = false;
        this.newSubtopicName = '';
        this.creatingSubtopic = false;
      },
      error: () => {
        this.creatingSubtopic = false;
      }
    });
  }

  onEditorChange(html: string): void {
    this.editorContent = html;
    this.form.patchValue({ content: html });
    this.form.get('content')?.markAsDirty();
    this.form.get('content')?.markAsTouched();
  }

  onExplanationChange(html: string): void {
    this.explanationContent = html;
    this.form.patchValue({ explanation: html });
    this.form.get('explanation')?.markAsDirty();
    this.form.get('explanation')?.markAsTouched();
  }

  formatLatex(text: string): string {
    if (!text) return '';
    let formatted = text;

    // Convert single-dollar $math$ to $$math$$ (enclosed in $$...$$)
    formatted = formatted.replace(/(^|[^$])\$([^$\n]+)\$([^$]|$)/g, '$1$$$$$2$$$$$3');

    // Remove empty math blocks $$ $$ or $$$$
    formatted = formatted.replace(/\$\$\s*\$\$/g, '');

    // If odd number of $$, strip unmatched leading/trailing $$
    const matches = formatted.match(/\$\$/g);
    if (matches && matches.length % 2 !== 0) {
      if (formatted.startsWith('$$')) {
        formatted = formatted.substring(2);
      } else if (formatted.endsWith('$$')) {
        formatted = formatted.substring(0, formatted.length - 2);
      }
    }
    return formatted;
  }

  onQuestionTypeChange(value: string): void {
    this.currentQuestionType = value;
    if (value !== 'SINGLE_MCQ' && value !== 'MULTI_MCQ') {
      this.options = [];
    } else if (this.options.length === 0) {
      this.options = [
        { id: 'A', text: '', isCorrect: false },
        { id: 'B', text: '', isCorrect: false }
      ];
    }
    this.optionError = '';
  }

  isMcqOrMsq(): boolean {
    return this.currentQuestionType === 'SINGLE_MCQ' || this.currentQuestionType === 'MULTI_MCQ';
  }

  isMcq(): boolean {
    return this.currentQuestionType === 'SINGLE_MCQ';
  }

  addOption(): void {
    if (this.options.length < 5) {
      this.options.push({ id: this.optionIds[this.options.length], text: '', isCorrect: false });
    }
  }

  removeOption(i: number): void {
    if (this.options.length > 2) this.options.splice(i, 1);
  }

  onMcqCorrectChange(checkedIndex: number): void {
    this.options.forEach((o, i) => { if (i !== checkedIndex) o.isCorrect = false; });
  }

  cancel(): void {
    this.close.emit(null);
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.valid) {
      const value: CreateQuestionRequest = { ...this.form.value };
      // Attach numeric hierarchy ids (source of truth for the backend link).
      // The form controls hold names; the selected entities carry the ids.
      value.subjectId = this.selectedSubject?.id ?? undefined;
      value.topicId = this.selectedTopic?.id ?? undefined;
      const selectedSubtopic = this.subtopics.find(st => st.name === value.subtopic);
      value.subtopicId = selectedSubtopic?.id ?? undefined;
      if (value.content) value.content = this.formatLatex(value.content);
      if (value.explanation) value.explanation = this.formatLatex(value.explanation);
      if (this.isMcqOrMsq() && this.options.length >= 2) {
        const correct = this.options.filter(o => o.isCorrect).length;
        if (this.isMcq() && correct !== 1) {
          this.optionError = 'MCQ requires exactly one correct option'; return;
        }
        if (!this.isMcq() && correct < 1) {
          this.optionError = 'MSQ requires at least one correct option'; return;
        }
        value.options = this.options.map((o, i) => ({
          id: this.optionIds[i],
          text: this.formatLatex(o.text),
          isCorrect: o.isCorrect
        }));
      }
      this.optionError = '';
      this.saveError = '';
      this.saving = true;

      const call = this.question
        ? this.questionService.updateQuestion(this.question.id, value)
        : this.questionService.createQuestion(value);

      call.subscribe({
        next: (res) => {
          this.saving = false;
          this.close.emit(res);
        },
        error: (err) => {
          this.saving = false;
          this.saveError = err?.error?.message || err?.error?.error || 'Failed to save question. Please try again.';
        }
      });
    }
  }
}
