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

import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  SubjectTopicService,
  Subject,
  Topic,
  Subtopic
} from './subject-topic.service';
import {
  QuestionService,
  QuestionResponse,
  CreateQuestionRequest,
  QuestionOptionDto
} from './question.service';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';
import { SubQuestionFormComponent } from '../../shared/components/sub-question-form/sub-question-form.component';

@Component({
  selector: 'app-question-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    RightDrawerComponent,
    SubQuestionFormComponent
  ],
  templateUrl: './question-form-dialog.component.html',
  styleUrls: ['./question-form-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class QuestionFormDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() question: QuestionResponse | null | undefined = null;
  @Output() close = new EventEmitter<QuestionResponse | null>();

  form!: FormGroup;

  difficulties = ['EASY', 'MEDIUM', 'HARD', 'EXPERT'];
  cognitiveLevels = ['REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'];
  questionTypes = [
    { value: 'SINGLE_MCQ', label: 'Single Choice (MCQ)' },
    { value: 'MULTI_MCQ', label: 'Multiple Choice (MSQ)' },
    { value: 'NUMERICAL', label: 'Numerical' },
    { value: 'DESCRIPTIVE', label: 'Descriptive' }
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

  /** Option validation error — passed as @Input to SubQuestionFormComponent. */
  optionError = '';
  saving = false;
  saveError = '';

  /** Option letter labels used when serialising options in save(). */
  readonly optionIds = ['A', 'B', 'C', 'D', 'E', 'F'];

  constructor(
    private fb: FormBuilder,
    private subjectTopicService: SubjectTopicService,
    private questionService: QuestionService,
    private cdr: ChangeDetectorRef
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadSubjects();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
      this.syncHierarchyFromQuestion();
    } else if (changes['question'] && this.isOpen) {
      this.initForm();
      this.syncHierarchyFromQuestion();
    }
  }

  private unescapeNewlines(text?: string | null): string {
    if (!text) return '';
    return text
      .replace(/\\r\\n/g, '\n')
      .replace(/\\n/g, '\n')
      .replace(/\\t/g, '\t');
  }

  initForm(): void {
    const q = this.question;
    const content = this.unescapeNewlines(q?.content);
    const explanation = this.unescapeNewlines(q?.explanation);

    // Build options FormArray — rich mode groups include imageUrl / imageAltText / isImageOnly
    let optionGroups: FormGroup[] = [];
    const qt = q?.questionType || '';
    if (q?.options && q.options.length > 0) {
      optionGroups = q.options.map((o: QuestionOptionDto) =>
        this.fb.group({
          text: [this.unescapeNewlines(o.text)],
          isCorrect: [o.isCorrect],
          imageUrl: [o.imageUrl || ''],
          imageAltText: [o.imageAltText || ''],
          isImageOnly: [!!o.imageUrl && (!o.text || !o.text.trim())]
        })
      );
    } else if (qt === 'SINGLE_MCQ' || qt === 'MULTI_MCQ') {
      optionGroups = [
        this.fb.group({ text: [''], isCorrect: [false], imageUrl: [''], imageAltText: [''], isImageOnly: [false] }),
        this.fb.group({ text: [''], isCorrect: [false], imageUrl: [''], imageAltText: [''], isImageOnly: [false] })
      ];
    }

    this.form = this.fb.group({
      subject: [q?.subject || '', Validators.required],
      topic: [q?.topic || '', Validators.required],
      subtopic: [q?.subtopic || ''],
      difficulty: [q?.difficulty || '', Validators.required],
      cognitiveLevel: [q?.cognitiveLevel || '', Validators.required],
      questionType: [q?.questionType || '', Validators.required],
      content: [content, Validators.required],
      answerKey: [q?.answerKey || ''],
      explanation: [explanation],
      options: this.fb.array(optionGroups)
    });

    this.optionError = '';
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe((subjects: Subject[]) => {
      this.subjects = subjects;
      if (this.question?.subject || this.question?.subjectId) {
        this.syncHierarchyFromQuestion();
      }
      this.cdr.markForCheck();
    });
  }

  syncHierarchyFromQuestion(): void {
    if (!this.question) {
      this.selectedSubject = null;
      this.selectedTopic = null;
      this.topics = [];
      this.subtopics = [];
      this.cdr.markForCheck();
      return;
    }

    const syncWithSubjects = (subjects: Subject[]) => {
      const qSubjectName = this.question?.subject;
      const qSubjectId = this.question?.subjectId;
      const subject = subjects.find(s =>
        (qSubjectId != null && s.id === qSubjectId) ||
        (qSubjectName && s.name.toLowerCase() === qSubjectName.toLowerCase())
      );

      if (subject) {
        this.selectedSubject = subject;
        this.form.patchValue({ subject: subject.name });
        this.subjectTopicService.getTopics(subject.id).subscribe((topics: Topic[]) => {
          this.topics = topics;
          const qTopicName = this.question?.topic;
          const qTopicId = this.question?.topicId;
          const topic = topics.find(t =>
            (qTopicId != null && t.id === qTopicId) ||
            (qTopicName && t.name.toLowerCase() === qTopicName.toLowerCase())
          );

          if (topic) {
            this.selectedTopic = topic;
            this.form.patchValue({ topic: topic.name });
            this.subjectTopicService.getSubtopics(subject.id, topic.id).subscribe((subtopics: Subtopic[]) => {
              this.subtopics = subtopics;
              const qSubtopicName = this.question?.subtopic;
              const qSubtopicId = this.question?.subtopicId;
              const subtopic = subtopics.find(st =>
                (qSubtopicId != null && st.id === qSubtopicId) ||
                (qSubtopicName && st.name.toLowerCase() === qSubtopicName.toLowerCase())
              );
              if (subtopic) {
                this.form.patchValue({ subtopic: subtopic.name });
              }
              this.cdr.markForCheck();
            });
          }
          this.cdr.markForCheck();
        });
      }
    };

    if (this.subjects.length > 0) {
      syncWithSubjects(this.subjects);
    } else {
      this.subjectTopicService.getSubjects().subscribe((subjects: Subject[]) => {
        this.subjects = subjects;
        syncWithSubjects(subjects);
      });
    }
  }

  onSubjectChange(subjectName: string): void {
    const subject = this.subjects.find(s => s.name === subjectName);
    this.selectedSubject = subject || null;
    this.selectedTopic = null;
    this.topics = [];
    this.subtopics = [];
    this.form.patchValue({ topic: '', subtopic: '' });

    if (subject) {
      this.subjectTopicService.getTopics(subject.id).subscribe((topics: Topic[]) => {
        this.topics = topics;
        this.cdr.markForCheck();
      });
    }
  }

  onTopicChange(topicName: string): void {
    const topic = this.topics.find(t => t.name === topicName);
    this.selectedTopic = topic || null;
    this.subtopics = [];
    this.form.patchValue({ subtopic: '' });

    if (this.selectedSubject && topic) {
      this.subjectTopicService.getSubtopics(this.selectedSubject.id, topic.id).subscribe((subtopics: Subtopic[]) => {
        this.subtopics = subtopics;
        this.cdr.markForCheck();
      });
    }
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
      next: (subject: Subject) => {
        this.subjects = [...this.subjects, subject];
        this.form.patchValue({ subject: subject.name });
        this.onSubjectChange(subject.name);
        this.showNewSubject = false;
        this.newSubjectName = '';
        this.creatingSubject = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.creatingSubject = false;
        this.cdr.markForCheck();
      }
    });
  }

  createNewTopic(): void {
    if (!this.newTopicName || !this.selectedSubject) return;
    this.creatingTopic = true;
    this.subjectTopicService.createTopic(this.selectedSubject.id, { name: this.newTopicName }).subscribe({
      next: (topic: Topic) => {
        this.topics = [...this.topics, topic];
        this.form.patchValue({ topic: topic.name });
        this.onTopicChange(topic.name);
        this.showNewTopic = false;
        this.newTopicName = '';
        this.creatingTopic = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.creatingTopic = false;
        this.cdr.markForCheck();
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
      next: (subtopic: Subtopic) => {
        this.subtopics = [...this.subtopics, subtopic];
        this.form.patchValue({ subtopic: subtopic.name });
        this.showNewSubtopic = false;
        this.newSubtopicName = '';
        this.creatingSubtopic = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.creatingSubtopic = false;
        this.cdr.markForCheck();
      }
    });
  }

  formatLatex(text: string): string {
    if (!text) return '';
    let formatted = text;

    // Convert single-dollar $math$ to $$math$$
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

  cancel(): void {
    this.close.emit(null);
  }

  save(): void {
    this.form.markAllAsTouched();
    if (!this.form.valid) return;

    const value: CreateQuestionRequest = { ...this.form.value };
    value.subjectId = this.selectedSubject?.id ?? undefined;
    value.topicId = this.selectedTopic?.id ?? undefined;
    const selectedSubtopic = this.subtopics.find(st => st.name === value.subtopic);
    value.subtopicId = selectedSubtopic?.id ?? undefined;

    if (value.content) value.content = this.formatLatex(value.content);
    if (value.explanation) value.explanation = this.formatLatex(value.explanation);

    const qt = this.form.get('questionType')?.value || '';
    const isMcqOrMsq = qt === 'SINGLE_MCQ' || qt === 'MULTI_MCQ';
    const isMcq = qt === 'SINGLE_MCQ';
    const optionsArray = this.form.get('options') as FormArray;

    if (isMcqOrMsq && optionsArray.length >= 2) {
      const correct = optionsArray.controls.filter(c => c.get('isCorrect')?.value).length;
      if (isMcq && correct !== 1) {
        this.optionError = 'MCQ requires exactly one correct option';
        this.cdr.markForCheck();
        return;
      }
      if (!isMcq && correct < 1) {
        this.optionError = 'MSQ requires at least one correct option';
        this.cdr.markForCheck();
        return;
      }
      for (let i = 0; i < optionsArray.length; i++) {
        const c = optionsArray.at(i);
        const hasText = (c.get('text')?.value || '').trim().length > 0;
        const hasImg = (c.get('imageUrl')?.value || '').trim().length > 0;
        if (!hasText && !hasImg) {
          this.optionError = `Option ${this.optionIds[i]} must have either text or an image.`;
          this.cdr.markForCheck();
          return;
        }
      }
      value.options = optionsArray.controls.map((c, i) => ({
        id: this.optionIds[i],
        text: this.formatLatex(c.get('text')?.value || ''),
        isCorrect: c.get('isCorrect')?.value,
        imageUrl: (c.get('imageUrl')?.value || '').trim() || undefined,
        imageAltText: (c.get('imageAltText')?.value || '').trim() || undefined
      }));
    }

    this.optionError = '';
    this.saving = true;
    this.saveError = '';

    const req$ = this.question?.id
      ? this.questionService.updateQuestion(this.question.id, value)
      : this.questionService.createQuestion(value);

    req$.subscribe({
      next: (savedQuestion: QuestionResponse) => {
        this.saving = false;
        this.close.emit(savedQuestion);
      },
      error: (err: any) => {
        this.saving = false;
        this.saveError = err?.error?.message || 'Failed to save question. Please verify all fields.';
        this.cdr.markForCheck();
      }
    });
  }
}
