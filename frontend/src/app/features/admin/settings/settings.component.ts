/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import { Component, signal, computed, OnInit, ChangeDetectionStrategy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { NotificationService } from '../../../core/services/notification.service';
import { AdminService } from '../services/admin.service';

interface SystemSettingsState {
  // Security & Auth
  authMfaEnforced: boolean;
  authSessionTimeoutMinutes: number;
  authMaxLoginAttempts: number;
  authLockoutDurationMinutes: number;
  authPasswordExpiryDays: number;
  authPasswordMinLength: number;

  // Delivery & Proctoring
  deliveryTamperDetectionEnabled: boolean;
  deliveryKioskModeEnforced: boolean;
  deliveryTelemetryHeartbeatSeconds: number;
  deliveryAutosaveIntervalSeconds: number;
  deliveryMaxDisconnectGraceSeconds: number;
  deliveryRetestAuthorizationRequired: boolean;

  // Candidate Practice & Learning
  practiceModeEnabled: boolean;
  practiceSolutionsVisible: boolean;

  // Assessment & Question Bank
  questionDualReviewRequired: boolean;
  questionAiGenerationEnabled: boolean;
  evaluationAutoGradeInstant: boolean;
  evaluationAnonymizeCandidateSheets: boolean;

  // Alerts & Ops
  alertFailedLoginSpikesEnabled: boolean;
  alertExamWindowStartEnabled: boolean;
  alertEmailRecipients: string;
  alertCriticalErrorWebhook: string;

  // DPI & Infrastructure
  dpiDigilockerVerificationEnabled: boolean;
  dpiFaceVerificationThreshold: number;
  platformMaintenanceMode: boolean;
  platformBannerMessage: string;
}

const DEFAULT_SETTINGS_STATE: SystemSettingsState = {
  authMfaEnforced: false,
  authSessionTimeoutMinutes: 30,
  authMaxLoginAttempts: 5,
  authLockoutDurationMinutes: 15,
  authPasswordExpiryDays: 90,
  authPasswordMinLength: 12,

  deliveryTamperDetectionEnabled: true,
  deliveryKioskModeEnforced: true,
  deliveryTelemetryHeartbeatSeconds: 10,
  deliveryAutosaveIntervalSeconds: 15,
  deliveryMaxDisconnectGraceSeconds: 180,
  deliveryRetestAuthorizationRequired: true,

  practiceModeEnabled: true,
  practiceSolutionsVisible: true,

  questionDualReviewRequired: true,
  questionAiGenerationEnabled: true,
  evaluationAutoGradeInstant: true,
  evaluationAnonymizeCandidateSheets: true,

  alertFailedLoginSpikesEnabled: true,
  alertExamWindowStartEnabled: true,
  alertEmailRecipients: 'sec-ops@nag.gov.in, admin@nag.gov.in',
  alertCriticalErrorWebhook: '',

  dpiDigilockerVerificationEnabled: true,
  dpiFaceVerificationThreshold: 85,
  platformMaintenanceMode: false,
  platformBannerMessage: ''
};

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    PageHeaderComponent
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './settings.component.html',
  styleUrls: ['./settings.component.scss']
})
export class SettingsComponent implements OnInit {
  private readonly adminService = inject(AdminService);
  private readonly notificationService = inject(NotificationService);

  readonly isLoading = signal<boolean>(true);
  readonly isSaving = signal<boolean>(false);
  readonly isResetting = signal<boolean>(false);

  // Editable Form State Signals
  readonly authMfaEnforced = signal<boolean>(DEFAULT_SETTINGS_STATE.authMfaEnforced);
  readonly authSessionTimeoutMinutes = signal<number>(DEFAULT_SETTINGS_STATE.authSessionTimeoutMinutes);
  readonly authMaxLoginAttempts = signal<number>(DEFAULT_SETTINGS_STATE.authMaxLoginAttempts);
  readonly authLockoutDurationMinutes = signal<number>(DEFAULT_SETTINGS_STATE.authLockoutDurationMinutes);
  readonly authPasswordExpiryDays = signal<number>(DEFAULT_SETTINGS_STATE.authPasswordExpiryDays);
  readonly authPasswordMinLength = signal<number>(DEFAULT_SETTINGS_STATE.authPasswordMinLength);

