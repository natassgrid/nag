import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ExaminationResponse } from './exam-management.service';

export interface ExamFormDialogData {
  exam?: ExaminationResponse;
}

@Component({
  selector: 'app-exam-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatCheckboxModule
  ],
  template: `
    <h2 mat-dialog-title>{{ data.exam ? 'Edit Examination' : 'Create Examination' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="exam-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Name</mat-label>
          <input matInput formControlName="name" placeholder="Examination name" />
          <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Duration (minutes)</mat-label>
            <input matInput type="number" formControlName="durationMinutes" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Total Marks</mat-label>
            <input matInput type="number" formControlName="totalMarks" />
          </mat-form-field>
        </div>

        <mat-slide-toggle formControlName="negativeMarkingEnabled" class="toggle-field">
          Negative Marking Enabled
        </mat-slide-toggle>

        <mat-form-field appearance="outline" *ngIf="form.get('negativeMarkingEnabled')?.value" class="full-width">
          <mat-label>Negative Marking Value</mat-label>
          <input matInput type="number" step="0.25" formControlName="negativeMarkingValue" />
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Navigation Policy</mat-label>
            <mat-select formControlName="navigationPolicy">
              <mat-option value="FREE">Free</mat-option>
              <mat-option value="LINEAR">Linear</mat-option>
              <mat-option value="SECTION_LOCKED">Section Locked</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Calculator Policy</mat-label>
            <mat-select formControlName="calculatorPolicy">
              <mat-option value="NONE">None</mat-option>
              <mat-option value="BASIC">Basic</mat-option>
              <mat-option value="SCIENTIFIC">Scientific</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <mat-checkbox formControlName="reviewFlagEnabled" class="checkbox-field">
          Review Flag Enabled
        </mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">
        {{ data.exam ? 'Update' : 'Create' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .exam-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
      min-width: 400px;
      padding-top: 8px;
    }
    .full-width {
      width: 100%;
    }
    .form-row {
      display: flex;
      gap: 16px;
    }
    .form-row mat-form-field {
      flex: 1;
    }
    .toggle-field, .checkbox-field {
      margin: 8px 0;
    }
  `]
})
export class ExamFormDialogComponent implements OnInit {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ExamFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ExamFormDialogData
  ) {}

  ngOnInit(): void {
    const exam = this.data.exam;
    this.form = this.fb.group({
      name: [exam?.name || '', Validators.required],
      durationMinutes: [exam?.durationMinutes || 60, [Validators.required, Validators.min(1)]],
      totalMarks: [exam?.totalMarks || 100, [Validators.required, Validators.min(1)]],
      negativeMarkingEnabled: [exam?.negativeMarkingEnabled || false],
      negativeMarkingValue: [exam?.negativeMarkingValue || 0],
      navigationPolicy: [exam?.navigationPolicy || 'FREE', Validators.required],
      calculatorPolicy: [exam?.calculatorPolicy || 'NONE', Validators.required],
      reviewFlagEnabled: [exam?.reviewFlagEnabled || false]
    });
  }

  save(): void {
    if (this.form.valid) {
      const value = this.form.value;
      // Default section when none provided (matches totalMarks)
      const sections = this.data.exam?.sections?.length
        ? this.data.exam.sections
        : [{ name: 'Section 1', questionCount: value.totalMarks, marksPerQuestion: 1 }];

      this.dialogRef.close({
        ...value,
        sections
      });
    }
  }
}
