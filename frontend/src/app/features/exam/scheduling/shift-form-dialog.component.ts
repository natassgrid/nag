import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { CreateShiftRequest, ShiftResponse } from './scheduling.service';

function toMinutes(timeStr: string): number {
  if (!timeStr) return 0;
  const parts = timeStr.split(':');
  if (parts.length < 2) return 0;
  const h = parseInt(parts[0], 10) || 0;
  const m = parseInt(parts[1], 10) || 0;
  return h * 60 + m;
}

function shiftTimingValidator(group: AbstractControl): ValidationErrors | null {
  const reporting = group.get('reportingTime')?.value;
  const gateClosing = group.get('gateClosingTime')?.value;
  const loginStart = group.get('loginStartTime')?.value;
  const examStart = group.get('examStartTime')?.value;
  const examEnd = group.get('examEndTime')?.value;
  const duration = group.get('durationMinutes')?.value;

  if (!reporting || !gateClosing || !loginStart || !examStart || !examEnd) {
    return null;
  }

  const rMin = toMinutes(reporting);
  const gMin = toMinutes(gateClosing);
  const lMin = toMinutes(loginStart);
  const sMin = toMinutes(examStart);
  const eMin = toMinutes(examEnd);

  const errors: ValidationErrors = {};

  if (rMin >= gMin) {
    errors['reportingGeGateClosing'] = 'Reporting time must be earlier than gate closing time.';
  }
  if (gMin >= lMin) {
    errors['gateClosingGeLoginStart'] = 'Gate closing time must be earlier than login start time.';
  }
  if (lMin >= sMin) {
    errors['loginStartGeExamStart'] = 'Login start time must be earlier than exam start time.';
  }
  if (sMin >= eMin) {
    errors['examStartGeExamEnd'] = 'Exam start time must be earlier than exam end time.';
  }
  if (duration !== null && duration !== undefined && eMin > sMin) {
    const diff = eMin - sMin;
    if (Number(duration) !== diff) {
      errors['durationMismatch'] = `Duration (${duration}m) does not match exam start to end time (${diff}m).`;
    }
  }

  return Object.keys(errors).length > 0 ? errors : null;
}

@Component({
  selector: 'app-shift-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>{{ isEdit ? 'Edit Shift' : 'Add New Shift' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-container">
        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Shift Number</mat-label>
            <input matInput type="number" formControlName="shiftNumber" min="1" required />
            <mat-error *ngIf="form.get('shiftNumber')?.hasError('required')">Required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Shift Name</mat-label>
            <input matInput formControlName="shiftName" placeholder="e.g. Morning / Afternoon" />
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Reporting Time</mat-label>
            <input matInput type="time" formControlName="reportingTime" required />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Gate Closing Time</mat-label>
            <input matInput type="time" formControlName="gateClosingTime" required />
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Login Start Time</mat-label>
            <input matInput type="time" formControlName="loginStartTime" required />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Exam Start Time</mat-label>
            <input matInput type="time" formControlName="examStartTime" required />
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Exam End Time</mat-label>
            <input matInput type="time" formControlName="examEndTime" required />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Exit Time</mat-label>
            <input matInput type="time" formControlName="exitTime" />
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Duration (Minutes)</mat-label>
            <input matInput type="number" formControlName="durationMinutes" min="1" required />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Buffer Minutes</mat-label>
            <input matInput type="number" formControlName="bufferMinutes" min="0" required />
          </mat-form-field>
        </div>

        <!-- Custom Validation Errors -->
        <div class="error-container" *ngIf="form.errors && form.touched">
          <div color="warn" class="error-msg" *ngIf="form.errors['reportingGeGateClosing']">
            {{ form.errors['reportingGeGateClosing'] }}
          </div>
          <div class="error-msg" *ngIf="form.errors['gateClosingGeLoginStart']">
            {{ form.errors['gateClosingGeLoginStart'] }}
          </div>
          <div class="error-msg" *ngIf="form.errors['loginStartGeExamStart']">
            {{ form.errors['loginStartGeExamStart'] }}
          </div>
          <div class="error-msg" *ngIf="form.errors['examStartGeExamEnd']">
            {{ form.errors['examStartGeExamEnd'] }}
          </div>
          <div class="error-msg" *ngIf="form.errors['durationMismatch']">
            {{ form.errors['durationMismatch'] }}
          </div>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">
        {{ isEdit ? 'Update Shift' : 'Add Shift' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding-top: 8px;
      min-width: 440px;
    }
    .row {
      display: flex;
      gap: 16px;
    }
    .row mat-form-field {
      flex: 1;
    }
    .error-container {
      background: #ffebee;
      border: 1px solid #ffcdd2;
      border-radius: 4px;
      padding: 8px 12px;
      margin-top: 8px;
    }
    .error-msg {
      color: #b71c1c;
      font-size: 12px;
      line-height: 1.4;
    }
  `]
})
export class ShiftFormDialogComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ShiftFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data?: ShiftResponse
  ) {}

  ngOnInit(): void {
    this.isEdit = !!this.data;

    this.form = this.fb.group(
      {
        shiftNumber: [this.data?.shiftNumber ?? 1, [Validators.required, Validators.min(1)]],
        shiftName: [this.data?.shiftName ?? ''],
        reportingTime: [this.data?.reportingTime ?? '07:30', Validators.required],
        gateClosingTime: [this.data?.gateClosingTime ?? '08:30', Validators.required],
        loginStartTime: [this.data?.loginStartTime ?? '08:45', Validators.required],
        examStartTime: [this.data?.examStartTime ?? '09:00', Validators.required],
        examEndTime: [this.data?.examEndTime ?? '12:00', Validators.required],
        exitTime: [this.data?.exitTime ?? '12:15'],
        durationMinutes: [this.data?.durationMinutes ?? 180, [Validators.required, Validators.min(1)]],
        bufferMinutes: [this.data?.bufferMinutes ?? 30, [Validators.required, Validators.min(0)]]
      },
      { validators: shiftTimingValidator }
    );

    // Auto-compute durationMinutes when examStartTime or examEndTime changes
    this.form.get('examStartTime')?.valueChanges.subscribe(() => this.recalculateDuration());
    this.form.get('examEndTime')?.valueChanges.subscribe(() => this.recalculateDuration());
  }

  private recalculateDuration(): void {
    const s = this.form.get('examStartTime')?.value;
    const e = this.form.get('examEndTime')?.value;
    if (s && e) {
      const sMin = toMinutes(s);
      const eMin = toMinutes(e);
      if (eMin > sMin) {
        this.form.get('durationMinutes')?.setValue(eMin - sMin, { emitEvent: false });
      }
    }
  }

  save(): void {
    if (this.form.valid) {
      const val = this.form.value;
      const result: CreateShiftRequest = {
        shiftNumber: val.shiftNumber,
        shiftName: val.shiftName || undefined,
        reportingTime: val.reportingTime,
        gateClosingTime: val.gateClosingTime,
        loginStartTime: val.loginStartTime,
        examStartTime: val.examStartTime,
        examEndTime: val.examEndTime,
        exitTime: val.exitTime || undefined,
        durationMinutes: val.durationMinutes,
        bufferMinutes: val.bufferMinutes
      };
      this.dialogRef.close(result);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