  readonly deliveryTamperDetectionEnabled = signal<boolean>(DEFAULT_SETTINGS_STATE.deliveryTamperDetectionEnabled);
  readonly deliveryKioskModeEnforced = signal<boolean>(DEFAULT_SETTINGS_STATE.deliveryKioskModeEnforced);
  readonly deliveryTelemetryHeartbeatSeconds = signal<number>(DEFAULT_SETTINGS_STATE.deliveryTelemetryHeartbeatSeconds);
  readonly deliveryAutosaveIntervalSeconds = signal<number>(DEFAULT_SETTINGS_STATE.deliveryAutosaveIntervalSeconds);
  readonly deliveryMaxDisconnectGraceSeconds = signal<number>(DEFAULT_SETTINGS_STATE.deliveryMaxDisconnectGraceSeconds);
  readonly deliveryRetestAuthorizationRequired = signal<boolean>(DEFAULT_SETTINGS_STATE.deliveryRetestAuthorizationRequired);

  readonly practiceModeEnabled = signal<boolean>(DEFAULT_SETTINGS_STATE.practiceModeEnabled);
  readonly practiceSolutionsVisible = signal<boolean>(DEFAULT_SETTINGS_STATE.practiceSolutionsVisible);

  readonly questionDualReviewRequired = signal<boolean>(DEFAULT_SETTINGS_STATE.questionDualReviewRequired);
  readonly questionAiGenerationEnabled = signal<boolean>(DEFAULT_SETTINGS_STATE.questionAiGenerationEnabled);
  readonly evaluationAutoGradeInstant = signal<boolean>(DEFAULT_SETTINGS_STATE.evaluationAutoGradeInstant);
  readonly evaluationAnonymizeCandidateSheets = signal<boolean>(DEFAULT_SETTINGS_STATE.evaluationAnonymizeCandidateSheets);

  readonly alertFailedLoginSpikesEnabled = signal<boolean>(DEFAULT_SETTINGS_STATE.alertFailedLoginSpikesEnabled);
  readonly alertExamWindowStartEnabled = signal<boolean>(DEFAULT_SETTINGS_STATE.alertExamWindowStartEnabled);
  readonly alertEmailRecipients = signal<string>(DEFAULT_SETTINGS_STATE.alertEmailRecipients);
  readonly alertCriticalErrorWebhook = signal<string>(DEFAULT_SETTINGS_STATE.alertCriticalErrorWebhook);

  readonly dpiDigilockerVerificationEnabled = signal<boolean>(DEFAULT_SETTINGS_STATE.dpiDigilockerVerificationEnabled);
  readonly dpiFaceVerificationThreshold = signal<number>(DEFAULT_SETTINGS_STATE.dpiFaceVerificationThreshold);
  readonly platformMaintenanceMode = signal<boolean>(DEFAULT_SETTINGS_STATE.platformMaintenanceMode);
  readonly platformBannerMessage = signal<string>(DEFAULT_SETTINGS_STATE.platformBannerMessage);

  // Baseline state to track dirty modifications
  private readonly baselineState = signal<SystemSettingsState>({ ...DEFAULT_SETTINGS_STATE });

