import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { ExaminationResponse, CreateExamRequest } from './exam-management.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-exam-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatSlideToggleModule,
    MatCheckboxModule,
    RightDrawerComponent
  ],
  template: `
    <app-right-drawer
      [isOpen]="isOpen"
      [title]="exam ? 'Edit Examination' : 'Create Examination'"
      [subtitle]="exam ? 'Update examination parameters, timing, and marking rules.' : 'Configure new examination specifications, total marks, and policies.'"
      width="500px"
      (close)="cancel()"
    >
      <div drawer-body>
        <form [formGroup]="form" class="exam-form">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" placeholder="Examination name" />
            <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
          </mat-form-field>

          <div class="form-row">
            <mat-form-field appearance="outline">
              <mat-label>Exam Code</mat-label>
              <input matInput formControlName="code" placeholder="e.g. JEE-MAIN-2027" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Conducting Authority</mat-label>
              <input matInput formControlName="conductingAuthority" placeholder="e.g. NTA, UPSC" />
            </mat-form-field>
          </div>

          <div class="form-row">
            <mat-form-field appearance="outline">
              <mat-label>Category</mat-label>
              <mat-select formControlName="category">
                <mat-option value="">None</mat-option>
                <mat-option value="RECRUITMENT">Recruitment</mat-option>
                <mat-option value="ENTRANCE">Entrance</mat-option>
                <mat-option value="CERTIFICATION">Certification</mat-option>
                <mat-option value="DEPARTMENTAL">Departmental</mat-option>
              </mat-select>
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Examination Type</mat-label>
              <mat-select formControlName="examinationType">
                <mat-option value="">None</mat-option>
                <mat-option value="PRELIMINARY">Preliminary</mat-option>
                <mat-option value="MAIN">Main</mat-option>
                <mat-option value="SKILL_TEST">Skill Test</mat-option>
                <mat-option value="INTERVIEW">Interview</mat-option>
                <mat-option value="PHYSICAL_TEST">Physical Test</mat-option>
              </mat-select>
            </mat-form-field>
          </div>

          <div class="form-row">
            <mat-form-field appearance="outline">
              <mat-label>Academic Year</mat-label>
              <input matInput formControlName="academicYear" placeholder="e.g. 2026-27" />
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Mode</mat-label>
              <mat-select formControlName="examinationMode">
                <mat-option value="">None</mat-option>
                <mat-option value="CBT">CBT (Computer Based)</mat-option>
                <mat-option value="OMR">OMR (Paper Based)</mat-option>
                <mat-option value="HYBRID">Hybrid</mat-option>
              </mat-select>
            </mat-form-field>
          </div>

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
                <mat-option value="FLEXIBLE">Flexible</mat-option>
                <mat-option value="SEQUENTIAL">Sequential</mat-option>
                <mat-option value="RESTRICTED">Restricted</mat-option>
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
      </div>

      <div drawer-footer>
        <button mat-button (click)="cancel()">Cancel</button>
        <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">
          {{ exam ? 'Update' : 'Create' }}
        </button>
      </div>
    </app-right-drawer>
  `,
  styles: [`
    .exam-form {
      display: flex;
      flex-direction: column;
      gap: 12px;
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
export class ExamFormDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() exam?: ExaminationResponse;
  @Output() close = new EventEmitter<CreateExamRequest | null>();

  form!: FormGroup;

  constructor(private fb: FormBuilder) {
    this.initForm();
  }

  ngOnInit(): void {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
    }
  }

  initForm(): void {
    const exam = this.exam;
    this.form = this.fb.group({
      name: [exam?.name || '', Validators.required],
      code: [exam?.code || ''],
      conductingAuthority: [exam?.conductingAuthority || ''],
      category: [exam?.category || ''],
      examinationType: [exam?.examinationType || ''],
      academicYear: [exam?.academicYear || ''],
      examinationMode: [exam?.examinationMode || ''],
      durationMinutes: [exam?.durationMinutes || 60, [Validators.required, Validators.min(1)]],
      totalMarks: [exam?.totalMarks || 100, [Validators.required, Validators.min(1)]],
      negativeMarkingEnabled: [exam?.negativeMarkingEnabled || false],
      negativeMarkingValue: [exam?.negativeMarkingValue || 0],
      navigationPolicy: [exam?.navigationPolicy || 'FLEXIBLE', Validators.required],
      calculatorPolicy: [exam?.calculatorPolicy || 'NONE', Validators.required],
      reviewFlagEnabled: [exam?.reviewFlagEnabled || false]
    });
  }

  cancel(): void {
    this.close.emit(null);
  }

  save(): void {
    if (this.form.valid) {
      const value = this.form.value;
      const sections = this.exam?.sections?.length
        ? this.exam.sections
        : [{ name: 'Section 1', questionCount: value.totalMarks, marksPerQuestion: 1 }];

      this.close.emit({
        ...value,
        sections
      });
    }
  }
}
