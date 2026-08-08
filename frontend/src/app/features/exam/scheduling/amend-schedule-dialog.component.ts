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
import { ScheduleResponse } from './scheduling.service';

export interface AmendScheduleDialogData {
  schedule: ScheduleResponse;
}

@Component({
  selector: 'app-amend-schedule-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatDatepickerModule, MatNativeDateModule,
  ],
  template: `
    <h2 mat-dialog-title>Amend Schedule (v{{ data.schedule.scheduleVersion }})</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Change Reason</mat-label>
          <textarea matInput formControlName="changeReason" rows="3"
                    placeholder="Explain why this schedule is being amended"></textarea>
          <mat-error *ngIf="form.get('changeReason')?.hasError('required')">Mandatory for amendments</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Schedule Name</mat-label>
          <input matInput formControlName="scheduleName" />
          <mat-error *ngIf="form.get('scheduleName')?.hasError('required')">Required</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Notification Number</mat-label>
          <input matInput formControlName="notificationNumber" />
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Exam Date</mat-label>
            <input matInput [matDatepicker]="examDp" formControlName="examDate" />
            <mat-datepicker-toggle matIconSuffix [for]="examDp"></mat-datepicker-toggle>
            <mat-datepicker #examDp></mat-datepicker>
            <mat-error *ngIf="form.get('examDate')?.hasError('required')">Required</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Reserve Date</mat-label>
            <input matInput [matDatepicker]="resDp" formControlName="reserveDate" />
            <mat-datepicker-toggle matIconSuffix [for]="resDp"></mat-datepicker-toggle>
            <mat-datepicker #resDp></mat-datepicker>
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Effective From</mat-label>
            <input matInput [matDatepicker]="effDp" formControlName="effectiveFrom" />
            <mat-datepicker-toggle matIconSuffix [for]="effDp"></mat-datepicker-toggle>
            <mat-datepicker #effDp></mat-datepicker>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Time Zone</mat-label>
            <mat-select formControlName="timeZone">
              <mat-option value="Asia/Kolkata">Asia/Kolkata (IST)</mat-option>
              <mat-option value="Asia/Colombo">Asia/Colombo</mat-option>
              <mat-option value="UTC">UTC</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">Submit Amendment</button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 12px; min-width: 460px; padding-top: 8px; }
    .full-width { width: 100%; }
    .form-row { display: flex; gap: 16px; }
    .form-row mat-form-field { flex: 1; }
  `]
})
export class AmendScheduleDialogComponent implements OnInit {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<AmendScheduleDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: AmendScheduleDialogData
  ) {}

  ngOnInit(): void {
    const s = this.data.schedule;
    this.form = this.fb.group({
      changeReason: ['', Validators.required],
      scheduleName: [s.scheduleName, Validators.required],
      notificationNumber: [s.notificationNumber || ''],
      examDate: [s.examDate ? new Date(s.examDate) : null, Validators.required],
      reserveDate: [s.reserveDate ? new Date(s.reserveDate) : null],
      effectiveFrom: [null],
      timeZone: [s.timeZone || 'Asia/Kolkata', Validators.required],
    });
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.dialogRef.close({
      changeReason: v.changeReason,
      scheduleName: v.scheduleName,
      notificationNumber: v.notificationNumber || undefined,
      examDate: this.toIso(v.examDate),
      reserveDate: v.reserveDate ? this.toIso(v.reserveDate) : undefined,
      effectiveFrom: v.effectiveFrom ? this.toIso(v.effectiveFrom) : undefined,
      timeZone: v.timeZone,
    });
  }

  private toIso(d: any): string {
    return d instanceof Date ? d.toISOString().split('T')[0] : d;
  }
}