  readonly isDirty = computed(() => {
    const base = this.baselineState();
    return (
      this.authMfaEnforced() !== base.authMfaEnforced ||
      this.authSessionTimeoutMinutes() !== base.authSessionTimeoutMinutes ||
      this.authMaxLoginAttempts() !== base.authMaxLoginAttempts ||
      this.authLockoutDurationMinutes() !== base.authLockoutDurationMinutes ||
      this.authPasswordExpiryDays() !== base.authPasswordExpiryDays ||
      this.authPasswordMinLength() !== base.authPasswordMinLength ||
      this.deliveryTamperDetectionEnabled() !== base.deliveryTamperDetectionEnabled ||
      this.deliveryKioskModeEnforced() !== base.deliveryKioskModeEnforced ||
      this.deliveryTelemetryHeartbeatSeconds() !== base.deliveryTelemetryHeartbeatSeconds ||
      this.deliveryAutosaveIntervalSeconds() !== base.deliveryAutosaveIntervalSeconds ||
      this.deliveryMaxDisconnectGraceSeconds() !== base.deliveryMaxDisconnectGraceSeconds ||
      this.deliveryRetestAuthorizationRequired() !== base.deliveryRetestAuthorizationRequired ||
      this.practiceModeEnabled() !== base.practiceModeEnabled ||
      this.practiceSolutionsVisible() !== base.practiceSolutionsVisible ||
      this.questionDualReviewRequired() !== base.questionDualReviewRequired ||
      this.questionAiGenerationEnabled() !== base.questionAiGenerationEnabled ||
      this.evaluationAutoGradeInstant() !== base.evaluationAutoGradeInstant ||
      this.evaluationAnonymizeCandidateSheets() !== base.evaluationAnonymizeCandidateSheets ||
      this.alertFailedLoginSpikesEnabled() !== base.alertFailedLoginSpikesEnabled ||
      this.alertExamWindowStartEnabled() !== base.alertExamWindowStartEnabled ||
      this.alertEmailRecipients() !== base.alertEmailRecipients ||
      this.alertCriticalErrorWebhook() !== base.alertCriticalErrorWebhook ||
      this.dpiDigilockerVerificationEnabled() !== base.dpiDigilockerVerificationEnabled ||
      this.dpiFaceVerificationThreshold() !== base.dpiFaceVerificationThreshold ||
      this.platformMaintenanceMode() !== base.platformMaintenanceMode ||
      this.platformBannerMessage() !== base.platformBannerMessage
    );
  });

  ngOnInit(): void {
    this.loadSettings();
  }

  loadSettings(): void {
    this.isLoading.set(true);
    this.adminService.getSystemConfigMap().subscribe({
      next: (configMap) => {
        this.applyConfigMapToState(configMap);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.warn('Could not load remote system configs, applying local defaults:', err);
        this.applyConfigMapToState({});
        this.isLoading.set(false);
      }
    });
  }

  saveSettings(): void {
    this.isSaving.set(true);
    const payload = this.exportStateToConfigMap();

    this.adminService.updateBulkSystemConfigs(payload).subscribe({
      next: (updatedMap) => {
        this.applyConfigMapToState(updatedMap);
        this.isSaving.set(false);
        this.notificationService.showSuccess('System settings updated and audited successfully');
      },
      error: (err) => {
        console.error('Failed to update system settings:', err);
        this.isSaving.set(false);
        this.notificationService.showError('Failed to save settings. Please check your connection or permissions.');
      }
    });
  }

  resetToDefaults(): void {
    if (confirm('Are you sure you want to restore all platform settings to standard system defaults?')) {
      this.isResetting.set(true);
      this.adminService.resetSystemConfigs().subscribe({
        next: (defaults) => {
          this.applyConfigMapToState(defaults);
          this.isResetting.set(false);
          this.notificationService.showInfo('Reset system settings to platform defaults');
        },
        error: (err) => {
          console.warn('Remote reset failed, resetting local signals:', err);
          this.applyConfigMapToState({});
          this.isResetting.set(false);
          this.notificationService.showInfo('Reset system settings to defaults (local)');
        }
      });
    }
  }

