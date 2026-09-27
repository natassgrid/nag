import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '@nag-frontend-workspace/shared-ui-components';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import {
  CandidateProfile,
  EducationEntry,
  ProfileTab,
  ProfileTabOption,
} from './models';
import {
  ProfileOverviewCardComponent,
  ProfileTabNavComponent,
  PersonalDetailsPanelComponent,
  ContactDetailsPanelComponent,
  EducationDetailsPanelComponent,
  KycDocumentsPanelComponent,
  DigiLockerPanelComponent,
  SecurityMfaPanelComponent,
} from './components';

export * from './models';

function createEmptyProfile(userId = '', username = ''): CandidateProfile {
  const isEmail = username.includes('@');
  return {
    candidateId: userId || 'NAG-CAN-NEW',
    fullName: isEmail ? '' : username,
    dateOfBirth: '',
    gender: 'MALE',
    nationality: 'Indian',
    category: 'GENERAL',
    reservationCategory: '',
    identityDocType: 'AADHAAR',
    identityDocNumber: '',
    mobile: '',
    email: isEmail ? username : '',
    emailVerified: false,
    mobileVerified: false,
    address: '',
    state: '',
    district: '',
    city: '',
    pinCode: '',
    kycStatus: 'PENDING',
    digiLockerStatus: 'NOT_LINKED',
    digiLockerUri: '',
    digiLockerClaims: [],
    education: [],
  };
}

@Component({
  selector: 'app-candidate-profile',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    ProfileOverviewCardComponent,
    ProfileTabNavComponent,
    PersonalDetailsPanelComponent,
    ContactDetailsPanelComponent,
    EducationDetailsPanelComponent,
    KycDocumentsPanelComponent,
    DigiLockerPanelComponent,
    SecurityMfaPanelComponent,
  ],
  templateUrl: './candidate-profile.component.html',
  styleUrl: './candidate-profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateProfileComponent implements OnInit {
  private readonly http = inject(HttpClient);
  readonly authService = inject(AuthService);

  readonly activeTab = signal<ProfileTab>('personal');
  readonly loading = signal<boolean>(false);
  readonly saving = signal<boolean>(false);
  readonly existsOnServer = signal<boolean>(false);
  readonly errorMessage = signal<string | null>(null);
  readonly successMessage = signal<string | null>(null);

  readonly tabs: ProfileTabOption[] = [
    { id: 'personal', label: 'Personal Details', icon: 'person' },
    { id: 'contact', label: 'Contact & Address', icon: 'pin_drop' },
    { id: 'education', label: 'Educational Details', icon: 'school' },
    { id: 'documents', label: 'KYC & Uploads', icon: 'cloud_upload' },
    { id: 'digilocker', label: 'DigiLocker Claims', icon: 'verified_user', badge: 'DPI' },
    { id: 'security', label: 'Security & 2FA', icon: 'security', badge: 'TOTP' },
  ];

  readonly profile = signal<CandidateProfile>(createEmptyProfile());

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user?.userId && user.userId !== 'user-unknown') {
      this.profile.set(createEmptyProfile(user.userId, user.username));
      this.loadProfileFromApi(user.userId);
    }
  }

  loadProfileFromApi(userId: string): void {
    this.loading.set(true);
    this.http.get<{ data: any }>(`/api/v1/candidate/profile/${userId}`).subscribe({
      next: (res) => {
        this.loading.set(false);
        if (res?.data) {
          this.existsOnServer.set(true);
          const d = res.data;
          this.profile.update((prev) => ({
            ...prev,
            fullName: d.fullName || prev.fullName,
            dateOfBirth: d.dateOfBirth || prev.dateOfBirth,
            gender: d.gender || prev.gender,
            nationality: d.nationality || prev.nationality,
            category: d.category || prev.category,
            reservationCategory: d.reservationCategory || prev.reservationCategory,
            identityDocType: d.identityDocType || prev.identityDocType,
            identityDocNumber: d.identityDocNumber || prev.identityDocNumber,
            mobile: d.mobile || prev.mobile,
            email: d.email || prev.email,
            emailVerified: d.emailVerified ?? prev.emailVerified,
            mobileVerified: d.mobileVerified ?? prev.mobileVerified,
            address: d.address || prev.address,
            state: d.state || prev.state,
            district: d.district || prev.district,
            city: d.city || prev.city,
            pinCode: d.pinCode || prev.pinCode,
            preferredRegionalLanguage: d.preferredRegionalLanguage || prev.preferredRegionalLanguage,
            kycStatus: d.kycStatus || prev.kycStatus,
            photoUrl: d.photoUrl || prev.photoUrl,
            photoAssetId: d.photoAssetId || prev.photoAssetId,
            signatureUrl: d.signatureUrl || prev.signatureUrl,
            signatureAssetId: d.signatureAssetId || prev.signatureAssetId,
            idProofUrl: d.idProofUrl || prev.idProofUrl,
            idProofAssetId: d.idProofAssetId || prev.idProofAssetId,
            digiLockerStatus: d.digiLockerStatus || prev.digiLockerStatus,
            digiLockerUri: d.digiLockerUri || prev.digiLockerUri,
            education: d.education || prev.education,
          }));
        }
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  onTabChange(tab: ProfileTab): void {
    this.activeTab.set(tab);
    this.errorMessage.set(null);
    this.successMessage.set(null);
  }

  saveProfile(): void {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);

    const prof = this.profile();
    const payload = {
      ...prof,
      userId: prof.candidateId,
    };

    const req$ = this.existsOnServer()
      ? this.http.put(`/api/v1/candidate/profile/${prof.candidateId}`, payload)
      : this.http.post('/api/v1/candidate/profile', payload);

    req$.subscribe({
      next: () => {
        this.saving.set(false);
        this.existsOnServer.set(true);
        this.successMessage.set('Profile successfully updated!');
      },
      error: (err) => {
        this.saving.set(false);
        this.errorMessage.set(
          err.error?.message || 'Failed to update profile. Please try again.'
        );
      },
    });
  }

  handleSaveEducation(event: { isNew: boolean; data: EducationEntry }): void {
    const entry = event.data;
    this.profile.update((p) => {
      const idx = p.education.findIndex((e) => e.id === entry.id);
      const updated = [...p.education];
      if (idx >= 0) {
        updated[idx] = entry;
      } else {
        updated.push(entry);
      }
      return { ...p, education: updated };
    });
    this.saveProfile();
  }

  handleDeleteEducation(id: string): void {
    this.profile.update((p) => ({
      ...p,
      education: p.education.filter((e) => e.id !== id),
    }));
    this.saveProfile();
  }
}
