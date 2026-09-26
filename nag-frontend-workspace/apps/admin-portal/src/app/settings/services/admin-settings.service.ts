import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import {
  DEFAULT_SYSTEM_SETTINGS,
  SystemSettingsState,
} from '../models/settings.model';

@Injectable({
  providedIn: 'root',
})
export class AdminSettingsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/admin/config';

  /**
   * Fetches current cluster configuration map from backend.
   */
  getSettings(): Observable<SystemSettingsState> {
    return this.http.get<Record<string, string>>(`${this.baseUrl}/map`).pipe(
      map((configMap) => this.mapToState(configMap)),
      catchError((err) => {
        console.warn('Could not fetch system configs from backend, falling back to defaults:', err);
        return of({ ...DEFAULT_SYSTEM_SETTINGS });
      })
    );
  }

  /**
   * Persists updated configuration map across the cluster.
   */
  saveSettings(state: SystemSettingsState): Observable<SystemSettingsState> {
    const payload = {
      configs: this.mapToBackend(state),
    };

    return this.http.put<Record<string, string>>(`${this.baseUrl}/bulk`, payload).pipe(
      map((resMap) => this.mapToState(resMap)),
      catchError((err) => {
        console.warn('Backend bulk config update error:', err);
        return of(state);
      })
    );
  }

  /**
   * Resets tenant configuration to platform defaults.
   */
  resetDefaults(): Observable<SystemSettingsState> {
    return this.http.post<Record<string, string>>(`${this.baseUrl}/reset`, {}).pipe(
      map((resMap) => this.mapToState(resMap)),
      catchError((err) => {
        console.warn('Backend config reset error:', err);
        return of({ ...DEFAULT_SYSTEM_SETTINGS });
      })
    );
  }

  private mapToState(map: Record<string, string>): SystemSettingsState {
    if (!map || Object.keys(map).length === 0) {
      return { ...DEFAULT_SYSTEM_SETTINGS };
    }

    return {
      authMfaEnforced: map['auth.mfa.enforced'] === 'true',
      authSessionTimeoutMinutes: parseInt(map['auth.session.timeout.minutes'] || '30', 10),
      authMaxLoginAttempts: parseInt(map['auth.max.login.attempts'] || '5', 10),
      authLockoutDurationMinutes: parseInt(map['auth.lockout.duration.minutes'] || '15', 10),
      authPasswordExpiryDays: parseInt(map['auth.password.expiry.days'] || '90', 10),
      authPasswordMinLength: parseInt(map['auth.password.min.length'] || '12', 10),
      vaultTransitKeyAutoRotate: true,

      deliveryTamperDetectionEnabled: map['delivery.tamper.detection.enabled'] !== 'false',
      deliveryKioskModeEnforced: map['delivery.kiosk.mode.enforced'] !== 'false',
      deliveryTelemetryHeartbeatSeconds: parseInt(map['delivery.telemetry.heartbeat.seconds'] || '10', 10),
      deliveryAutosaveIntervalSeconds: parseInt(map['delivery.autosave.interval.seconds'] || '15', 10),
      deliveryMaxDisconnectGraceSeconds: parseInt(map['delivery.max.disconnect.grace.seconds'] || '180', 10),
      deliveryRetestAuthorizationRequired: map['delivery.retest.authorization.required'] !== 'false',

      dpiDigilockerVerificationEnabled: map['dpi.digilocker.verification.enabled'] !== 'false',
      dpiFaceVerificationThreshold: parseInt(map['dpi.face.verification.threshold'] || '85', 10),
      aiLiteLlmGatewayUrl: map['ai.litellm.gateway.url'] || 'http://nag-ai-gateway:8000',
      aiIndicTrans2Workers: parseInt(map['ai.indictrans2.workers'] || '4', 10),

      questionDualReviewRequired: map['question.dual.review.required'] !== 'false',
      questionAiGenerationEnabled: map['question.ai.generation.enabled'] !== 'false',
      evaluationAutoGradeInstant: map['evaluation.auto.grade.instant'] !== 'false',
      evaluationAnonymizeCandidateSheets: map['evaluation.anonymize.candidate.sheets'] !== 'false',

      alertCriticalErrorWebhook: map['alert.critical.error.webhook'] || '',
      alertEmailRecipients: map['alert.email.recipients'] || 'sec-ops@nag.gov.in, admin@nag.gov.in',
      platformMaintenanceMode: map['platform.maintenance.mode'] === 'true',
      platformBannerMessage: map['platform.banner.message'] || '',
    };
  }

  private mapToBackend(state: SystemSettingsState): Record<string, string> {
    return {
      'auth.mfa.enforced': String(state.authMfaEnforced),
      'auth.session.timeout.minutes': String(state.authSessionTimeoutMinutes),
      'auth.max.login.attempts': String(state.authMaxLoginAttempts),
      'auth.lockout.duration.minutes': String(state.authLockoutDurationMinutes),
      'auth.password.expiry.days': String(state.authPasswordExpiryDays),
      'auth.password.min.length': String(state.authPasswordMinLength),

      'delivery.tamper.detection.enabled': String(state.deliveryTamperDetectionEnabled),
      'delivery.kiosk.mode.enforced': String(state.deliveryKioskModeEnforced),
      'delivery.telemetry.heartbeat.seconds': String(state.deliveryTelemetryHeartbeatSeconds),
      'delivery.autosave.interval.seconds': String(state.deliveryAutosaveIntervalSeconds),
      'delivery.max.disconnect.grace.seconds': String(state.deliveryMaxDisconnectGraceSeconds),
      'delivery.retest.authorization.required': String(state.deliveryRetestAuthorizationRequired),

      'dpi.digilocker.verification.enabled': String(state.dpiDigilockerVerificationEnabled),
      'dpi.face.verification.threshold': String(state.dpiFaceVerificationThreshold),
      'ai.litellm.gateway.url': state.aiLiteLlmGatewayUrl,
      'ai.indictrans2.workers': String(state.aiIndicTrans2Workers),

      'question.dual.review.required': String(state.questionDualReviewRequired),
      'question.ai.generation.enabled': String(state.questionAiGenerationEnabled),
      'evaluation.auto.grade.instant': String(state.evaluationAutoGradeInstant),
      'evaluation.anonymize.candidate.sheets': String(state.evaluationAnonymizeCandidateSheets),

      'alert.critical.error.webhook': state.alertCriticalErrorWebhook,
      'alert.email.recipients': state.alertEmailRecipients,
      'platform.maintenance.mode': String(state.platformMaintenanceMode),
      'platform.banner.message': state.platformBannerMessage,
    };
  }
}
