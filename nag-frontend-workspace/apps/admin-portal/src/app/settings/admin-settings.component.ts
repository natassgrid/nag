import {
  Component,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';

export interface SystemSettingsState {
  authMfaEnforced: boolean;
  authSessionTimeoutMinutes: number;
  authMaxLoginAttempts: number;
  deliveryTamperDetectionEnabled: boolean;
  deliveryKioskModeEnforced: boolean;
  deliveryTelemetryHeartbeatSeconds: number;
  questionDualReviewRequired: boolean;
  questionAiGenerationEnabled: boolean;
  evaluationAnonymizeCandidateSheets: boolean;
  dpiDigilockerVerificationEnabled: boolean;
  alertCriticalErrorWebhook: string;
  platformMaintenanceMode: boolean;
}

@Component({
  selector: 'app-admin-settings',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
  ],
  templateUrl: './admin-settings.component.html',
  styleUrl: './admin-settings.component.scss',
})
export class AdminSettingsComponent {
  saving = signal<boolean>(false);

  settings: SystemSettingsState = {
    authMfaEnforced: true,
    authSessionTimeoutMinutes: 30,
    authMaxLoginAttempts: 5,
    deliveryTamperDetectionEnabled: true,
    deliveryKioskModeEnforced: true,
    deliveryTelemetryHeartbeatSeconds: 10,
    questionDualReviewRequired: true,
    questionAiGenerationEnabled: true,
    evaluationAnonymizeCandidateSheets: true,
    dpiDigilockerVerificationEnabled: true,
    alertCriticalErrorWebhook: 'https://ops.nag.gov.in/webhooks/security',
    platformMaintenanceMode: false,
  };

  resetDefaults(): void {
    this.settings = {
      authMfaEnforced: true,
      authSessionTimeoutMinutes: 30,
      authMaxLoginAttempts: 5,
      deliveryTamperDetectionEnabled: true,
      deliveryKioskModeEnforced: true,
      deliveryTelemetryHeartbeatSeconds: 10,
      questionDualReviewRequired: true,
      questionAiGenerationEnabled: true,
      evaluationAnonymizeCandidateSheets: true,
      dpiDigilockerVerificationEnabled: true,
      alertCriticalErrorWebhook: 'https://ops.nag.gov.in/webhooks/security',
      platformMaintenanceMode: false,
    };
  }

  saveSettings(): void {
    this.saving.set(true);
    setTimeout(() => {
      this.saving.set(false);
      alert('System configuration policies updated and propagated across all cluster nodes.');
    }, 600);
  }
}
