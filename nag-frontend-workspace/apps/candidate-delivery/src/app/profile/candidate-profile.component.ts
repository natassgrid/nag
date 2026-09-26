import {
  ChangeDetectionStrategy,
  Component,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
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
  ],
  templateUrl: './candidate-profile.component.html',
  styleUrl: './candidate-profile.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateProfileComponent {
  readonly authService = inject(AuthService);

  readonly activeTab = signal<ProfileTab>('personal');
  readonly saving = signal<boolean>(false);

  readonly tabs: ProfileTabOption[] = [
    { id: 'personal', label: 'Personal Details', icon: 'person' },
    { id: 'contact', label: 'Contact & Address', icon: 'pin_drop' },
    { id: 'education', label: 'Educational Details', icon: 'school' },
    { id: 'documents', label: 'KYC & Uploads', icon: 'cloud_upload' },
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

  onTabChange(tabId: ProfileTab): void {
    this.activeTab.set(tabId);
  }

  addEducation(): void {
    this.profile.update((p) => ({
      ...p,
      education: [
        ...p.education,
        {
          id: String(Date.now()),
          qualification: 'New Qualification',
          boardOrUniversity: 'Board / University',
          passingYear: 2024,
          percentageOrCgpa: 'N/A',
        },
      ],
    }));
  }

  removeEducation(index: number): void {
    this.profile.update((p) => ({
      ...p,
      education: p.education.filter((_, idx) => idx !== index),
    }));
  }

  saveProfile(): void {
    this.saving.set(true);
    setTimeout(() => {
      this.saving.set(false);
      alert('Candidate Profile updated and anchored to secure vault.');
    }, 600);
  }
}
