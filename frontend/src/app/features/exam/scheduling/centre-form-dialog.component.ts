/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { GeoLocationService, GeoCountry, GeoState, GeoCity } from './geo-location.service';
import { CentreResponse, CreateCentreRequest } from './scheduling.service';
import { RightDrawerComponent } from '../../../shared/components/right-drawer/right-drawer.component';

@Component({
  selector: 'app-centre-form-dialog',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatSlideToggleModule, RightDrawerComponent
  ],
  template: `
    <app-right-drawer
      [isOpen]="isOpen"
      [title]="centre ? 'Edit Centre' : 'Create Examination Centre'"
      [subtitle]="centre ? 'Update venue location, capacity, and lab details.' : 'Register a new venue location and laboratory capacity.'"
      width="480px"
      (close)="cancel()"
    >
      <div drawer-body>
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
      </div>

      <div drawer-footer>
        <button mat-button (click)="cancel()">Cancel</button>
        <button mat-raised-button color="primary" [disabled]="form.invalid" (click)="save()">
          {{ centre ? 'Update' : 'Create' }}
        </button>
      </div>
    </app-right-drawer>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 12px; }
    .full-width { width: 100%; }
    .form-row { display: flex; gap: 16px; }
    .form-row mat-form-field { flex: 1; }
    .toggle-field { margin: 8px 0; }
  `]
})
export class CentreFormDialogComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() centre?: CentreResponse;
  @Output() close = new EventEmitter<CreateCentreRequest | null>();

  form!: FormGroup;
  countries: GeoCountry[] = [];
  states: GeoState[] = [];
  cities: GeoCity[] = [];

  constructor(
    private fb: FormBuilder,
    private geoService: GeoLocationService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadCountries();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['isOpen'] && this.isOpen) {
      this.initForm();
      this.loadCountries();
    }
  }

  initForm(): void {
    const c = this.centre;
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
  }

  loadCountries(): void {
    const c = this.centre;
    this.geoService.getCountries().subscribe(list => {
      this.countries = list;
      if (c?.countryId) {
        this.geoService.getStates(c.countryId).subscribe(s => {
          this.states = s;
          if (c?.stateId) {
            this.geoService.getCities(c.stateId).subscribe(ci => this.cities = ci);
          }
        });
      }
    });

    this.form.get('countryId')!.valueChanges.subscribe(countryId => {
      this.states = [];
      this.cities = [];
      this.form.patchValue({ stateId: null, cityId: null, state: '', city: '' }, { emitEvent: false });
      if (countryId) {
        this.geoService.getStates(countryId).subscribe(s => this.states = s);
      }
    });

    this.form.get('stateId')!.valueChanges.subscribe(stateId => {
      this.cities = [];
      this.form.patchValue({ cityId: null, city: '' }, { emitEvent: false });
      if (stateId) {
        const sel = this.states.find(s => s.id === stateId);
        if (sel) this.form.patchValue({ state: sel.name }, { emitEvent: false });
        this.geoService.getCities(stateId).subscribe(c => this.cities = c);
      }
    });

    this.form.get('cityId')!.valueChanges.subscribe(cityId => {
      if (cityId) {
        const sel = this.cities.find(c => c.id === cityId);
        if (sel) this.form.patchValue({ city: sel.name }, { emitEvent: false });
      }
    });
  }

  cancel(): void {
    this.close.emit(null);
  }

  save(): void {
    if (this.form.invalid) return;
    const v = this.form.value;
    this.close.emit({
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
