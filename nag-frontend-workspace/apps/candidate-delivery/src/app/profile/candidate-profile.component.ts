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
            state: data.state || curr.state,
            district: data.district || curr.district,
            city: data.city || curr.city,
            pinCode: data.pinCode || curr.pinCode,
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

    // Load educational qualifications
    this.loadEducationList(userId);
  }

  private loadEducationList(userId: string): void {
    this.http.get<any[]>(`/api/v1/candidates/${userId}/education`).subscribe({
      next: (eduList) => {
        if (eduList && Array.isArray(eduList)) {
          this.profile.update((curr) => ({
            ...curr,
            education: eduList.map((e) => ({
              id: e.id || String(Date.now()),
              qualification: e.qualification || '',
              courseName: e.courseName || undefined,
              boardOrUniversity: e.boardOrUniversity || '',
              institutionName: e.institutionName || undefined,
              passingYear: e.passingYear || 2024,
              percentageOrCgpa: e.percentageOrCgpa ? String(e.percentageOrCgpa) : '',
              specialization: e.specialization || undefined,
              rollNumber: e.rollNumber || undefined,
              certificateAssetId: e.certificateAssetId || undefined,
            })),
          }));
        }
      },
      error: () => {
        // Educational records loading failure handled silently
      },
    });
  }

  onTabChange(tabId: ProfileTab): void {
    this.activeTab.set(tabId);
  }

  handleSaveEducation(event: { isNew: boolean; data: EducationEntry }): void {
    const user = this.authService.currentUser();
    const userId = user?.userId && user.userId !== 'user-unknown' ? user.userId : this.profile().candidateId;

    if (!userId || userId === 'user-unknown' || userId === 'NAG-CAN-NEW') {
      // Optimistically store in local state if user profile is local
      if (event.isNew) {
        this.profile.update((p) => ({ ...p, education: [...p.education, event.data] }));
      } else {
        this.profile.update((p) => ({
          ...p,
          education: p.education.map((e) => (e.id === event.data.id ? event.data : e)),
        }));
      }
      this.successMessage.set('Academic qualification saved.');
      return;
    }

    // Convert string score (e.g. "85.50%" or "8.50 CGPA") to BigDecimal numeric format
    const rawScore = event.data.percentageOrCgpa.replace(/[^0-9.]/g, '');
    const numScore = rawScore ? parseFloat(rawScore) : null;

    const payload: any = {
      qualification: event.data.qualification,
      courseName: event.data.courseName || null,
      boardOrUniversity: event.data.boardOrUniversity,
      institutionName: event.data.institutionName || null,
      passingYear: event.data.passingYear,
      percentageOrCgpa: numScore,
      gradeOrDivision: event.data.gradeOrDivision || null,
      specialization: event.data.specialization || null,
      rollNumber: event.data.rollNumber || null,
      certificateAssetId: event.data.certificateAssetId && event.data.certificateAssetId.startsWith('asset-') ? null : event.data.certificateAssetId,
    };

    if (event.isNew) {
      this.http.post<any>(`/api/v1/candidates/${userId}/education`, payload).subscribe({
        next: (saved) => {
          const newEntry: EducationEntry = {
            id: saved.id || event.data.id,
            qualification: saved.qualification || event.data.qualification,
            courseName: saved.courseName || event.data.courseName,
            boardOrUniversity: saved.boardOrUniversity || event.data.boardOrUniversity,
            institutionName: saved.institutionName || event.data.institutionName,
            passingYear: saved.passingYear || event.data.passingYear,
            percentageOrCgpa: event.data.percentageOrCgpa,
            specialization: saved.specialization || event.data.specialization,
            rollNumber: saved.rollNumber || event.data.rollNumber,
            certificateAssetId: saved.certificateAssetId || event.data.certificateAssetId,
            certificateFileName: event.data.certificateFileName,
          };
          this.profile.update((p) => ({ ...p, education: [...p.education, newEntry] }));
          this.successMessage.set('Academic qualification added successfully!');
          this.errorMessage.set(null);
        },
        error: (err) => {
          const detail = err.error?.message || err.error?.error || 'Failed to add qualification record.';
          this.errorMessage.set(detail);
        },
      });
    } else {
      this.http.put<any>(`/api/v1/candidates/${userId}/education/${event.data.id}`, payload).subscribe({
        next: () => {
          this.profile.update((p) => ({
            ...p,
            education: p.education.map((e) => (e.id === event.data.id ? event.data : e)),
          }));
          this.successMessage.set('Academic qualification updated successfully!');
          this.errorMessage.set(null);
        },
        error: (err) => {
          const detail = err.error?.message || err.error?.error || 'Failed to update qualification record.';
          this.errorMessage.set(detail);
        },
      });
    }
  }

  handleDeleteEducation(educationId: string): void {
    const user = this.authService.currentUser();
    const userId = user?.userId && user.userId !== 'user-unknown' ? user.userId : this.profile().candidateId;

    this.profile.update((p) => ({
      ...p,
      education: p.education.filter((e) => e.id !== educationId),
    }));

    if (userId && userId !== 'user-unknown' && userId !== 'NAG-CAN-NEW' && !educationId.startsWith('edu-')) {
      this.http.delete(`/api/v1/candidates/${userId}/education/${educationId}`).subscribe({
        next: () => {
          this.successMessage.set('Academic record removed.');
          this.errorMessage.set(null);
        },
        error: (err) => {
          const detail = err.error?.message || 'Failed to delete record from server.';
          this.errorMessage.set(detail);
        },
      });
    } else {
      this.successMessage.set('Academic record removed.');
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
      const payload = {
        fullName: p.fullName,
        dateOfBirth: p.dateOfBirth || null,
        gender: p.gender,
        nationality: p.nationality || 'Indian',
        category: p.category || 'GENERAL',
        reservationCategory: p.reservationCategory || null,
        address: p.address || null,
        state: p.state || null,
        district: p.district || null,
        city: p.city || p.district || null,
        pinCode: p.pinCode || null,
        country: p.nationality || 'India',
        mobile: p.mobile,
        email: p.email,
        identityDocType: p.identityDocType || 'AADHAAR',
        identityDocNumber: p.identityDocNumber || null,
        preferredRegionalLanguage: p.preferredRegionalLanguage || 'en',
      };

      this.http.put(`/api/v1/candidates/${userId}`, payload).subscribe({
        next: () => {
          this.saving.set(false);
          this.existsOnServer.set(true);
          this.successMessage.set('Candidate profile successfully updated and synchronized.');
        },
        error: (err) => {
          this.saving.set(false);
          const detail = err.error?.message || err.error?.error || 'Failed to update profile.';
          this.errorMessage.set(detail);
        },
      });
    } else {
      this.saving.set(false);
      this.successMessage.set('Profile changes saved in local session.');
    }
  }
}
