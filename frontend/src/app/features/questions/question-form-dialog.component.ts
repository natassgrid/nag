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
  OnInit,
  OnChanges,
  SimpleChanges,
  ChangeDetectionStrategy,
  ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
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
import { AssetPickerDialogComponent } from '../assets/asset-picker-dialog.component';
import { ImagePasteDialogComponent } from './image-paste-dialog.component';
import { AssetResponse } from '../assets/asset.model';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';
import { MathRendererComponent } from '../../shared/components/math-renderer/math-renderer.component';
import { ExamEditorComponent } from '../../shared/components/exam-editor/exam-editor.component';

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
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatDialogModule,
    MatSnackBarModule,
    RightDrawerComponent,
    MathRendererComponent,
    ExamEditorComponent
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

  options: {
    id: string;
    text: string;
    isCorrect: boolean;
    imageUrl?: string;
    imageAltText?: string;
    isImageOnly?: boolean;
  }[] = [];
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
    private questionService: QuestionService,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer,
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
      this.initForm();
      this.syncHierarchyFromQuestion();
    } else if (changes['question'] && this.isOpen) {
      this.initForm();
      this.syncHierarchyFromQuestion();
    }
  }

  getSafeImageUrl(url?: string | null): SafeUrl | string {
    if (!url) return '';
    if (url.startsWith('data:') || url.startsWith('blob:')) {
      return this.sanitizer.bypassSecurityTrustUrl(url);
    }
    return url;
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

    this.form = this.fb.group({
      subject: [q?.subject || '', Validators.required],
      topic: [q?.topic || '', Validators.required],
      subtopic: [q?.subtopic || ''],
      difficulty: [q?.difficulty || '', Validators.required],
      cognitiveLevel: [q?.cognitiveLevel || '', Validators.required],
      questionType: [q?.questionType || '', Validators.required],
      content: [content, Validators.required],
      answerKey: [q?.answerKey || ''],
      explanation: [explanation]
    });

    this.editorContent = content;
    this.explanationContent = explanation;

    if (q?.options && q.options.length > 0) {
      this.options = q.options.map((o: QuestionOptionDto) => ({
        id: o.id,
        text: this.unescapeNewlines(o.text),
        isCorrect: o.isCorrect,
        imageUrl: o.imageUrl || '',
        imageAltText: o.imageAltText || '',
        isImageOnly: !!o.imageUrl && (!o.text || !o.text.trim())
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

  openContentAssetPicker(): void {
    const ref = this.dialog.open(AssetPickerDialogComponent, {
      width: '800px',
      data: { assetType: 'IMAGE', title: 'Insert Image into Content' }
    });
    ref.afterClosed().subscribe((asset: AssetResponse) => {
      if (asset) {
        const alt = asset.altText || asset.title || asset.originalFilename || 'Diagram';
        const url = `/api/v1/assets/${asset.id}/download`;
        const mdImage = `\n![${alt}](${url})\n`;
        this.editorContent = (this.editorContent || '') + mdImage;
        this.onEditorChange(this.editorContent);
        this.cdr.markForCheck();
      }
    });
  }

  openContentDataUriDialog(): void {
    const ref = this.dialog.open(ImagePasteDialogComponent, {
      width: '560px',
      data: {
        title: 'Insert Base64 / Data URI Image into Content',
        showAltText: true
      }
    });
    ref.afterClosed().subscribe((res) => {
      if (res) {
        const alt = res.altText || 'Diagram';
        const mdImage = `\n![${alt}](${res.imageUrl})\n`;
        this.editorContent = (this.editorContent || '') + mdImage;
        this.onEditorChange(this.editorContent);
        this.cdr.markForCheck();
      }
    });
  }

  openExplanationAssetPicker(): void {
    const ref = this.dialog.open(AssetPickerDialogComponent, {
      width: '800px',
      data: { assetType: 'IMAGE', title: 'Insert Image into Explanation' }
    });
    ref.afterClosed().subscribe((asset: AssetResponse) => {
      if (asset) {
        const alt = asset.altText || asset.title || asset.originalFilename || 'Explanation Diagram';
        const url = `/api/v1/assets/${asset.id}/download`;
        const mdImage = `\n![${alt}](${url})\n`;
        this.explanationContent = (this.explanationContent || '') + mdImage;
        this.onExplanationChange(this.explanationContent);
        this.cdr.markForCheck();
      }
    });
  }

  openExplanationDataUriDialog(): void {
    const ref = this.dialog.open(ImagePasteDialogComponent, {
      width: '560px',
      data: {
        title: 'Insert Base64 / Data URI Image into Explanation',
        showAltText: true
      }
    });
    ref.afterClosed().subscribe((res) => {
      if (res) {
        const alt = res.altText || 'Explanation Diagram';
        const mdImage = `\n![${alt}](${res.imageUrl})\n`;
        this.explanationContent = (this.explanationContent || '') + mdImage;
        this.onExplanationChange(this.explanationContent);
        this.cdr.markForCheck();
      }
    });
  }

  openOptionAssetPicker(index: number): void {
    const ref = this.dialog.open(AssetPickerDialogComponent, {
      width: '800px',
      data: { assetType: 'IMAGE', title: `Select Image for Option ${this.options[index].id}` }
    });
    ref.afterClosed().subscribe((asset: AssetResponse) => {
      if (asset) {
        this.options[index].imageUrl = `/api/v1/assets/${asset.id}/download`;
        if (!this.options[index].imageAltText) {
          this.options[index].imageAltText = asset.altText || asset.title || `Option ${this.options[index].id}`;
        }
        this.cdr.markForCheck();
      }
    });
  }

  openOptionDataUriDialog(index: number): void {
    const ref = this.dialog.open(ImagePasteDialogComponent, {
      width: '560px',
      data: {
        title: `Paste Base64 / Data URI for Option ${this.options[index].id}`,
        currentUrl: this.options[index].imageUrl,
        altText: this.options[index].imageAltText || `Option ${this.options[index].id}`
      }
    });
    ref.afterClosed().subscribe((res) => {
      if (res) {
        this.options[index].imageUrl = res.imageUrl;
        this.options[index].imageAltText = res.altText || `Option ${this.options[index].id}`;
        this.cdr.markForCheck();
      }
    });
  }

  clearOptionImage(index: number): void {
    this.options[index].imageUrl = '';
    this.options[index].imageAltText = '';
    this.options[index].isImageOnly = false;
    this.cdr.markForCheck();
  }

  toggleImageOnly(index: number, isImageOnly: boolean): void {
    this.options[index].isImageOnly = isImageOnly;
    if (isImageOnly) {
      this.options[index].text = '';
      if (!this.options[index].imageUrl) {
        this.openOptionDataUriDialog(index);
      }
    }
    this.cdr.markForCheck();
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
        { id: 'A', text: '', isCorrect: false, imageUrl: '', imageAltText: '', isImageOnly: false },
        { id: 'B', text: '', isCorrect: false, imageUrl: '', imageAltText: '', isImageOnly: false }
      ];
    }
    this.optionError = '';
    this.cdr.markForCheck();
  }

  isMcqOrMsq(): boolean {
    return this.currentQuestionType === 'SINGLE_MCQ' || this.currentQuestionType === 'MULTI_MCQ';
  }

  isMcq(): boolean {
    return this.currentQuestionType === 'SINGLE_MCQ';
  }

  addOption(): void {
    if (this.options.length < 5) {
      this.options.push({
        id: this.optionIds[this.options.length],
        text: '',
        isCorrect: false,
        imageUrl: '',
        imageAltText: '',
        isImageOnly: false
      });
      this.cdr.markForCheck();
    }
  }

  removeOption(i: number): void {
    if (this.options.length > 2) {
      this.options.splice(i, 1);
      this.cdr.markForCheck();
    }
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
        for (const opt of this.options) {
          const hasText = opt.text && opt.text.trim().length > 0;
          const hasImg = opt.imageUrl && opt.imageUrl.trim().length > 0;
          if (!hasText && !hasImg) {
            this.optionError = `Option ${opt.id} must have either text or an image.`;
            return;
          }
        }
        value.options = this.options.map((o, i) => ({
          id: this.optionIds[i],
          text: this.formatLatex(o.text || ''),
          isCorrect: o.isCorrect,
          imageUrl: o.imageUrl?.trim() || undefined,
          imageAltText: o.imageAltText?.trim() || undefined
        }));
      }

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
}
