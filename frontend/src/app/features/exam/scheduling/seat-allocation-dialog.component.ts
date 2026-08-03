import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SchedulingService, CentreResponse, SeatAllocationResponse } from './scheduling.service';

export interface SeatAllocationDialogData {
  allocation?: SeatAllocationResponse;
}

@Component({
  selector: 'app-seat-allocation-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.allocation ? 'Edit Seat Allocation' : 'Add Seat Allocation' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">

        <!-- Centre selector (only for new allocations) -->
        <mat-form-field appearance="outline" class="full-width" *ngIf="!data.allocation">
          <mat-label>Centre</mat-label>
          <mat-select formControlName="centreId">
            <mat-option *ngFor="let c of centres" [value]="c.id">
              {{ c.centreName }} — {{ c.cityName || c.city }}, {{ c.stateName || c.state }}
            </mat-option>
          </mat-select>
          <mat-error *ngIf="form.get('centreId')?.hasError('required')">Select a centre</mat-error>
        </mat-form-field>

        <!-- Read-only centre display when editing -->
        <div class="readonly-field" *ngIf="data.allocation">
          <mat-icon>location_on</mat-icon>
          <span>Centre ID: {{ data.allocation.centreId | slice:0:8 }}…</span>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Total Seats</mat-label>
            <input matInput type="number" formControlName="totalSeats" min="0" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Available Seats</mat-label>
            <input matInput type="number" formControlName="availableSeats" min="0" />
            <mat-hint>Must not be negative</mat-hint>
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Reserved Seats</mat-label>
            <input matInput type="number" formControlName="reservedSeats" min="0" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>PwD Seats</mat-label>
            <input matInput type="number" formControlName="pwdSeats" min="0" />
            <mat-hint>Persons with Disabilities</mat-hint>
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Emergency Buffer</mat-label>
            <input matInput type="number" formControlName="emergencyBufferSeats" min="0" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Female Reserved</mat-label>
            <input matInput type="number" formControlName="femaleReservedSeats" min="0" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Special Category Seats</mat-label>
          <input matInput type="number" formControlName="specialCategorySeats" min="0" />
          <mat-hint>SC/ST/OBC/EWS etc.</mat-hint>
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">
        {{ data.allocation ? 'Update' : 'Save' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 12px; min-width: 440px; padding-top: 8px; }
    .full-width { width: 100%; }
    .form-row { display: flex; gap: 16px; }
    .form-row mat-form-field { flex: 1; }
    .readonly-field {
      display: flex; align-items: center; gap: 8px;
      padding: 10px 14px; background: #f5f5f5; border-radius: 6px;
      font-size: 13px; color: #616161;
    }
  `]
})
export class SeatAllocationDialogComponent implements OnInit {
  form!: FormGroup;
  centres: CentreResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<SeatAllocationDialogComponent>,
    private schedulingService: SchedulingService,
    @Inject(MAT_DIALOG_DATA) public data: SeatAllocationDialogData
  ) {}

  ngOnInit(): void {
    const a = this.data.allocation;
    this.form = this.fb.group({
      centreId: [a?.centreId || '', a ? [] : [Validators.required]],
      totalSeats: [a?.totalSeats || 0, [Validators.required, Validators.min(0)]],
      availableSeats: [a?.availableSeats || 0, [Validators.required, Validators.min(0)]],
      reservedSeats: [a?.reservedSeats || 0, Validators.min(0)],
      pwdSeats: [a?.pwdSeats || 0, Validators.min(0)],
      emergencyBufferSeats: [a?.emergencyBufferSeats || 0, Validators.min(0)],
      femaleReservedSeats: [a?.femaleReservedSeats || 0, Validators.min(0)],
      specialCategorySeats: [a?.specialCategorySeats || 0, Validators.min(0)],
    });

    // Load centres for dropdown (only needed when creating)
    if (!a) {
      this.schedulingService.listCentres().subscribe(list => this.centres = list);
    }
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.dialogRef.close({
      centreId: this.data.allocation?.centreId || v.centreId,
      totalSeats: v.totalSeats,
      availableSeats: v.availableSeats,
      reservedSeats: v.reservedSeats,
      pwdSeats: v.pwdSeats,
      emergencyBufferSeats: v.emergencyBufferSeats,
      femaleReservedSeats: v.femaleReservedSeats,
      specialCategorySeats: v.specialCategorySeats,
    });
  }
}
