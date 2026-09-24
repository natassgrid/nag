import {
  Component,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import {
  StatCardComponent,
  StatusBadgeComponent,
  QrCodeComponent,
} from '@nag-frontend-workspace/shared-ui-components';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';

export interface EnrolledExam {
  id: string;
  code: string;
  title: string;
  scheduledDate: string;
  scheduledTime: string;
  durationMinutes: number;
  centerName: string;
  centerAddress: string;
  rollNumber: string;
  status: 'LIVE' | 'UPCOMING' | 'COMPLETED';
  admitCardReady: boolean;
}

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    StatCardComponent,
    StatusBadgeComponent,
    QrCodeComponent,
  ],
  templateUrl: './candidate-dashboard.component.html',
  styleUrl: './candidate-dashboard.component.scss',
})
export class CandidateDashboardComponent {
  readonly authService = inject(AuthService);

  enrolledExams = signal<EnrolledExam[]>([
    {
      id: 'exam-1',
      code: 'NES-2026-S1',
      title: 'National Eligibility Screening (Computer Science & AI)',
      scheduledDate: '2026-09-28',
      scheduledTime: '09:00 AM - 12:00 PM',
      durationMinutes: 180,
      centerName: 'Zone 4 - Center 102 (IIT Delhi Sector 6)',
      centerAddress: 'Hauz Khas, New Delhi - 110016',
      rollNumber: '849202',
      status: 'LIVE',
      admitCardReady: true,
    },
    {
      id: 'exam-2',
      code: 'GATE-DPI-2026',
      title: 'Graduate Assessment for Open DPI Engineering',
      scheduledDate: '2026-10-15',
      scheduledTime: '02:00 PM - 05:00 PM',
      durationMinutes: 180,
      centerName: 'Zone 1 - Center 044 (Bangalore Tech Hub)',
      centerAddress: 'Electronic City, Bangalore - 560100',
      rollNumber: '310948',
      status: 'UPCOMING',
      admitCardReady: false,
    },
  ]);

  liveCount = computed(
    () => this.enrolledExams().filter((e) => e.status === 'LIVE').length
  );

  selectedAdmitCard = signal<EnrolledExam | null>(null);

  openAdmitCard(exam: EnrolledExam): void {
    this.selectedAdmitCard.set(exam);
  }

  closeAdmitCard(): void {
    this.selectedAdmitCard.set(null);
  }

  printAdmitCard(): void {
    window.print();
  }
}
