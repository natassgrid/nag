import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { CandidateProfileComponent } from './candidate-profile.component';
import { AuthService, AuthUser } from '@nag-frontend-workspace/shared-data-access-auth';
import { EducationEntry } from './models';

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

  it('should load profile from API on init and mark existsOnServer as true', () => {
    const req = httpMock.expectOne('/api/v1/candidates/cand-001');
    expect(req.request.method).toBe('GET');
    req.flush({
      candidateId: 'cand-001',
      fullName: 'Aarav Kumar',
      email: 'aarav@example.com',
      gender: 'MALE',
      category: 'GENERAL',
      kycStatus: 'VERIFIED',
      digiLockerStatus: 'LINKED',
    });

    expect(component.profile().fullName).toBe('Aarav Kumar');
    expect(component.profile().kycStatus).toBe('VERIFIED');
    expect(component.profile().digiLockerStatus).toBe('LINKED');
    expect(component.existsOnServer()).toBe(true);
    expect(component.loading()).toBe(false);
  });

  it('should handle API load failure gracefully without crashing', () => {
    const req = httpMock.expectOne('/api/v1/candidates/cand-001');
    req.error(new ProgressEvent('error'), { status: 404, statusText: 'Not Found' });

    expect(component.existsOnServer()).toBe(false);
    expect(component.loading()).toBe(false);
  });

  it('should switch tabs and clear feedback messages', () => {
    const req = httpMock.expectOne('/api/v1/candidates/cand-001');
    req.flush({});

    component.errorMessage.set('Some error');
    component.successMessage.set('Some success');

    component.onTabChange('digilocker');
    expect(component.activeTab()).toBe('digilocker');
    expect(component.errorMessage()).toBeNull();
    expect(component.successMessage()).toBeNull();
  });

  it('should POST new profile when existsOnServer is false', () => {
    const initReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    initReq.error(new ProgressEvent('error'), { status: 404, statusText: 'Not Found' });

    component.profile.update((p) => ({ ...p, fullName: 'New Candidate' }));
    component.saveProfile();
    expect(component.saving()).toBe(true);

    const postReq = httpMock.expectOne('/api/v1/candidates');
    expect(postReq.request.method).toBe('POST');
    expect(postReq.request.body.fullName).toBe('New Candidate');
    postReq.flush({ success: true });

    expect(component.saving()).toBe(false);
    expect(component.existsOnServer()).toBe(true);
    expect(component.successMessage()).toBe('Profile successfully updated!');
  });

  it('should PUT existing profile when existsOnServer is true', () => {
    const initReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    initReq.flush({
      candidateId: 'cand-001',
      fullName: 'Aarav Kumar',
      address: 'Old Address',
    });

    component.profile.update((p) => ({ ...p, address: 'Updated Address, New Delhi' }));
    component.saveProfile();
    expect(component.saving()).toBe(true);

    const putReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    expect(putReq.request.method).toBe('PUT');
    expect(putReq.request.body.address).toBe('Updated Address, New Delhi');
    putReq.flush({ success: true });

    expect(component.saving()).toBe(false);
    expect(component.successMessage()).toBe('Profile successfully updated!');
  });

  it('should display error message if saving profile fails', () => {
    const initReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    initReq.flush({ candidateId: 'cand-001' });

    component.saveProfile();

    const putReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    putReq.flush({ message: 'Validation failed on server' }, { status: 400, statusText: 'Bad Request' });

    expect(component.saving()).toBe(false);
    expect(component.errorMessage()).toBe('Validation failed on server');
  });

  it('should handle adding new education entry and auto-save', () => {
    const initReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    initReq.flush({ candidateId: 'cand-001' });

    const newEdu: EducationEntry = {
      id: 'edu-101',
      qualification: 'B.Tech',
      boardOrUniversity: 'Delhi University',
      passingYear: 2022,
      percentageOrCgpa: 8.5,
      documentRef: 'asset-doc-123',
    };

    component.handleSaveEducation({ isNew: true, data: newEdu });

    expect(component.profile().education).toContainEqual(newEdu);

    const putReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    expect(putReq.request.method).toBe('PUT');
    putReq.flush({ success: true });
  });

  it('should handle updating existing education entry and auto-save', () => {
    const existingEdu: EducationEntry = {
      id: 'edu-101',
      qualification: 'B.Tech',
      boardOrUniversity: 'Delhi University',
      passingYear: 2022,
      percentageOrCgpa: 8.5,
    };

    const initReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    initReq.flush({ candidateId: 'cand-001', education: [existingEdu] });

    const updatedEdu: EducationEntry = {
      ...existingEdu,
      percentageOrCgpa: 9.0,
    };

    component.handleSaveEducation({ isNew: false, data: updatedEdu });

    expect(component.profile().education[0].percentageOrCgpa).toBe(9.0);

    const putReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    putReq.flush({ success: true });
  });

  it('should handle deleting education entry and auto-save', () => {
    const existingEdu: EducationEntry = {
      id: 'edu-101',
      qualification: 'B.Tech',
      boardOrUniversity: 'Delhi University',
      passingYear: 2022,
    };

    const initReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    initReq.flush({ candidateId: 'cand-001', education: [existingEdu] });

    component.handleDeleteEducation('edu-101');

    expect(component.profile().education.length).toBe(0);

    const putReq = httpMock.expectOne('/api/v1/candidates/cand-001');
    putReq.flush({ success: true });
  });
});
