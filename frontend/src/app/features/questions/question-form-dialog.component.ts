import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { CreateQuestionRequest, QuestionResponse } from './question.service';
import { SubjectTopicService, Subject, Topic, Subtopic } from './subject-topic.service';

export interface QuestionFormDialogData {
  question?: QuestionResponse;
}

@Component({
  selector: 'app-question-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.question ? 'Edit Question' : 'Create Question' }}</h2>
    <mat-dialog-content>
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
          <mat-select formControlName="questionType">
            <mat-option *ngFor="let t of questionTypes" [value]="t">{{ t }}</mat-option>
          </mat-select>
          <mat-error *ngIf="form.get('questionType')?.hasError('required')">Type is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Content</mat-label>
          <textarea matInput formControlName="content" rows="5"></textarea>
          <mat-error *ngIf="form.get('content')?.hasError('required')">Content is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Answer Key</mat-label>
          <textarea matInput formControlName="answerKey" rows="3"></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">
        Save
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .question-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      min-width: 450px;
      padding-top: 8px;
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
  `]
})
export class QuestionFormDialogComponent implements OnInit {
  form: FormGroup;

  difficulties = ['EASY', 'MEDIUM', 'HARD'];
  cognitiveLevels = ['KNOWLEDGE', 'COMPREHENSION', 'APPLICATION', 'ANALYSIS', 'SYNTHESIS', 'EVALUATION'];
  questionTypes = ['MCQ', 'MSQ', 'NUMERICAL', 'DESCRIPTIVE'];

  // Subject/Topic/Subtopic data
  subjects: Subject[] = [];
  topics: Topic[] = [];
  subtopics: Subtopic[] = [];

  selectedSubject: Subject | null = null;
  selectedTopic: Topic | null = null;

  // Inline create state
  showNewSubject = false;
  showNewTopic = false;
  showNewSubtopic = false;
  newSubjectName = '';
  newTopicName = '';
  newSubtopicName = '';
  creatingSubject = false;
  creatingTopic = false;
  creatingSubtopic = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<QuestionFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: QuestionFormDialogData,
    private subjectTopicService: SubjectTopicService
  ) {
    const q = data.question;
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
  }

  ngOnInit(): void {
    this.loadSubjects();
  }

  loadSubjects(): void {
    this.subjectTopicService.getSubjects().subscribe(subjects => {
      this.subjects = subjects;
      // If editing, auto-select subject and load topics
      if (this.data.question?.subject) {
        const match = subjects.find(s => s.name === this.data.question!.subject);
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
      // If editing, auto-select topic and load subtopics
      if (this.data.question?.topic) {
        const match = topics.find(t => t.name === this.data.question!.topic);
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

  // Inline create methods
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

  save(): void {
    if (this.form.valid) {
      const value: CreateQuestionRequest = this.form.value;
      this.dialogRef.close(value);
    }
  }
}
