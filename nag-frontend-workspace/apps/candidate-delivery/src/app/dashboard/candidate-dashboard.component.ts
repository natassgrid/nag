import {
  Component,
  OnInit,
  inject,
  signal,
  computed,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '@nag-frontend-workspace/shared-data-access-auth';
import { EnrolledExam, DigitalAdmitCard } from './models';
import { CandidateDashboardService } from './services';
import {
  DashboardWelcomeBannerComponent,
  DashboardKpiStatsComponent,
  EnrolledAssessmentCardComponent,
  CandidateAdmitCardDialogComponent,
} from './components';

export * from './models';
export * from './services';
export * from './components';

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    DashboardWelcomeBannerComponent,
    DashboardKpiStatsComponent,
    EnrolledAssessmentCardComponent,
    CandidateAdmitCardDialogComponent,
  ],
  templateUrl: './candidate-dashboard.component.html',
  styleUrl: './candidate-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CandidateDashboardComponent implements OnInit {
  readonly authService = inject(AuthService);
  readonly dashboardService = inject(CandidateDashboardService);

  readonly activeFilter = signal<'ALL' | 'LIVE' | 'UPCOMING' | 'PRACTICE' | 'COMPLETED'>('ALL');
  readonly selectedAdmitCard = signal<DigitalAdmitCard | null>(null);

  readonly candidateName = computed(() => {
    return this.authService.currentUser()?.username || 'Aryan Sharma';
  });

  readonly filteredExams = computed(() => {
    const filter = this.activeFilter();
    const list = this.dashboardService.enrolledExams();
    if (filter === 'ALL') return list;
    if (filter === 'PRACTICE') return list.filter((e) => e.isPractice || e.practiceAvailable);
    if (filter === 'UPCOMING') return list.filter((e) => !e.isPractice && (e.status === 'UPCOMING' || e.status === 'SCHEDULED'));
    return list.filter((e) => e.status === filter);
  });

  ngOnInit(): void {
    this.dashboardService.loadEnrolledExams().subscribe();
  }

  setFilter(filter: 'ALL' | 'LIVE' | 'UPCOMING' | 'PRACTICE' | 'COMPLETED'): void {
    this.activeFilter.set(filter);
  }

  openAdmitCard(exam: EnrolledExam): void {
    this.dashboardService
      .getAdmitCard(exam, this.candidateName())
      .subscribe((card) => this.selectedAdmitCard.set(card));
  }

  closeAdmitCard(): void {
    this.selectedAdmitCard.set(null);
  }

  printAdmitCard(): void {
    window.print();
  }

  retryLoad(): void {
    this.dashboardService.loadEnrolledExams().subscribe();
  }
}
