import {
  Component,
  ChangeDetectionStrategy,
  input,
  output,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  CreateCentreRequest,
  GeoCountry,
  GeoState,
  GeoCity,
} from '@nag-frontend-workspace/examinations-data-access';
import { CentreLocationFormComponent } from '../centre-location-form/centre-location-form.component';
import { CentreFacilityFormComponent } from '../centre-facility-form/centre-facility-form.component';

@Component({
  selector: 'nag-centre-create-drawer',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    CentreLocationFormComponent,
    CentreFacilityFormComponent,
  ],
  templateUrl: './centre-create-drawer.component.html',
  styleUrl: './centre-create-drawer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CentreCreateDrawerComponent {
  readonly isOpen = input<boolean>(false);
  readonly isSaving = input<boolean>(false);
  readonly countries = input<GeoCountry[]>([]);
  readonly states = input<GeoState[]>([]);
  readonly cities = input<GeoCity[]>([]);

  readonly closeDrawer = output<void>();
  readonly save = output<CreateCentreRequest>();
  readonly countryChange = output<number | null>();
  readonly stateChange = output<number | null>();
  readonly cityChange = output<number | null>();

  readonly validationError = signal<string | null>(null);

  formCountryId: number | null = null;
  formStateId: number | null = null;
  formCityId: number | null = null;
  formStateName = '';
  formCityName = '';
  formDistrict = '';
  formCentreName = '';
  formBuilding = '';
  formFloor = '';
  formLabIdentifier = '';
  formTotalCapacity = 250;
  formActive = true;

  onCountryChanged(countryId: number | null): void {
    this.formCountryId = countryId;
    this.formStateId = null;
    this.formCityId = null;
    this.formStateName = '';
    this.formCityName = '';
    this.countryChange.emit(countryId);
  }

  onStateChanged(stateId: number | null): void {
    this.formStateId = stateId;
    this.formCityId = null;
    this.formCityName = '';
    if (stateId) {
      const selected = (this.states() || []).find((s) => s?.id === stateId);
      if (selected) {
        this.formStateName = selected.name;
      }
    }
    this.stateChange.emit(stateId);
  }

  onCityChanged(cityId: number | null): void {
    this.formCityId = cityId;
    if (cityId) {
      const selected = (this.cities() || []).find((c) => c?.id === cityId);
      if (selected) {
        this.formCityName = selected.name;
      }
    }
    this.cityChange.emit(cityId);
  }

  onSave(): void {
    this.validationError.set(null);
    if (!this.formCentreName.trim()) {
      this.validationError.set('Test Centre Name is required.');
      return;
    }
    if (!this.formStateName.trim()) {
      this.validationError.set('State is required.');
      return;
    }
    if (!this.formCityName.trim()) {
      this.validationError.set('City is required.');
      return;
    }
    if (!this.formTotalCapacity || this.formTotalCapacity <= 0) {
      this.validationError.set('Total Capacity must be greater than zero.');
      return;
    }

    this.save.emit({
      countryId: this.formCountryId || undefined,
      stateId: this.formStateId || undefined,
      cityId: this.formCityId || undefined,
      state: this.formStateName.trim(),
      district: this.formDistrict.trim() || undefined,
      city: this.formCityName.trim(),
      centreName: this.formCentreName.trim(),
      building: this.formBuilding.trim() || undefined,
      floor: this.formFloor.trim() || undefined,
      laboratoryIdentifier: this.formLabIdentifier.trim() || undefined,
      totalCapacity: this.formTotalCapacity,
      active: this.formActive,
    });
  }

  reset(): void {
    this.validationError.set(null);
    this.formCentreName = '';
    this.formDistrict = '';
    this.formBuilding = '';
    this.formFloor = '';
    this.formLabIdentifier = '';
    this.formTotalCapacity = 250;
    this.formActive = true;
  }
}
