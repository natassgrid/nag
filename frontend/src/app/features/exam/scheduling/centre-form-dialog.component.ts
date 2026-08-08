import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GeoLocationService, GeoCountry, GeoState, GeoCity } from './geo-location.service';
import { CentreResponse } from './scheduling.service';

export interface CentreFormDialogData {
  centre?: CentreResponse;
}

@Component({
  selector: 'app-centre-form-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatSlideToggleModule,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.centre ? 'Edit Centre' : 'Create Examination Centre' }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <!-- Cascading geo dropdowns -->
        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Country</mat-label>
            <mat-select formControlName="countryId">
              <mat-option *ngFor="let c of countries" [value]="c.id">{{ c.name }}</mat-option>
            </mat-select>
            <mat-hint>Select country first</mat-hint>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>State / UT</mat-label>
            <mat-select formControlName="stateId" [disabled]="!states.length">
              <mat-option *ngFor="let s of states" [value]="s.id">{{ s.name }}</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>City</mat-label>
            <mat-select formControlName="cityId" [disabled]="!cities.length">
              <mat-option *ngFor="let c of cities" [value]="c.id">{{ c.name }}</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>District</mat-label>
          <input matInput formControlName="district" placeholder="Optional district name" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Centre Name</mat-label>
          <input matInput formControlName="centreName" placeholder="e.g. NDMC Centre 1" />
          <mat-error *ngIf="form.get('centreName')?.hasError('required')">Required</mat-error>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Building</mat-label>
            <input matInput formControlName="building" placeholder="Block / Building name" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Floor</mat-label>
            <input matInput formControlName="floor" placeholder="e.g. Ground, 1st" />
          </mat-form-field>
        </div>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Laboratory ID</mat-label>
            <input matInput formControlName="laboratoryIdentifier" placeholder="e.g. LAB-A" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Total Capacity</mat-label>
            <input matInput type="number" formControlName="totalCapacity" min="0" />
          </mat-form-field>
        </div>

        <mat-slide-toggle formControlName="active" class="toggle-field">Active</mat-slide-toggle>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">
        {{ data.centre ? 'Update' : 'Create' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 12px; min-width: 520px; padding-top: 8px; }
    .full-width { width: 100%; }
    .form-row { display: flex; gap: 16px; }
    .form-row mat-form-field { flex: 1; }
    .toggle-field { margin: 8px 0; }
  `]
})
export class CentreFormDialogComponent implements OnInit {
  form!: FormGroup;
  countries: GeoCountry[] = [];
  states: GeoState[] = [];
  cities: GeoCity[] = [];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CentreFormDialogComponent>,
    private geoService: GeoLocationService,
    @Inject(MAT_DIALOG_DATA) public data: CentreFormDialogData
  ) {}

  ngOnInit(): void {
    const c = this.data.centre;
    this.form = this.fb.group({
      countryId: [c?.countryId || null],
      stateId: [c?.stateId || null],
      cityId: [c?.cityId || null],
      state: [c?.state || '', Validators.required],
      city: [c?.city || '', Validators.required],
      district: [c?.district || ''],
      centreName: [c?.centreName || '', Validators.required],
      building: [c?.building || ''],
      floor: [c?.floor || ''],
      laboratoryIdentifier: [c?.laboratoryIdentifier || ''],
      totalCapacity: [c?.totalCapacity || 0, Validators.min(0)],
      active: [c?.active !== false],
    });

    // Load countries
    this.geoService.getCountries().subscribe(list => {
      this.countries = list;
      // If editing, cascade pre-selections
      if (c?.countryId) {
        this.geoService.getStates(c.countryId).subscribe(s => {
          this.states = s;
          if (c?.stateId) {
            this.geoService.getCities(c.stateId).subscribe(ci => this.cities = ci);
          }
        });
      }
    });

    // Cascade: country → states
    this.form.get('countryId')!.valueChanges.subscribe(countryId => {
      this.states = [];
      this.cities = [];
      this.form.patchValue({ stateId: null, cityId: null, state: '', city: '' }, { emitEvent: false });
      if (countryId) {
        this.geoService.getStates(countryId).subscribe(s => this.states = s);
      }
    });

    // Cascade: state → cities + set state name
    this.form.get('stateId')!.valueChanges.subscribe(stateId => {
      this.cities = [];
      this.form.patchValue({ cityId: null, city: '' }, { emitEvent: false });
      if (stateId) {
        const sel = this.states.find(s => s.id === stateId);
        if (sel) this.form.patchValue({ state: sel.name }, { emitEvent: false });
        this.geoService.getCities(stateId).subscribe(c => this.cities = c);
      }
    });

    // Cascade: city → set city name
    this.form.get('cityId')!.valueChanges.subscribe(cityId => {
      if (cityId) {
        const sel = this.cities.find(c => c.id === cityId);
        if (sel) this.form.patchValue({ city: sel.name }, { emitEvent: false });
      }
    });
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.dialogRef.close({
      countryId: v.countryId || undefined,
      stateId: v.stateId || undefined,
      cityId: v.cityId || undefined,
      region: undefined,
      state: v.state,
      district: v.district || undefined,
      city: v.city,
      centreName: v.centreName,
      building: v.building || undefined,
      floor: v.floor || undefined,
      laboratoryIdentifier: v.laboratoryIdentifier || undefined,
      totalCapacity: v.totalCapacity || 0,
      active: v.active,
    });
  }
}
