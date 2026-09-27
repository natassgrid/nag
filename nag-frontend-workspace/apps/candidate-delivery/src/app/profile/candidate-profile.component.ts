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
  ];

  readonly profile = signal<CandidateProfile>(createEmptyProfile());

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user?.userId && user.userId !== 'user-unknown') {
      this.profile.set(createEmptyProfile(user.userId, user.username));
      this.loadProfileFromApi(user.userId);
    }
  }

  private loadProfileFromApi(userId: string): void {
    this.loading.set(true);
    const user = this.authService.currentUser();

    this.http.get<any>(`/api/v1/candidates/${userId}`).subscribe({
      next: (data) => {
        this.loading.set(false);
        this.existsOnServer.set(true);
        if (data) {
          this.profile.update((curr) => ({
            ...curr,
            candidateId: data.userId || curr.candidateId,
            fullName: data.fullName || curr.fullName,
            dateOfBirth: data.dateOfBirth || curr.dateOfBirth,
            gender: data.gender || curr.gender,
            nationality: data.nationality || curr.nationality,
            category: data.category || curr.category,
            reservationCategory: data.reservationCategory || curr.reservationCategory,
            address: data.address || curr.address,
            mobile: data.mobile || curr.mobile,
            email: data.email || (user?.username?.includes('@') ? user.username : curr.email),
            kycStatus: data.digiLockerVerified === 'VERIFIED' ? 'VERIFIED' : curr.kycStatus,
            digiLockerStatus: data.digiLockerVerified || curr.digiLockerStatus,
          }));
        }
      },
      error: () => {
        this.loading.set(false);
        this.existsOnServer.set(false);
        // Retain user identity if profile is missing on server
        this.profile.update((curr) => ({
          ...curr,
          candidateId: userId,
          email: user?.username?.includes('@') ? user.username : curr.email,
        }));
      },
    });

    // Also fetch verification status
    this.authService.getVerificationStatus(userId).subscribe({
      next: (status) => {
        if (status) {
          this.profile.update((curr) => ({
            ...curr,
            emailVerified: status.emailVerified,
            mobileVerified: status.mobileVerified,
          }));
        }
      },
      error: () => {
        // Verification status endpoint failure is non-blocking
      },
    });

    // Also load educational qualifications if available
    this.http.get<any[]>(`/api/v1/candidates/${userId}/education`).subscribe({
      next: (eduList) => {
        if (eduList && Array.isArray(eduList) && eduList.length > 0) {
          this.profile.update((curr) => ({
            ...curr,
            education: eduList.map((e) => ({
              id: e.id || String(Date.now()),
              qualification: e.qualification || '',
              boardOrUniversity: e.boardOrUniversity || '',
              passingYear: e.passingYear || 2024,
              percentageOrCgpa: e.percentageOrCgpa || '',
              certificateAssetId: e.certificateAssetId,
            })),
          }));
        } else {
          this.profile.update((curr) => ({ ...curr, education: [] }));
        }
      },
      error: () => {
        this.profile.update((curr) => ({ ...curr, education: [] }));
      },
    });
  }

  onTabChange(tabId: ProfileTab): void {
    this.activeTab.set(tabId);
  }

  addEducation(): void {
    const user = this.authService.currentUser();
    const newEdu = {
      id: String(Date.now()),
      qualification: '',
      boardOrUniversity: '',
      passingYear: new Date().getFullYear(),
      percentageOrCgpa: '',
    };

    this.profile.update((p) => ({
      ...p,
      education: [...p.education, newEdu],
    }));

    if (user?.userId && user.userId !== 'user-unknown' && this.existsOnServer()) {
      this.http
        .post(`/api/v1/candidates/${user.userId}/education`, {
          qualification: newEdu.qualification,
          boardOrUniversity: newEdu.boardOrUniversity,
          passingYear: newEdu.passingYear,
          percentageOrCgpa: newEdu.percentageOrCgpa,
        })
        .subscribe({
          error: () => {},
        });
    }
  }

  removeEducation(index: number): void {
    const edu = this.profile().education[index];
    this.profile.update((p) => ({
      ...p,
      education: p.education.filter((_, idx) => idx !== index),
    }));

    const user = this.authService.currentUser();
    if (user?.userId && edu?.id && user.userId !== 'user-unknown' && this.existsOnServer()) {
      this.http
        .delete(`/api/v1/candidates/${user.userId}/education/${edu.id}`)
        .subscribe({
          error: () => {},
        });
    }
  }

  saveProfile(): void {
    this.saving.set(true);
    this.errorMessage.set(null);
    this.successMessage.set(null);
    const user = this.authService.currentUser();
    const p = this.profile();

    if (!p.fullName || !p.fullName.trim()) {
      this.saving.set(false);
      this.errorMessage.set('Full Name is required.');
      this.activeTab.set('personal');
      return;
    }

    if (!p.mobile || !p.mobile.trim()) {
      this.saving.set(false);
      this.errorMessage.set('Mobile number is required.');
      this.activeTab.set('contact');
      return;
    }

    const userId = user?.userId && user.userId !== 'user-unknown' ? user.userId : p.candidateId;
    if (userId && userId !== 'user-unknown' && userId !== 'NAG-CAN-NEW') {
      const updatePayload = {
        fullName: p.fullName.trim(),
        dateOfBirth: p.dateOfBirth || '2000-01-01',
        gender: p.gender || 'MALE',
        nationality: p.nationality || 'Indian',
        category: p.category || 'GENERAL',
        mobile: p.mobile.trim(),
        email: p.email || (user?.username?.includes('@') ? user.username : ''),
        address: p.address || '',
        reservationCategory: p.reservationCategory || '',
        identityDocNumber: p.identityDocNumber || '',
      };

      if (this.existsOnServer()) {
        this.http.put(`/api/v1/candidates/${userId}`, updatePayload).subscribe({
          next: () => {
            this.saving.set(false);
            this.successMessage.set('Candidate profile updated successfully.');
            this.errorMessage.set(null);
          },
          error: (err) => {
            if (err.status === 404) {
              this.createProfile(userId, p);
            } else {
              this.saving.set(false);
              this.handleApiError(err, 'Failed to update candidate profile.');
            }
          },
        });
      } else {
        this.createProfile(userId, p);
      }
    } else {
      setTimeout(() => {
        this.saving.set(false);
        this.successMessage.set('Candidate profile updated locally (offline mode).');
      }, 500);
    }
  }

  private createProfile(userId: string, p: CandidateProfile): void {
    const createPayload = {
      userId: userId,
      fullName: p.fullName?.trim() || 'Candidate',
      dateOfBirth: p.dateOfBirth || '2000-01-01',
      gender: p.gender || 'MALE',
      nationality: p.nationality || 'Indian',
      category: p.category || 'GENERAL',
      mobile: p.mobile?.trim() || '',
      email: p.email || '',
      address: p.address || '',
      reservationCategory: p.reservationCategory || '',
      identityDocNumber: p.identityDocNumber || 'DOC' + Date.now(),
    };

    this.http.post(`/api/v1/candidates`, createPayload).subscribe({
      next: () => {
        this.saving.set(false);
        this.existsOnServer.set(true);
        this.successMessage.set('Candidate profile created successfully.');
        this.errorMessage.set(null);
      },
      error: (err) => {
        this.saving.set(false);
        this.handleApiError(err, 'Failed to save candidate profile on server.');
      },
    });
  }

  private handleApiError(err: any, fallbackMessage: string): void {
    const fieldErrors = err?.error?.fieldErrors;
    const detail = err?.error?.detail || err?.error?.message || err?.error?.error;
    if (fieldErrors && typeof fieldErrors === 'object' && Object.keys(fieldErrors).length > 0) {
      const messages = Object.entries(fieldErrors)
        .map(([field, msg]) => `${field}: ${msg}`)
        .join(', ');
      this.errorMessage.set(`Validation Failed: ${messages}`);
    } else if (detail && typeof detail === 'string') {
      this.errorMessage.set(detail);
    } else {
      this.errorMessage.set(fallbackMessage);
    }
    this.successMessage.set(null);
  }
}
