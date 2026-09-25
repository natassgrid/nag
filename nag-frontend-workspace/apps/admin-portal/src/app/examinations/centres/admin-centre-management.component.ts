import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import {
  CentreManagementService,
  CentreResponse,
  CreateCentreRequest,
  GeoLocationService,
  GeoCountry,
  GeoState,
  GeoCity,
} from '@nag-frontend-workspace/examinations-data-access';

@Component({
  selector: 'nag-admin-centre-management',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './admin-centre-management.component.html',
  styleUrls: ['./admin-centre-management.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminCentreManagementComponent implements OnInit {
  private readonly centreService = inject(CentreManagementService);
  private readonly geoService = inject(GeoLocationService);
  private readonly snackBar = inject(MatSnackBar);

  readonly centres = this.centreService.centres;
  readonly loading = this.centreService.loading;

  // Search and Filters
  readonly searchQuery = signal<string>('');
  readonly stateFilter = signal<string>('ALL');
  readonly statusFilter = signal<string>('ALL');

  // Drawer / Form State
  readonly drawerOpen = signal<boolean>(false);
  readonly isSaving = signal<boolean>(false);

  // Geo lists for Form
  readonly countries = signal<GeoCountry[]>([]);
  readonly states = signal<GeoState[]>([]);
  readonly cities = signal<GeoCity[]>([]);

  // Form Fields
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

  // Computed KPIs
  readonly totalCentresCount = computed(() => this.centres().length);
  readonly totalCapacitySum = computed(() =>
    this.centres().reduce((sum, c) => sum + (c.totalCapacity || 0), 0)
  );
  readonly activeCentresCount = computed(
    () => this.centres().filter((c) => c.active !== false).length
  );
  readonly uniqueStates = computed(() => {
    const list = this.centres().map((c) => c.stateName || c.state).filter(Boolean);
    return Array.from(new Set(list));
  });

  readonly filteredCentres = computed(() => {
    const q = this.searchQuery().toLowerCase().trim();
    const stFilter = this.stateFilter();
    const statusF = this.statusFilter();

    return this.centres().filter((c) => {
      const matchSearch =
        !q ||
        c.centreName.toLowerCase().includes(q) ||
        c.city.toLowerCase().includes(q) ||
        (c.cityName && c.cityName.toLowerCase().includes(q)) ||
        c.state.toLowerCase().includes(q) ||
        (c.district && c.district.toLowerCase().includes(q));

      const matchState =
        stFilter === 'ALL' ||
        (c.stateName || c.state).toLowerCase() === stFilter.toLowerCase();

      const matchStatus =
        statusF === 'ALL' ||
        (statusF === 'ACTIVE' && c.active !== false) ||
        (statusF === 'INACTIVE' && c.active === false);

      return matchSearch && matchState && matchStatus;
    });
  });

  ngOnInit(): void {
    this.loadCentres();
    this.loadCountries();
  }

  loadCentres(): void {
    this.centreService.listCentres(undefined, undefined, 0, 100).subscribe({
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to load test centres',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  loadCountries(): void {
    this.geoService.getCountries().subscribe({
      next: (list) => {
        this.countries.set(list);
        if (list.length > 0 && !this.formCountryId) {
          const india = list.find((c) => c.name.toLowerCase().includes('india')) || list[0];
          this.formCountryId = india.id;
          this.onCountryChange(india.id);
        }
      },
      error: () => {},
    });
  }

  onCountryChange(countryId: number | null): void {
    this.states.set([]);
    this.cities.set([]);
    this.formStateId = null;
    this.formCityId = null;
    this.formStateName = '';
    this.formCityName = '';

    if (countryId) {
      this.geoService.getStates(countryId).subscribe((states) => {
        this.states.set(states);
      });
    }
  }

  onStateChange(stateId: number | null): void {
    this.cities.set([]);
    this.formCityId = null;
    this.formCityName = '';

    if (stateId) {
      const selected = this.states().find((s) => s.id === stateId);
      if (selected) {
        this.formStateName = selected.name;
      }
      this.geoService.getCities(stateId).subscribe((cities) => {
        this.cities.set(cities);
      });
    }
  }

  onCityChange(cityId: number | null): void {
    if (cityId) {
      const selected = this.cities().find((c) => c.id === cityId);
      if (selected) {
        this.formCityName = selected.name;
      }
    }
  }

  openCreate(): void {
    this.resetForm();
    this.drawerOpen.set(true);
    if (this.countries().length > 0 && !this.formCountryId) {
      const india = this.countries().find((c) => c.name.toLowerCase().includes('india')) || this.countries()[0];
      this.formCountryId = india.id;
      this.onCountryChange(india.id);
    }
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  saveCentre(): void {
    if (!this.formCentreName.trim()) {
      this.snackBar.open('Centre Name is required', 'Dismiss', { duration: 3000 });
      return;
    }
    if (!this.formStateName.trim()) {
      this.snackBar.open('State is required', 'Dismiss', { duration: 3000 });
      return;
    }
    if (!this.formCityName.trim()) {
      this.snackBar.open('City is required', 'Dismiss', { duration: 3000 });
      return;
    }

    const payload: CreateCentreRequest = {
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
    };

    this.isSaving.set(true);
    this.centreService.createCentre(payload).subscribe({
      next: () => {
        this.isSaving.set(false);
        this.closeDrawer();
        this.snackBar.open('Examination Centre registered successfully', 'OK', {
          duration: 3000,
        });
      },
      error: (err) => {
        this.isSaving.set(false);
        this.snackBar.open(
          err?.error?.message || 'Failed to register centre',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  deactivateCentre(centre: CentreResponse): void {
    this.centreService.deactivateCentre(centre.id).subscribe({
      next: () => {
        this.snackBar.open(`Centre "${centre.centreName}" marked as INACTIVE`, 'OK', {
          duration: 3000,
        });
      },
      error: (err) => {
        this.snackBar.open(
          err?.error?.message || 'Failed to deactivate centre',
          'Dismiss',
          { duration: 4000 }
        );
      },
    });
  }

  private resetForm(): void {
    this.formCentreName = '';
    this.formDistrict = '';
    this.formBuilding = '';
    this.formFloor = '';
    this.formLabIdentifier = '';
    this.formTotalCapacity = 250;
    this.formActive = true;
    if (this.formCountryId) {
      this.onCountryChange(this.formCountryId);
    }
  }
}
