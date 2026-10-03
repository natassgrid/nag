import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { AdminProfileComponent } from './admin-profile.component';
import { ProfileService } from './profile.service';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import { AdminUserProfile, ActiveSessionInfo, PermissionItem } from './profile.model';

describe('AdminProfileComponent', () => {
  let component: AdminProfileComponent;
  let fixture: ComponentFixture<AdminProfileComponent>;

  const mockProfile: AdminUserProfile = {
    id: 'usr-admin-001',
    username: 'admin@nag.gov.in',
    email: 'admin@nag.gov.in',
    fullName: 'Dr. Ramesh Chandra',
    phoneNumber: '+91 98765 43210',
    specialization: 'Physics & Assessment Analytics',
    department: 'National Examination Board',
    employeeId: 'NAG-ADM-9942',
    roles: ['SUPER_ADMIN', 'EXAM_CONTROLLER'],
    status: 'ACTIVE',
    twoFactorEnabled: false,
    twoFactorMethod: 'TOTP',
    lastLoginAt: '2026-10-01T10:00:00Z',
    createdAt: '2025-01-15T09:30:00Z',
    tenantId: 'default',
  };

  const mockSessions: ActiveSessionInfo[] = [
    {
      id: 'sess-current',
      ipAddress: '192.168.1.100',
      device: 'Admin PC',
      browser: 'Chrome 129',
      os: 'Windows 11',
      lastActive: 'Just now',
      isCurrent: true,
    },
    {
      id: 'sess-mobile',
      ipAddress: '103.20.10.5',
      device: 'Tablet Device',
      browser: 'Safari Mobile',
      os: 'iOS 17',
      lastActive: '2 hours ago',
      isCurrent: false,
    },
  ];

  const mockPermissions: PermissionItem[] = [
    {
      code: 'QUESTION_VIEW',
      name: 'View Questions',
      category: 'QUESTIONS',
      description: 'View questions',
      granted: true,
    },
    {
      code: 'ROLE_MANAGE',
      name: 'Manage Roles',
      category: 'IDENTITY',
      description: 'Manage roles',
      granted: true,
    },
  ];

  let mockProfileService: {
    getProfile: jest.Mock;
    updateProfile: jest.Mock;
    changePassword: jest.Mock;
    setupTotp: jest.Mock;
    verifyTotp: jest.Mock;
    disableTotp: jest.Mock;
    getActiveSessions: jest.Mock;
    revokeSession: jest.Mock;
    revokeOtherSessions: jest.Mock;
    getSystemPermissions: jest.Mock;
    getSystemRoleDetails: jest.Mock;
  };

  let mockNotificationService: {
    success: jest.Mock;
    error: jest.Mock;
    warning: jest.Mock;
    info: jest.Mock;
    confirm: jest.Mock;
  };

  beforeEach(async () => {
    mockProfileService = {
      getProfile: jest.fn().mockReturnValue(of(mockProfile)),
      updateProfile: jest.fn().mockImplementation((payload) =>
        of({ ...mockProfile, ...payload })
      ),
      changePassword: jest.fn().mockReturnValue(of(undefined)),
      setupTotp: jest.fn().mockReturnValue(
        of({
          secret: 'SECRET123',
          secretKey: 'SECRET123',
          otpauthUri: 'otpauth://totp/NAG:admin?secret=SECRET123',
          backupCodes: ['1111-2222', '3333-4444'],
          issuer: 'NAG',
          username: 'admin@nag.gov.in',
        })
      ),
      verifyTotp: jest.fn().mockReturnValue(of(undefined)),
      disableTotp: jest.fn().mockReturnValue(of(undefined)),
      getActiveSessions: jest.fn().mockReturnValue(of(mockSessions)),
      revokeSession: jest.fn().mockReturnValue(of(undefined)),
      revokeOtherSessions: jest.fn().mockReturnValue(of(undefined)),
      getSystemPermissions: jest.fn().mockReturnValue(mockPermissions),
      getSystemRoleDetails: jest.fn().mockReturnValue({
        code: 'SUPER_ADMIN',
        name: 'Super Administrator',
        badgeTone: 'purple',
        description: 'Full system sovereignty',
        systemRole: true,
      }),
    };

    mockNotificationService = {
      success: jest.fn(),
      error: jest.fn(),
      warning: jest.fn(),
      info: jest.fn(),
      confirm: jest.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [AdminProfileComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ProfileService, useValue: mockProfileService },
        { provide: NotificationService, useValue: mockNotificationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminProfileComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize and load profile, permissions, and active sessions', () => {
    expect(mockProfileService.getProfile).toHaveBeenCalled();
    expect(mockProfileService.getActiveSessions).toHaveBeenCalled();
    expect(component.profile()).toEqual(mockProfile);
    expect(component.permissions().length).toBe(2);
    expect(component.sessions().length).toBe(2);
    expect(component.activeTab()).toBe('personal');
  });

  it('should handle tab navigation', () => {
    component.activeTab.set('security');
    expect(component.activeTab()).toBe('security');

    component.activeTab.set('roles');
    expect(component.activeTab()).toBe('roles');

    component.activeTab.set('sessions');
    expect(component.activeTab()).toBe('sessions');
  });

  it('should update user profile successfully', () => {
    component.onUpdateProfile({
      fullName: 'Dr. Ramesh Kumar Chandra',
      phoneNumber: '+91 99999 00000',
    });

    expect(mockProfileService.updateProfile).toHaveBeenCalledWith({
      fullName: 'Dr. Ramesh Kumar Chandra',
      phoneNumber: '+91 99999 00000',
    });
    expect(mockNotificationService.success).toHaveBeenCalledWith(
      'Profile details updated successfully'
    );
    expect(component.profile()?.fullName).toBe('Dr. Ramesh Kumar Chandra');
    expect(component.savingProfile()).toBe(false);
  });

  it('should handle profile update error', () => {
    mockProfileService.updateProfile.mockReturnValueOnce(
      throwError(() => new Error('Validation failed'))
    );

    component.onUpdateProfile({ fullName: 'Error Name' });
    expect(mockNotificationService.error).toHaveBeenCalledWith('Validation failed');
    expect(component.savingProfile()).toBe(false);
  });

  it('should change password successfully', () => {
    component.onChangePassword({
      currentPassword: 'CurrentPassword123!',
      newPassword: 'NewSecurePassword123!',
    });

    expect(mockProfileService.changePassword).toHaveBeenCalledWith({
      currentPassword: 'CurrentPassword123!',
      newPassword: 'NewSecurePassword123!',
    });
    expect(mockNotificationService.success).toHaveBeenCalledWith(
      'Password changed successfully'
    );
    expect(component.changingPassword()).toBe(false);
  });

  it('should handle password change error', () => {
    mockProfileService.changePassword.mockReturnValueOnce(
      throwError(() => new Error('Invalid current password'))
    );

    component.onChangePassword({
      currentPassword: 'WrongPassword',
      newPassword: 'NewPassword123!',
    });

    expect(mockNotificationService.error).toHaveBeenCalled();
    expect(component.changingPassword()).toBe(false);
  });

  it('should initiate TOTP 2FA setup', () => {
    component.onInitTotpSetup();

    expect(mockProfileService.setupTotp).toHaveBeenCalled();
    expect(component.totpSetupData()?.secret).toBe('SECRET123');
    expect(component.processingTotp()).toBe(false);
  });

  it('should verify and activate TOTP 2FA', () => {
    component.onVerifyTotp({
      secret: 'SECRET123',
      code: '123456',
      backupCodes: ['1111-2222'],
    });

    expect(mockProfileService.verifyTotp).toHaveBeenCalledWith({
      secret: 'SECRET123',
      code: '123456',
      backupCodes: ['1111-2222'],
    });
    expect(mockNotificationService.success).toHaveBeenCalledWith(
      'Two-Factor Authentication (2FA) successfully activated!'
    );
    expect(component.profile()?.twoFactorEnabled).toBe(true);
    expect(component.processingTotp()).toBe(false);
  });

  it('should disable TOTP 2FA', () => {
    component.onDisableTotp();

    expect(mockProfileService.disableTotp).toHaveBeenCalled();
    expect(mockNotificationService.info).toHaveBeenCalledWith(
      'Two-Factor Authentication (2FA) has been disabled'
    );
    expect(component.profile()?.twoFactorEnabled).toBe(false);
    expect(component.processingTotp()).toBe(false);
  });

  it('should revoke a single active session', () => {
    component.onRevokeSession('sess-mobile');

    expect(mockProfileService.revokeSession).toHaveBeenCalledWith('sess-mobile');
    expect(mockNotificationService.success).toHaveBeenCalledWith(
      'Session terminated successfully'
    );
    expect(component.sessions().length).toBe(1);
    expect(component.sessions()[0].id).toBe('sess-current');
  });

  it('should revoke all other active sessions', () => {
    component.onRevokeOtherSessions();

    expect(mockProfileService.revokeOtherSessions).toHaveBeenCalled();
    expect(mockNotificationService.success).toHaveBeenCalledWith(
      'All other active sessions have been invalidated'
    );
    expect(component.sessions().length).toBe(1);
    expect(component.sessions()[0].isCurrent).toBe(true);
  });
});
