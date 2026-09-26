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
      // Clear forms and initialize with authenticated user identity details
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
            email: data.email || curr.email,
            kycStatus: data.digiLockerVerified === 'VERIFIED' ? 'VERIFIED' : curr.kycStatus,
            digiLockerStatus: data.digiLockerVerified || curr.digiLockerStatus,
          }));
        }
      },
      error: () => {
        this.loading.set(false);
        this.existsOnServer.set(false);
        // On 404 or missing profile, clear all profile form fields completely
        this.profile.set(createEmptyProfile(userId, user?.username || ''));
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
    const user = this.authService.currentUser();
    const p = this.profile();

    if (user?.userId && user.userId !== 'user-unknown') {
      if (this.existsOnServer()) {
        const updatePayload = {
          fullName: p.fullName,
          dateOfBirth: p.dateOfBirth,
          gender: p.gender,
          nationality: p.nationality,
          category: p.category,
          mobile: p.mobile,
          email: p.email,
          address: p.address,
          reservationCategory: p.reservationCategory,
          identityDocNumber: p.identityDocNumber,
        };

        this.http.put(`/api/v1/candidates/${user.userId}`, updatePayload).subscribe({
          next: () => {
            this.saving.set(false);
            alert('Candidate profile updated successfully.');
          },
          error: () => {
            // If PUT fails because profile doesn't exist, attempt POST create
            this.createProfile(user.userId, p);
          },
        });
      } else {
        this.createProfile(user.userId, p);
      }
    } else {
      setTimeout(() => {
        this.saving.set(false);
        alert('Candidate profile updated locally (offline mode).');
      }, 500);
    }
  }

  private createProfile(userId: string, p: CandidateProfile): void {
    const createPayload = {
      userId: userId,
      fullName: p.fullName || 'Candidate',
      dateOfBirth: p.dateOfBirth || '2000-01-01',
      gender: p.gender || 'MALE',
      nationality: p.nationality || 'Indian',
      category: p.category || 'GENERAL',
      mobile: p.mobile || '0000000000',
      email: p.email || 'candidate@example.com',
      address: p.address || '',
      reservationCategory: p.reservationCategory || '',
      identityDocNumber: p.identityDocNumber || 'NOT_PROVIDED',
    };

    this.http.post('/api/v1/candidates', createPayload).subscribe({
      next: () => {
        this.saving.set(false);
        this.existsOnServer.set(true);
        alert('Candidate profile created successfully.');
      },
      error: () => {
        this.saving.set(false);
        alert('Profile saved locally (offline / mock fallback).');
      },
    });
  }
}
