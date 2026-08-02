import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CreateCentreRequest } from './scheduling.service';

@Component({
  selector: 'app-centre-form-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSlideToggleModule
  ],
  template: `
    <h2 mat-dialog-title>Create Examination Centre</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="form-container">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Centre Name</mat-label>
          <input matInput formControlName="centreName" placeholder="e.g. NDMC Centre 1" required />
          <mat-error *ngIf="form.get('centreName')?.hasError('required')">Centre name is required</mat-error>
        </mat-form-field>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>State</mat-label>
            <input matInput formControlName="state" placeholder="e.g. Delhi" required />
            <mat-error *ngIf="form.get('state')?.hasError('required')">State is required</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>City</mat-label>
            <input matInput formControlName="city" placeholder="e.g. New Delhi" required />
            <mat-error *ngIf="form.get('city')?.hasError('required')">City is required</mat-error>
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Region</mat-label>
            <input matInput formControlName="region" placeholder="e.g. North" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>District</mat-label>
            <input matInput formControlName="district" placeholder="e.g. Central Delhi" />
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Building</mat-label>
            <input matInput formControlName="building" placeholder="e.g. Block A" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Floor</mat-label>
            <input matInput formControlName="floor" placeholder="e.g. Ground" />
          </mat-form-field>
        </div>

        <div class="row">
          <mat-form-field appearance="outline">
            <mat-label>Laboratory Identifier</mat-label>
            <input matInput formControlName="laboratoryIdentifier" placeholder="e.g. LAB-01" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Total Capacity</mat-label>
            <input matInput type="number" formControlName="totalCapacity" min="0" required />
            <mat-error *ngIf="form.get('totalCapacity')?.hasError('required')">Capacity is required</mat-error>
          </mat-form-field>
        </div>

        <div class="toggle-row">
          <mat-slide-toggle formControlName="active" color="primary">Active</mat-slide-toggle>
        </div>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="cancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">Create Centre</button>
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
    .toggle-row {
      padding: 8px 0;
    }
  `]
})
export class CentreFormDialogComponent implements OnInit {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CentreFormDialogComponent>
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      region: [''],
      state: ['', Validators.required],
      district: [''],
      city: ['', Validators.required],
      centreName: ['', Validators.required],
      building: [''],
      floor: [''],
      laboratoryIdentifier: [''],
      totalCapacity: [100, [Validators.required, Validators.min(0)]],
      active: [true]
    });
  }

  save(): void {
    if (this.form.valid) {
      const val = this.form.value;
      const result: CreateCentreRequest = {
        region: val.region || undefined,
        state: val.state,
        district: val.district || undefined,
        city: val.city,
        centreName: val.centreName,
        building: val.building || undefined,
        floor: val.floor || undefined,
        laboratoryIdentifier: val.laboratoryIdentifier || undefined,
        totalCapacity: val.totalCapacity,
        active: val.active
      };
      this.dialogRef.close(result);
    }
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
