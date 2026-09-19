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
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormsModule,
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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { PassageService, PassageRequest, PassageResponse } from '../passage.service';
import { SubjectTopicService, Subject, Topic, Subtopic } from '../subject-topic.service';
import { ExamEditorComponent } from '../../../shared/components/exam-editor/exam-editor.component';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';
import { SubQuestionFormComponent } from '../../../shared/components/sub-question-form/sub-question-form.component';

@Component({
  selector: 'app-passage-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    ExamEditorComponent,
    RightDrawerComponent,
    SubQuestionFormComponent
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

  readonly difficulties = ['EASY', 'MEDIUM', 'HARD', 'EXPERT'];
  readonly cognitiveLevels = [
    'REMEMBER', 'UNDERSTAND', 'APPLY', 'ANALYZE', 'EVALUATE', 'CREATE'
  ];
  readonly questionTypes = [
    { value: 'SINGLE_MCQ', label: 'Single Choice (MCQ)' },
    { value: 'MULTI_MCQ', label: 'Multiple Choice (MSQ)' },
    { value: 'NUMERICAL', label: 'Numerical' },
    { value: 'DESCRIPTIVE', label: 'Descriptive' }
  ];

  readonly optionIds = ['A', 'B', 'C', 'D', 'E', 'F'];

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

  /** Returns the sub-question FormGroup at the given index for passing to SubQuestionFormComponent. */
  getSubQuestionGroup(index: number): FormGroup {
    return this.subQuestionsArray.at(index) as FormGroup;
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

    // Default 2 sub-questions for a new comprehension passage
    this.addSubQuestion();
    this.addSubQuestion();
  }

  createSubQuestionGroup(orderIndex: number): FormGroup {
    return this.fb.group({
      passageOrderIndex: [orderIndex],
      content: ['', Validators.required],
      difficulty: ['MEDIUM', Validators.required],
      cognitiveLevel: ['UNDERSTAND', Validators.required],
      questionType: ['SINGLE_MCQ', Validators.required],
      answerKey: [''],
      explanation: [''],
      options: this.fb.array([
        this.createOptionGroup('', false),
        this.createOptionGroup('', false),
        this.createOptionGroup('', false),
        this.createOptionGroup('', false)
      ])
    });
  }

  createOptionGroup(
    text = '',
    isCorrect = false,
    imageUrl = '',
    imageAltText = '',
    isImageOnly = false
  ): FormGroup {
    return this.fb.group({
      text: [text],
      isCorrect: [isCorrect],
      imageUrl: [imageUrl],
      imageAltText: [imageAltText],
      isImageOnly: [isImageOnly]
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
    this.selectedSubject = selected || null;
    this.selectedTopic = null;
    this.topics = [];
    this.subtopics = [];
    if (selected) {
      this.form.patchValue({ subject: selected.name, topicId: null, subtopicId: null, topic: '', subtopic: '' });
      this.subjectTopicService.getTopics(subjectId).subscribe({
        next: (topics) => {
          this.topics = topics || [];
          this.cdr.markForCheck();
        }
      });
    } else {
      this.form.patchValue({ subject: '', topicId: null, subtopicId: null, topic: '', subtopic: '' });
    }
  }

  onTopicChange(topicId: number): void {
    const selected = this.topics.find(t => t.id === topicId);
    this.selectedTopic = selected || null;
    this.subtopics = [];
    const subjectId = this.form.get('subjectId')?.value;
    if (selected) {
      this.form.patchValue({ topic: selected.name, subtopicId: null, subtopic: '' });
      if (subjectId) {
        this.subjectTopicService.getSubtopics(subjectId, topicId).subscribe({
          next: (subtopics) => {
            this.subtopics = subtopics || [];
            this.cdr.markForCheck();
          }
        });
      }
    } else {
      this.form.patchValue({ topic: '', subtopicId: null, subtopic: '' });
    }
  }

  onSubtopicChange(subtopicId: number | null): void {
    if (subtopicId) {
      const selected = this.subtopics.find(st => st.id === subtopicId);
      this.form.patchValue({ subtopic: selected?.name || '' });
    } else {
      this.form.patchValue({ subtopic: '' });
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
        this.form.patchValue({ subjectId: subject.id, subject: subject.name });
        this.onSubjectChange(subject.id);
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
        this.form.patchValue({ topicId: topic.id, topic: topic.name });
        this.onTopicChange(topic.id);
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
        this.form.patchValue({ subtopicId: subtopic.id, subtopic: subtopic.name });
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

  private normalizeCognitiveLevel(val?: string): string {
    if (!val) return 'UNDERSTAND';
    const map: Record<string, string> = {
      KNOWLEDGE: 'REMEMBER',
      COMPREHENSION: 'UNDERSTAND',
      APPLICATION: 'APPLY',
      ANALYSIS: 'ANALYZE',
      EVALUATION: 'EVALUATE',
      SYNTHESIS: 'CREATE'
    };
    return map[val] || val;
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
        this.selectedSubject = this.subjects.find(s => s.id === p.subjectId) || null;
        this.selectedTopic = this.topics.find(top => top.id === p.topicId) || null;
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
          cognitiveLevel: [this.normalizeCognitiveLevel(sq.cognitiveLevel), Validators.required],
          questionType: [sq.questionType || 'SINGLE_MCQ', Validators.required],
          answerKey: [sq.answerKey || ''],
          explanation: [sq.explanation || ''],
          options: this.fb.array(
            (sq.options || []).map(opt =>
              this.createOptionGroup(
                opt.text || '',
                Boolean(opt.isCorrect),
                opt.imageUrl || '',
                opt.imageAltText || '',
                !!opt.imageUrl && (!opt.text || !opt.text.trim())
              )
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
      subjectId: null,
      topicId: null,
      subtopicId: null,
      subject: '',
      topic: '',
      subtopic: '',
      content: ''
    });
    this.subQuestionsArray.clear();
    this.addSubQuestion();
    this.addSubQuestion();
    this.topics = [];
    this.subtopics = [];
    this.selectedSubject = null;
    this.selectedTopic = null;
    this.showNewSubject = false;
    this.showNewTopic = false;
    this.showNewSubtopic = false;
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

    // Validate sub-questions options
    for (let i = 0; i < this.subQuestionsArray.length; i++) {
      const sq = this.subQuestionsArray.at(i);
      const qt = sq.get('questionType')?.value || 'SINGLE_MCQ';
      const isMcq = qt === 'SINGLE_MCQ';
      const isMsq = qt === 'MULTI_MCQ';
      const opts = sq.get('options') as FormArray;

      if (isMcq || isMsq) {
        if (opts.length < 2) {
          this.snackBar.open(`Question ${i + 1} requires at least 2 options.`, 'OK', { duration: 3000 });
          return;
        }
        const correctCount = opts.controls.filter(c => c.get('isCorrect')?.value).length;
        if (isMcq && correctCount !== 1) {
          this.snackBar.open(`Question ${i + 1} (MCQ) requires exactly one correct option.`, 'OK', { duration: 3000 });
          return;
        }
        if (isMsq && correctCount < 1) {
          this.snackBar.open(`Question ${i + 1} (MSQ) requires at least one correct option.`, 'OK', { duration: 3000 });
          return;
        }
      }
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
        answerKey: sq.answerKey || undefined,
        explanation: sq.explanation || undefined,
        options: (sq.options || []).map((opt: any, optIdx: number) => ({
          id: this.optionIds[optIdx] || String.fromCharCode(65 + optIdx),
          text: opt.text || '',
          isCorrect: Boolean(opt.isCorrect),
          imageUrl: (opt.imageUrl || '').trim() || undefined,
          imageAltText: (opt.imageAltText || '').trim() || undefined
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
