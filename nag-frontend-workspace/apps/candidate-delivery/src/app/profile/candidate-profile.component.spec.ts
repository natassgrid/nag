import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { signal } from '@angular/core';
import { CandidateProfileComponent } from './candidate-profile.component';
import { AuthService, AuthUser } from '@nag-frontend-workspace/shared-data-access-auth';

describe('CandidateProfileComponent', () => {
  let component: CandidateProfileComponent;
  let fixture: ComponentFixture<CandidateProfileComponent>;
  let httpMock: HttpTestingController;
  let mockAuthService: {
    currentUser: jest.Mock;
    isAuthenticated: jest.Mock;
  };

  const dummyUser: AuthUser = {
    userId: 'cand-001',
    username: 'aarav@example.com',
    roles: ['ROLE_CANDIDATE'],
    token: 'jwt-dummy',
    email: 'aarav@example.com',
  };

  beforeEach(async () => {
    mockAuthService = {
      currentUser: jest.fn().mockReturnValue(dummyUser),
      isAuthenticated: jest.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [CandidateProfileComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: mockAuthService },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CandidateProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should load profile from API on init', () => {
    const req = httpMock.expectOne('/api/v1/candidates/cand-001');
    expect(req.request.method).toBe('GET');
    req.flush({
      candidateId: 'cand-001',
      fullName: 'Aarav Kumar',
      email: 'aarav@example.com',
      gender: 'MALE',
      category: 'GENERAL',
    });

    expect(component.profile().fullName).toBe('Aarav Kumar');
    expect(component.existsOnServer()).toBe(true);
  });

  it('should switch tabs', () => {
    const req = httpMock.expectOne('/api/v1/candidates/cand-001');
    req.flush({});

    component.activeTab.set('security');
    expect(component.activeTab()).toBe('security');
  });
});