  private applyConfigMapToState(map: Record<string, string>): void {
    const parseBool = (val?: string, def = false) => (val !== undefined ? val.toLowerCase() === 'true' : def);
    const parseNum = (val?: string, def = 0) => {
      if (val === undefined) return def;
      const parsed = parseInt(val, 10);
      return isNaN(parsed) ? def : parsed;
    };
    const parseStr = (val?: string, def = '') => (val !== undefined ? val : def);

    const s: SystemSettingsState = {
      authMfaEnforced: parseBool(map['auth.mfa.enforced'], DEFAULT_SETTINGS_STATE.authMfaEnforced),
      authSessionTimeoutMinutes: parseNum(map['auth.session.timeout.minutes'], DEFAULT_SETTINGS_STATE.authSessionTimeoutMinutes),
      authMaxLoginAttempts: parseNum(map['auth.max.login.attempts'], DEFAULT_SETTINGS_STATE.authMaxLoginAttempts),
      authLockoutDurationMinutes: parseNum(map['auth.lockout.duration.minutes'], DEFAULT_SETTINGS_STATE.authLockoutDurationMinutes),
      authPasswordExpiryDays: parseNum(map['auth.password.expiry.days'], DEFAULT_SETTINGS_STATE.authPasswordExpiryDays),
      authPasswordMinLength: parseNum(map['auth.password.min.length'], DEFAULT_SETTINGS_STATE.authPasswordMinLength),

      deliveryTamperDetectionEnabled: parseBool(map['delivery.tamper.detection.enabled'], DEFAULT_SETTINGS_STATE.deliveryTamperDetectionEnabled),
      deliveryKioskModeEnforced: parseBool(map['delivery.kiosk.mode.enforced'], DEFAULT_SETTINGS_STATE.deliveryKioskModeEnforced),
      deliveryTelemetryHeartbeatSeconds: parseNum(map['delivery.telemetry.heartbeat.seconds'], DEFAULT_SETTINGS_STATE.deliveryTelemetryHeartbeatSeconds),
      deliveryAutosaveIntervalSeconds: parseNum(map['delivery.autosave.interval.seconds'], DEFAULT_SETTINGS_STATE.deliveryAutosaveIntervalSeconds),
      deliveryMaxDisconnectGraceSeconds: parseNum(map['delivery.max.disconnect.grace.seconds'], DEFAULT_SETTINGS_STATE.deliveryMaxDisconnectGraceSeconds),
      deliveryRetestAuthorizationRequired: parseBool(map['delivery.retest.authorization.required'], DEFAULT_SETTINGS_STATE.deliveryRetestAuthorizationRequired),

      practiceModeEnabled: parseBool(map['practice.mode.enabled'], DEFAULT_SETTINGS_STATE.practiceModeEnabled),
      practiceSolutionsVisible: parseBool(map['practice.solutions.visible'], DEFAULT_SETTINGS_STATE.practiceSolutionsVisible),

      questionDualReviewRequired: parseBool(map['question.dual.review.required'], DEFAULT_SETTINGS_STATE.questionDualReviewRequired),
      questionAiGenerationEnabled: parseBool(map['question.ai.generation.enabled'], DEFAULT_SETTINGS_STATE.questionAiGenerationEnabled),
      evaluationAutoGradeInstant: parseBool(map['evaluation.auto.grade.instant'], DEFAULT_SETTINGS_STATE.evaluationAutoGradeInstant),
      evaluationAnonymizeCandidateSheets: parseBool(map['evaluation.anonymize.candidate.sheets'], DEFAULT_SETTINGS_STATE.evaluationAnonymizeCandidateSheets),

      alertFailedLoginSpikesEnabled: parseBool(map['alert.failed.login.spikes.enabled'], DEFAULT_SETTINGS_STATE.alertFailedLoginSpikesEnabled),
      alertExamWindowStartEnabled: parseBool(map['alert.exam.window.start.enabled'], DEFAULT_SETTINGS_STATE.alertExamWindowStartEnabled),
      alertEmailRecipients: parseStr(map['alert.email.recipients'], DEFAULT_SETTINGS_STATE.alertEmailRecipients),
      alertCriticalErrorWebhook: parseStr(map['alert.critical.error.webhook'], DEFAULT_SETTINGS_STATE.alertCriticalErrorWebhook),

      dpiDigilockerVerificationEnabled: parseBool(map['dpi.digilocker.verification.enabled'], DEFAULT_SETTINGS_STATE.dpiDigilockerVerificationEnabled),
      dpiFaceVerificationThreshold: parseNum(map['dpi.face.verification.threshold'], DEFAULT_SETTINGS_STATE.dpiFaceVerificationThreshold),
      platformMaintenanceMode: parseBool(map['platform.maintenance.mode'], DEFAULT_SETTINGS_STATE.platformMaintenanceMode),
      platformBannerMessage: parseStr(map['platform.banner.message'], DEFAULT_SETTINGS_STATE.platformBannerMessage)
    };

    // Set form signals
    this.authMfaEnforced.set(s.authMfaEnforced);
    this.authSessionTimeoutMinutes.set(s.authSessionTimeoutMinutes);
    this.authMaxLoginAttempts.set(s.authMaxLoginAttempts);
    this.authLockoutDurationMinutes.set(s.authLockoutDurationMinutes);
    this.authPasswordExpiryDays.set(s.authPasswordExpiryDays);
    this.authPasswordMinLength.set(s.authPasswordMinLength);

    this.deliveryTamperDetectionEnabled.set(s.deliveryTamperDetectionEnabled);
    this.deliveryKioskModeEnforced.set(s.deliveryKioskModeEnforced);
    this.deliveryTelemetryHeartbeatSeconds.set(s.deliveryTelemetryHeartbeatSeconds);
    this.deliveryAutosaveIntervalSeconds.set(s.deliveryAutosaveIntervalSeconds);
    this.deliveryMaxDisconnectGraceSeconds.set(s.deliveryMaxDisconnectGraceSeconds);
    this.deliveryRetestAuthorizationRequired.set(s.deliveryRetestAuthorizationRequired);

    this.practiceModeEnabled.set(s.practiceModeEnabled);
    this.practiceSolutionsVisible.set(s.practiceSolutionsVisible);

    this.questionDualReviewRequired.set(s.questionDualReviewRequired);
    this.questionAiGenerationEnabled.set(s.questionAiGenerationEnabled);
    this.evaluationAutoGradeInstant.set(s.evaluationAutoGradeInstant);
    this.evaluationAnonymizeCandidateSheets.set(s.evaluationAnonymizeCandidateSheets);

    this.alertFailedLoginSpikesEnabled.set(s.alertFailedLoginSpikesEnabled);
    this.alertExamWindowStartEnabled.set(s.alertExamWindowStartEnabled);
    this.alertEmailRecipients.set(s.alertEmailRecipients);
    this.alertCriticalErrorWebhook.set(s.alertCriticalErrorWebhook);

    this.dpiDigilockerVerificationEnabled.set(s.dpiDigilockerVerificationEnabled);
    this.dpiFaceVerificationThreshold.set(s.dpiFaceVerificationThreshold);
    this.platformMaintenanceMode.set(s.platformMaintenanceMode);
    this.platformBannerMessage.set(s.platformBannerMessage);

    // Baseline copy for dirty tracking
    this.baselineState.set({ ...s });
  }

