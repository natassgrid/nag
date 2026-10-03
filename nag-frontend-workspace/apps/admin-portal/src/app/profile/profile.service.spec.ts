import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProfileService } from './profile.service';
import { AuthService, UserRoleService } from '@nag-frontend-workspace/shared-data-access-auth';
import { of } from 'rxjs';

describe('ProfileService', () => {
  let service: ProfileService;
  let httpMock: HttpTestingController;

  const mockAuthService = {
    currentUser: jest.fn().mockReturnValue({
      userId: 'usr-123',
      username: 'admin@nag.gov.in',
      roles: ['SUPER_ADMIN'],
    }),
    getTenantId: jest.fn().mockReturnValue('default'),
    changePassword: jest.fn().mockReturnValue(of(undefined)),
    setupTotp: jest.fn().mockReturnValue(
      of({
        secret: 'TESTSECRETKEY123',
        otpauthUri: 'otpauth://totp/NAG:admin?secret=TESTSECRETKEY123',
        backupCodes: ['1111-2222', '3333-4444'],
      })
    ),
    verifyTotpSetup: jest.fn().mockReturnValue(of(undefined)),
    disableTotp: jest.fn().mockReturnValue(of(undefined)),
  };

  const mockUserRoleService = {
    getRoles: jest.fn().mockReturnValue(of([])),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ProfileService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: mockAuthService },
        { provide: UserRoleService, useValue: mockUserRoleService },
      ],
    });

    service = TestBed.inject(ProfileService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch user profile from API', (done) => {
    const mockResponse = {
      data: {
        id: 'usr-123',
        username: 'admin@nag.gov.in',
        fullName: 'Dr. Test Administrator',
        roles: ['SUPER_ADMIN', 'EXAM_CONTROLLER'],
        status: 'ACTIVE',
        specialization: 'Physics',
      },
    };

    service.getProfile().subscribe((profile) => {
      expect(profile).toBeTruthy();
      expect(profile.id).toBe('usr-123');
      expect(profile.fullName).toBe('Dr. Test Administrator');
      expect(profile.roles).toContain('SUPER_ADMIN');
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/users/me');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('should fallback to currentUser when getProfile API fails', (done) => {
    service.getProfile().subscribe((profile) => {
      expect(profile).toBeTruthy();
      expect(profile.username).toBe('admin@nag.gov.in');
      expect(profile.roles).toContain('SUPER_ADMIN');
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/users/me');
    req.error(new ProgressEvent('Network error'));
  });

  it('should update user profile', (done) => {
    const updatePayload = {
      fullName: 'Updated Name',
      phoneNumber: '+91 99999 88888',
      specialization: 'Computer Science',
    };

    service.updateProfile(updatePayload).subscribe((res) => {
      expect(res.fullName).toBe('Updated Name');
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/users/me');
    expect(req.request.method).toBe('PUT');
    req.flush({
      data: {
        id: 'usr-123',
        fullName: 'Updated Name',
        username: 'admin@nag.gov.in',
        roles: ['SUPER_ADMIN'],
      },
    });
  });

  it('should delegate changePassword to authService', (done) => {
    service
      .changePassword({ currentPassword: 'OldPassword123!', newPassword: 'NewPassword123!' })
      .subscribe(() => {
        expect(mockAuthService.changePassword).toHaveBeenCalledWith({
          currentPassword: 'OldPassword123!',
          newPassword: 'NewPassword123!',
        });
        done();
      });
  });

  it('should delegate setupTotp to authService', (done) => {
    service.setupTotp().subscribe((res) => {
      expect(res.secret).toBe('TESTSECRETKEY123');
      expect(res.backupCodes?.length).toBe(2);
      done();
    });
  });

  it('should delegate verifyTotp to authService', (done) => {
    service.verifyTotp({ secret: 'TESTSECRET', code: '123456' }).subscribe(() => {
      expect(mockAuthService.verifyTotpSetup).toHaveBeenCalledWith({
        userId: 'usr-123',
        secret: 'TESTSECRET',
        code: '123456',
        backupCodes: undefined,
      });
      done();
    });
  });

  it('should delegate disableTotp to authService', (done) => {
    service.disableTotp().subscribe(() => {
      expect(mockAuthService.disableTotp).toHaveBeenCalledWith('usr-123');
      done();
    });
  });

  it('should fetch active sessions', (done) => {
    service.getActiveSessions().subscribe((sessions) => {
      expect(sessions.length).toBeGreaterThan(0);
      done();
    });

    const req = httpMock.expectOne('/api/v1/identity/users/sessions');
    req.flush({
      data: [
        {
          id: 'sess-1',
          ipAddress: '192.168.1.1',
          browser: 'Chrome',
          os: 'Windows',
          current: true,
        },
      ],
    });
  });

  it('should return system permissions mapped to assigned roles', () => {
    const perms = service.getSystemPermissions(['SUPER_ADMIN']);
    expect(perms.length).toBeGreaterThan(5);
    const superPerm = perms.find((p) => p.code === 'ROLE_MANAGE');
    expect(superPerm?.granted).toBe(true);
  });
});
