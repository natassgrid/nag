import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CreateQuestionRequest, QuestionResponse } from './question.service';

export interface QuestionFormDialogData {
  question?: QuestionResponse;
}

@Component({
  selector: 'app-question-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.question ? 'Edit Question' : 'Create Question' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="question-form">
        <mat-form-field appearance="outline">
          <mat-label>Subject</mat-label>
          <input matInput formControlName="subject" />
          <mat-error *ngIf="form.get('subject')?.hasError('required')">Subject is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Topic</mat-label>
          <input matInput formControlName="topic" />
          <mat-error *ngIf="form.get('topic')?.hasError('required')">Topic is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Subtopic</mat-label>
          <input matInput formControlName="subtopic" />
        </mat-form-field>

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
      min-width: 400px;
      padding-top: 8px;
    }
    mat-form-field {
      width: 100%;
    }
    textarea {
      resize: vertical;
    }
  `]
})
export class QuestionFormDialogComponent {
  form: FormGroup;

  difficulties = ['EASY', 'MEDIUM', 'HARD'];
  cognitiveLevels = ['KNOWLEDGE', 'COMPREHENSION', 'APPLICATION', 'ANALYSIS', 'SYNTHESIS', 'EVALUATION'];
  questionTypes = ['MCQ', 'MSQ', 'NUMERICAL', 'DESCRIPTIVE'];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<QuestionFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: QuestionFormDialogData
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

  save(): void {
    if (this.form.valid) {
      const value: CreateQuestionRequest = this.form.value;
      this.dialogRef.close(value);
    }
  }
}
