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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.\n */

import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  FormArray,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PassageService, PassageRequest, PassageResponse } from '../passage.service';
import { SubjectTopicService, Subject, Topic, Subtopic } from '../subject-topic.service';
import { ExamEditorComponent } from '../../../shared/components/exam-editor/exam-editor.component';

@Component({
  selector: 'app-passage-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,
    MatSnackBarModule,
    MatTabsModule,
    MatTooltipModule,
    ExamEditorComponent
  ],
  templateUrl: './passage-form-dialog.component.html',
  styleUrls: ['./passage-form-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PassageFormDialogComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() passage?: PassageResponse;
  @Output() close = new EventEmitter<boolean>();

  form!: FormGroup;
  isSubmitting = false;

  subjects: Subject[] = [];
  topics: Topic[] = [];
  subtopics: Subtopic[] = [];

  readonly difficultyOptions = ['EASY', 'MEDIUM', 'HARD'];
  readonly cognitiveLevels = ['KNOWLEDGE', 'COMPREHENSION', 'APPLICATION', 'ANALYSIS', 'SYNTHESIS', 'EVALUATION'];
  readonly questionTypes = [
    { label: 'Multiple Choice (Single)', value: 'SINGLE_MCQ' },
    { label: 'Multiple Choice (Multiple)', value: 'MULTIPLE_MCQ' },
    { label: 'True / False', value: 'TRUE_FALSE' },
    { label: 'Numerical / Integer', value: 'NUMERICAL' }
  ];

  constructor(
    private fb: FormBuilder,
    private passageService: PassageService,
    private subjectTopicService: SubjectTopicService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {
    this.buildForm();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.loadSubjects();
      if (this.passage) {
        this.populateForm(this.passage);
      } else {
        this.resetForm();
      }
    }
  }

  get subQuestionsArray(): FormArray {
    return this.form.get('subQuestions') as FormArray;
  }

  private buildForm(): void {
    this.form = this.fb.group({
      title: [''],
      subjectId: [null, Validators.required],
      topicId: [null, Validators.required],
      subtopicId: [null],
      subject: [''],
      topic: [''],
      subtopic: [''],
      content: ['', [Validators.required, Validators.minLength(20)]],
      subQuestions: this.fb.array([])
    });

    // Default 2 sub questions for a new comprehension passage
    this.addSubQuestion();
    this.addSubQuestion();
  }

  createSubQuestionGroup(orderIndex: number): FormGroup {
    return this.fb.group({
      passageOrderIndex: [orderIndex],
      content: ['', Validators.required],
      difficulty: ['MEDIUM', Validators.required],
      cognitiveLevel: ['COMPREHENSION', Validators.required],
      questionType: ['SINGLE_MCQ', Validators.required],
      explanation: [''],
      options: this.fb.array([
        this.createOptionGroup('Option A', true),
        this.createOptionGroup('Option B', false),
        this.createOptionGroup('Option C', false),
        this.createOptionGroup('Option D', false)
      ])
    });
  }

  createOptionGroup(text: string = '', isCorrect: boolean = false): FormGroup {
    return this.fb.group({
      text: [text, Validators.required],
      isCorrect: [isCorrect]
    });
  }

  getOptionsArray(qIndex: number): FormArray {
    return this.subQuestionsArray.at(qIndex).get('options') as FormArray;
  }

  addSubQuestion(): void {
    if (this.subQuestionsArray.length >= 10) {
      this.snackBar.open('Maximum 10 sub-questions allowed per passage.', 'OK', { duration: 3000 });
      return;
    }
    const newIdx = this.subQuestionsArray.length + 1;
    this.subQuestionsArray.push(this.createSubQuestionGroup(newIdx));
    this.cdr.markForCheck();
  }

  removeSubQuestion(index: number): void {
    if (this.subQuestionsArray.length <= 2) {
      this.snackBar.open('A comprehension passage requires at least 2 sub-questions.', 'OK', { duration: 3000 });
      return;
    }
    this.subQuestionsArray.removeAt(index);
    // Re-index remaining questions
    for (let i = 0; i < this.subQuestionsArray.length; i++) {
      this.subQuestionsArray.at(i).get('passageOrderIndex')?.setValue(i + 1);
    }
    this.cdr.markForCheck();
  }

  setCorrectOption(qIndex: number, optIndex: number): void {
    const opts = this.getOptionsArray(qIndex);
    for (let i = 0; i < opts.length; i++) {
      opts.at(i).get('isCorrect')?.setValue(i === optIndex);
    }
    this.cdr.markForCheck();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe({
      next: (subs) => {
        this.subjects = subs || [];
        this.cdr.markForCheck();
      }
    });
  }

  onSubjectChange(subjectId: number): void {
    const selected = this.subjects.find(s => s.id === subjectId);
    if (selected) {
      this.form.patchValue({ subject: selected.name, topicId: null, subtopicId: null });
      this.topics = [];
      this.subtopics = [];
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (topics) => {
          this.topics = topics || [];
          this.cdr.markForCheck();
        }
      });
    }
  }

  onTopicChange(topicId: number): void {
    const selected = this.topics.find(t => t.id === topicId);
    const subjectId = this.form.get('subjectId')?.value;
    if (selected) {
      this.form.patchValue({ topic: selected.name, subtopicId: null });
      this.subtopics = [];
      if (subjectId) {
        this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({
          next: (subtopics) => {
            this.subtopics = subtopics || [];
            this.cdr.markForCheck();
          }
        });
      }
    }
  }

  onSubtopicChange(subtopicId: number): void {
    const selected = this.subtopics.find(st => st.id === subtopicId);
    if (selected) {
      this.form.patchValue({ subtopic: selected.name });
    }
  }

  private populateForm(p: PassageResponse): void {
    this.form.patchValue({
      title: p.title || '',
      subjectId: p.subjectId,
      topicId: p.topicId,
      subtopicId: p.subtopicId,
      subject: p.subject || '',
      topic: p.topic || '',
      subtopic: p.subtopic || '',
      content: p.content
    });

    if (p.subjectId) {
      this.subjectTopicService.getTopics(p.subjectId).subscribe(t => {
        this.topics = t || [];
        this.cdr.markForCheck();
      });
    }
    if (p.subjectId && p.topicId) {
      this.subjectTopicService.getSubtopics(p.subjectId, p.topicId).subscribe(st => {
        this.subtopics = st || [];
        this.cdr.markForCheck();
      });
    }

    this.subQuestionsArray.clear();
    if (p.subQuestions && p.subQuestions.length > 0) {
      p.subQuestions.forEach((sq, idx) => {
        const group = this.fb.group({
          passageOrderIndex: [sq.passageOrderIndex || idx + 1],
          content: [sq.content, Validators.required],
          difficulty: [sq.difficulty || 'MEDIUM', Validators.required],
          cognitiveLevel: [sq.cognitiveLevel || 'COMPREHENSION', Validators.required],
          questionType: [sq.questionType || 'SINGLE_MCQ', Validators.required],
          explanation: [sq.explanation || ''],
          options: this.fb.array(
            (sq.options || []).map(opt =>
              this.fb.group({
                text: [opt.text, Validators.required],
                isCorrect: [opt.isCorrect || false]
              })
            )
          )
        });
        this.subQuestionsArray.push(group);
      });
    }
    this.cdr.markForCheck();
  }

  private resetForm(): void {
    this.form.reset({
      title: '',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'COMPREHENSION',
      questionType: 'SINGLE_MCQ'
    });
    this.subQuestionsArray.clear();
    this.addSubQuestion();
    this.addSubQuestion();
    this.topics = [];
    this.subtopics = [];
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.snackBar.open('Please fill all required fields correctly.', 'OK', { duration: 3000 });
      return;
    }

    if (this.subQuestionsArray.length < 2) {
      this.snackBar.open('Comprehension passage requires at least 2 sub-questions.', 'OK', { duration: 3000 });
      return;
    }

    this.isSubmitting = true;
    this.cdr.markForCheck();

    const val = this.form.value;
    const req: PassageRequest = {
      title: val.title || undefined,
      subjectId: val.subjectId,
      topicId: val.topicId,
      subtopicId: val.subtopicId || undefined,
      subject: val.subject,
      topic: val.topic,
      subtopic: val.subtopic || undefined,
      content: val.content,
      subQuestions: val.subQuestions.map((sq: any, idx: number) => ({
        passageOrderIndex: idx + 1,
        content: sq.content,
        difficulty: sq.difficulty,
        cognitiveLevel: sq.cognitiveLevel,
        questionType: sq.questionType,
        explanation: sq.explanation || undefined,
        options: sq.options.map((opt: any) => ({
          text: opt.text,
          isCorrect: Boolean(opt.isCorrect)
        }))
      }))
    };

    const call$ = this.passage
      ? this.passageService.updatePassage(this.passage.id, req)
      : this.passageService.createPassage(req);

    call$.subscribe({
      next: () => {
        this.isSubmitting = false;
        this.snackBar.open(
          this.passage ? 'Passage updated successfully.' : 'Passage and sub-questions created successfully.',
          'OK',
          { duration: 3000 }
        );
        this.close.emit(true);
      },
      error: (err) => {
        this.isSubmitting = false;
        this.snackBar.open(err?.error?.message || 'Failed to save passage.', 'OK', { duration: 4000 });
        this.cdr.markForCheck();
      }
    });
  }

  onCancel(): void {
    this.close.emit(false);
  }
}
