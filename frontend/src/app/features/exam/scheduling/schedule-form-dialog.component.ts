import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { CreateScheduleRequest } from './scheduling.service';

@Component({
  selector: 'app-schedule-form-dialog',
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
    <h2 mat-dialog-title>Create New Schedule</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-container">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Schedule Name</mat-label>
          <input matInput formControlName="scheduleName" placeholder="e.g. Annual Main Exam Schedule 2027" required />
          <mat-error *ngIf="form.get('scheduleName')?.hasError('required')">Schedule name is required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Notification Number</mat-label>
          <input matInput formControlName="notificationNumber" placeholder="e.g. NOTIF/2027/001" />
          <mat-hint>Government gazette reference</mat-hint>
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

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Time Zone</mat-label>
          <mat-select formControlName="timeZone" required>
            <mat-option value="Asia/Kolkata">Asia/Kolkata</mat-option>
            <mat-option value="Asia/Colombo">Asia/Colombo</mat-option>
            <mat-option value="UTC">UTC</mat-option>
          </mat-select>
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">Create Schedule</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .form-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding-top: 8px;
      min-width: 400px;
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
export class ScheduleFormDialogComponent implements OnInit {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<ScheduleFormDialogComponent>
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      scheduleName: ['', Validators.required],
      notificationNumber: [''],
      examDate: [null, Validators.required],
      reserveDate: [null],
      timeZone: ['Asia/Kolkata', Validators.required]
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
      const result: CreateScheduleRequest = {
        scheduleName: val.scheduleName,
        notificationNumber: val.notificationNumber || null,
        examDate: this.formatDate(val.examDate)!,
        reserveDate: this.formatDate(val.reserveDate),
        timeZone: val.timeZone
      };
      this.dialogRef.close(result);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