  private exportStateToConfigMap(): Record<string, string> {
    return {
      'auth.mfa.enforced': String(this.authMfaEnforced()),
      'auth.session.timeout.minutes': String(this.authSessionTimeoutMinutes()),
      'auth.max.login.attempts': String(this.authMaxLoginAttempts()),
      'auth.lockout.duration.minutes': String(this.authLockoutDurationMinutes()),
      'auth.password.expiry.days': String(this.authPasswordExpiryDays()),
      'auth.password.min.length': String(this.authPasswordMinLength()),

      'delivery.tamper.detection.enabled': String(this.deliveryTamperDetectionEnabled()),
      'delivery.kiosk.mode.enforced': String(this.deliveryKioskModeEnforced()),
      'delivery.telemetry.heartbeat.seconds': String(this.deliveryTelemetryHeartbeatSeconds()),
      'delivery.autosave.interval.seconds': String(this.deliveryAutosaveIntervalSeconds()),
      'delivery.max.disconnect.grace.seconds': String(this.deliveryMaxDisconnectGraceSeconds()),
      'delivery.retest.authorization.required': String(this.deliveryRetestAuthorizationRequired()),

      'practice.mode.enabled': String(this.practiceModeEnabled()),
      'practice.solutions.visible': String(this.practiceSolutionsVisible()),

      'question.dual.review.required': String(this.questionDualReviewRequired()),
      'question.ai.generation.enabled': String(this.questionAiGenerationEnabled()),
      'evaluation.auto.grade.instant': String(this.evaluationAutoGradeInstant()),
      'evaluation.anonymize.candidate.sheets': String(this.evaluationAnonymizeCandidateSheets()),

      'alert.failed.login.spikes.enabled': String(this.alertFailedLoginSpikesEnabled()),
      'alert.exam.window.start.enabled': String(this.alertExamWindowStartEnabled()),
      'alert.email.recipients': this.alertEmailRecipients() || '',
      'alert.critical.error.webhook': this.alertCriticalErrorWebhook() || '',

      'dpi.digilocker.verification.enabled': String(this.dpiDigilockerVerificationEnabled()),
      'dpi.face.verification.threshold': String(this.dpiFaceVerificationThreshold()),
      'platform.maintenance.mode': String(this.platformMaintenanceMode()),
      'platform.banner.message': this.platformBannerMessage() || ''
    };
  }
}
