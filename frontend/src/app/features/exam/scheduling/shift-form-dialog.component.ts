import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ShiftResponse } from './scheduling.service';

export interface ShiftFormDialogData {
  shift?: ShiftResponse;
}

@Component({
  selector: 'app-shift-form-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatButtonModule, MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.shift ? 'Edit Shift' : 'Add Shift' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Shift Number</mat-label>
            <input matInput type="number" formControlName="shiftNumber" min="1" />
            <mat-error *ngIf="form.get('shiftNumber')?.hasError('required')">Required</mat-error>
            <mat-error *ngIf="form.get('shiftNumber')?.hasError('min')">Min 1</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Shift Name</mat-label>
            <input matInput formControlName="shiftName" placeholder="Morning / Afternoon / Evening" />
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Reporting Time</mat-label>
            <input matInput type="time" formControlName="reportingTime" />
            <mat-error *ngIf="form.get('reportingTime')?.hasError('required')">Required</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Gate Closing Time</mat-label>
            <input matInput type="time" formControlName="gateClosingTime" />
            <mat-error *ngIf="form.get('gateClosingTime')?.hasError('required')">Required</mat-error>
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Login Start Time</mat-label>
            <input matInput type="time" formControlName="loginStartTime" />
            <mat-error *ngIf="form.get('loginStartTime')?.hasError('required')">Required</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Exam Start Time</mat-label>
            <input matInput type="time" formControlName="examStartTime" />
            <mat-error *ngIf="form.get('examStartTime')?.hasError('required')">Required</mat-error>
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Exam End Time</mat-label>
            <input matInput type="time" formControlName="examEndTime" />
            <mat-error *ngIf="form.get('examEndTime')?.hasError('required')">Required</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Exit Time</mat-label>
            <input matInput type="time" formControlName="exitTime" />
            <mat-hint>Optional</mat-hint>
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Duration (minutes)</mat-label>
            <input matInput type="number" formControlName="durationMinutes" min="1" />
            <mat-hint>Auto-computed from Start/End</mat-hint>
            <mat-error *ngIf="form.get('durationMinutes')?.hasError('required')">Required</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Buffer Minutes</mat-label>
            <input matInput type="number" formControlName="bufferMinutes" min="0" />
            <mat-hint>Gap before next shift</mat-hint>
          </mat-form-field>
        </div>

        <!-- Timing validation errors -->
        <div class="validation-errors" *ngIf="timingErrors.length > 0">
          <div class="timing-error" *ngFor="let err of timingErrors">
            <mat-icon>warning</mat-icon> {{ err }}
          </div>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary"
              [disabled]="form.invalid || timingErrors.length > 0"
              (click)="save()">
        {{ data.shift ? 'Update' : 'Add' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 12px; min-width: 500px; padding-top: 8px; }
    .form-row { display: flex; gap: 16px; }
    .form-row mat-form-field { flex: 1; }
    .validation-errors { background: #fff3e0; border-left: 4px solid #e65100; padding: 10px 14px; border-radius: 4px; }
    .timing-error { display: flex; align-items: center; gap: 6px; font-size: 13px; color: #e65100; margin: 4px 0; }
    .timing-error mat-icon { font-size: 18px; height: 18px; width: 18px; }
  `]
})
export class ShiftFormDialogComponent implements OnInit {
  form!: FormGroup;
  timingErrors: string[] = [];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ShiftFormDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ShiftFormDialogData
  ) {}

  ngOnInit(): void {
    const s = this.data.shift;
    this.form = this.fb.group({
      shiftNumber: [s?.shiftNumber || 1, [Validators.required, Validators.min(1)]],
      shiftName: [s?.shiftName || ''],
      reportingTime: [s?.reportingTime || '', Validators.required],
      gateClosingTime: [s?.gateClosingTime || '', Validators.required],
      loginStartTime: [s?.loginStartTime || '', Validators.required],
      examStartTime: [s?.examStartTime || '', Validators.required],
      examEndTime: [s?.examEndTime || '', Validators.required],
      exitTime: [s?.exitTime || ''],
      durationMinutes: [s?.durationMinutes || 180, [Validators.required, Validators.min(1)]],
      bufferMinutes: [s?.bufferMinutes || 0, Validators.min(0)],
    });

    // Auto-compute duration when start/end change
    this.form.get('examStartTime')!.valueChanges.subscribe(() => this.autoComputeDuration());
    this.form.get('examEndTime')!.valueChanges.subscribe(() => this.autoComputeDuration());

    // Auto-fill downstream times when reportingTime changes (only if fields are empty or in create mode)
    this.form.get('reportingTime')!.valueChanges.subscribe((reporting) => {
      if (!reporting || this.data.shift) return; // Skip auto-fill in edit mode
      const gate = this.form.get('gateClosingTime')!.value;
      const login = this.form.get('loginStartTime')!.value;
      const start = this.form.get('examStartTime')!.value;
      // Only auto-fill if downstream fields are empty
      if (!gate) this.form.get('gateClosingTime')!.setValue(this.addMinutes(reporting, 60), { emitEvent: false });
      if (!login) this.form.get('loginStartTime')!.setValue(this.addMinutes(reporting, 75), { emitEvent: false });
      if (!start) this.form.get('examStartTime')!.setValue(this.addMinutes(reporting, 90), { emitEvent: false });
      if (!this.form.get('examEndTime')!.value && this.form.get('durationMinutes')!.value > 0) {
        const examStart = this.form.get('examStartTime')!.value || this.addMinutes(reporting, 90);
        this.form.get('examEndTime')!.setValue(
          this.addMinutes(examStart, this.form.get('durationMinutes')!.value), { emitEvent: false }
        );
      }
      this.validateTimings();
    });

    // Validate timings on any change
    this.form.valueChanges.subscribe(() => this.validateTimings());
  }

  private addMinutes(time: string, minutes: number): string {
    const [h, m] = time.split(':').map(Number);
    const total = h * 60 + m + minutes;
    const nh = Math.floor(total / 60) % 24;
    const nm = total % 60;
    return `${nh.toString().padStart(2, '0')}:${nm.toString().padStart(2, '0')}`;
  }

  private autoComputeDuration(): void {
    const start = this.form.get('examStartTime')!.value;
    const end = this.form.get('examEndTime')!.value;
    if (start && end) {
      const mins = this.diffMinutes(start, end);
      if (mins > 0) {
        this.form.get('durationMinutes')!.setValue(mins, { emitEvent: false });
      }
    }
  }

  private validateTimings(): void {
    const errors: string[] = [];
    const v = this.form.value;

    if (v.reportingTime && v.gateClosingTime && v.reportingTime >= v.gateClosingTime) {
      errors.push('Reporting Time must be before Gate Closing Time');
    }
    if (v.gateClosingTime && v.loginStartTime && v.gateClosingTime >= v.loginStartTime) {
      errors.push('Gate Closing Time must be before Login Start Time');
    }
    if (v.loginStartTime && v.examStartTime && v.loginStartTime >= v.examStartTime) {
      errors.push('Login Start Time must be before Exam Start Time');
    }
    if (v.examStartTime && v.examEndTime && v.examStartTime >= v.examEndTime) {
      errors.push('Exam Start Time must be before Exam End Time');
    }
    if (v.examStartTime && v.examEndTime && v.durationMinutes) {
      const computed = this.diffMinutes(v.examStartTime, v.examEndTime);
      if (computed > 0 && computed !== v.durationMinutes) {
        errors.push(`Duration must equal End − Start (${computed} min), got ${v.durationMinutes}`);
      }
    }

    this.timingErrors = errors;
  }

  private diffMinutes(start: string, end: string): number {
    const [sh, sm] = start.split(':').map(Number);
    const [eh, em] = end.split(':').map(Number);
    return (eh * 60 + em) - (sh * 60 + sm);
  }

  save(): void {
    if (this.form.invalid || this.timingErrors.length > 0) return;
    const v = this.form.value;
    this.dialogRef.close({
      shiftNumber: v.shiftNumber,
      shiftName: v.shiftName || undefined,
      reportingTime: v.reportingTime,
      gateClosingTime: v.gateClosingTime,
      loginStartTime: v.loginStartTime,
      examStartTime: v.examStartTime,
      examEndTime: v.examEndTime,
      exitTime: v.exitTime || undefined,
      durationMinutes: v.durationMinutes,
      bufferMinutes: v.bufferMinutes || 0,
    });
  }
}
