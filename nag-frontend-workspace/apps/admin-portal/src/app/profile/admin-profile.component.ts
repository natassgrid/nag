import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  NotificationService,
} from '@nag-frontend-workspace/shared-ui-components';
import { ProfileService } from './profile.service';
import {
  AdminUserProfile,
  UpdateProfilePayload,
  ChangePasswordPayload,
  TotpSetupResult,
  ActiveSessionInfo,
  PermissionItem,
} from './profile.model';
import {
  ProfileHeaderComponent,
  PersonalInfoCardComponent,
  RolesPermissionsCardComponent,
  SecurityMfaCardComponent,
  ActiveSessionsCardComponent,
} from './components';

export type ProfileTab = 'personal' | 'roles' | 'security' | 'sessions';

@Component({
  selector: 'app-admin-profile',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    ProfileHeaderComponent,
    PersonalInfoCardComponent,
    RolesPermissionsCardComponent,
    SecurityMfaCardComponent,
    ActiveSessionsCardComponent,
  ],
  templateUrl: './admin-profile.component.html',
  styleUrl: './admin-profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminProfileComponent implements OnInit {
  private readonly profileService = inject(ProfileService);
  private readonly notificationService = inject(NotificationService);

  // Main State Signals
  readonly profile = signal<AdminUserProfile | null>(null);
  readonly permissions = signal<PermissionItem[]>([]);
  readonly sessions = signal<ActiveSessionInfo[]>([]);
  readonly totpSetupData = signal<TotpSetupResult | null>(null);
  readonly activeTab = signal<ProfileTab>('personal');

  // Loading & Action State Signals
  readonly loading = signal<boolean>(false);
  readonly savingProfile = signal<boolean>(false);
  readonly changingPassword = signal<boolean>(false);
  readonly processingTotp = signal<boolean>(false);
  readonly revokingSession = signal<boolean>(false);

  readonly tabs: { key: ProfileTab; label: string; icon: string }[] = [
    { key: 'personal', label: 'Personal Details', icon: 'person' },
    { key: 'roles', label: 'Roles & Permissions', icon: 'admin_panel_settings' },
    { key: 'security', label: 'Security & 2FA', icon: 'security' },
    { key: 'sessions', label: 'Active Sessions & Audit', icon: 'devices' },
  ];

  ngOnInit(): void {
    this.loadProfile();
    this.loadSessions();
  }

  loadProfile(): void {
    this.loading.set(true);
    this.profileService.getProfile().subscribe({
      next: (data) => {
        this.profile.set(data);
        const perms = this.profileService.getSystemPermissions(data.roles || []);
        this.permissions.set(perms);
        this.loading.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to load user profile');
        this.loading.set(false);
      },
    });
  }

  loadSessions(): void {
    this.profileService.getActiveSessions().subscribe({
      next: (data) => this.sessions.set(data || []),
      error: () => this.sessions.set([]),
    });
  }

  onUpdateProfile(payload: UpdateProfilePayload): void {
    this.savingProfile.set(true);
    this.profileService.updateProfile(payload).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.notificationService.success('Profile details updated successfully');
        this.savingProfile.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to update profile');
        this.savingProfile.set(false);
      },
    });
  }

  onChangePassword(payload: ChangePasswordPayload): void {
    this.changingPassword.set(true);
    this.profileService.changePassword(payload).subscribe({
      next: () => {
        this.notificationService.success('Password changed successfully');
        this.changingPassword.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to change password. Please check your current password.');
        this.changingPassword.set(false);
      },
    });
  }

  onInitTotpSetup(): void {
    this.processingTotp.set(true);
    this.profileService.setupTotp().subscribe({
      next: (data) => {
        this.totpSetupData.set(data);
        this.processingTotp.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to initiate 2FA setup');
        this.processingTotp.set(false);
      },
    });
  }

  onVerifyTotp(payload: { secret: string; code: string; backupCodes?: string[] }): void {
    this.processingTotp.set(true);
    this.profileService.verifyTotp(payload).subscribe({
      next: () => {
        this.notificationService.success('Two-Factor Authentication (2FA) successfully activated!');
        this.totpSetupData.set(null);
        if (this.profile()) {
          this.profile.update((p) => (p ? { ...p, twoFactorEnabled: true, twoFactorMethod: 'TOTP' } : null));
        }
        this.processingTotp.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Invalid verification code. Please check your authenticator app.');
        this.processingTotp.set(false);
      },
    });
  }

  onDisableTotp(): void {
    this.processingTotp.set(true);
    this.profileService.disableTotp().subscribe({
      next: () => {
        this.notificationService.info('Two-Factor Authentication (2FA) has been disabled');
        if (this.profile()) {
          this.profile.update((p) => (p ? { ...p, twoFactorEnabled: false } : null));
        }
        this.processingTotp.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to disable 2FA');
        this.processingTotp.set(false);
      },
    });
  }

  onRevokeSession(sessionId: string): void {
    this.revokingSession.set(true);
    this.profileService.revokeSession(sessionId).subscribe({
      next: () => {
        this.sessions.update((list) => list.filter((s) => s.id !== sessionId));
        this.notificationService.success('Session terminated successfully');
        this.revokingSession.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to terminate session');
        this.revokingSession.set(false);
      },
    });
  }

  onRevokeOtherSessions(): void {
    this.revokingSession.set(true);
    this.profileService.revokeOtherSessions().subscribe({
      next: () => {
        this.sessions.update((list) => list.filter((s) => s.isCurrent));
        this.notificationService.success('All other active sessions have been invalidated');
        this.revokingSession.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to invalidate other sessions');
        this.revokingSession.set(false);
      },
    });
  }
}
