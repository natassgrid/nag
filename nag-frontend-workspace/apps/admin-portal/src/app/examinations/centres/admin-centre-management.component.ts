import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ViewChild,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import {
  CentreManagementService,
  CentreResponse,
  CreateCentreRequest,
  GeoLocationService,
  GeoCountry,
  GeoState,
  GeoCity,
} from '@nag-frontend-workspace/examinations-data-access';
import {
  CentreKpiCardsComponent,
  CentreFilterBarComponent,
  CentreTableListComponent,
  CentreCreateDrawerComponent,
} from './components';

@Component({
  selector: 'nag-admin-centre-management',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule,
    CentreKpiCardsComponent,
    CentreFilterBarComponent,
    CentreTableListComponent,
    CentreCreateDrawerComponent,
  ],
  templateUrl: './admin-centre-management.component.html',
  styleUrls: ['./admin-centre-management.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminCentreManagementComponent implements OnInit {
  @ViewChild(CentreCreateDrawerComponent) createDrawerComponent?: CentreCreateDrawerComponent;

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

  // Computed KPIs
  readonly totalCentresCount = computed(() => (this.centres() || []).length);
  readonly totalCapacitySum = computed(() =>
    (this.centres() || []).reduce((sum, c) => sum + (c?.totalCapacity || 0), 0)
  );
  readonly activeCentresCount = computed(
    () => (this.centres() || []).filter((c) => c?.active !== false).length
  );
  readonly uniqueStates = computed(() => {
    const list = (this.centres() || [])
      .map((c) => c?.stateName || c?.state)
      .filter((s): s is string => !!s);
    return Array.from(new Set(list));
  });

  readonly filteredCentres = computed(() => {
    const list = this.centres() || [];
    const q = this.searchQuery().toLowerCase().trim();
    const stFilter = this.stateFilter();
    const statusF = this.statusFilter();

    return list.filter((c) => {
      if (!c) return false;
      const matchSearch =
        !q ||
        (c.centreName && c.centreName.toLowerCase().includes(q)) ||
        (c.city && c.city.toLowerCase().includes(q)) ||
        (c.cityName && c.cityName.toLowerCase().includes(q)) ||
        (c.state && c.state.toLowerCase().includes(q)) ||
        (c.district && c.district.toLowerCase().includes(q));

      const stateVal = c.stateName || c.state || '';
      const matchState =
        stFilter === 'ALL' ||
        (stateVal && stateVal.toLowerCase() === stFilter.toLowerCase());

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
        const items = list || [];
        this.countries.set(items);
        if (items.length > 0 && this.createDrawerComponent && !this.createDrawerComponent.formCountryId) {
          const india = items.find((c) => c?.name && c.name.toLowerCase().includes('india')) || items[0];
          if (india) {
            this.createDrawerComponent.formCountryId = india.id;
            this.onCountryChange(india.id);
          }
        }
      },
      error: () => {},
    });
  }

  onCountryChange(countryId: number | null): void {
    this.states.set([]);
    this.cities.set([]);

    if (countryId) {
      this.geoService.getStates(countryId).subscribe({
        next: (states) => this.states.set(states || []),
        error: () => this.states.set([]),
      });
    }
  }

  onStateChange(stateId: number | null): void {
    this.cities.set([]);

    if (stateId) {
      this.geoService.getCities(stateId).subscribe({
        next: (cities) => this.cities.set(cities || []),
        error: () => this.cities.set([]),
      });
    }
  }

  onCityChange(_cityId: number | null): void {
    // City selected
  }

  openCreate(): void {
    this.createDrawerComponent?.reset();
    this.drawerOpen.set(true);
    const countryList = this.countries() || [];
    if (countryList.length > 0 && this.createDrawerComponent) {
      const india = countryList.find((c) => c?.name && c.name.toLowerCase().includes('india')) || countryList[0];
      if (india) {
        this.createDrawerComponent.formCountryId = india.id;
        this.onCountryChange(india.id);
      }
    }
  }

  closeDrawer(): void {
    this.drawerOpen.set(false);
  }

  saveCentre(payload: CreateCentreRequest): void {
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
}
