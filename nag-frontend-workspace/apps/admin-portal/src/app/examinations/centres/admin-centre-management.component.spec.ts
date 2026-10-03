import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { AdminCentreManagementComponent } from './admin-centre-management.component';
import { CentreManagementService, GeoLocationService } from '@nag-frontend-workspace/examinations-data-access';

describe('AdminCentreManagementComponent', () => {
  let component: AdminCentreManagementComponent;
  let fixture: ComponentFixture<AdminCentreManagementComponent>;
  let mockCentreService: {
    centres: any;
    loading: any;
    listCentres: jest.Mock;
    loadCentres: jest.Mock;
    createCentre: jest.Mock;
  };
  let mockGeoService: {
    getCountries: jest.Mock;
  };

  beforeEach(async () => {
    mockCentreService = {
      centres: signal([
        {
          id: 'c-1',
          centreCode: 'DEL-01',
          name: 'Delhi North Examination Centre',
          city: 'New Delhi',
          state: 'Delhi',
          country: 'India',
          totalCapacity: 500,
          activeCapacity: 450,
          active: true,
          isSecuredHub: true,
        },
      ]),
      loading: signal(false),
      listCentres: jest.fn().mockReturnValue(of([])),
      loadCentres: jest.fn(),
      createCentre: jest.fn().mockReturnValue(of({})),
    };

    mockGeoService = {
      getCountries: jest.fn().mockReturnValue(of([{ code: 'IN', name: 'India' }])),
    };

    await TestBed.configureTestingModule({
      imports: [AdminCentreManagementComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: CentreManagementService, useValue: mockCentreService },
        { provide: GeoLocationService, useValue: mockGeoService },
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminCentreManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize centre list and KPIs', () => {
    expect(component.centres().length).toBe(1);
    expect(component.centres()[0].centreCode).toBe('DEL-01');
    expect(component.totalCentresCount()).toBe(1);
    expect(component.totalCapacitySum()).toBe(500);
  });

  it('should filter centres by search query', () => {
    component.searchQuery.set('Delhi');
    expect(component.filteredCentres().length).toBe(1);

    component.searchQuery.set('Mumbai');
    expect(component.filteredCentres().length).toBe(0);
  });
});
