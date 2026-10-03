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
  PersonalAccessToken,
  CreateTokenPayload,
  CreatedTokenResult,
  AdminActivityLog,
  PageResponse,
} from './profile.model';
import {
  ProfileHeaderComponent,
  PersonalInfoCardComponent,
  RolesPermissionsCardComponent,
  SecurityMfaCardComponent,
  ActiveSessionsCardComponent,
  PersonalAccessTokensCardComponent,
  ActivityTimelineCardComponent,
} from './components';

export type ProfileTab = 'personal' | 'roles' | 'security' | 'sessions' | 'tokens' | 'activity';

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
    PersonalAccessTokensCardComponent,
    ActivityTimelineCardComponent,
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
  readonly tokens = signal<PersonalAccessToken[]>([]);
  readonly activityPage = signal<PageResponse<AdminActivityLog> | null>(null);
  readonly totpSetupData = signal<TotpSetupResult | null>(null);
  readonly createdTokenSecret = signal<CreatedTokenResult | null>(null);
  readonly activeTab = signal<ProfileTab>('personal');

  // Filter & Page State
  readonly activityCategory = signal<string>('ALL');
  readonly activityPageNumber = signal<number>(0);

  // Loading & Action State Signals
  readonly loading = signal<boolean>(false);
  readonly savingProfile = signal<boolean>(false);
  readonly changingPassword = signal<boolean>(false);
  readonly processingTotp = signal<boolean>(false);
  readonly revokingSession = signal<boolean>(false);
  readonly tokenCreating = signal<boolean>(false);
  readonly loadingActivity = signal<boolean>(false);

  readonly tabs: { key: ProfileTab; label: string; icon: string }[] = [
    { key: 'personal', label: 'Personal & Preferences', icon: 'person' },
    { key: 'roles', label: 'Roles & Permissions', icon: 'admin_panel_settings' },
    { key: 'security', label: 'Security & 2FA', icon: 'security' },
    { key: 'sessions', label: 'Active Sessions', icon: 'devices' },
    { key: 'tokens', label: 'API Access Tokens', icon: 'vpn_key' },
    { key: 'activity', label: 'Audit Timeline', icon: 'history' },
  ];

  ngOnInit(): void {
    this.loadProfile();
    this.loadSessions();
    this.loadTokens();
    this.loadActivity();
  }

  onRefreshData(): void {
    this.loadProfile();
    this.loadSessions();
    this.loadTokens();
    this.loadActivity();
    this.notificationService.info('Profile, tokens, and audit data refreshed');
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

  loadTokens(): void {
    this.profileService.getTokens().subscribe({
      next: (data) => this.tokens.set(data || []),
      error: () => this.tokens.set([]),
    });
  }

  loadActivity(): void {
    this.loadingActivity.set(true);
    this.profileService
      .getActivityLogs(this.activityPageNumber(), 10, this.activityCategory())
      .subscribe({
        next: (page) => {
          this.activityPage.set(page);
          this.loadingActivity.set(false);
        },
        error: () => {
          this.loadingActivity.set(false);
        },
      });
  }

  onUpdateProfile(payload: UpdateProfilePayload): void {
    this.savingProfile.set(true);
    this.profileService.updateProfile(payload).subscribe({
      next: (updated) => {
        this.profile.set(updated);
        this.notificationService.success('Profile details and regional preferences updated successfully');
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

  // Personal Access Tokens Handlers
  onCreateToken(payload: CreateTokenPayload): void {
    this.tokenCreating.set(true);
    this.profileService.createToken(payload).subscribe({
      next: (result) => {
        this.createdTokenSecret.set(result);
        this.tokens.update((toks) => [result.token, ...toks]);
        this.notificationService.success('Access token generated successfully');
        this.tokenCreating.set(false);
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to create access token');
        this.tokenCreating.set(false);
      },
    });
  }

  onRevokeToken(tokenId: string): void {
    this.profileService.revokeToken(tokenId).subscribe({
      next: () => {
        this.tokens.update((toks) =>
          toks.map((t) => (t.id === tokenId ? { ...t, revoked: true } : t))
        );
        this.notificationService.success('Token revoked successfully');
      },
      error: (err) => {
        this.notificationService.error(err?.message || 'Failed to revoke token');
      },
    });
  }

  // Activity Timeline Handlers
  onFilterActivity(category: string): void {
    this.activityCategory.set(category);
    this.activityPageNumber.set(0);
    this.loadActivity();
  }

  onChangeActivityPage(page: number): void {
    this.activityPageNumber.set(page);
    this.loadActivity();
  }

  onExportActivity(format: 'csv' | 'json'): void {
    this.profileService.exportActivityLogs(format).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `admin-activity-export.${format}`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.notificationService.success(`Activity log ${format.toUpperCase()} export downloaded`);
      },
      error: () => {
        this.notificationService.error('Failed to download activity log export');
      },
    });
  }
}
