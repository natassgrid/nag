import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { AmendScheduleRequest, ScheduleResponse } from './scheduling.service';

@Component({
  selector: 'app-amend-schedule-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  template: `
    <h2 mat-dialog-title>Amend Schedule (v{{ data.scheduleVersion }})</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-container">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Change Reason</mat-label>
          <textarea
            matInput
            formControlName="changeReason"
            rows="3"
            placeholder="Explain why this schedule is being amended..."
            required
          ></textarea>
          <mat-hint>Mandatory — explain why this schedule is being amended</mat-hint>
          <mat-error *ngIf="form.get('changeReason')?.hasError('required')">Change reason is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Schedule Name</mat-label>
          <input matInput formControlName="scheduleName" required />
          <mat-error *ngIf="form.get('scheduleName')?.hasError('required')">Schedule name is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Notification Number</mat-label>
          <input matInput formControlName="notificationNumber" />
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Exam Date</mat-label>
            <input matInput [matDatepicker]="examDatePicker" formControlName="examDate" required />
            <mat-datepicker-toggle matIconSuffix [for]="examDatePicker"></mat-datepicker-toggle>
            <mat-datepicker #examDatePicker></mat-datepicker>
            <mat-error *ngIf="form.get('examDate')?.hasError('required')">Exam date is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Reserve Date</mat-label>
            <input matInput [matDatepicker]="reserveDatePicker" formControlName="reserveDate" />
            <mat-datepicker-toggle matIconSuffix [for]="reserveDatePicker"></mat-datepicker-toggle>
            <mat-datepicker #reserveDatePicker></mat-datepicker>
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Effective From</mat-label>
            <input matInput [matDatepicker]="effectiveFromPicker" formControlName="effectiveFrom" />
            <mat-datepicker-toggle matIconSuffix [for]="effectiveFromPicker"></mat-datepicker-toggle>
            <mat-datepicker #effectiveFromPicker></mat-datepicker>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Time Zone</mat-label>
            <mat-select formControlName="timeZone" required>
              <mat-option value="Asia/Kolkata">Asia/Kolkata</mat-option>
              <mat-option value="Asia/Colombo">Asia/Colombo</mat-option>
              <mat-option value="UTC">UTC</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="warn" [disabled]="form.invalid" (click)="save()">Amend Schedule</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-top: 8px;
      min-width: 440px;
    }
    .full-width {
      width: 100%;
    }
    .row {
      display: flex;
      gap: 16px;
    }
    .row mat-form-field {
      flex: 1;
    }
  `]
})
export class AmendScheduleDialogComponent implements OnInit {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AmendScheduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ScheduleResponse
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      changeReason: ['', Validators.required],
      scheduleName: [this.data.scheduleName || '', Validators.required],
      notificationNumber: [this.data.notificationNumber || ''],
      examDate: [this.data.examDate ? new Date(this.data.examDate) : null, Validators.required],
      reserveDate: [this.data.reserveDate ? new Date(this.data.reserveDate) : null],
      effectiveFrom: [this.data.effectiveFrom ? new Date(this.data.effectiveFrom) : null],
      timeZone: [this.data.timeZone || 'Asia/Kolkata', Validators.required]
    });
  }

  private formatDate(val: any): string | null {
    if (!val) return null;
    if (val instanceof Date) {
      return val.toISOString().split('T')[0];
    }
    return val;
  }

  save(): void {
    if (this.form.valid) {
      const val = this.form.value;
      const result: AmendScheduleRequest = {
        changeReason: val.changeReason,
        scheduleName: val.scheduleName,
        notificationNumber: val.notificationNumber || null,
        examDate: this.formatDate(val.examDate)!,
        reserveDate: this.formatDate(val.reserveDate),
        effectiveFrom: this.formatDate(val.effectiveFrom),
        timeZone: val.timeZone
      };
      this.dialogRef.close(result);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
