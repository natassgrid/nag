import {
  ChangeDetectionStrategy,
  Component,
  input,
  output,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import {
  GeoCountry,
  GeoState,
  GeoCity,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-centre-location-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './centre-location-form.component.html',
  styleUrl: './centre-location-form.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CentreLocationFormComponent {
  readonly countries = input<GeoCountry[]>([]);
  readonly states = input<GeoState[]>([]);
  readonly cities = input<GeoCity[]>([]);

  readonly countryId = input<number | null>(null);
  readonly stateId = input<number | null>(null);
  readonly cityId = input<number | null>(null);
  readonly stateName = input<string>('');
  readonly cityName = input<string>('');
  readonly district = input<string>('');

  readonly countryChange = output<number | null>();
  readonly stateChange = output<number | null>();
  readonly cityChange = output<number | null>();
  readonly stateNameChange = output<string>();
  readonly cityNameChange = output<string>();
  readonly districtChange = output<string>();

  onCountryChanged(countryId: number | null): void {
    this.countryChange.emit(countryId);
  }

  onStateChanged(stateId: number | null): void {
    this.stateChange.emit(stateId);
  }

  onCityChanged(cityId: number | null): void {
    this.cityChange.emit(cityId);
  }
}
