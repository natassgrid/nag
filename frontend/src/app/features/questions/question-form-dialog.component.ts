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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { CreateQuestionRequest, QuestionResponse, QuestionService } from './question.service';
import { SubjectTopicService, Subject, Topic, Subtopic } from './subject-topic.service';
import { ExamEditorComponent, ExamDocument, EMPTY_DOCUMENT } from '../../shared/components/exam-editor';
import { RightDrawerComponent } from '../../shared/components/right-drawer/right-drawer.component';

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
    ExamEditorComponent,
    RightDrawerComponent
  ],
  template: `
    <app-right-drawer
      [isOpen]="isOpen"
      [title]="question ? 'Edit Question' : 'Create Question'"
      [subtitle]="question ? 'Modify existing examination question parameters and content.' : 'Add a new question with options, subject, and difficulty metrics.'"
      width="540px"
      (close)="cancel()"
    >
      <div drawer-body>
        <form [formGroup]="form" class="question-form">
          <!-- Subject dropdown with inline create -->
          <div class="field-with-action">
            <mat-form-field appearance="outline" class="flex-field">
              <mat-label>Subject</mat-label>
              <mat-select formControlName="subject" (selectionChange)="onSubjectChange($event.value)">
                <mat-option *ngFor="let s of subjects" [value]="s.name">{{ s.name }}</mat-option>
              </mat-select>
              <mat-error *ngIf="form.get('subject')?.hasError('required')">Subject is required</mat-error>
            </mat-form-field>
            <button mat-icon-button color="primary" type="button" (click)="toggleNewSubject()"
                    matTooltip="Add new subject">
              <mat-icon>add_circle</mat-icon>
            </button>
          </div>
          <div *ngIf="showNewSubject" class="inline-create">
            <mat-form-field appearance="outline" class="flex-field">
              <mat-label>New Subject Name</mat-label>
              <input matInput [(ngModel)]="newSubjectName" [ngModelOptions]="{standalone: true}" />
            </mat-form-field>
            <button mat-raised-button color="accent" type="button"
                    [disabled]="!newSubjectName || creatingSubject"
                    (click)="createNewSubject()">
              <mat-spinner *ngIf="creatingSubject" diameter="18"></mat-spinner>
              <span *ngIf="!creatingSubject">Create</span>
            </button>
            <button mat-button type="button" (click)="showNewSubject = false">Cancel</button>
          </div>

          <!-- Topic dropdown with inline create -->
          <div class="field-with-action">
            <mat-form-field appearance="outline" class="flex-field">
              <mat-label>Topic</mat-label>
              <mat-select formControlName="topic" (selectionChange)="onTopicChange($event.value)"
                          [disabled]="!selectedSubject">
                <mat-option *ngFor="let t of topics" [value]="t.name">{{ t.name }}</mat-option>
              </mat-select>
              <mat-error *ngIf="form.get('topic')?.hasError('required')">Topic is required</mat-error>
            </mat-form-field>
            <button mat-icon-button color="primary" type="button" (click)="toggleNewTopic()"
                    [disabled]="!selectedSubject" matTooltip="Add new topic">
              <mat-icon>add_circle</mat-icon>
            </button>
          </div>
          <div *ngIf="showNewTopic" class="inline-create">
            <mat-form-field appearance="outline" class="flex-field">
              <mat-label>New Topic Name</mat-label>
              <input matInput [(ngModel)]="newTopicName" [ngModelOptions]="{standalone: true}" />
            </mat-form-field>
            <button mat-raised-button color="accent" type="button"
                    [disabled]="!newTopicName || creatingTopic"
                    (click)="createNewTopic()">
              <mat-spinner *ngIf="creatingTopic" diameter="18"></mat-spinner>
              <span *ngIf="!creatingTopic">Create</span>
            </button>
            <button mat-button type="button" (click)="showNewTopic = false">Cancel</button>
          </div>

          <!-- Subtopic dropdown with inline create -->
          <div class="field-with-action">
            <mat-form-field appearance="outline" class="flex-field">
              <mat-label>Subtopic</mat-label>
              <mat-select formControlName="subtopic" [disabled]="!selectedTopic">
                <mat-option value="">-- None --</mat-option>
                <mat-option *ngFor="let st of subtopics" [value]="st.name">{{ st.name }}</mat-option>
              </mat-select>
            </mat-form-field>
            <button mat-icon-button color="primary" type="button" (click)="toggleNewSubtopic()"
                    [disabled]="!selectedTopic" matTooltip="Add new subtopic">
              <mat-icon>add_circle</mat-icon>
            </button>
          </div>
          <div *ngIf="showNewSubtopic" class="inline-create">
            <mat-form-field appearance="outline" class="flex-field">
              <mat-label>New Subtopic Name</mat-label>
              <input matInput [(ngModel)]="newSubtopicName" [ngModelOptions]="{standalone: true}" />
            </mat-form-field>
            <button mat-raised-button color="accent" type="button"
                    [disabled]="!newSubtopicName || creatingSubtopic"
                    (click)="createNewSubtopic()">
              <mat-spinner *ngIf="creatingSubtopic" diameter="18"></mat-spinner>
              <span *ngIf="!creatingSubtopic">Create</span>
            </button>
            <button mat-button type="button" (click)="showNewSubtopic = false">Cancel</button>
          </div>

          <mat-form-field appearance="outline">
            <mat-label>Difficulty</mat-label>
            <mat-select formControlName="difficulty">
              <mat-option *ngFor="let d of difficulties" [value]="d">{{ d }}</mat-option>
            </mat-select>
            <mat-error *ngIf="form.get('difficulty')?.hasError('required')">Difficulty is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Cognitive Level</mat-label>
            <mat-select formControlName="cognitiveLevel">
              <mat-option *ngFor="let c of cognitiveLevels" [value]="c">{{ c }}</mat-option>
            </mat-select>
            <mat-error *ngIf="form.get('cognitiveLevel')?.hasError('required')">Cognitive level is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Question Type</mat-label>
            <mat-select formControlName="questionType" (selectionChange)="onQuestionTypeChange($event.value)">
              <mat-option *ngFor="let t of questionTypes" [value]="t.value">{{ t.label }}</mat-option>
            </mat-select>
            <mat-error *ngIf="form.get('questionType')?.hasError('required')">Type is required</mat-error>
          </mat-form-field>

          <div class="editor-field">
            <label class="editor-label">Content <span class="required">*</span></label>
            <exam-editor
              [value]="editorDocument"
              (valueChange)="onEditorChange($event)"
              placeholder="Enter question content..."
            ></exam-editor>
            <mat-error *ngIf="form.get('content')?.hasError('required') && form.get('content')?.touched">Content is required</mat-error>
          </div>

          <!-- MCQ/MSQ Options section -->
          <div *ngIf="isMcqOrMsq()" class="options-section">
            <div class="options-header">
              <span class="options-label">Answer Options</span>
              <span class="options-hint">{{ isMcq() ? 'Select exactly one correct answer' : 'Select one or more correct answers' }}</span>
              <button mat-icon-button type="button" color="primary" (click)="addOption()"
                      [disabled]="options.length >= 6" matTooltip="Add option (max 6)">
                <mat-icon>add</mat-icon>
              </button>
            </div>
            <div *ngFor="let opt of options; let i = index" class="option-row">
              <span class="option-id">{{ optionIds[i] }}</span>
              <mat-form-field appearance="outline" class="flex-field">
                <input matInput [(ngModel)]="opt.text" [ngModelOptions]="{standalone: true}"
                       placeholder="Option text" />
              </mat-form-field>
              <mat-checkbox [(ngModel)]="opt.isCorrect" [ngModelOptions]="{standalone: true}"
                            (change)="isMcq() && onMcqCorrectChange(i)"
                            matTooltip="Mark as correct">
              </mat-checkbox>
              <button mat-icon-button type="button" color="warn" (click)="removeOption(i)"
                      [disabled]="options.length <= 2" matTooltip="Remove option">
                <mat-icon>remove_circle</mat-icon>
              </button>
            </div>
            <mat-error *ngIf="optionError">{{ optionError }}</mat-error>
          </div>

          <mat-form-field appearance="outline" *ngIf="!isMcqOrMsq()">
            <mat-label>Answer Key</mat-label>
            <textarea matInput formControlName="answerKey" rows="3"></textarea>
          </mat-form-field>
        </form>
        <div *ngIf="saveError" style="color: #c62828; font-size: 13px; margin-top: 8px;">{{ saveError }}</div>
      </div>

      <div drawer-footer>
        <button mat-button [disabled]="saving" (click)="cancel()">Cancel</button>
        <button mat-raised-button color="primary" [disabled]="saving" (click)="save()">
          <mat-spinner *ngIf="saving" diameter="18" style="display:inline-block;margin-right:6px;vertical-align:middle;"></mat-spinner>
          {{ saving ? 'Saving…' : 'Save' }}
        </button>
      </div>
    </app-right-drawer>
  `,
  styles: [`
    .question-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    mat-form-field {
      width: 100%;
    }
    textarea {
      resize: vertical;
    }
    .field-with-action {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .flex-field {
      flex: 1;
    }
    .inline-create {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      padding-left: 16px;
    }
    .inline-create mat-form-field {
      flex: 1;
    }
    .inline-create mat-spinner {
      display: inline-block;
    }
    .options-section {
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      padding: 12px;
      margin-bottom: 8px;
    }
    .options-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }
    .options-label {
      font-weight: 500;
      font-size: 14px;
      flex: 1;
    }
    .options-hint {
      font-size: 12px;
      color: #888;
    }
    .option-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 4px;
    }
    .option-id {
      font-weight: 600;
      width: 20px;
      color: #3f51b5;
    }
    .option-row .flex-field {
      flex: 1;
      margin-bottom: -4px;
    }
    .editor-field {
      margin-bottom: 8px;
    }
    .editor-label {
      font-size: 13px;
      color: #666;
      margin-bottom: 4px;
      display: block;
    }
    .editor-label .required {
      color: #c62828;
    }
  `]
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

  editorDocument: ExamDocument = [...EMPTY_DOCUMENT];

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
      answerKey: [q?.answerKey || '']
    });

    if (q?.content) {
      try {
        this.editorDocument = JSON.parse(q.content);
      } catch {
        this.editorDocument = [{ type: 'paragraph', children: [{ text: q.content }] }];
      }
    } else {
      this.editorDocument = [...EMPTY_DOCUMENT];
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

  onEditorChange(doc: ExamDocument): void {
    this.editorDocument = doc;
    const serialized = JSON.stringify(doc);
    this.form.patchValue({ content: serialized });
    this.form.get('content')?.markAsDirty();
    this.form.get('content')?.markAsTouched();
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
    if (this.options.length < 6) {
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
      const value: CreateQuestionRequest = this.form.value;
      if (this.isMcqOrMsq() && this.options.length >= 2) {
        const correct = this.options.filter(o => o.isCorrect).length;
        if (this.isMcq() && correct !== 1) {
          this.optionError = 'MCQ requires exactly one correct option'; return;
        }
        if (!this.isMcq() && correct < 1) {
          this.optionError = 'MSQ requires at least one correct option'; return;
        }
        value.options = this.options.map((o, i) => ({ id: this.optionIds[i], text: o.text, isCorrect: o.isCorrect }));
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
