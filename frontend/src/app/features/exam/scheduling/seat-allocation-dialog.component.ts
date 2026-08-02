import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { CentreResponse, SeatAllocationRequest, SeatAllocationResponse } from './scheduling.service';

export interface SeatAllocationDialogData {
  allocation?: SeatAllocationResponse;
  centres: CentreResponse[];
}

@Component({
  selector: 'app-seat-allocation-dialog',
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
    <h2 mat-dialog-title>{{ isEdit ? 'Edit Seat Allocation' : 'Allocate Seats for Shift' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-container">
        <!-- Centre Field -->
        <mat-form-field appearance="outline" class="full-width" *ngIf="!isEdit">
          <mat-label>Examination Centre</mat-label>
          <mat-select formControlName="centreId" required>
            <mat-option *ngFor="let c of data.centres" [value]="c.id">
              {{ c.centreName }} ({{ c.city }}, {{ c.state }}) - Cap: {{ c.totalCapacity }}
            </mat-option>
          </mat-select>
          <mat-error *ngIf="form.get('centreId')?.hasError('required')">Centre is required</mat-error>
        </mat-form-field>

        <div class="centre-display" *ngIf="isEdit">
          <strong>Centre:</strong> {{ selectedCentreName }}
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Total Seats</mat-label>
            <input matInput type="number" formControlName="totalSeats" min="0" required />
            <mat-error *ngIf="form.get('totalSeats')?.hasError('required')">Required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Available Seats</mat-label>
            <input matInput type="number" formControlName="availableSeats" min="0" required />
            <mat-hint>Must not be negative</mat-hint>
            <mat-error *ngIf="form.get('availableSeats')?.hasError('required')">Required</mat-error>
            <mat-error *ngIf="form.get('availableSeats')?.hasError('min')">Must not be negative</mat-error>
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Reserved Seats</mat-label>
            <input matInput type="number" formControlName="reservedSeats" min="0" required />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>PwD Seats</mat-label>
            <input matInput type="number" formControlName="pwdSeats" min="0" required />
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Emergency Buffer Seats</mat-label>
            <input matInput type="number" formControlName="emergencyBufferSeats" min="0" required />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Female Reserved Seats</mat-label>
            <input matInput type="number" formControlName="femaleReservedSeats" min="0" required />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Special Category Seats</mat-label>
          <input matInput type="number" formControlName="specialCategorySeats" min="0" required />
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">Save Allocation</button>
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
    .centre-display {
      margin-bottom: 12px;
      font-size: 14px;
    }
  `]
})
export class SeatAllocationDialogComponent implements OnInit {
  form!: FormGroup;
  isEdit = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<SeatAllocationDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: SeatAllocationDialogData
  ) {}

  ngOnInit(): void {
    this.isEdit = !!this.data.allocation;

    this.form = this.fb.group({
      centreId: [this.data.allocation?.centreId ?? '', Validators.required],
      totalSeats: [this.data.allocation?.totalSeats ?? 100, [Validators.required, Validators.min(0)]],
      availableSeats: [this.data.allocation?.availableSeats ?? 80, [Validators.required, Validators.min(0)]],
      reservedSeats: [this.data.allocation?.reservedSeats ?? 10, [Validators.required, Validators.min(0)]],
      pwdSeats: [this.data.allocation?.pwdSeats ?? 5, [Validators.required, Validators.min(0)]],
      emergencyBufferSeats: [this.data.allocation?.emergencyBufferSeats ?? 5, [Validators.required, Validators.min(0)]],
      femaleReservedSeats: [this.data.allocation?.femaleReservedSeats ?? 0, [Validators.required, Validators.min(0)]],
      specialCategorySeats: [this.data.allocation?.specialCategorySeats ?? 0, [Validators.required, Validators.min(0)]]
    });
  }

  get selectedCentreName(): string {
    const cId = this.form.get('centreId')?.value;
    const found = this.data.centres.find(c => c.id === cId);
    return found ? `${found.centreName} (${found.city})` : cId;
  }

  save(): void {
    if (this.form.valid) {
      const val = this.form.value;
      const result: SeatAllocationRequest = {
        centreId: val.centreId,
        totalSeats: val.totalSeats,
        availableSeats: val.availableSeats,
        reservedSeats: val.reservedSeats,
        pwdSeats: val.pwdSeats,
        emergencyBufferSeats: val.emergencyBufferSeats,
        femaleReservedSeats: val.femaleReservedSeats,
        specialCategorySeats: val.specialCategorySeats
      };
      this.dialogRef.close(result);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
