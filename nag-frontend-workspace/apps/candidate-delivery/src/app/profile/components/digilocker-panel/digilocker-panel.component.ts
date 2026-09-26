import {
  ChangeDetectionStrategy,
  Component,
  computed,
  model,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CandidateProfile, DigiLockerClaim } from '../../models';
import {
  DigiLockerClaimCardComponent,
  DigiLockerConsentModalComponent,
} from './components';

@Component({
  selector: 'nag-digilocker-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    DigiLockerClaimCardComponent,
    DigiLockerConsentModalComponent,
  ],
  templateUrl: './digilocker-panel.component.html',
  styleUrl: './digilocker-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DigiLockerPanelComponent {
  readonly profile = model.required<CandidateProfile>();

  readonly isConnecting = signal<boolean>(false);
  readonly showConsentModal = signal<boolean>(false);
  readonly otpStep = signal<boolean>(false);
  readonly otpValue = signal<string>('948210');

  readonly isVerified = computed(() => this.profile()?.digiLockerStatus === 'VERIFIED');
  readonly claims = computed(() => this.profile()?.digiLockerClaims || []);

  openConnectFlow(): void {
    this.otpStep.set(false);
    this.showConsentModal.set(true);
  }

  proceedToOtp(): void {
    this.otpStep.set(true);
  }

  setOtpValue(val: string): void {
    this.otpValue.set(val);
  }

  closeConsentModal(): void {
    this.showConsentModal.set(false);
  }

  verifyAndSync(): void {
    this.isConnecting.set(true);
    setTimeout(() => {
      const mockClaims: DigiLockerClaim[] = [
        {
          id: 'dl-claim-1',
          docType: 'AADHAAR',
          docName: 'Aadhaar e-KYC Identity Claim',
          issuerName: 'Unique Identification Authority of India (UIDAI)',
          docNumber: 'XXXXXXXX8921',
          issuedDate: '2018-05-12',
          verifiedAt: new Date().toISOString(),
          status: 'VERIFIED',
          hashDigest: 'sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
        },
        {
          id: 'dl-claim-2',
          docType: 'CLASS_X_CERT',
          docName: 'Secondary School Examination (Class X) Certificate',
          issuerName: 'Central Board of Secondary Education (CBSE)',
          docNumber: 'CBSE-X-2018-918230',
          issuedDate: '2018-06-15',
          verifiedAt: new Date().toISOString(),
          status: 'VERIFIED',
          hashDigest: 'sha256:4a5b6c7d8e9f0123456789abcdef0123456789abcdef0123456789abcdef0123',
        },
        {
          id: 'dl-claim-3',
          docType: 'CLASS_XII_CERT',
          docName: 'Senior School Certificate Examination (Class XII)',
          issuerName: 'Central Board of Secondary Education (CBSE)',
          docNumber: 'CBSE-XII-2020-582910',
          issuedDate: '2020-07-20',
          verifiedAt: new Date().toISOString(),
          status: 'VERIFIED',
          hashDigest: 'sha256:7b8c9d0e1f23456789abcdef0123456789abcdef0123456789abcdef01234567',
        },
      ];

      this.profile.update((p) => ({
        ...p,
        digiLockerStatus: 'VERIFIED',
        digiLockerUri: `in.gov.digilocker:user:${p.candidateId}:claims`,
        digiLockerClaims: mockClaims,
      }));

      this.isConnecting.set(false);
      this.showConsentModal.set(false);
    }, 900);
  }

  disconnectDigiLocker(): void {
    if (confirm('Are you sure you want to disconnect DigiLocker credentials?')) {
      this.profile.update((p) => ({
        ...p,
        digiLockerStatus: 'NOT_LINKED',
        digiLockerUri: undefined,
        digiLockerClaims: [],
      }));
    }
  }
}
