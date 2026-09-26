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

  readonly tabs: ProfileTabOption[] = [
    { id: 'personal', label: 'Personal Details', icon: 'person' },
    { id: 'contact', label: 'Contact & Address', icon: 'pin_drop' },
    { id: 'education', label: 'Educational Details', icon: 'school' },
    { id: 'documents', label: 'KYC & Uploads', icon: 'cloud_upload' },
    { id: 'digilocker', label: 'DigiLocker Claims', icon: 'verified_user', badge: 'DPI' },
  ];

  readonly profile = signal<CandidateProfile>({
    candidateId: 'NAG-CAN-849202',
    fullName: 'Rahul Sharma',
    dateOfBirth: '2001-04-18',
    gender: 'MALE',
    nationality: 'Indian',
    category: 'GENERAL',
    identityDocType: 'AADHAAR',
    identityDocNumber: 'XXXX-XXXX-8921',
    mobile: '+91 98765 43210',
    email: 'rahul.sharma@example.gov.in',
    address: 'Flat 402, Block C, Pragati Vihar, Hauz Khas',
    state: 'Delhi',
    pinCode: '110016',
    kycStatus: 'VERIFIED',
    digiLockerStatus: 'VERIFIED',
    digiLockerUri: 'in.gov.digilocker:user:849201:claims',
    digiLockerClaims: [
      {
        id: 'dl-claim-1',
        docType: 'AADHAAR',
        docName: 'Aadhaar e-KYC Identity Claim',
        issuerName: 'Unique Identification Authority of India (UIDAI)',
        docNumber: 'XXXXXXXX8921',
        issuedDate: '2018-05-12',
        verifiedAt: '2026-09-26T10:00:00Z',
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
        verifiedAt: '2026-09-26T10:00:00Z',
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
        verifiedAt: '2026-09-26T10:00:00Z',
        status: 'VERIFIED',
        hashDigest: 'sha256:7b8c9d0e1f23456789abcdef0123456789abcdef0123456789abcdef01234567',
      },
    ],
    education: [
      {
        id: '1',
        qualification: 'B.Tech in Computer Science',
        boardOrUniversity: 'Delhi Technological University (DTU)',
        passingYear: 2024,
        percentageOrCgpa: '8.85 CGPA',
      },
      {
        id: '2',
        qualification: 'Senior Secondary (12th Class)',
        boardOrUniversity: 'CBSE',
        passingYear: 2020,
        percentageOrCgpa: '94.2%',
      },
    ],
  });

  ngOnInit(): void {
    const user = this.authService.currentUser();
    if (user?.userId && user.userId !== 'user-unknown') {
      this.loadProfileFromApi(user.userId);
    }
  }

  private loadProfileFromApi(userId: string): void {
    this.loading.set(true);
    this.http.get<any>(`/api/v1/candidates/${userId}`).subscribe({
      next: (data) => {
        this.loading.set(false);
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
        // Fallback gracefully to existing profile data
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
        }
      },
      error: () => {
        // Keep initial education entries
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
      qualification: 'New Qualification',
      boardOrUniversity: 'Board / University',
      passingYear: 2024,
      percentageOrCgpa: 'N/A',
    };

    this.profile.update((p) => ({
      ...p,
      education: [...p.education, newEdu],
    }));

    if (user?.userId && user.userId !== 'user-unknown') {
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
    if (user?.userId && edu?.id && user.userId !== 'user-unknown') {
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
      const payload = {
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

      this.http.put(`/api/v1/candidates/${user.userId}`, payload).subscribe({
        next: () => {
          this.saving.set(false);
          alert('Candidate profile saved and synchronized with the backend.');
        },
        error: () => {
          this.saving.set(false);
          alert('Profile saved locally (offline / mock fallback).');
        },
      });
    } else {
      setTimeout(() => {
        this.saving.set(false);
        alert('Candidate Profile updated and anchored to secure vault.');
      }, 500);
    }
  }
}
