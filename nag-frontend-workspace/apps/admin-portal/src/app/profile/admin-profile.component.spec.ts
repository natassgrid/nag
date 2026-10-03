import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { AdminProfileComponent } from './admin-profile.component';
import { ProfileService } from './profile.service';
import { NotificationService } from '@nag-frontend-workspace/shared-ui-components';
import {
  AdminUserProfile,
  ActiveSessionInfo,
  PermissionItem,
  PersonalAccessToken,
  CreatedTokenResult,
  AdminActivityLog,
  PageResponse,
} from './profile.model';

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

  const mockTokens: PersonalAccessToken[] = [
    {
      id: 'tok-123',
      name: 'CI Token',
      tokenPrefix: 'nag_pat_1234',
      scopes: ['READ', 'WRITE'],
      revoked: false,
      createdAt: '2026-10-01T00:00:00Z',
    },
  ];

  const mockActivityPage: PageResponse<AdminActivityLog> = {
    content: [
      {
        id: 'act-1',
        timestamp: '2026-10-01T12:00:00Z',
        action: 'USER_LOGIN',
        category: 'AUTHENTICATION',
        details: 'Admin login successful',
        ipAddress: '127.0.0.1',
        status: 'SUCCESS',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    size: 10,
    number: 0,
  };

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
    getTokens: jest.Mock;
    createToken: jest.Mock;
    revokeToken: jest.Mock;
    getActivityLogs: jest.Mock;
    exportActivityLogs: jest.Mock;
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
      getTokens: jest.fn().mockReturnValue(of(mockTokens)),
      createToken: jest.fn().mockReturnValue(
        of({
          token: {
            id: 'tok-new',
            name: 'New Token',
            tokenPrefix: 'nag_pat_neww',
            scopes: ['READ'],
            revoked: false,
            createdAt: '2026-10-02T00:00:00Z',
          },
          rawSecret: 'nag_pat_newwsecret1234567890',
        } as CreatedTokenResult)
      ),
      revokeToken: jest.fn().mockReturnValue(of(undefined)),
      getActivityLogs: jest.fn().mockReturnValue(of(mockActivityPage)),
      exportActivityLogs: jest.fn().mockReturnValue(of(new Blob(['test'], { type: 'text/csv' }))),
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

  it('should initialize and load profile, permissions, tokens, activity and active sessions', () => {
    expect(mockProfileService.getProfile).toHaveBeenCalled();
    expect(mockProfileService.getActiveSessions).toHaveBeenCalled();
    expect(mockProfileService.getTokens).toHaveBeenCalled();
    expect(mockProfileService.getActivityLogs).toHaveBeenCalled();
    expect(component.profile()).toEqual(mockProfile);
    expect(component.permissions().length).toBe(2);
    expect(component.sessions().length).toBe(2);
    expect(component.tokens().length).toBe(1);
    expect(component.activeTab()).toBe('personal');
  });

  it('should handle tab navigation', () => {
    component.activeTab.set('security');
    expect(component.activeTab()).toBe('security');

    component.activeTab.set('roles');
    expect(component.activeTab()).toBe('roles');

    component.activeTab.set('sessions');
    expect(component.activeTab()).toBe('sessions');

    component.activeTab.set('tokens');
    expect(component.activeTab()).toBe('tokens');

    component.activeTab.set('activity');
    expect(component.activeTab()).toBe('activity');
  });

  it('should update personal profile information', () => {
    const updatePayload = {
      fullName: 'Dr. New Name',
      phoneNumber: '+91 99999 00000',
      specialization: 'Applied Statistics',
      department: 'Testing Council',
    };

    component.onUpdateProfile(updatePayload);

    expect(mockProfileService.updateProfile).toHaveBeenCalledWith(updatePayload);
    expect(mockNotificationService.success).toHaveBeenCalledWith(
      'Profile details and regional preferences updated successfully'
    );
    expect(component.profile()?.fullName).toBe('Dr. New Name');
    expect(component.savingProfile()).toBe(false);
  });

  it('should handle update profile error', () => {
    mockProfileService.updateProfile.mockReturnValueOnce(
      throwError(() => new Error('Server error'))
    );

    component.onUpdateProfile({ fullName: 'Fail' });

    expect(mockNotificationService.error).toHaveBeenCalled();
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

  it('should create and revoke PAT tokens', () => {
    component.onCreateToken({ name: 'New Token', scopes: ['READ'] });
    expect(mockProfileService.createToken).toHaveBeenCalled();
    expect(component.createdTokenSecret()?.rawSecret).toBe('nag_pat_newwsecret1234567890');
    expect(component.tokens().length).toBe(2);

    component.onRevokeToken('tok-123');
    expect(mockProfileService.revokeToken).toHaveBeenCalledWith('tok-123');
  });

  it('should filter activity and change page', () => {
    component.onFilterActivity('AUTHENTICATION');
    expect(component.activityCategory()).toBe('AUTHENTICATION');
    expect(mockProfileService.getActivityLogs).toHaveBeenCalled();

    component.onChangeActivityPage(1);
    expect(component.activityPageNumber()).toBe(1);
  });

  it('should refresh profile and sessions data when onRefreshData is called', () => {
    mockProfileService.getProfile.mockClear();
    mockProfileService.getActiveSessions.mockClear();

    component.onRefreshData();

    expect(mockProfileService.getProfile).toHaveBeenCalled();
    expect(mockProfileService.getActiveSessions).toHaveBeenCalled();
    expect(mockNotificationService.info).toHaveBeenCalledWith(
      'Profile, tokens, and audit data refreshed'
    );
  });
});
