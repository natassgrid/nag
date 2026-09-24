import {
  Component,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  PageHeaderComponent,
  StatusBadgeComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

export interface EducationEntry {
  id: string;
  qualification: string;
  boardOrUniversity: string;
  passingYear: number;
  percentageOrCgpa: string;
}

export interface CandidateProfile {
  candidateId: string;
  fullName: string;
  dateOfBirth: string;
  gender: 'MALE' | 'FEMALE' | 'OTHER';
  nationality: string;
  category: 'GENERAL' | 'OBC' | 'SC' | 'ST' | 'EWS';
  identityDocType: 'AADHAAR' | 'PAN' | 'PASSPORT' | 'VOTER_ID' | 'DRIVING_LICENSE';
  identityDocNumber: string;
  mobile: string;
  email: string;
  address: string;
  state: string;
  pinCode: string;
  kycStatus: 'VERIFIED' | 'PENDING' | 'REJECTED';
  education: EducationEntry[];
}

@Component({
  selector: 'app-candidate-profile',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    PageHeaderComponent,
    StatusBadgeComponent,
  ],
  templateUrl: './candidate-profile.component.html',
  styleUrl: './candidate-profile.component.scss',
})
export class CandidateProfileComponent {
  readonly authService = inject(AuthService);

  activeTab = signal<'personal' | 'contact' | 'education' | 'documents'>('personal');
  saving = signal<boolean>(false);

  tabs = [
    { id: 'personal' as const, label: 'Personal Details', icon: 'person' },
    { id: 'contact' as const, label: 'Contact & Address', icon: 'pin_drop' },
    { id: 'education' as const, label: 'Educational Details', icon: 'school' },
    { id: 'documents' as const, label: 'KYC & Uploads', icon: 'cloud_upload' },
  ];

  profile = signal<CandidateProfile>({
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
